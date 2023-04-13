"""
© Copyright IBM Corporation 2022. All Rights Reserved.
LICENSE: MIT License https://opensource.org/licenses/MIT
SPDX-License-Identifier: MIT
"""

import subprocess
import unittest
import os

OUTPUT_PATH_WOLFRAM = "../output/wolfram/"


def call_wolfram(input_expression, file_name=None, debug=False):
    command = f"wolframscript -code {input_expression}".split(" ")
    if debug:
        print(command)
    wolfram_call = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    wolfram_output, _ = wolfram_call.communicate()
    if debug:
        print("Done.")
    wolfram_output = wolfram_output.decode("utf-8")
    wolfram_output = wolfram_output.replace("\n", "")
    if debug:
        print(wolfram_output)
        if file_name:
            out_file = OUTPUT_PATH_WOLFRAM + file_name + "_out.txt"
            # save the output
            new_file = open(out_file, "w+")
            new_file.write(wolfram_output)
            new_file.close()
    return wolfram_output


def read_expressions(expressions_list_file):
    expressions_list = []
    token = None
    file = open(expressions_list_file, "r")
    file_content = file.readlines()
    for line in file_content:
        if not(line[0] == '#' or len(line) < 2):
            if 'function' in line:
                token = line.split('\t')[1]
                token = token.replace('\n', '')
                pass
            else:
                expr = line.replace('\n', '')
                expressions_list.append(expr)
    file.close()
    if token:
        return expressions_list, token
    else:
        return expressions_list


def print_results(results, file_name, debug):
    file = open(file_name, "w")
    expressions = [x for x in results.keys()]
    axioms = [x for x in results[expressions[0]].keys()]
    line = '---------------------------------------------------------------------------\n'
    if debug:
        print(line)
    file.write(line)
    line = 'Axioms list: '
    if debug:
        print(line)
    file.write(line)
    file.write('\n')
    for axiom in axioms:
        line = f'Axiom {str(axioms.index(axiom))}: {str(axiom)}'
        if debug:
            print(line)
        file.write(line)
        file.write('\n')
    file.write('\n')
    line = '---------------------------------------------------------------------------\n'
    if debug:
        print(line)
    file.write(line)
    for expression in expressions:
        line = f'Evaluating expression: {str(expression)}'
        if debug:
            print(line)
        file.write(line)
        file.write('\n')
        for axiom in axioms:
            if debug:
                line = f'Axiom: {str(axiom)} ==> {str(results[expression][axiom])}'
            else:
                line = f'Axiom {str(axioms.index(axiom))}: ==> {str(results[expression][axiom])}'
            if debug:
                print(line)
            file.write(line)
            file.write('\n')
        if debug:
            print('\n')
        file.write('\n')
        line = '---------------------------------------------------------------------------\n'
        if debug:
            print(line)
        file.write(line)
    file.close()


def combine_expressions(token, expression, axiom):
    axiom_vector = axiom.split('\t')
    combined_expression = axiom_vector[0].replace(token, expression)
    if len(axiom_vector) > 1:
        combined_expression += '/.{'
        for condition in axiom_vector[1:]:
            combined_expression += condition
            combined_expression += ","
        combined_expression = combined_expression[:-1]
        combined_expression += '}'
    return combined_expression


def preprecess_pipeline(expressions_list_file, axioms_file, results_file=None, debug=False):
    expressions_list, token = read_expressions(expressions_list_file)
    axioms_list = read_expressions(axioms_file)
    results = {}
    for expression in expressions_list:
        if expression not in results.keys():
            results[expression] = {}
        for axiom in axioms_list:
            condition = combine_expressions(token, expression, axiom)
            wolfram_output = call_wolfram(condition, debug=debug)
            if 'True' in wolfram_output:
                results[expression][axiom] = 'YES'
            elif 'False' in wolfram_output:
                results[expression][axiom] = 'NO'
            elif '$Failed' in wolfram_output:
                results[expression][axiom] = 'ERROR'
            else:
                if debug:
                    results[expression][axiom] = wolfram_output
                else:
                    results[expression][axiom] = 'NO'  # negation as failure
    if results_file:
        print_results(results, results_file, debug)
    return results


def langmuir():
    if not os.path.exists(OUTPUT_PATH_WOLFRAM):
        os.mkdir(OUTPUT_PATH_WOLFRAM)
    formulas_langmuir = '../../data/langmuir/langmuir_candidates_wolfram.txt'
    axioms_langmuir = '../../data/langmuir/langmuir_constraints_wolfram.txt'
    results_file = OUTPUT_PATH_WOLFRAM + 'langmuir.txt'
    results = preprecess_pipeline(formulas_langmuir, axioms_langmuir, results_file, debug=True)


def debug1():
    test_formulas_file1 = '../test_data/test_wolfram/formulas1.txt'
    test_axioms_file1 = '../test_data/test_wolfram/axioms1.txt'
    results = preprecess_pipeline(test_formulas_file1, test_axioms_file1)
    print(results)
    flag = True
    for key in results:
        for key2 in results[key]:
            if results[key][key2] == 'ERROR':
                flag = False
                break
    print(f'Test succeeded: {flag}')


def debug2():
    test_formulas_file2 = '../test_data/test_wolfram/formulas2.txt'
    test_axioms_file2 = '../test_data/test_wolfram/axioms2.txt'
    results = preprecess_pipeline(test_formulas_file2, test_axioms_file2)
    print(results)
    flag = True
    for key in results:
        for key2 in results[key]:
            if results[key][key2] == 'ERROR':
                flag = False
                break
    print(f'Test succeeded: {flag}')


class Test(unittest.TestCase):

    def test_wolfram1(self):
        expression = 'Limit[1/x,x->0]'
        result = call_wolfram(expression, file_name='test/test1')
        self.assertEqual(result, 'Indeterminate')

    def test_wolfram2(self):
        formula_name = '2+2'
        result = call_wolfram(formula_name, file_name='test/test2')
        self.assertEqual(result, '4')

    def test_wolfram3(self):
        formula_name = 'Limit[Sin[x]/x,x->0]'
        result = call_wolfram(formula_name, file_name='test/test3')
        self.assertEqual(result, '1')


if __name__ == '__main__':
    # TESTs for Mathematica
    #unittest.main()

    #call_wolfram('Limit[1/x,x->Infinity]<Infinity', file_name='test', debug=True)
    #debug1()
    #debug2()

    # Experiment for Langmuir with constraints K
    langmuir()
