"""
© Copyright IBM Corporation 2022. All Rights Reserved.
LICENSE: MIT License https://opensource.org/licenses/MIT
SPDX-License-Identifier: MIT
"""

import os
import subprocess
import sys
import unittest
from math import *
import shutil

OUTPUT_PATH_KEYMAERA = "../output/keymaera/keymaera_output/"
INPUT_PATH_KEYMAERA = "../output/keymaera/keymaera_files/"

# ------------------
# PARAMETERS
# ------------------
SENSITIVITY_ERROR = 0.1     # 0.1
MAGNITUDE_FACTOR = 5        # 5
ITERATIONS = 53             # max 53 which is machine precision
TIMEOUT = 1200              # 300=5min 600=10min 1200=20min
TOLERANCE = 10              # 10
PRECISION = 1e-4            # > 1e-6 which is machine precision
MEASURES = ['interval', 'dependencies', 'pointwiseL2', 'pointwiseLinf_efficient', 'derivation', 'weak_derivation']
# IMPORTANT: measure 'dependencies' depends on measure 'interval'
# IMPORTANT: measure 'pointwiseLinf_efficient' depends on measure 'pointwiseL2'
# IMPORTANT: the others are independent from each other
# MEASURES = ['interval', 'dependencies', 'pointwiseL2', 'pointwiseLinf', 'pointwiseLinf_efficient', 'derivation', 'weak_derivation']
# ------------------


class Formula:
    def __init__(self, keymaera_string, python_string):
        self.keymaera_string = keymaera_string
        self.python_string = python_string
        self.operators = {'sqrt': sqrt}
        self.mapping = {}
        self.existential_constants = []
        self.keymaera_string_constants = keymaera_string
        index = 1
        name_map = {}
        for i in self.keymaera_string.split():
            try:
                result = float(i)
                name_map[result] = i
                if result not in self.mapping:
                    self.mapping[result] = 'c' + str(index)
                    self.existential_constants.append('c' + str(index))
                    index += 1
            except:
                continue
        for i in self.mapping:
            self.keymaera_string_constants = self.keymaera_string_constants.replace(name_map[i], self.mapping[i])
        pass

    def compute_output(self, input_data_dict):
        return eval(self.python_string, self.operators, input_data_dict)

    def __str__(self):
        return str(self.keymaera_string)



def print_results(measure_vector, file_name):
    new_file = open(file_name, "w+")
    for formula in measure_vector:
        new_file.write('Formula: ' + formula.keymaera_string)
        new_file.write('\n')
        print('Formula:' + formula.keymaera_string)
        for measure in MEASURES:

            if measure == 'interval':
                results_interval = measure_vector[formula]['interval']  # num
                new_file.write('\t- Results interval: ' + str(results_interval[0]) + " " + results_interval[1])
                new_file.write('\n')
                print('\t- Results interval: ', results_interval)

            elif measure == 'dependencies':
                results_dependencies = measure_vector[formula]['dependencies']  # dict of bool
                string_dependency = '{ '
                for i in results_dependencies:
                    string_dependency += i + ": " + str(results_dependencies[i]) + " "
                string_dependency += '}'
                new_file.write('\t- Results dependencies: ' + str(string_dependency))
                new_file.write('\n')
                print('\t- Results dependencies: ', results_dependencies)

            elif measure == 'pointwiseL2':
                results_pointwiseL2 = measure_vector[formula]['pointwiseL2']  # num
                new_file.write('\t- Results pointwiseL2: ' + str(results_pointwiseL2[0]) + " " + str(results_pointwiseL2[1]))
                new_file.write('\n')
                print('\t- Results pointwiseL2: ', results_pointwiseL2)

            elif measure == 'pointwiseLinf_efficient':
                results_pointwiseLinf_efficient = measure_vector[formula]['pointwiseLinf_efficient']  # num
                new_file.write('\t- Results pointwiseLinf: ' + str(results_pointwiseLinf_efficient[0]) + " " + str(results_pointwiseLinf_efficient[1]))
                new_file.write('\n')
                print('\t- Results pointwiseLinf: ', results_pointwiseLinf_efficient)

            elif measure == 'pointwiseLinf':
                results_pointwiseLinf = measure_vector[formula]['pointwiseLinf']  # num
                new_file.write('\t- Results pointwiseLinf: ' + str(results_pointwiseLinf[0]) + " " + str(results_pointwiseLinf[1]))
                new_file.write('\n')
                print('\t- Results pointwiseLinf: ', results_pointwiseLinf)

            elif measure == 'derivation':
                results_derivation = measure_vector[formula]['derivation']  # bool
                new_file.write('\t- Results derivation: ' + str(results_derivation))
                new_file.write('\n')
                print('\t- Results derivation: ', results_derivation)

            elif measure == 'weak_derivation':
                results_week_derivation = measure_vector[formula]['weak_derivation']  # bool
                new_file.write('\t- Results weak derivation: ' + str(results_week_derivation))
                new_file.write('\n')
                print('\t- Results derivation with existential constants: ', results_week_derivation)

        new_file.write('\n')
    new_file.close()


def run_pipeline(variables, constants, data_points, axioms, interest_variable, input_formulas, precision, experiment_name):
    if not os.path.exists(OUTPUT_PATH_KEYMAERA):
        os.mkdir(OUTPUT_PATH_KEYMAERA)
    formulas_list = []
    for ff in input_formulas:
        formulas_list.append(Formula(ff[0], ff[1]))
    results_formulas = {}
    for formula in formulas_list:
        formula_index = str(formulas_list.index(formula))
        print('evaluating formula: ' + str(formulas_list.index(formula) + 1) + '/' + str(len(formulas_list)))
        results = {'interval': None, 'dependencies': None, 'pointwiseL2': None, 'pointwiseLinf_efficient': None, 'pointwiseLinf': None, 'derivation': None}
        for measure in MEASURES:
            print('\t - evaluating measure: ' + measure)
            if measure == 'interval':
                file_name = INPUT_PATH_KEYMAERA + experiment_name + '/formula' + formula_index + '__' + measure
                results['interval'] = binary_search(precision, constants, variables, interest_variable, axioms, formula, 'interval', file_name, data_points)
            elif measure == 'dependencies':
                relerr_dependencies = {}
                # substitute this for block with the 2 for blocks below for computing the exact value of the dependency errors
                for var_data in data_points[0]:
                    if var_data != interest_variable:
                        file_name = INPUT_PATH_KEYMAERA + experiment_name + '/formula' + formula_index + '__' + measure + '_' + var_data
                        value2test = SENSITIVITY_ERROR + results['interval'][0]
                        convert_problem_keymaera(constants, variables, interest_variable, axioms, formula, 'dependencies', file_name + '.kyx', data=data_points, var=var_data, relerr=value2test)
                        result = call_keymaera(file_name)
                        if result == "proved":
                            relerr_dependencies[var_data] = False
                        else:
                            relerr_dependencies[var_data] = True
                '''                
                for var_data in data_points[0]:
                    file_name = INPUT_PATH_KEYMAERA + experiment_name + '/formula' + formula_index + '__' + measure + '_' + var_data
                    if var_data != interest_variable:
                        print('\t\t > var: ' + var_data)
                        relerr_dependencies[var_data] = binary_search(precision, constants, variables, interest_variable, axioms, formula, 'dependencies', file_name, data_points, var_data)
                
                for var_data in data_points[0]:
                    if var_data != interest_variable:
                        if abs(relerr_dependencies[var_data][0] - results['interval'][0]) > SENSITIVITY_ERROR:
                            relerr_dependencies[var_data] = True
                        else:
                            relerr_dependencies[var_data] = False
                '''
                results['dependencies'] = relerr_dependencies
            elif measure == 'pointwiseL2':
                relerr_pointwiseL2_list = []
                for data_point in data_points:
                    print('\t\t > point: ' + str(data_points.index(data_point)+1) + '/' + str(len(data_points)))
                    file_name = INPUT_PATH_KEYMAERA + experiment_name + '/formula' + formula_index + '__' + measure + '_' + str(data_points.index(data_point))
                    relerr_pointwiseL2_list.append(binary_search(precision, constants, variables, interest_variable, axioms, formula, 'pointwiseL2', file_name, data_points, data_points.index(data_point)))
                precision_string_l2 = ''
                for i in relerr_pointwiseL2_list:
                    precision_string_l2 += i[1]
                precision_string_l2 = precision_string_l2.replace("precision:", ",")
                precision_string_l2 = "precision:" + precision_string_l2[1:] + ' .'
                results['pointwiseL2'] = [sqrt(sum(i[0]*i[0] for i in relerr_pointwiseL2_list)), precision_string_l2]
            elif measure == 'pointwiseLinf_efficient':
                results['pointwiseLinf_efficient'] = [max(i[0] for i in relerr_pointwiseL2_list), precision_string_l2]
            elif measure == 'pointwiseLinf':
                file_name = INPUT_PATH_KEYMAERA + experiment_name + '/formula' + formula_index + '__' + measure
                results['pointwiseLinf'] = binary_search(precision, constants, variables, interest_variable, axioms, formula, 'pointwiseLinf', file_name, data_points)
            elif measure == 'derivation':
                file_name = INPUT_PATH_KEYMAERA + experiment_name + '/formula' + formula_index + '__' + measure
                convert_problem_keymaera(constants, variables, interest_variable, axioms, formula, 'derivation', file_name + '.kyx' )
                result_derivation = call_keymaera(file_name)
                results['derivation'] = result_derivation
            elif measure == 'weak_derivation':
                file_name = INPUT_PATH_KEYMAERA + experiment_name + '/formula' + formula_index + '__' + measure + "_constants"
                convert_problem_keymaera(constants, variables, interest_variable, axioms, formula, 'derivation_constants', file_name + '.kyx')
                result_derivation = call_keymaera(file_name)
                results['weak_derivation'] = result_derivation
        results_formulas[formula] = results
        print_results({formula: results_formulas[formula]}, OUTPUT_PATH_KEYMAERA + "results_" + experiment_name + "_" + formula_index + ".txt")
        print('>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>')
        print('>>>  Results for Formula ' + str(formula_index) + ' can be found at: ')
        print('>>>  ' + OUTPUT_PATH_KEYMAERA + "results_" + experiment_name + "_" + formula_index + ".txt")
        print('>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>')
    print_results(results_formulas, OUTPUT_PATH_KEYMAERA + "results_" + experiment_name + ".txt")
    print('>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>')
    print('>>>  Summary results can be found at: ')
    print('>>>  ' + OUTPUT_PATH_KEYMAERA + "results_" + experiment_name + ".txt")
    print('>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>')
    return results_formulas


def binary_search(precision, constants, variables, interest_variable, axioms, formula, measure, file_name, data_points, var=None):
    lowerbound = 0.
    upperbound = 1.
    # find proper upperbound for the relative error
    convert_problem_keymaera(constants, variables, interest_variable, axioms, formula, measure,  file_name + '.kyx', data=data_points, var=var, relerr=upperbound)
    result = call_keymaera(file_name)
    while result == "not_proved":
        lowerbound = upperbound
        upperbound = 10*upperbound
        convert_problem_keymaera(constants, variables, interest_variable, axioms, formula, measure, file_name + '.kyx', data=data_points, var=var, relerr=upperbound)
        result = call_keymaera(file_name)
    # start binary search
    while (upperbound - lowerbound) > precision:
        print('\t\t * precision: ' + str(upperbound - lowerbound))
        test_value = (lowerbound + upperbound)/2
        convert_problem_keymaera(constants, variables, interest_variable, axioms, formula, measure, file_name + '.kyx', data=data_points, var=var, relerr=test_value)
        result = call_keymaera(file_name)
        if result == 'proved':
            upperbound = test_value
        elif result == 'not_proved':
            lowerbound = test_value
        else:
            break
    status = "ok"
    if result not in ['proved', 'not_proved']:
        status = result
    print("exit binary search with status: ", status)
    binary_search_precision = upperbound - lowerbound
    return [upperbound, "precision: "+str(binary_search_precision)]


def call_keymaera(file_name, tool='mathematica'):
    '''
    :param tool: 'mathematica' or 'z3'
    :return: 'proved', 'not_proved', 'timeout_error', 'keymaera_error', 'mathematica_error'
    '''
    assert tool in ['mathematica', 'z3']
    # keymaerax_instance = kmx.KeYmaeraXTool()
    # result = keymaerax_instance.check(formula, tactic)
    formula_file = file_name + '.kyx'
    formula_file_out = file_name + '.kyp'
    # it is possible to specify the location of the Mathematica Kernel, e.g.
    # -mathkernel /Applications/Mathematica.app/Contents/MacOS/MathKernel
    command = f"java -jar keymaerax/keymaerax.jar -tool {tool} -timeout {TIMEOUT} -prove {formula_file} -out {formula_file_out}".split(" ")
    '''
    DELETE THIS
    if tool == 'mathematica':
        #command = f"java -jar keymaerax/keymaerax.jar -tool {tool} -timeout {TIMEOUT} -prove {formula_file} -out {formula_file_out} -mathkernel /Applications/Mathematica.app/Contents/MacOS/MathKernel".split(" ")
        command = f"java -jar keymaerax/keymaerax.jar -tool {tool} -timeout {TIMEOUT} -prove {formula_file} -out {formula_file_out}".split(" ")
    else:
        command = f"java -jar keymaerax/keymaerax.jar -tool {tool} -timeout {TIMEOUT} -prove {formula_file} -out {formula_file_out}".split(" ")
    '''
    num_bad_runs = -1
    out = ""
    while (out not in ['proved', 'not_proved', 'timeout_error']) and num_bad_runs < TOLERANCE:
        num_bad_runs += 1
        keymaera_call = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        keymaera_output, _ = keymaera_call.communicate()
        keymaera_output = keymaera_output.decode("utf-8")
        out_file = file_name + "_out.txt"
        # save the output
        new_file = open(out_file, "w+")
        new_file.write(keymaera_output)
        new_file.close()
        if 'PROVED' in keymaera_output:
            out = 'proved'
            return out
        elif 'expected to have proved, but got false' in keymaera_output:
            out = 'not_proved'
            return out
        elif 'expected to have proved, but got open goals' in keymaera_output:
            out = 'not_proved'
            return out
        elif 'TIMEOUT' in keymaera_output:
            out = 'timeout_error'
            return out
        else:
            if tool == 'mathematica' and 'Mathematica terminated' in keymaera_output:
                out = 'mathematica_error'
                print(f'---------->>> ERROR ({out}) for file {str(file_name)}')
            else:
                out = 'keymaera_error'
                print(f'---------->>> ERROR ({out}) for file {str(file_name)}')
    return out


def convert_problem_keymaera(constants, variables, interest_variable, axioms, formula, measure, file_name, data=None, var=None, relerr=None):
    '''
    :param constants: constants list
    :param variables: variables list
    :param axioms: background theory
    :param formula: formula induced from numerical data
    :param interestvariable: variable of interest (of which we want to find the formula)
    :param measure: interval(data) dependencies(data,var) pointwiseL2(data,var) pointwiseLinf(data) derivation
    :param data: data points dictionaries (interval,dependencies,pointwiseL2)
    :param var: variable on which perform the optimization (dependencies) or data point to consider (pointwiseL2)
    :return: return the file name and path where the keymaera instance was created
    '''
    # Constants
    formula_keymaera = ['Definitions']
    for i in constants:
        formula_keymaera.append('  Real ' + i + ';')
    if measure == 'interval' or measure == 'dependencies' or measure == 'pointwiseL2' or measure == 'pointwiseLinf':
        formula_keymaera.append('  Real relerr;')
    formula_keymaera.append('End.')
    # Functions
    formula_keymaera.append('Functions')
    formula_keymaera.append('End.')
    # Variables
    formula_keymaera.append('ProgramVariables')
    for i in variables:
        if not i == interest_variable:
            formula_keymaera.append('  Real ' + i + ';')
    formula_keymaera.append('  Real ' + interest_variable + ';')
    formula_keymaera.append('End.')
    formula_keymaera.append('Problem')
    if measure == "derivation_constants":
        for i in formula.existential_constants:
            formula_keymaera.append(" \exists " + i)
        for variable_c in variables:
            formula_keymaera.append(" \\forall " + variable_c)
        formula_keymaera.append(" \\forall " + interest_variable)
        formula_keymaera.append(" (")
    formula_keymaera.append('\t(')
    formula_keymaera.append('\t\t(')
    # Axioms
    for i in axioms:
        if axioms.index(i) == 0:
            formula_keymaera.append('\t\t  ' + i + '')
        else:
            formula_keymaera.append('\t\t  & ' + i + '')
    if measure == 'interval' or measure == 'dependencies':
        relevant_vars = list(data[0].keys())
        relevant_vars.remove(interest_variable)
        intervals = find_intervals_data(relevant_vars, data)
        if measure == 'dependencies':
            intervals[var] = change_order_magnitude(intervals[var])
        for i in intervals:
            if i != interest_variable:
                value1 = str(intervals[i][0]).replace('e', '*10^')
                value2 = str(intervals[i][1])
                formula_keymaera.append('\t\t  & ' + i + '>=' + value1 + ' & ' + i + '<=' + value2 + '')
    if measure == 'pointwiseL2':
        stringa = '\t\t '
        for i in data[var]:
            if i != interest_variable:
                stringa += ' & ' + i + '=' + str(data[var][i])
        formula_keymaera.append(stringa)
    if measure == 'interval' or measure == 'dependencies' or measure == 'pointwiseL2' or measure == 'pointwiseLinf':
        rel_err_10 = str(relerr).replace('e', '*10^')
        formula_keymaera.append('\t\t  & relerr = ' + rel_err_10)
    # End Axioms
    formula_keymaera.append('\t\t)')
    formula_keymaera.append('\t->')
    formula_keymaera.append('\t\t(')
    if measure == 'interval':
        formula_keymaera.append('\t\t abs( ( ' + formula.keymaera_string + ' ) - ' + interest_variable + ' ) / ' + interest_variable + ' < relerr')
    elif measure == 'dependencies':
        formula_keymaera.append('\t\t abs( ( ' + formula.keymaera_string + ' ) - ' + interest_variable + ' ) / ' + interest_variable + ' < relerr')
    elif measure == 'pointwiseL2':
        formula_keymaera.append('\t\t ( abs( ( ' + formula.keymaera_string + ' ) - ' + interest_variable + ' ) / ' + interest_variable + ') < relerr')
    elif measure == 'pointwiseLinf':
        not_first_line = False
        for datapoint in data:
            stringa = '\t\t '
            if not_first_line:
                stringa += ' & '
            else:
                stringa += '   '
                not_first_line = True
            stringa += ' ( ('
            not_first_variable = False
            for i in datapoint:
                if i != interest_variable:
                    if not_first_variable:
                        stringa += ' & '
                    else:
                        not_first_variable = True
                    stringa += i + '=' + str(datapoint[i])
            stringa += ' ) -> ('
            stringa += ' abs( ( ' + formula.keymaera_string + ' ) - ' + interest_variable + ' ) / ' + interest_variable + ' < relerr'
            stringa += ' ) ) '
            formula_keymaera.append(stringa)
    elif measure == 'derivation':
        formula_keymaera.append('\t\t\t' + interest_variable + ' = ' + formula.keymaera_string)
    elif measure == "derivation_constants":
        for i in formula.existential_constants:
            formula_keymaera.append('\t\t\t' + i + ' > 0 & ')
        formula_keymaera.append('\t\t\t' + interest_variable + ' = ' + formula.keymaera_string_constants)
    else:
        raise Exception
    formula_keymaera.append('\t\t)')
    formula_keymaera.append('\t)')
    if measure == "derivation_constants":
        formula_keymaera.append(')')
    formula_keymaera.append('End.')
    # save on file
    file = open(file_name, "w")
    for line in formula_keymaera:
        file.write(line)
        file.write("\n")
    file.close()


def change_order_magnitude(interval):
    min = interval[0]
    max = interval[1]
    new_interval = [min * (10**(-MAGNITUDE_FACTOR)), max * (10**(MAGNITUDE_FACTOR))]
    return new_interval


def find_intervals_data(variables, datapoints):
    intervals = {}
    for var in variables:
        intervals[var] = [float('inf'), float('-inf')]
    for data in datapoints:
        for var in variables:
            if data[var] < intervals[var][0]:
                intervals[var][0] = data[var]
            if data[var] > intervals[var][1]:
                intervals[var][1] = data[var]
    return intervals


def run_feynman():
    file_name = INPUT_PATH_KEYMAERA + "feynman/Feynman_I_27_6"
    result_1 = call_keymaera(file_name)
    file_name = INPUT_PATH_KEYMAERA + "feynman/Feynman_I_16_6"
    result_2 = call_keymaera(file_name)
    file_name = INPUT_PATH_KEYMAERA + "feynman/Feynman_I_15_10"
    result_3 = call_keymaera(file_name)
    print("Result for Feynman_I_27_6: " + str(result_1))
    print("Result for Feynman_I_16_6: " + str(result_2))
    print("Result for Feynman_I_15_10: " + str(result_3))


def run_langmuir():
    file_name_list = [INPUT_PATH_KEYMAERA + 'langmuir/langmuir', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_1', INPUT_PATH_KEYMAERA+'langmuir/langmuir_const_2',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_3', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_4',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_5', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_6',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_7', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_8',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_9', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_10',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_11', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_12',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_13', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_const_14',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_1', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_2',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_3', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_4',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_5', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_6',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_7', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_8',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_9', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_10',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_11', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_12',
                      INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_13', INPUT_PATH_KEYMAERA + 'langmuir/langmuir_2sites_const_14']
    for file_name in file_name_list:
        result = call_keymaera(file_name)
        print("Result for "+file_name+": " + str(result))


def run_kepler():
    file_name = INPUT_PATH_KEYMAERA + "kepler/Kepler"
    result_1 = call_keymaera(file_name)
    print("Result for Kepler: " + str(result_1))
    file_name = INPUT_PATH_KEYMAERA + "kepler/Kepler_const"
    result_2 = call_keymaera(file_name)
    print("Result for Kepler with constants: " + str(result_2))


def run_kepler_solar():
    new_dir = f'{INPUT_PATH_KEYMAERA}/KeplerExperimentsSolar'
    if os.path.exists(new_dir):
        shutil.rmtree(new_dir)
    os.mkdir(new_dir)
    variables = ['m1', 'm2', 'd', 'm1N', 'm2N', 'dN', 'PN', 'P', 'd1', 'd2', 'w', 'Fg', 'Fc']
    constants = ['G', 'pi', 'convP', 'convm2', 'convm1', 'convD']
    interest_variable = 'PN'
    data_points = [{'PN': 0.0880, 'm1N': 1, 'm2N': 0.0553, 'dN': 0.3870},
                   {'PN': 0.2247, 'm1N': 1, 'm2N': 0.815, 'dN': 0.7233},
                   {'PN': 0.3652, 'm1N': 1, 'm2N': 1, 'dN': 1},
                   {'PN': 0.6870, 'm1N': 1, 'm2N': 0.107, 'dN': 1.5234},
                   {'PN': 4.331, 'm1N': 1, 'm2N': 317.83, 'dN': 5.2045},
                   {'PN': 10.747, 'm1N': 1, 'm2N': 95.16, 'dN': 9.5822},
                   {'PN': 30.589, 'm1N': 1, 'm2N': 14.54, 'dN': 19.2012},
                   {'PN': 59.800, 'm1N': 1, 'm2N': 17.15, 'dN': 30.0475}]
    axioms = ['m1>0 & m2>0 & P>0 & d2>0 & d1>0 & m1N>0 & m2N>0 & PN>0',
              'G = (6.674 * 10^(-11)) & pi = (3.14)',
              '( m1 * d1 = m2 * d2 ) ',
              '( d = d1 + d2 )  ',
              '( Fg = (G * m1 * m2) / d^2 ) ',
              '( Fc =  m2 * d2 * w^2 )',
              '( Fg =  Fc )',
              '( P = (2 * pi )/w )',
              'convP = (1000 * 24 * 60 * 60 )',
              'convm1 = (1.9885 * 10^30  )',
              'convm2 = (5.972 * 10^24)',
              'convD = (1.496 * 10^11)',
              'm1N = m1 / convm1',
              'm2N = m2 / convm2',
              'dN = d / convD',
              'PN = P / convP']
    # IMPORTANT: add spaces after each term
    #   (in particular for constant terms like (X+0.2) should be written as ( X + 0.2 )
    #   for integer constants add .0 , for example (x+1) should be written as ( x + 1.0 )
    #   formulas is a list of lists. the innermost list are a pair of [keymeara_formula, python_formula]
    #   keymeara_formula and python_formula represent the same formula in keymaera and python format respectively
    #   pay attention to:
    #       - power operator: keymaera ^ ;  python **
    #       - square root operator: keymaera (expr)^(1/2) ;  python sqrt(expr)
    formulas = [['( 0.1319 * dN^3 )^(1/2)', 'sqrt( 0.1319 * dN**3 )'],
                ['( 0.1316 * ( dN^3 + dN ) )^(1/2)', 'sqrt( 0.1316 * ( dN**3 + dN ) )'],
                ['(( 0.03765 * dN^3 ) + dN^2 )/( 2.0 + dN )', '(( 0.03765 * dN**3 ) + dN**2 )/( 2.0 + dN )']]
    run_pipeline(variables, constants, data_points, axioms, interest_variable, formulas, PRECISION, "KeplerExperimentsSolar")


def run_kepler_exoplanets():
    new_dir = f'{INPUT_PATH_KEYMAERA}/KeplerExperimentsExoPlanets'
    if os.path.exists(new_dir):
        shutil.rmtree(new_dir)
    os.mkdir(new_dir)
    variables = ['m1', 'm2', 'd', 'm1N', 'm2N', 'dN', 'PN', 'P', 'd1', 'd2', 'w', 'Fg', 'Fc']
    constants = ['G', 'pi', 'convP', 'convm2', 'convm1', 'convD']
    interest_variable = 'PN'
    data_points = [{'PN': 0.0880, 'm1N': 1, 'm2N': 0.000174, 'dN': 0.3870},
                   {'PN': 0.2247, 'm1N': 1, 'm2N': 0.00256, 'dN': 0.7233},
                   {'PN': 0.3652, 'm1N': 1, 'm2N': 0.00315, 'dN': 1},
                   {'PN': 0.6870, 'm1N': 1, 'm2N': 0.000338, 'dN': 1.5234},
                   {'PN': 4.331, 'm1N': 1, 'm2N': 1, 'dN': 5.2045},
                   {'PN': 10.747, 'm1N': 1, 'm2N': 0.299, 'dN': 9.5822},
                   {'PN': 30.589, 'm1N': 1, 'm2N': 0.0457, 'dN': 19.2012},
                   {'PN': 59.800, 'm1N': 1, 'm2N': 0.0540, 'dN': 30.0475},
                   {'PN': 0.0072004, 'm1N': 0.33, 'm2N': 0.018, 'dN': 0.0505},
                   {'PN': 0.02814, 'm1N': 0.33, 'm2N': 0.012, 'dN': 0.125},
                   {'PN': 0.06224, 'm1N': 0.33, 'm2N': 0.008, 'dN': 0.213},
                   {'PN': 0.039026, 'm1N': 0.33, 'm2N': 0.008, 'dN': 0.156},
                   {'PN': 0.2562, 'm1N': 0.33, 'm2N': 0.014, 'dN': 0.549},
                   {'PN': 0.0015109, 'm1N': 0.08, 'm2N': 0.0027, 'dN': 0.0111},
                   {'PN': 0.0024218, 'm1N': 0.08, 'm2N': 0.0043, 'dN': 0.0152},
                   {'PN': 0.0040496, 'm1N': 0.08, 'm2N': 0.0013, 'dN': 0.0214},
                   {'PN': 0.0060996, 'm1N': 0.08, 'm2N': 0.002, 'dN': 0.0282},
                   {'PN': 0.0092067, 'm1N': 0.08, 'm2N': 0.0021, 'dN': 0.0371},
                   {'PN': 0.0123529, 'm1N': 0.08, 'm2N': 0.0042, 'dN': 0.0451},
                   {'PN': 0.018767, 'm1N': 0.08, 'm2N': 0.086, 'dN': 0.063}]
    axioms = ['m1>0 & m2>0 & P>0 & d2>0 & d1>0 & m1N>0 & m2N>0 & PN>0',
              'G = (6.674 * 10^(-11)) & pi = (3.14)',
              '( m1 * d1 = m2 * d2 ) ',
              '( d = d1 + d2 )  ',
              '( Fg = (G * m1 * m2) / d^2 ) ',
              '( Fc =  m2 * d2 * w^2 )',
              '( Fg =  Fc )',
              '( P = (2 * pi )/w )',
              'convP = (1000 * 24 * 60 * 60 )',
              'convm1 = (1.9885 * 10^30  )',
              'convm2 = (1.898 * 10^27)',
              'convD = (1.496 * 10^11)',
              'm1N = m1 / convm1',
              'm2N = m2 / convm2',
              'dN = d / convD',
              'PN = P / convP']
    # IMPORTANT: add spaces after each term
    #   (in particular for constant terms like (X+0.2) should be written as ( X + 0.2 )
    #   for integer constants add .0 , for example (x+1) should be written as ( x + 1.0 )
    #   formulas is a list of lists. the innermost list are a pair of [keymeara_formula, python_formula]
    #   keymeara_formula and python_formula represent the same formula in keymaera and python format respectively
    #   pay attention to:
    #       - power operator: keymaera ^ ;  python **
    #       - square root operator: keymaera (expr)^(1/2) ;  python sqrt(expr)
    formulas = [['(( 0.1319 * (dN^3)) / m1N )^(1/2)', 'sqrt(( 0.1319 * (dN**3)) / m1N)'],
                ['( ( ( m1N^2 * m2N^3 )/( dN ) ) + ( 0.1319 * ( dN^3 / m1N )))^(1/2)', 'sqrt(( ( m1N**2 * m2N**3 )/( dN ) ) + ( 0.1319 * ( dN**3 / m1N )))'],
                ['(( ( 1.0 - ( 0.7362 * m1N )) * ( dN^3 ))/ 2.0 )^(1/2)', 'sqrt(( ( 1.0 - ( 0.7362 * m1N )) * ( dN**3 ) )/ 2.0)']]
    run_pipeline(variables, constants, data_points, axioms, interest_variable, formulas, PRECISION, "KeplerExperimentsExoPlanets")


def run_kepler_binarystars():
    new_dir = f'{INPUT_PATH_KEYMAERA}/KeplerExperimentsBinaryStars'
    if os.path.exists(new_dir):
        shutil.rmtree(new_dir)
    os.mkdir(new_dir)
    variables = ['m1', 'm2', 'd', 'm1N', 'm2N', 'dN', 'PN', 'P', 'd1', 'd2', 'w', 'Fg', 'Fc']
    constants = ['G', 'pi', 'convP', 'convm2', 'convm1', 'convD']
    interest_variable = 'PN'
    data_points = [{'PN': 1089, 'm1N': 0.54, 'm2N': 0.5, 'dN': 107.27},
                   {'PN': 143.1, 'm1N': 1.33, 'm2N': 1.41, 'dN': 38.235},
                   {'PN': 930, 'm1N': 0.88, 'm2N': 0.82, 'dN': 113.769},
                   {'PN': 675.5, 'm1N': 3.06, 'm2N': 1.97, 'dN': 131.352}]
    axioms = ['m1>0 & m2>0 & P>0 & d2>0 & d1>0 & m1N>0 & m2N>0 & PN>0',
              'G = (6.674 * 10^(-11)) & pi = (3.14)',
              '( m1 * d1 = m2 * d2 ) ',
              '( d = d1 + d2 )  ',
              '( Fg = (G * m1 * m2) / d^2 ) ',
              '( Fc =  m2 * d2 * w^2 )',
              '( Fg =  Fc )',
              '( P = (2 * pi )/w )',
              'convP = (365 * 24 * 60 * 60  )',
              'convm1 = ( 1.9885 * 10^30 )',
              'convm2 = ( 1.9885 * 10^30 )',
              'convD = (1.496 * 10^11  )',
              'm1N = m1 / convm1',
              'm2N = m2 / convm2',
              'dN = d / convD',
              'PN = P / convP']
    # IMPORTANT: add spaces after each term
    #   (in particular for constant terms like (X+0.2) should be written as ( X + 0.2 )
    #   for integer constants add .0 , for example (x+1) should be written as ( x + 1.0 )
    #   formulas is a list of lists. the innermost list are a pair of [keymeara_formula, python_formula]
    #   keymeara_formula and python_formula represent the same formula in keymaera and python format respectively
    #   pay attention to:
    #       - power operator: keymaera ^ ;  python **
    #       - square root operator: keymaera (expr)^(1/2) ;  python sqrt(expr)
    #formulas = [['( 1/(dN^2 * m1N^2 )) + ( 1 / (dN * m2N^2) ) - m1N^3 * m2N^2 + (( (0.4787 * dN^3)/m2N) + ( dN^2 * m2N^2 ))^(1/2)',
    #             '( 1/(dN**2 * m1N**2 )) + ( 1 / (dN * m2N**2) ) - m1N**3 * m2N**2 + sqrt(( (0.4787 * dN**3)/m2N) + ( dN**2 * m2N**2 ))'],
    #            ['( (dN^3)^(1/2) + ( ( m1N^3 * m2N ) / dN^(1/2)) )/(( m1N + m2N )^(1/2)) ',
    #             '(sqrt( dN**3 ) + ( ( m1N**3 * m2N )/(sqrt(dN)) ) )/(sqrt( m1N + m2N ))'],
    #            ['(( dN^3*m1N + 1/(m1N^3 * m2N^3))^(1/2))/(m1N + 0.4201 * m2N)',
    #             '(sqrt( dN**3 * m1N + 1/(m1N**3 * m2N**3)))/(m1N + 0.4201 * m2N)']]
    formulas = [['( dN^(-2) * m1N^(-2) ) + ( dN^(-2) * m2N^(-2) ) - ( m1N^3 * m2N^2 ) + (( 0.4787 * dN^3 * m2N^(-1) ) + ( dN^2 * m2N^2 ))^(1/2)',
                   '( dN^(-2) * m1N**(-2) ) + ( dN**(-2) * m2N**(-2) ) - ( m1N**3 * m2N**2 ) + sqrt(( 0.4787 * dN**3 * m2N**(-1) ) + ( dN**2 * m2N**2 ))'],
                ['( dN^(3/2) + ( ( m1N^3 * m2N ) / ( ( dN )^(1/2))) )/(( m1N + m2N )^(1/2)) ',
                   '( sqrt( dN**3 ) + ( ( m1N**3 * m2N )/(sqrt( dN )) ) )/(sqrt( m1N + m2N ))'],
                ['( dN^3 / (( 0.9967 * m1N ) + m2N ))^(1/2)',
                   'sqrt( dN**3 /( ( 0.9967 * m1N ) + m2N))']]
    run_pipeline(variables, constants, data_points, axioms, interest_variable, formulas, PRECISION, "KeplerExperimentsBinaryStars")


def counterexample_generator(constants, variables, interestvariable, axioms, file_name_input, data, precision, lowerbound, upperbound):
    new_dir = f'{INPUT_PATH_KEYMAERA}/KeplerExperimentsSolarCounterexample'
    if os.path.exists(new_dir):
        shutil.rmtree(new_dir)
    os.mkdir(new_dir)
    counterexample = data.copy()
    while upperbound - lowerbound > precision:
        print('[' + str(lowerbound) + ',' + str(upperbound) + ']')
        test_value = (lowerbound + upperbound) / 2
        file_name = INPUT_PATH_KEYMAERA + file_name_input + '/counterexample.kyx'
        # CONVERT INPUT
        '''
        :param constants: constants list
        :param variables: variables list
        :param axioms: background theory
        :param formula: formula induced from numerical data
        :param measure: interval(data) dependencies(data,var) pointwiseL2(data,var) pointwiseLinf(data) derivation
        :param data: data points dictionary for counterexample variables
        :param interestvariable: variable of interest on which perform search the value
        :return: return the file name and path where the keymaera instance was created
        '''
        formula_keymaera = []
        # Constants
        formula_keymaera.append('Definitions')
        for i in constants:
            formula_keymaera.append('  Real ' + i + ';')
        formula_keymaera.append('  Real err;')
        formula_keymaera.append('End.')
        # Functions
        formula_keymaera.append('Functions')
        formula_keymaera.append('End.')
        # Variables
        formula_keymaera.append('ProgramVariables')
        for i in variables:
            formula_keymaera.append('  Real ' + i + ';')
        formula_keymaera.append('End.')
        formula_keymaera.append('Problem')
        formula_keymaera.append('\t(')
        formula_keymaera.append('\t\t(')
        # Axioms
        for i in axioms:
            if axioms.index(i) == 0:
                formula_keymaera.append('\t\t  ' + i + '')
            else:
                formula_keymaera.append('\t\t  & ' + i + '')
        stringa = '\t\t '
        for i in data.keys():
            if i != interestvariable:
                stringa += ' & ' + i + '=' + str(data[i])
        formula_keymaera.append(stringa)
        rel_err_10 = str(test_value).replace('e', '*10^')
        formula_keymaera.append('\t\t  & err = ' + rel_err_10)
        # End Axioms
        formula_keymaera.append('\t\t)')
        formula_keymaera.append('\t->')
        formula_keymaera.append('\t\t(')
        formula_keymaera.append('\t\t ' + interestvariable + ' < err')
        formula_keymaera.append('\t\t)')
        formula_keymaera.append('\t)')
        formula_keymaera.append('End.')
        # save on file
        file = open(file_name, "w")
        for line in formula_keymaera:
            file.write(line)
            file.write("\n")
        file.close()
        # END COVERT INPUT
        result = call_keymaera(INPUT_PATH_KEYMAERA + file_name_input + '/counterexample')
        iterations_no_change = 0
        while result not in ['proved', 'not_proved', 'timeout_error'] and iterations_no_change < TOLERANCE:
            result = call_keymaera(INPUT_PATH_KEYMAERA + file_name_input + '/counterexample')
            if result in ['keymaera_error', 'mathematica_error']:
                print('ERROR :' + result)
                print('File :' + str(file_name))
            iterations_no_change += 1
        if result == 'proved':
            upperbound = test_value
        elif result in ['not_proved']:
            lowerbound = test_value
    counterexample[interestvariable] = upperbound
    return counterexample


def run_kepler_solar_counterexample():
    # counterexample Keymaera tactic = cex
    variables = ['m1', 'm2', 'd', 'm1N', 'm2N', 'dN', 'PN', 'P', 'd1', 'd2', 'w', 'Fg', 'Fc']
    constants = ['G', 'pi', 'convP', 'convm2', 'convm1', 'convD']
    interest_variable = 'PN'
    data_points = {'m1N': 1, 'm2N': 0.055, 'dN': 0.3871}  # 'PN': 0.0879691
    axioms = ['m1>0 & m2>0 & P>0 & d2>0 & d1>0 & m1N>0 & m2N>0 & PN>0',
              'G = (6.674 * 10^(-11)) & pi = (3.14)',
              '( m1 * d1 = m2 * d2 ) ',
              '( d = d1 + d2 )  ',
              '( Fg = (G * m1 * m2) / d^2 ) ',
              '( Fc =  m2 * d2 * w^2 )',
              '( Fg =  Fc )',
              '( P = (2 * pi )/w )',
              'convP = (1000 * 24 * 60 * 60 )',
              'convm1 = (1.9885 * 10^30  )',
              'convm2 = (5.972 * 10^24)',
              'convD = (1.496 * 10^11)',
              'm1N = m1 / convm1',
              'm2N = m2 / convm2',
              'dN = d / convD',
              'PN = P / convP']
    lowerbound = 0
    upperbound = 1
    counterexample = counterexample_generator(constants, variables, interest_variable, axioms, "KeplerExperimentsSolarCounterexample", data_points, PRECISION, lowerbound, upperbound)
    print(counterexample)


def run_time_dilation():
    file_name_list = [INPUT_PATH_KEYMAERA + 'time_dilation/time_dilation',
                      INPUT_PATH_KEYMAERA + 'time_dilation/time_dilation_f1_relativistic_A',
                      INPUT_PATH_KEYMAERA + 'time_dilation/time_dilation_f2_relativistic_A',
                      INPUT_PATH_KEYMAERA + 'time_dilation/time_dilation_f3_relativistic_A',
                      INPUT_PATH_KEYMAERA + 'time_dilation/time_dilation_f4_relativistic_A',
                      INPUT_PATH_KEYMAERA + 'time_dilation/time_dilation_f1_relativistic_R',
                      INPUT_PATH_KEYMAERA + 'time_dilation/time_dilation_f2_relativistic_R',
                      INPUT_PATH_KEYMAERA + 'time_dilation/time_dilation_f3_relativistic_R',
                      INPUT_PATH_KEYMAERA + 'time_dilation/time_dilation_f4_relativistic_R']
    for file_name in file_name_list:
        result = call_keymaera(file_name)
        print("Result for " + file_name + ": " + str(result))


class Test(unittest.TestCase):

    def test_keymaera_z3(self):
        formula_name = '../test_data/test_keymaera/test'
        result = call_keymaera(formula_name, tool='z3')
        self.assertEqual(result, 'proved')

    def test_keymaera_mathematica(self):
        formula_name = '../test_data/test_keymaera/test_neg'
        result = call_keymaera(formula_name, tool='mathematica')
        self.assertEqual(result, 'not_proved')

    def test_eval_formula(self):
        formula_keymaera = 'x * 2 + y^3'
        formula_python = 'x*2 + y**3'
        input_dict1 = {'x': 0, 'y': 1}
        input_dict2 = {'x': 2, 'y': 2}
        formula = Formula(formula_keymaera, formula_python)
        answ1 = formula.compute_output(input_dict1)
        answ2 = formula.compute_output(input_dict2)
        self.assertEqual(answ1, 1)
        self.assertEqual(answ2, 12)
        formula_keymaera = 'x * 2 + y^(1/2)'
        formula_python = 'x*2 + sqrt(y)'
        input_dict3 = {'x': 2, 'y': 4}
        formula = Formula(formula_keymaera, formula_python)
        answ3 = formula.compute_output(input_dict3)
        self.assertEqual(answ3, 6)


if __name__ == '__main__':
    # TESTS for Keymaera and the connection with Mathematica
    # if not connected with Mathematica, only 2 will succeed
    unittest.main()

    # EXPERIMENTS on the 3 problems + some FSRD

    # run_kepler()
    # run_kepler_solar()
    # run_kepler_exoplanets()
    # run_kepler_binarystars()
    # run_kepler_solar_counterexample()

    # run_langmuir()

    # run_time_dilation()

    # run_feynman()

