"""
© Copyright IBM Corporation 2022. All Rights Reserved.
LICENSE: MIT License https://opensource.org/licenses/MIT
SPDX-License-Identifier: MIT
"""

import os
import subprocess
import sys
import shutil
import unittest
from math import *
from pipeline_keymaera_3problems import Formula, convert_problem_keymaera


OUTPUT_PATH = "../output/"

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


def call_keymaera(path, file_name, tool='mathematica'):
    '''
    :param tool: 'mathematica' or 'z3'
    :return: 'proved', 'not_proved', 'timeout_error', 'keymaera_error', 'mathematica_error'
    '''
    formula_file = path + file_name + '.kyx'
    formula_file_out = path + file_name + '_out.txt'
    command = f"java -jar keymaerax/keymaerax.jar -tool {tool} -timeout {TIMEOUT} -prove {formula_file} -out {formula_file_out}".split(" ")
    num_bad_runs = -1
    out = ""
    while (out not in ['proved', 'not_proved', 'timeout_error']) and num_bad_runs < TOLERANCE:
        num_bad_runs += 1
        keymaera_call = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        keymaera_output, _ = keymaera_call.communicate()
        keymaera_output = keymaera_output.decode("utf-8")
        out_file = path + file_name + "_out.txt"
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


def read_var_file(file_name):
    input = {'interestvariable': [], 'variables': [], 'constants': [], 'gt_formula': []}
    with open(file_name, "r") as f:
        line = f.readline()
        mode = None
        while line:
            if '#' in line:
                if 'interest' in line:
                    mode = 'interestvariable'
                elif 'variables' in line:
                    mode = 'variables'
                elif 'constants' in line:
                    mode = 'constants'
                elif 'truth' in line:
                    mode = 'gt_formula'
            else:
                line = line.replace('\n', '')
                if len(line) > 0:
                    input[mode].append(line)
            line = f.readline()

    assert len(input['interestvariable']) == 1
    if len(input['gt_formula']) == 1:
        input['gt_formula'][0] = Formula(input['gt_formula'][0],None)
    if input['gt_formula']:
        gt = input['gt_formula'][0]
    else:
        gt = None
    return input['interestvariable'][0], input['variables'], input['constants'], gt


def read_formulas_file(file_name):
    axioms = []
    with open(file_name, "r") as f:
        line = f.readline()
        while line:
            line = line.replace('\n', '')
            if '#' not in line and len(line) > 0:
                axioms.append(line)
            line = f.readline()
    return axioms


def read_data_file(file_name):
    data = []
    with open(file_name, "r") as f:
        line = f.readline()
        while line:
            line = line.replace('\n', '')
            if len(line) > 1:
                if '#' in line:
                    line = f.readline()
                    continue
                elif "**" in line:
                    line = line.replace("** ",'')
                    line = ' '.join(line.split())
                    variables = line.split(' ')
                else:
                    line = ' '.join(line.split())
                    data_tmp = line.split(' ')
                    dict_tmp = {}
                    for idx, el in enumerate(data_tmp):
                        dict_tmp[variables[idx]] = float(el)
                    data.append(dict_tmp)
            line = f.readline()
    return data


def read_input_files(theory_path, data_file, full=False):
    # read files
    interest_variable, variables, constants, gt_formula = read_var_file(theory_path + "var_const_gt.txt")
    axioms = read_formulas_file(theory_path + "axioms.txt")
    formulas2prove = read_formulas_file(theory_path + "candidates.txt")
    if full:
        formulas2prove_python = read_formulas_file(theory_path + "candidates_py.txt")
    data = read_data_file(data_file)
    for idx, i in enumerate(formulas2prove):
        if full:
            formulas2prove[idx] = Formula(i, formulas2prove_python[idx])
        else:
            formulas2prove[idx] = Formula(i, None)
    return interest_variable, variables, constants, gt_formula, axioms, formulas2prove, data


def convert_input_problem(files_path, input_f,  modality, index_f=0):
    # derivation derivation_constants interval dependencies pointwiseL2 pointwiseLinf pointwiseLinf

    if os.path.exists(files_path):
        shutil.rmtree(files_path)
    os.mkdir(files_path)

    [interest_variable, variables, constants, gt_formula, axioms, formulas2prove, data] = input_f
    file_name_list = []

    if modality == 'derivation':
        file_name = files_path + "keymaera_file_derivation.kyx"
        file_name_list.append("keymaera_file_derivation.kyx")

        convert_problem_keymaera(constants, variables, interest_variable, axioms, formulas2prove[index_f], "derivation", file_name, data=data, var=None, relerr=None)

    elif modality == 'derivation_constants':
        file_name = files_path + "keymaera_file_derivation_constants.kyx"
        file_name_list.append("keymaera_file_derivation_constants.kyx")
        convert_problem_keymaera(constants, variables, interest_variable, axioms, formulas2prove[index_f], "derivation_constants", file_name, data=data, var=None, relerr=None)

    elif modality == 'pointwiseLinf':
        file_name = files_path + "keymaera_file_pointwiseLinf.kyx"
        file_name_list.append("keymaera_file_pointwiseLinf.kyx")
        convert_problem_keymaera(constants, variables, interest_variable, axioms, formulas2prove[index_f], "pointwiseLinf", file_name, data=data, var=None, relerr=None)

    elif modality == 'interval':
        file_name = files_path + "keymaera_file_interval.kyx"
        file_name_list.append("keymaera_file_interval.kyx")
        convert_problem_keymaera(constants, variables, interest_variable, axioms, formulas2prove[index_f], "interval", file_name, data=data, var=None, relerr=None)

    elif modality == 'dependencies':
        for var in data[0].keys():
            if var != interest_variable:
                file_name = f"{files_path}keymaera_file_dependencies_{var}.kyx"
                file_name_list.append(f"keymaera_file_dependencies_{var}.kyx")
                convert_problem_keymaera(constants, variables, interest_variable, axioms, formulas2prove[index_f], "dependencies", file_name, data=data, var=var, relerr=None)

    elif modality == 'pointwiseL2':
        file_name = files_path + "keymaera_file_pointwiseL2.kyx"
        file_name_list.append("keymaera_file_pointwiseL2.kyx")
        convert_problem_keymaera(constants, variables, interest_variable, axioms, formulas2prove[index_f], "pointwiseL2", file_name, data=data, var=None, relerr=None)
    else:
        raise Exception

    file_name_list_tmp = []
    for f in file_name_list:
        ff = f.split('/')
        file_name_list_tmp.append(ff[-1].replace('.kyx', ''))

    return file_name_list_tmp

def run_problem_derivation(problem_name, path_to_theory, data_file):
    output_path = OUTPUT_PATH + problem_name + '/'
    input_f = read_input_files(path_to_theory, data_file)
    modality = ['derivation', 'derivation_constants']
    for m in modality:
        print(f'\nmodality: {m}')
        for i in range(len(input_f[5])):
            print(f'- formula: {str(input_f[5][i])}')
            file_name_list = convert_input_problem(output_path, input_f, m, i)
            for file_name in file_name_list:
                result = call_keymaera(output_path, file_name)
                print("Result for " + file_name + ": " + str(result))


def run_problem_full(problem_name, path_to_theory, data_file):
    print('----------_>>>> run_problem_full Not tested yet')
    output_path = OUTPUT_PATH + problem_name + '/'
    input_f = read_input_files(path_to_theory, data_file, full=True)
    modality = ['derivation', 'weak_derivation', 'interval', 'dependencies', 'pointwiseL2', 'pointwiseLinf_efficient']

    for m in modality:
        print(f'\nmodality: {m}')
        for i in range(len(input_f[5])):
            print(f'- formula: {str(input_f[5][i])}')
            file_name_list = convert_input_problem(output_path, input_f, m, i)
            for file_name in file_name_list:
                result = call_keymaera(output_path, file_name)
                print("Result for " + file_name + ": " + str(result))


class Test(unittest.TestCase):

    def test_keymaera_z3(self):
        formula_name = 'test'
        path = '../test_data/test_keymaera/'
        result = call_keymaera(path, formula_name, tool='z3')
        self.assertEqual(result, 'proved')

    def test_keymaera_mathematica(self):
        formula_name = 'test_neg'
        path = '../test_data/test_keymaera/'
        result = call_keymaera(path, formula_name, tool='mathematica')
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

    # FSRD I.27.6
    # problem_name = 'I_27_6'
    # path_to_theory = '../../data/FSRD/I.27.6/'
    # data_file = '../../data/FSRD_noise/I.27.6/input.dat'
    # run_problem_derivation(problem_name, path_to_theory, data_file)

    # FSRD I.34.8
    # problem_name = 'I_34_8'
    # path_to_theory = '../../data/FSRD/I.34.8/'
    # data_file = '../../data/FSRD_noise/I.34.8/input.dat'
    # run_problem_derivation(problem_name, path_to_theory, data_file)

    # FSRD I.43.16
    # problem_name = 'I_43_16'
    # path_to_theory = '../../data/FSRD/I.43.16/'
    # data_file = '../../data/FSRD_noise/I.43.16/input.dat'
    # run_problem_derivation(problem_name, path_to_theory, data_file)

    # FSRD II.10.9
    # problem_name = 'II_10_9'
    # path_to_theory = '../../data/FSRD/II.10.9/'
    # data_file = '../../data/FSRD_noise/II.10.9/input.dat'
    # run_problem_derivation(problem_name, path_to_theory, data_file)

    # FSRD II.34.2
    # problem_name = 'II_34_2/'
    # path_to_theory = '../../data/FSRD/II.34.2/'
    # data_file = '../../data/FSRD_noise/II.34.2/input.dat'
    # run_problem_derivation(problem_name, path_to_theory, data_file)

    # CUSTOM PROBLEM
    # add in "custom_folder" the files: axioms, candidates, var_const_gt
    # problem_name = 'custom_problem'
    # path_to_theory = '../../data/custom_folder/'
    # data_file = '../../data/custom_folder/data_file.dat'
    # run_problem_derivation(problem_name, path_to_theory, data_file)
    # run_problem_full(problem_name, path_to_theory, data_file) # not tested yet
