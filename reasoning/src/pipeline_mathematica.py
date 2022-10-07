"""
© Copyright IBM Corporation 2022. All Rights Reserved.
LICENSE: MIT License https://opensource.org/licenses/MIT
SPDX-License-Identifier: MIT
"""

import os
import subprocess
from abc import ABC, abstractmethod
import sys
import unittest
from math import *

OUTPUT_PATH_WOLFRAM = "../data/wolfram/output/"
INPUT_PATH_WOLFRAM = "../data/wolfram/input/"


def call_wolfram(input_expression, file_name="test", debug=False):
    command = f"wolframscript -code {input_expression}".split(" ")
    #command = f"java -jar keymaerax/keymaerax.jar -tool {tool} -timeout {TIMEOUT} -prove {formula_file} -out {formula_file_out} -mathkernel /Applications/Mathematica.app/Contents/MacOS/MathKernel".split(" ")
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
        out_file = OUTPUT_PATH_WOLFRAM + file_name + "_out.txt"
        # save the output
        new_file = open(out_file, "w+")
        new_file.write(wolfram_output)
        new_file.close()
    return wolfram_output


def read_expressions(expressions_list_file):
    expressions_list = []
    token = None
    file = open(INPUT_PATH_WOLFRAM + expressions_list_file, "r")
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
    file = open(OUTPUT_PATH_WOLFRAM + "wolfram_results_" + file_name , "w")
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
        line = 'Axiom ' + str(axioms.index(axiom)) + ': ' + str(axiom)
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
        line = 'Evaluating expression: ' + str(expression)
        if debug:
            print(line)
        file.write(line)
        file.write('\n')
        for axiom in axioms:
            if debug:
                line = 'Axiom: ' + str(axiom) +' ==> ' + str(results[expression][axiom])
            else:
                line = 'Axiom ' + str(axioms.index(axiom)) +': ==> ' + str(results[expression][axiom])
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


def preprecess_pipeline(expressions_list_file, axioms_file, debug=False):
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
    print_results(results, expressions_list_file, debug)


def langmuir():
    formulas_langmuir = 'formulas_langmuir.txt'
    axioms_langmuir = 'axioms_langmuir.txt'
    preprecess_pipeline(formulas_langmuir, axioms_langmuir, debug=True)


def test1():
    test_formulas_file1 = 'formulas1.txt'
    test_axioms_file1 = 'axioms1.txt'
    preprecess_pipeline(test_formulas_file1, test_axioms_file1)


def test2():
    test_formulas_file2 = 'formulas2.txt'
    test_axioms_file2 = 'axioms2.txt'
    preprecess_pipeline(test_formulas_file2, test_axioms_file2)


class Test(unittest.TestCase):

    def test_wolfram1(self):
        expression = 'Limit[1/x,x->0]'
        result = call_wolfram(expression, file_name='test1')
        self.assertEqual(result, 'Indeterminate')

    def test_wolfram2(self):
        formula_name = '2+2'
        result = call_wolfram(formula_name, file_name='test2')
        self.assertEqual(result, '4')

    def test_wolfram3(self):
        formula_name = 'Limit[Sin[x]/x,x->0]'
        result = call_wolfram(formula_name, file_name='test3')
        self.assertEqual(result, '1')


if __name__ == '__main__':
    unittest.main()
    #call_wolfram('Limit[1/x,x->Infinity]<Infinity', file_name='test', debug=True)
    #test1()
    #test2()
    #langmuir()
