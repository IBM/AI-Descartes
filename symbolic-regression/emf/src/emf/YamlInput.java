// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.BExpr.*;
import static utils.UList.*;
import static utils.VRAUtils.*;

import java.io.IOException;
import java.util.Optional;

import emf.BExpr.BVar;
import net.sourceforge.yamlbeans.YamlException;
import utils.UList;
import utils.UMap;
import utils.VRAFileUtils;
import utils.YamlOpts;

public class YamlInput extends YamlOpts {

	public final Integer izero = 0;

	public final String baron_exec = stringVal("baron_exec", "./baron");
	public final String baron_license = stringVal("LicName", "");
	public final int max_num_jobs = intVal("max_num_jobs", 7);
	public final int reportIntervalSecs = intVal("reportIntervalSecs", 60);

	// if max_error >= 0, then a max constraint is added
	public final double max_error = doubleVal("max_error", -1);
	public final boolean constraint_instead_of_objective = boolVal("constraint_instead_of_objective", false);

	public final boolean positive_deriviative_constraints = boolVal("positive_deriviative_constraints", false);

	// if this is false, then we don't minimize at all
	//	public final boolean minimize_error = boolVal("minimize_error", true);

	//	public final double initial_baron_MaxTime = doubleVal("initial_baron_MaxTime", 60);
	private final UList<String> default_baronOpts = stringList("default_baronOpts");
	private final UList<String> baronOpts0 = stringList("baronOpts");
	public final UList<String> baronOpts = combineBaronOpts(default_baronOpts, baronOpts0);

	// kill any process that is still running after this time (if >0)
	public final double killTime = doubleVal("killTime", 0);

	private final String infile0 = stringVal("infile", "");
	public final String infile = VRAFileUtils.fileExists(infile0) ? infile0
			: infile0.endsWith(".dat") && VRAFileUtils.fileExists(infile0.replace(".dat", ".csv"))
					? infile0.replace(".dat", ".csv")
					: infile0;

	public final int max_tree_depth = intVal("max_tree_depth", 3);
	public final int max_complexity = max_tree_depth;

	public final int M_complexity = intVal("M_complexity", 3);
	public final int addsub_complexity = intVal("addsub_complexity", 1);
	public final int mul_complexity = intVal("mul_complexity", 2);
	public final int div_complexity = intVal("div_complexity", 3);
	public final int pow_complexity = intVal("pow_complexity", 3);
	public final int exp_complexity = intVal("exp_complexity", 3);

	// RECENT HACK
	public final int sin_complexity = intVal("sin_complexity", 3);

	public final boolean print_dbg = boolVal("print_dbg", false);

	public final boolean nonconst_plusminus = boolVal("nonconst_plusminus", false);
	private final boolean nonconst_sub_expr = boolVal("nonconst_sub_expr", false);
	private final boolean nonconst_sub_one_minus_one = boolVal("nonconst_sub_one_minus_one", false);
	private final boolean nonconst_sub_one_minus_one_zero = boolVal("nonconst_sub_one_minus_one_zero", false);
	{
		if (nonconst_sub_expr || nonconst_sub_one_minus_one || nonconst_sub_one_minus_one_zero) {
			myassert(nonconst_plusminus);
			myassert(mkUList1(nonconst_sub_expr, nonconst_sub_one_minus_one, nonconst_sub_one_minus_one_zero)
					.count(true) <= 1);
		}

		if (nonconst_plusminus)
			myassert(mkUList1(nonconst_sub_expr, nonconst_sub_one_minus_one, nonconst_sub_one_minus_one_zero)
					.count(true) == 1);
	}

	public final BExpr nonconst_str(MLInstance mli) {
		myassert(!nonconst_sub_expr);
		return (nonconst_sub_expr ? null // negONE.pow(mli.ci) "(-1^Ci#)*"
				: nonconst_sub_one_minus_one ? eTWO.mul(mli.ci).sub(1)
						/* "(-1+2*Ci#)*" */ : nonconst_sub_one_minus_one_zero ? mli.ci.sub(1) /* "(-1+Ci#)*"*/ : eONE);
	}

	private UList<String> combineBaronOpts(UList<String> default_baronOpts, UList<String> baronOpts0) {
		UList<String> optnms = baronOpts0.map(s -> s.replaceAll(":.*", ""));
		UList<String> defs = default_baronOpts.filter(s -> !optnms.contains(s.replaceAll(":.*", "")));
		return defs.concat(baronOpts0);
	}

	public final double nonconst_val(double Ci) {
		return (nonconst_sub_expr ? Math.pow(-1, Ci)
				: nonconst_sub_one_minus_one ? (-1 + 2 * Ci) : nonconst_sub_one_minus_one_zero ? (-1 + Ci) : 1.0);

	}

	// if true, then model constants as:  (USE_C0*C0 + 1)
	// if false, model as just C0
	// and add constraints to define USE_C0 from the value of C0
	// it appears that it is better to multiply after all
	public final boolean multiply_use_c = boolVal("multiply_use_c", false);

	// if true, limit choose_cX by squaring it, otherwise use two constraints (pos and neg)
	// (only matters if multiply_use_c is false)
	public final boolean choose_c_sqr = boolVal("choose_c_sqr", false);

	// this is used to relate USE_C# to C#, when multiply_use_c is false
	// this implicitly imposes a bound of this magnitude on constants
	//	public final double c_def_mul = 1000;

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

	// implement monos using multiplication and binary vars instead of exponents
	public final boolean mono_by_mul = boolVal("mono_by_mul", false);

	// just don't use USE_C at all.
	public final boolean drop_use_c = boolVal("drop_use_c", true);

	public final boolean drop_usex = boolVal("drop_usex", true);

	public final boolean multitask = boolVal("multitask", true);
	// how many secs to run each task before suspending
	// don't make too small, it takes time to resume processes
	public final int multitask_secs = intVal("multitask_secs", 10);

	// this is useful for demos
	public final int total_maxsecs = intVal("total_maxsecs", 10000000);
	// this is useful for demos
	public final int max_num_forms = intVal("max_num_forms", 10000000);

	//	public final boolean drop_all_constraints = boolVal("drop_all_constraints", false);

	// if true:  diff^2  else  (diff*diff)
	// squaring is slower...
	// but sometimes lower bounds not 0 with d*d
	public final boolean square_diffs_in_obj = boolVal("square_diffs_in_obj", true);

	// instead of sum-of-squares, just use y-f(x) or f(x)-y
	public final boolean diff_in_obj = boolVal("diff_in_obj", false);

	// (assumes diff_in_obj) 
	// if true,  then use y-f(x) and add constraints y>=f(x);
	// if false, then use f(x)-y and add constraints y<=f(x);
	public final boolean diff_in_obj_y_sub_fx = boolVal("diff_in_obj_y_sub_fx", false);

	// true:  choose_c0: 1000000.0*USE_C0 - (1-C0)*(1-C0) >= 0;
	// false: choose_c0: 1000000.0*USE_C0 - C0*C0 >= 0;
	//	public final boolean mulConst1based = true; // 'FALSE' DOES NOT WORK!  DO NOT USE!

	public final boolean generateAllBNodes = boolVal("generateAllBNodes", false);

	public final boolean trace_treegen = boolVal("trace_treegen", false);

	public final boolean reuse_baron_files = boolVal("reuse_baron_files", false);
	public final boolean reuse_baron_output = boolVal("reuse_baron_output", false);

	public final Integer max_num_consts = nonnegIntegerVal("max_num_consts");

	// if >=0, then any constant must have dims st asb(sum of the dims) <= this val
	// if == 0, then constants must be dimensionless.
	public final int max_const_dim_sum = intVal("max_const_dim_sum", -1);

	public final double max_const_val = doubleVal("max_const_val", 100);
	public final double min_const_val = doubleVal("min_const_val", -100);

	// if >0, then the sum of all exps in a single prod <= that
	private final int prod_exp_limit0 = intVal("prod_exp_limit", 0);
	public final Optional<Integer> prod_exp_limit = prod_exp_limit0 == 0 ? Optional.empty()
			: Optional.of(prod_exp_limit0);

	// the maximum exponent value for a variable
	public final int max_var_exp = intVal("max_var_exp", 2);

	// if not neg_expvars, still allow exp to be negative
	public final boolean neg_exps = boolVal("neg_exps", true);
	public final boolean always_neg_exps = boolVal("always_neg_exps", false);

	public final boolean subConstInObj = boolVal("subConstInObj", false);

	public final boolean all_base_e = boolVal("all_base_e", true);
	public final boolean const_in_exp = boolVal("const_in_exp", false);

	// slower!
	public final boolean force_lex_order = boolVal("force_lex_order", false);

	public final int obj_variations = intVal("obj_variations", 0);
	public final boolean C0_C1 = boolVal("C0_C1", false);

	// if >=0, with a single variable (for now), input 0 => output 0 is required, up to this tolerance
	public final double special_zero_tol = doubleVal("special_zero_tol", -1);
	public final boolean use_special_zero_tol = special_zero_tol >= 0;

	public final boolean stop_hopeless_jobs = boolVal("stop_hopeless_jobs", true);
	public final double hopelessJobThresh = doubleVal("hopelessJobThresh", 1);

	public final int trace_baron_tree = intVal("trace_baron_tree", -1);
	public final boolean print_baron = boolVal("print_baron", true) && trace_baron_tree < 0;
	public final boolean trace_baron = boolVal("trace_baron", false);

	public final boolean focusOnBest = boolVal("focusOnBest", false);

	public final static boolean group_addmul = false;

	public final boolean zimpl = false;
	public final static String baron_extension = ".bar";
	public final String extension = zimpl ? ".zpl" : ".bar";
	public final String comment = zimpl ? "#" : "//";

	// can't call stringList more than once per string
	private final UList<String> raw_operators = stringList("operators", "*", "/", "+", "sqrt");

	public final UList<Double> exp_inds = doubleList("exp_inds");
	//	public final double exp_range_low = doubleVal("exp_range_low", 0);
	//	public final double exp_range_high = doubleVal("exp_range_high", 0);

	//	public final boolean has_exp = exp_inds.isNotEmpty() || exp_range_low < exp_range_high;

	// if true, prints all baron files before executing any of them; otherwise prints as it goes.
	public final boolean printBaronFilesUpfront = boolVal("printBaronFilesUpfront", false);

	// allows variable exponents to be continuous
	// this make P represent exp(P,n) for continuous n [0,1] 
	public final boolean relaxUSE_I = boolVal("relaxUSE_I", false);

	public final boolean fexp_eq_int = boolVal("fexp_eq_int", false);

	// if num consts restricted, only those monos with consts may be fractional
	// this forces the others to have a value equal to a corresponding int var
	// otherwise, no change to exp in OBJ, just new vars and constraints
	// (also tried using fractional var in exponent (iexp*fexp) but was slower)
	// (1-USE_C0)*(expi0x-exp0x) == 0;
	public final boolean relaxUSE_I_eq_int = boolVal("relaxUSE_I_eq_int", false);

	// this seems faster than relaxUSE_I_eq_int:
	// nonconst_int_0x: (-2 + 4*USE_C0) - exp0x <= 0;
	// foo0: 4*(1-USE_C0) - add0 >= 0;
	// this idea (continuous + integer, bounding each as needed using boolean USE var)
	// is used for the C variable, and turns out to be faster, so I adapted it to the exponent.
	public final boolean relaxUSE_I_add_int = boolVal("relaxUSE_I_add_int", false);
	//	{
	//		if (relaxUSE_I) {
	//			myassert(relaxUSE_I_add_int || relaxUSE_I_eq_int);
	//			myassert(!relaxUSE_I_add_int || !relaxUSE_I_eq_int);
	//		}
	//	}
	//	public final boolean ignoreUnlikely = boolVal("ignoreUnlikely", false);

	// we model input vars using x^pow
	// it turns out that 0^pow slows down Baron hugely, so just avoid it
	public final boolean drop0inputs = boolVal("drop0inputs", true);

	// with multitasking, suspended baron jobs are left lying around.  This kills them on startup.
	public final boolean kill_existing_baron_jobs = boolVal("kill_existing_baron_jobs", false);

	public final boolean special_div_process = boolVal("special_div_process", false);

	public final boolean div_denom_constraints = boolVal("div_denom_constraints", false);

	public final UList<String> only_process = stringList("only_process");

	public final boolean split_on_all_consts = boolVal("split_on_all_consts", false);

	public final boolean split_on_all_exps = boolVal("split_on_all_exps", false);
	public final boolean split_on_exp0 = boolVal("split_on_exp0", false);
	public final boolean split_on_denom_exp = boolVal("split_on_denom_exp", false);

	public final boolean multiply_out_obj = boolVal("multiply_out_obj", false);

	// instead of (x_i - y_i)^2, use (x_i/y_i - 1)^2   (where x_i is the predicated and y_i the actual output)  
	public final boolean relative_squared_error = boolVal("relative_squared_error", false);

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

	public final boolean print_timing_info = boolVal("print_timing_info", false);

	public boolean square_output_hack = boolVal("square_output_hack", false);

	public final boolean add_noise_to_input = boolVal("add_noise_to_input", false);
	//mean should be 0, stdev should be: epsilon*rms(y), where you let epsilon be 10^{-2} or 10^{-1}.
	public final int noise_seed = intVal("noise_seed", 0);
	public final double noise_mean = doubleVal("noise_mean", 0.0);
	public final double noise_epsilon = doubleVal("noise_epsilon", 1e-2);

	public final boolean use_dimensional_analysis = boolVal("use_dimensional_analysis", false);

	public final boolean stop_on_good_fit = boolVal("stop_on_good_fit", true);
	public final double stop_on_good_fit_thresh = doubleVal("stop_on_good_fit_thresh", 1e-5);

	public final boolean dimensional_shift = boolVal("dimensional_shift", false);

	public final UList<DimUnit> dimensionUnits = DimUnit.mkUnits(stringList("dimensionUnits"));

	//	private final UMap<String, DimUnit> dimMap = dimensionUnits.mkrevmap(unit -> unit.nm);
	//	public final UMap<DimUnit, UList<Integer>> dimensionVars = mapOf("dimensionVars", yamlString, yamlIntList)
	//			.mapKeys(nm -> dimMap.get(nm));
	public final UMap<String, UList<Integer>> dimensionVars0 = mapOf("dimensionVars", yamlString, yamlIntList);
	{
		dimensionVars0.forEachEntry((vr, units) -> myassert(units.size() == dimensionUnits.size()));
	}

	// if min_const_units is -2, then a const with units 1/m^3 will not be considered
	public final int min_const_units = intVal("min_const_units", -2);
	// if max_const_units is 2, then a const with units m^3 will not be considered
	public final int max_const_units = intVal("max_const_units", 2);

	// for intermediate variables
	public final int min_dimanal_units = intVal("min_dimanal_units", -3);
	public final int max_dimanal_units = intVal("max_dimanal_units", 3);

	public final boolean only_dimless_consts = boolVal("only_dimless_consts", true);

	//	public final UMap<String, UMap<DimUnit, Integer>> dimensionVarsByDimUnit0 = dimensionVars0
	//			.mapValues((vr, dims) -> dimensionUnits.zipMap(dims));//	private UList<Operator> maybeExp() {
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
	final UList<String> pow_nms = mkUList1("sqrt", "cbrt", "sqr");
	public final UList<Operator> operators = raw_operators
			.map(nm -> pow_nms.contains(nm) ? Operator.lookupOperator("pow") : Operator.lookupOperator(nm)).distinct();

	//public final UList<Operator> root_only_operators = UList.mkUList1(Operator.POW);
	public final UList<Operator> root_only_operators = UList.empty();

	public final boolean using_expn_op = operators.contains(Operator.POW);

	//	public final UList<Double> pow_values = doubleList("pow_values",2.0); 
	public final UList<Double> pow_values = raw_operators.intersection(pow_nms)
			.map(nm -> nm.equals("sqrt") ? 0.5 : nm.equals("cbrt") ? 1.0 / 3.0 : nm.equals("sqr") ? 2.0 : -10000.0);

	public final UList<Operator> unary_operators = operators.filter(n -> n.arity == 1);

	{
		printf("UNOPS %s %s %s%n", operators, unary_operators, pow_values);
	}

	// e.g. P/P, which amounts to P, ignoring issues of constant ranges.
	public final boolean discard_redundant_exprs = boolVal("discard_redundant_exprs", true);

	public final UList<Operator> operators2 = operators.filter(op -> op.arity == 2)
	//				.maybeAdd(unary_operators.isEmpty() ? null : Operator.UNARY_FN)
	;

	// where to put constant in P/(P+P)?
	// default: with P
	// invert constant, so (1/C) instead of C; can be anywhere
	public final boolean invert_numerator_const = boolVal("invert_numerator_const", false);

	// put const in front: C*(Pc/(P+P))
	//...

	// put const in denom: Pc/(C*P+C*P)
	public final boolean numerator_const_in_denom = boolVal("numerator_const_in_denom", false);

	// put const in denom: Pc/(C*(P+P))
	//	public final boolean numerator_const_in_denom_in_front = boolVal("numerator_const_in_denom_in_front", false);

	//	public final UList<ExpPower> power_operator_exponents = raw_operators.filter(nm -> nm.contains("rt"))
	//			.map(nm -> nm.equals("sqrt") ? Operator.sqrt_exp : nm.equals("cbrt") ? Operator.cbrt_exp : null);

	public final UList<String> dataLines = stringList("dataLines");
	public final String dataVars = stringVal("dataVars", "");
	public final NestedList normExprSpec = treeOf("normExpr");
	public final NestedList instExprSpec = treeOf("instExpr");
	public final int normExprNum = intVal("normExprNum", 0);

	public BVar dimConst(MLInstance mli, DimUnit unit) {
		String nm = "C" + mli.nodeNo + unit.nm;
		return getBVar(nm, min_const_units, max_const_units, true);
	}

	public YamlInput(String[] args) throws YamlException, IOException {
		super(args);

		checkRegistered();

		if (raw_operators.contains("pow"))
			die("'pow' is no longer accepted as an operator");

		dump("BOP ", baronOpts);
		if (javascript_pow_hack)
			ProdVal.javascript_pow_hack = true;

		if (use_dimensional_analysis && dimensionUnits.isEmpty())
			die("Can't do use_dimensional_analysis, because no dimensionUnits were provided.");
		//		if (exp_range_low != 0 || exp_range_high != 0)
		//			myassert(exp_range_low < exp_range_high);

	}
}
