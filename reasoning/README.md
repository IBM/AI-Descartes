# Reasoning for Scientific Discovery

There are 3 main pipelines (details below):
- [pipeline KeYmaeraX 3 problems](#pipeline-keymaerax-3-problems)
- [pipeline KeYmaeraX](#pipeline-keymaerax)
- [pipeline Mathematica](#pipeline-mathematica)

***
## Setup
* Download KeYmaeraX (4.9.3) from [here](https://keymaerax.org) (older versions can be found [here](https://github.com/LS-Lab/KeYmaeraX-release/releases)) and add it in [src/keymaerax](src/keymaerax). 
* Install Matematica (Tested with versions: 12 and 13.2)
* Install wolframscript (terminal command for Matematica)
* Connect Matematica to KeYmaeraX, otherwise use `z3` (see KeYmaeraX website for more info)

Please note that the results reported in the paper are by using the pipeline with KeYmaeraX+Mathematica.
Using KeYmaeraX+z3 in some cases produces worse results.

***
## Pipeline KeYmaeraX 3 problems


**Script:** [reasoning/src/pipeline_keymaera_3problems.py](src/pipeline_keymaera_3problems.py)

In the main function are available the following experiments:

### Unit-test
**Function:** `unittest.main()`

This function perform 3 tests: 2 test for KeYmaera, one using `z3` and one using `mathematica`; one test for the function `compute_output`

Run this to check that KeYmaera is set up well in your machine and that everything works properly.

### Kepler
**Function:** `run_kepler()`

This runs the two predefined KeYmaera files for the Kepler problem:
*  `reasoning/output/keymaera/keymaera_files/kepler/Kepler.kyx`: regular Kepler (correct formula)
*  `reasoning/output/keymaera/keymaera_files/kepler/Kepler_const.kyx`: Kepler with constants (correct formula with constants)

The results are printed on the terminal.

### Kepler Solar system
**Function:** `run_kepler_solar()`

This run the experiments on the Solar system dataset. 
It will compute the following measures:
* `interval`: the interval reasoning metric
* `dependencies`: the dependencies of the non-target variables (IMPORTANT: to use this always compute the `interval` first)
* `pointwiseL2`: the l2 reasoning metric (point-wise)
* `pointwiseLinf`: the l_inf reasoning metric (point-wise) 
* `pointwiseLinf_efficient`: the l_inf reasoning metric (point-wise, and computed separately for each point) (IMPORTANT: to use this always compute `pointwiseL2` first)
* `derivation`: derive the formula as it is (with the numerical constants)
* `weak_derivation`: derive the formula substituting the numerical constants with `c1, c2, ...`

**To change the measures** modify the global variable `MEASURES` at the beginning of the file.
The full list is:
```
MEASURES = ['interval', 'dependencies', 'pointwiseL2', 'pointwiseLinf', 'pointwiseLinf_efficient', 'derivation', 'weak_derivation']
```
IMPORTANT: 
* measure `dependencies` depends on measure `interval`
* measure `pointwiseLinf_efficient` depends on measure `pointwiseL2`
* the others are independent of each other.


**To change the precision level**  modify the global variable `PRECISION` at the beginning of the file.
By default it is set as `1e-4`.

**To change the input candidate functions** add them in the body of the function `run_kepler_solar()`.
The input format is the following:

``` formulas = [['formula1_keymaeraFormat','formula1_pythonFormat'], ... ,['formulaN_keymaeraFormat','formulaN_pythonFormat']]```

Thus `formulas` is a list of lists of length 2, where the first element is a string containing the formula in KeYmaera format, while the second one is the same formula in python format.
For example
```
formulas = [['( 0.1319 * dN^3 )^(1/2)', 'sqrt( 0.1319 * dN**3 )'], ['( 0.1316 * ( dN^3 + dN ) )^(1/2)', 'sqrt( 0.1316 * ( dN**3 + dN ) )'],
            ['(( 0.03765 * dN^3 ) + dN^2 )/( 2.0 + dN )', '(( 0.03765 * dN**3 ) + dN**2 )/( 2.0 + dN )']]
```

IMPORTANT: 
* add spaces after each term (in particular for constant terms like `(X+0.2)` should be written as `( X + 0.2 )`
* for integer constants add .0 , for example (x+1) should be written as ( x + 1.0 )
* `formulas` is a list of lists. the innermost list are a pair of `[keymeara_formula, python_formula]` where `keymeara_formula` and `python_formula` represent the same formula in KeYmaera and python format respectively
* pay attention to:
  * power operator: KeYmaera `^` ;  python `**`
  * square root operator: KeYmaera `(expr)^(1/2)` ;  python `sqrt(expr)`

###  Kepler Exo-planets
**Function:** `run_kepler_exoplanets()`

Same as  for `run_kepler_solar()` but for Exo-planet dataset.

### Kepler Binary Stars
**Function:** `run_kepler_binarystars()`

Same as  for `run_kepler_solar()` but for Binary star dataset.

### Kepler counterexamples
**Function:** `run_kepler_solar_counterexample()`

This function generate counterexamples (assignment of the variable of interest PN) for a specific input non-derivable formula for the solar system dataset for Kepler.
To change the input points do it directly in the function `run_kepler_solar_counterexample` and modify `data_points` dictionary.

* **input:** assignment of the variables `m1N`, `m2N`, `dN`, e.g. `data_points = {'m1N': 1, 'm2N': 0.055, 'dN': 0.3871}`
* **output:** value of `PN`


### Langmuir
**Function:** `run_langmuir()`

This runs predefined KeYmaera files for the Langmuir problem:
*  `reasoning/output/keymaera/keymaera_files/langmuir/langmuir.kyx`: regular Langmuir (correct formula)
*  `reasoning/output/keymaera/keymaera_files/langmuir/langmuir_const.kyx`: Langmuir with constants (correct formula with constants)
*  `reasoning/output/keymaera/keymaera_files/langmuir/langmuir_2sites.kyx`: Langmuir 2 sites (correct formula with two sites)

The results are printed on the terminal.

### Time dilation 
**Function:** `run_time_dilation()`

This runs predefined KeYmaera files for the Time dilation  problem. 
In particular it evaluate the 4 different function reported in the paper both with the absolute and relative error:
*  `reasoning/output/keymaera/keymaera_files/time_dilation/time_dilation.kyx`: regular time dilation (correct formula)
*  `reasoning/output/keymaera/keymaera_files/time_dilation/time_dilation_fi_relativistic_A.kyx`: time dilation with relativistic background theory an absolute error for function f_i
*  `reasoning/output/keymaera/keymaera_files/time_dilation/time_dilation_fi_relativistic_R.kyx`: time dilation with relativistic background theory an relative error for function f_i

The results are printed on the terminal.


### Feynman problems
**Function:** `run_feynman()`

This runs predefined files from AI-Feynman dataset:
*  `data/keymaera/input/Feynman_I_27_6.kyx`: for the Feynman problem Volume I Chapter 27 Equation 6
*  `data/keymaera/input/Feynman_I_16_6.kyx`: for the Feynman problem Volume I Chapter 16 Equation 6
*  `data/keymaera/input/Feynman_I_15_10.kyx`: for the Feynman problem Volume I Chapter 15 Equation 10

The results are printed on the terminal.

***




## Pipeline KeYmaeraX

**Script**: [resoning/src/pipeline_keymaera.py](src/pipeline_keymaera.py))


In the main function are available the following experiments:

### Unit-test
**Function:** `unittest.main()`

This function perform 3 tests: 2 test for KeYmaera, one using `z3` and one using `mathematica`; one test for the function `compute_output`

Run this to check that KeYmaera is set up well in your machine and that everything works properly.

### FSRD problems

There are 7 Feynman problems: 

To reproduce the results on these problems uncomment the 3 lines defining:

* `problem_name`
* `path_to_theory`
* `data_file`

and the last line with the function `run_problem_derivation(problem_name, path_to_theory, data_file)`

### Custom problems

To run the reasoning module of AI-Descartes on a custom problem add the following files
* `problem_name` with the name of the problem
* `path_to_theory` with the path to the theory folder that will have to contain 3 files (for examples see the `data` folder):
  * `axioms.txt` with the background theory axioms
  * `candidates.txt` with the candidate formulas to evaluate (Keymaera format)
  * `candidates_py.txt` with the candidate formulas to evaluate (python format)
  * `var_const_gt.txt` with the list of constants, variables, and variable of interest 
* `data_file` with the path to the file with the data points

and execute the last line with the function `run_problem_derivation(problem_name, path_to_theory, data_file)`

***

## Pipeline Mathematica

**Script**: `resoning/src/pipeline_mathematica.py`

In the main function are available the following experiments:

### Unit-Test
**Function:** `unittest.main()`


This function perform 3 tests to make sure Mathematica is working properly.
Run this to check that Mathematica is set up well in your machine and that everything works properly.


###  Direct run Mathematica
**Function:** `call_wolfram()`

Use this function to directly call Mathematica for debugging purposes. It calls Mathematica on a specific file and formula.
For example:
```
call_wolfram('Limit[1/x,x->Infinity]<Infinity', file_name='test')
```
will try to prove that the limit of `1/x` at infinity is not infinity.

If the `debug` flag is set to `True` then output can be found in `data/wolfram/output/`

### Debug 1
**Function:** `debug1()`
Perform a test trying the axioms in `reasoning/test_data/test_wolfram/axioms1.txt` over the formulas in `reasoning/test_data/test_wolfram/formulas1.txt`. This example performs a check for unary functions. 



### Debug 2
**Function:** `debug2()`

Perform a test trying the axioms in `reasoning/test_data/test_wolfram/axioms2.txt` over the formulas in `reasoning/test_data/test_wolfram/formulas2.txt`. This example performs a check for binary functions. 

### Langmuir
**Function:** `langmuir()`

This function will run the experiments of the paper regarding Langmuir with the constraints K.

* The formulas to check are in the file: `data/langmuir/langmuir_candidates_wolfram.txt`
* The constraints K to apply are in the file = `data/langmuir/langmuir_constraints_wolfram.txt`

The output can be found at: `reasoning/output/wolfram/langmuir.txt`

**Custom functions-axioms check**

In general the function `preprecess_pipeline(expressions_list_file, axioms_file, results_file=None, debug=False)` allows you to perform a check of a set of constraints (in the file `axioms_file`) over a set of functions (in the file `expressions_list_file`)

**How to format Axioms and Formulas**:
* The axioms have to be formatted in Wolfram Language one per line
* The formulas have to be formatted in Wolfram Language one per line
* To assign a variable in the formula to a specific number write: `axiom    var1->number1   var2->number2 ...` each separated by a tab. For example if I want to check if the function `f(x,y)` is equal to `0.2` when `x=5` and `y=5` I write `f(x,y)==0.2	x->5    y->5`.
* IMPORTANT:
  * in the formulas file the first line should contain the function specification writing:
    * `function	f(x)` for unary function
    * `function	f(x,y)` for binary 
    * etc.
  * Use a tab separator between the keyword `function` and the function form.
  * The function has to be called `f(..)` with any number arguments. 
  * Be consistent between the name of the variables in the function file and the axioms file.
* In the formulas files you can comment lines using the `#` character at the beginning of the line
* For better results, write all the numbers in form of fractions: e.g. `0.00345` should be written as `(345/100000)` (otherwise Mathematica sometime behaves strange).

***



