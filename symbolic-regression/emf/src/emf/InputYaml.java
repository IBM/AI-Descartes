// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

import java.io.IOException;

import net.sourceforge.yamlbeans.YamlException;
import utils.UList;
import utils.YamlOpts;

public class InputYaml extends YamlOpts {

	public final String baron_exec = stringVal("baron_exec", "./baron");
	public final String baron_license = stringVal("LicName", "");
	public final int max_num_jobs = intVal("max_num_jobs", 7);
	public final int reportIntervalSecs = intVal("reportIntervalSecs", 60);

	// if max_error >= 0, then a max constraint is added
	public final double max_error = doubleVal("max_error", -1);

	// if this is false, then we don't minimize at all
	//	public final boolean minimize_error = boolVal("minimize_error", true);

	//	public final double initial_baron_MaxTime = doubleVal("initial_baron_MaxTime", 60);

	public final UList<String> baronOpts = stringList("baronOpts", "MaxTime: 60;");

	// kill any process that is still running after this time (if >0)
	public final double killTime = doubleVal("killTime", 0);

	public final String infile = stringVal("infile", "");;

	public final int max_tree_depth = intVal("max_tree_depth", 3);
	public final int max_complexity = max_tree_depth;

	public final boolean print_dbg = boolVal("print_dbg", false);

	// if true, then model constants as:  (USE_C0*C0 + 1)
	// if false, model as just C0
	// and add constraints to define USE_C0 from the value of C0
	// it appears that it is better to multiply after all
	public final boolean multiply_use_c = boolVal("multiply_use_c", true);

	// if true, limit choose_cX by squaring it, otherwise use two constraints (pos and neg)
	// (only matters if multiply_use_c is false)
	public final boolean choose_c_sqr = boolVal("choose_c_sqr", false);

	// this is used to relate USE_C# to C#, when multiply_use_c is false
	// this implicitly imposes a bound of this magnitude on constants
	public final double c_def_mul = 1000;

	// if true, variable exponents are use#%s*exp#%s, no constraints between use and exp;
	// otherwise, variable exponents are exp#%s, use and defined from exp via constraints
	public final boolean multiply_exp = boolVal("multiply_exp", true);

	// if true, (1-use#%s)*v^exp#%s
	// else v^(use#%s*exp#%s)
	public final boolean multiply_exp2 = boolVal("multiply_exp2", false);

	// tried use0 + use1*v0 + use2*v1 + use3*v1*v2
	// with use0+use1+use2+use3==1
	// slower
	public final boolean all_var_combs = boolVal("all_var_combs", false);

	// just don't use USE_C at all.
	public final boolean drop_use_c = boolVal("drop_use_c", true);

	public final boolean drop_usex = boolVal("drop_usex", true);

	public final boolean drop_all_constraints = boolVal("drop_all_constraints", true);

	// if true:  diff^2  else  (diff*diff)
	// squaring is slower...
	// but sometimes lower bounds not 0 with d*d
	public final boolean square_diffs_in_obj = boolVal("square_diffs_in_obj", true);

	// true:  choose_c0: 1000000.0*USE_C0 - (1-C0)*(1-C0) >= 0;
	// false: choose_c0: 1000000.0*USE_C0 - C0*C0 >= 0;
	public final boolean mulConst1based = true; // 'FALSE' DOES NOT WORK!  DO NOT USE!

	public final boolean generateAllBNodes = false;

	public final boolean cbrt_root_hack = boolVal("cbrt_root_hack", false);

	//	public final boolean root_div_hack = boolVal("root_div_hack", false);

	public final boolean trace_treegen = boolVal("trace_treegen", false);

	public final boolean reuse_baron_files = boolVal("reuse_baron_files", false);
	public final boolean reuse_baron_output = boolVal("reuse_baron_output", false);

	//	public final int max_num_consts = intVal("max_num_consts", -1);

	public final double max_const_val = doubleVal("max_const_val", 100);
	public final double min_const_val = doubleVal("min_const_val", -100);

	// if >0, then the sum of all exps in a single prod <= that
	public final int prod_exp_limit = intVal("prod_exp_limit", 0);

	// the maximum exponent value for a variable
	public final int max_var_exp = intVal("max_var_exp", 2);

	// two vars, one pos one neg, exponent=m-n, so that we can put limits on pos exps
	public final boolean neg_expvars = boolVal("neg_expvars", false);
	//	public final boolean posneg_exp_vars = boolVal("posneg_exp_vars", true);

	// if not neg_expvars, still allow exp to be negative
	public final boolean neg_exps = boolVal("neg_exps", true);

	public final boolean all_base_e = boolVal("all_base_e", false);
	public final boolean const_in_exp = boolVal("const_in_exp", false);

	public final int obj_variations = intVal("obj_variations", 0);
	public final boolean C0_C1 = boolVal("C0_C1", false);

	// if >=0, with a single variable (for now), input 0 => output 0 is required, up to this tolerance
	public final double special_zero_tol = doubleVal("special_zero_tol", -1);
	public final boolean use_special_zero_tol = special_zero_tol >= 0;

	public final int trace_baron_tree = intVal("trace_baron_tree", -1);
	public final boolean print_baron = boolVal("print_baron", true) && trace_baron_tree < 0;
	public final boolean trace_baron = boolVal("trace_baron", false);

	public final boolean focusOnBest = boolVal("focusOnBest", false);

	public final static boolean group_addmul = false;

	public final boolean zimpl = false;
	public final String extension = zimpl ? ".zpl" : ".bar";
	public final String comment = zimpl ? "#" : "//";

	// can't call stringList more than once per string
	private final UList<String> raw_operators = stringList("operators", "*", "/", "+");

	public final UList<Double> exp_inds = doubleList("exp_inds");
	//	public final double exp_range_low = doubleVal("exp_range_low", 0);
	//	public final double exp_range_high = doubleVal("exp_range_high", 0);

	//	public final boolean has_exp = exp_inds.isNotEmpty() || exp_range_low < exp_range_high;

	// if true, prints all baron files before executing any of them; otherwise prints as it goes.
	public final boolean printBaronFilesUpfront = boolVal("printBaronFilesUpfront", false);

	// allows variable exponents to be fractional
	public final boolean relaxUSE_I = boolVal("relaxUSE_I", true);

	public final boolean ignoreUnlikely = boolVal("ignoreUnlikely", true);

	// we model input vars using x^pow
	// it turns out that 0^pow slows down Baron hugely, so just avoid it
	public final boolean drop0inputs = boolVal("drop0inputs", true);

	public final boolean special_div_process = boolVal("special_div_process", false);

	public final boolean div_denom_constraints = boolVal("div_denom_constraints", false);

	public final UList<String> only_process = stringList("only_process");

	public final boolean split_on_all_consts = boolVal("split_on_all_consts", false);

	public final boolean split_on_all_exps = boolVal("split_on_all_exps", false);
	public final boolean split_on_exp0 = boolVal("split_on_exp0", false);
	public final boolean split_on_denom_exp = boolVal("split_on_denom_exp", false);

	public final boolean multiply_out_obj = boolVal("multiply_out_obj", false);

	// XX/P ==> XX*Pinv
	public final boolean div_Pinv = boolVal("div_Pinv", false);

	public final String genDataFromExpr = stringVal("genDataFromExpr", "");
	public final UList<Double> genDataConsts = doubleList("genDataConsts");

	public final UList<UList<Integer>> genDataExps = listOf("genDataExps", yamlIntList);

	public final UList<String> genDataVars = stringList("genDataVars");
	public final int genDataSeed = intVal("genDataSeed", 0);
	public final int genDataNumLines = intVal("genDataNumLines", 10);
	public final int genDataNumDummyVars = intVal("genDataNumDummyVars", 0);

	public final int inputSampleSize = intVal("inputSampleSize", 0);
	public final int inputSampleSeed = intVal("inputSampleSeed", inputSampleSize);

	public final int expn_ubound = intVal("expn_ubound", 2);
	public final boolean expn_integral = boolVal("expn_integral", false);

	public final boolean javascript_pow_hack = boolVal("javascript_pow_hack", false);

	//	private UList<Operator> maybeExp() {
	//		if (raw_operators.some(nm -> nm.contains("rt")))
	//			return mkUList1(Operator.POW);
	//		return UList.empty();
	//	}

	//	 p 7
	//	BRANCHING_PRIORITIES{
	//	x3: 10;
	//	x5: 0; }
	//	The effect of this input is that variable x3 will be given higher priority than all others,
	//	while variable x5 will never be branched upon.

	//	public final static UList<ExpPower> exponents = mkUList1(cbrt_exp, sqrt_exp);
	//	public final static UList<ExpPower> exponents = mkUList1(cbrt_exp);

	//	public final UList<Operator> operators = raw_operators.filter(nm -> !nm.contains("rt"))
	//			.map(nm -> Operator.lookupOperator(nm)).concat(maybeExp());
	//	public final UList<ExpPower> power_operator_exponents = raw_operators.filter(nm -> nm.contains("rt"))
	//			.map(nm -> nm.equals("sqrt") ? Operator.sqrt_exp : nm.equals("cbrt") ? Operator.cbrt_exp : null);

	public final UList<Operator> operators = raw_operators.map(nm -> Operator.lookupOperator(nm));

	public final UList<Operator> unary_operators = operators.filter(n -> n.arity == 1);

	{
		printf("UNOPS %s %s%n", operators, unary_operators);
	}

	// only allow exp() on prods
	public final boolean fractional_mli = boolVal("fractional_mli", false);

	public final UList<Operator> operators2 = operators.filter(op -> op.arity == 2)
	//				.maybeAdd(unary_operators.isEmpty() ? null : Operator.UNARY_FN)
	;

	//	public final UList<ExpPower> power_operator_exponents = raw_operators.filter(nm -> nm.contains("rt"))
	//			.map(nm -> nm.equals("sqrt") ? Operator.sqrt_exp : nm.equals("cbrt") ? Operator.cbrt_exp : null);

	public InputYaml(String[] args) throws YamlException, IOException {
		super(args);

		checkRegistered();

		dump("BOP ", baronOpts);
		if (javascript_pow_hack)
			ProdVal.javascript_pow_hack = true;

		if (neg_exps && neg_expvars)
			die("Can't have both neg_exps and neg_expvars.  Pick one.");
		//		if (exp_range_low != 0 || exp_range_high != 0)
		//			myassert(exp_range_low < exp_range_high);
	}
}
