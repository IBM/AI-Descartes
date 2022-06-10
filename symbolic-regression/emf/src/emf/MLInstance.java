// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.BExpr.*;
import static emf.Constraint.*;
import static emf.ExprAndConstraints.*;
import static emf.Operator.*;
import static utils.UList.*;
import static utils.VRAUtils.*;

import java.util.LinkedHashMap;
import java.util.Optional;

import emf.BExpr.BVar;
import utils.Pair;
import utils.UList;
import utils.UMap;

public class MLInstance implements Comparable<MLInstance> { //extends INode {
	public final int nodeNo;

	//	public final MultiNode mn;
	public final String nodeNm;

	public final Operator redop = MUL;
	public final String opx = "^";;

	public final boolean allow_neg_exps;
	public final boolean dimshifted;

	public final int min_prod_exp;
	public final Optional<Integer> max_prod_exp;

	public final MConst mconst;

	//	public final int depth;
	public final String cnm;
	public final BVar use_c;
	public final BVar c;
	public final BVar ci;

	public final UMap<InVar, BVar> varexps_map;
	public final UList<BVar> varexps;

	//	public final UMap<InVar, BVar> dsvarexps_map;
	//	public final UList<BVar> dsvarexps;

	private MLInstance(Input inpx, int nodeNo, MConst mconst, boolean neg_exps, int min_prod_exp,
			Optional<Integer> max_prod_exp) {
		this.nodeNo = nodeNo;
		//		this.mn = mn;
		this.nodeNm = "P";
		this.mconst = mconst;
		this.allow_neg_exps = neg_exps;
		this.min_prod_exp = min_prod_exp;
		this.max_prod_exp = inpx.numInVars == 1
				? Optional.of(
						max_prod_exp.isPresent() ? Math.min(max_prod_exp.get(), inpx.max_var_exp) : inpx.max_var_exp)
				: max_prod_exp;
		//		printf("MAX P: %s%n", inpx.prod_exp_limit);
		//		this.depth = depth;
		this.cnm = "C" + nodeNo;
		this.use_c = getBVar(use_cnm());

		// nuts
		myassert(isIntegral(mconst.cnst_lbnd));
		myassert(isIntegral(mconst.cnst_ubnd));
		this.c = getBVar(cnm, (int) mconst.cnst_lbnd, (int) mconst.cnst_ubnd, false);
		this.ci = getBVar("Ci" + nodeNo);

		this.dimshifted = inpx.dimensional_shift;
		if (dimshifted) {
			this.varexps_map = null;
			this.varexps = null;
			//			this.dsvarexps_map = inpx.invars.mkmap(nm -> getBVar("dse" + nodeNo + nm.nm,
			//					allow_neg_exps ? -inpx.max_var_exp : 0, inpx.max_var_exp, !inpx.relaxUSE_I));
			//			this.varexps = varexps_map.values();

		} else {
			this.varexps_map = inpx.invars.mkmap(nm -> getBVar("exp" + nodeNo + nm.nm,
					allow_neg_exps ? -inpx.max_var_exp : 0, inpx.max_var_exp, !inpx.relaxUSE_I));
			this.varexps = varexps_map.values();
			//			this.dsvarexps = null;
			//			this.dsvarexps_map = null;
		}
	}

	public String use_cnm() {
		return "USE_C" + nodeNo;
	}

	private MLInstance(MLInstance diese, MConst mconst, int min_prod_exp, Optional<Integer> max_prod_exp) {

		this.nodeNo = diese.nodeNo;
		this.nodeNm = diese.nodeNm;
		this.mconst = mconst;
		this.allow_neg_exps = diese.allow_neg_exps;
		this.min_prod_exp = min_prod_exp;
		this.max_prod_exp = max_prod_exp;

		this.cnm = diese.cnm;
		this.use_c = diese.use_c;
		this.c = diese.c;
		this.ci = diese.ci;
		this.varexps = diese.varexps;
		this.varexps_map = diese.varexps_map;

		this.dimshifted = diese.dimshifted;
	}

	// can't use Univ1, since need Input
	private static LinkedHashMap<UList<Object>, MLInstance> univ = new LinkedHashMap<>();

	public static MLInstance getMLInstance(Input inpx, int nodeNo, MConst mconst, boolean neg_exps, int min_prod_exp,
			Optional<Integer> max_prod_exp) {
		return univ.computeIfAbsent(mkUList1(nodeNo, mconst, neg_exps, min_prod_exp, max_prod_exp),
				nn -> new MLInstance(inpx, nodeNo, mconst, neg_exps, min_prod_exp, max_prod_exp));
	}

	public MLInstance getMLInstance(MConst mconst, int min_prod_exp, Optional<Integer> max_prod_exp) {
		return univ.computeIfAbsent(mkUList1(nodeNo, mconst, min_prod_exp, max_prod_exp),
				nn -> new MLInstance(this, mconst, min_prod_exp, max_prod_exp));
	}

	// just represents that node's constant
	//	public static MLInstance getMLInstanceConst(Input inpx, MLInstance mli) {
	//		myassert(!mli.allowConst);
	//		return getMLInstance(inpx, mli.nodeNo, true, false, 0, 0);
	//	}

	public String toString() {
		String p = (mconst.allowConst ? "P" : "p");
		String n = allow_neg_exps ? "n" : "";
		//return "[" + p + n + nodeNo + "," + min_prod_exp + "," + max_prod_exp + "]";
		return p + n + nodeNo + "_" + min_prod_exp + (max_prod_exp.isPresent() ? "_" + max_prod_exp.get() : "");
		//return c + (nodeNo == normNodeNo ? nodeNm : nodeNm + nodeNo);
	}

	@Override
	public int compareTo(MLInstance that) {
		myassert(this.nodeNm.equals("P"));
		myassert(mconst.allowConst);
		return chainCompare(this.nodeNo, that.nodeNo);
	}

	public String lexOrder(Input inpx) {
		int mx = inpx.max_var_exp;
		if (allow_neg_exps)
			return varexps.mapI((i, bv) -> Math.pow(2 * mx + 1, i) + "*(" + bv.nm + "+" + mx + ")").join("+");
		else
			return varexps.mapI((i, bv) -> Math.pow(mx, i) + "*" + bv.nm).join("+");
	}

	public ResForm mkres(RunResultVal result) {
		//		Input inpx = result.baronJob.inpx;
		return ResForm.getResForm(result, this);

		//		UList<String> vs_used = (inpx.drop_usex
		//				? result.var_exp_map.get(nodeNo).filterByValues(vl -> Math.abs(vl) > 1e-6)
		//				: result.use_i_map.get(nodeNo).filterByValues(vl -> vl)).keySet();
		//		UList<String> vs = vs_used.map(nm -> {
		//			double vl = result.var_exp_map.get(nodeNo).get(nm);
		//			myassert(opx.equals("^"));
		//			return vl == 1 ? nm : (int) vl == vl ? nm + opx + (int) vl : nm + opx + vl;
		//		});
		//		//			printf("mkres VS %s%n", vs);
		//		//			printf("mkres CVS0 %s %s%n", op,
		//		//					(inpx.drop_use_c || result.use_c_map.get(nodeNo) ? cons(result.c_map_strs.get(nodeNo), vs) : vs));
		//
		//		UList<String> cval = //result.c_map_strs.get(nodeNo)
		//				allowConst ? mkUList1("" + result.c_map.get(nodeNo)) : UList.empty();
		//		String cvs = (//inpx.drop_use_c || result.use_c_map.get(nodeNo) ? 
		//		cval.concat(vs)
		//		//: vs
		//		).join("*"); // ASSUMES ALL MULTNODES *
		//		//			printf("mkres CVS %s%n", cvs);
		//		//			final String invop = op == MLMUL ? "/" : "-";
		//
		//		String rval = String.format("(%s)", cvs);
		//		if (!fractional)
		//			// ProdVal apparently assumed integral exps
		//			myassertEqual(rval.trim(), new ProdVal(result, this, nodeNo).toString().trim());
		//		return rval;
	}

	private static ExprAndConstraints eac(BExpr s) {
		return getExprAndConstraints(s);
	}

	public UList<Constraint> dimAnal(Input inpx, DimUnit unit, boolean use_max_num_consts, BExpr indim,
			boolean constraintConst, BExpr expDivDim, ExpInstance expinst) {
		BExpr thisdim = addAll(inpx.invars.filter(iv -> inpx.dimensionVarsByDimUnit.get(iv).get(unit) != 0)
				.map(iv -> varexps_map.get(iv).mul(inpx.dimensionVarsByDimUnit.get(iv).get(unit))));
		boolean const_is_dimless = inpx.max_const_dim_sum == 0;
		BVar bv = inpx.dimConst(this, unit);
		boolean allowConst = mconst.allowConst;
		BExpr thisdimVal = (!allowConst || const_is_dimless ? eZERO : bv).add(thisdim);
		UList<Constraint> cs = mkUList1(getEqConstraint("dim" + nodeNo + unit, expDivDim.mul(thisdimVal), indim));
		if (allowConst && !const_is_dimless && constraintConst) {
			// if every mono has a const, there is no point, right??
			String use = use_max_num_consts ? "*" + use_cnm() : "";
			cs = cs.add(getLeConstraint(bv.nm + "_ub", bv, use_c.mul(inpx.max_const_units)));
			cs = cs.add(getGeConstraint(bv.nm + "_lb", bv, use_c.mul(inpx.min_const_units)));
			//			cs = cs.add(getConstraint(bv, bv.nm + " - " + inpx.min_const_units + "*" + use_cnm() + " >= 0"));
		}
		//		return getExprAndConstraints(thisdimVal, cs);
		return cs;
	}

	public String Cinm() {
		return "Ci" + nodeNo;
	}

	public ExprAndConstraints form(Input inpx, UMap<InVar, Double> ivars, int nodeNo, boolean hasConstantMult,
			UMap<InVar, IntExpSplit> intexpvals, ExpInstance expInstance, boolean use_max_num_consts0) {
		boolean use_max_num_consts = use_max_num_consts0 && mconst.allowConst;
		//			if (ivars.size() == 1)
		//				die("bad form: %s", this); // IMPROVE THIS CASE .... HOW?

		//			return varLineMul(ivars, nodeNo, hasConstantMult);
		myassert(hasConstantMult == mconst.allowConst);

		//final boolean exp_integral = !(inpx.using_expn_op && inpx.numInVars == 1);
		// can do a bit better with only one var, but not for now
		final boolean exp_integral = true;

		//		int ub = varexps.get(nm.pos).ubound;
		// it seems faster to NOT make these integer 

		//exp_innms
		//		boolean nonconst_cnst = inpx.nonconst_plusminus;
		//		UMap<VarExp, BVar> varexps2 = varexps.mkmap(nm -> getBVar(nm.nm, nm.lbound, nm.ubound, exp_integral));
		//		UList<Constraint> exp_kludge = mkUList1(getConstraint("fooie" + nodeNo, "1 == 0", 
		//				varexps.maybeAdd(				!hasConstantMult && nonconst_cnst, getBVar(Cinm(), 0, inpx.nonconst_sub_one_minus_one ? 1 : 2, true))));

		BVar subN = getBVar("sub" + nodeNo);

		//		printf("HAS CONST: %s %s%n", this, hasConstantMult);
		BExpr c00 = c;
		BExpr c0 = !hasConstantMult ? inpx.nonconst_str(this)
				: (/*inpx.drop_use_c*/!use_max_num_consts ? c00
						//: inpx.multiply_use_c ? "(USE_C#*C#+1-USE_C#-2*NEG_C#)*" : c00 + "*"
						:
						// use_max_num_consts
						(inpx.multiply_use_c ? use_c.mul(c).add(eONE.sub(use_c))
								: (inpx.subConstInObj ? c00.sub(eTWO.mul(subN)) : c00)));
		//		String c0 = hasConstantMult ? c00 + "*" : "";
		UList<Constraint> max_num_consts_constraints3 = (use_max_num_consts && //!inpx.drop_use_c && 
				!inpx.multiply_use_c) ? mkUList1(true, false).map(ubnd -> {
					myassert(!inpx.choose_c_sqr); // check
					int mli = nodeNo;
					//String fmt = (inpx.mulConst1based) ? "(1-C%s)" : "C%s";
					//String fmt = "(1-C#*C#)".replaceAll("#", mli + "");
					if (ubnd)
						return getLeConstraint("ubnd_" + cnm, c, use_c.mul(inpx.max_const_val).add(1).sub(subN.mul(2)));
					else
						return getGeConstraint("lbnd_" + cnm, c,
								use_c.mul(-inpx.max_const_val).add(1).sub(subN.mul(2)));
					//					return getConstraint((ubnd ? "ubnd_" : "lbnd_") + cnm,
					//							String.format(ubnd ? "(%s*%s + 1 - 2*sub%s) - %s >= 0" : "(-%s*%s + 1 - 2*sub%s) - %s <= 0",
					//									inpx.max_const_val + 10, use_cnm(), nodeNo, cnm),

					//						if (inpx.choose_c_sqr)
					//							out.printf(fmt + "*" + fmt + " >= 0;%n", mli, mli);
					//						else
					//							out.printf(fmt + " >= 0;%n", mli);
					//						if (inpx.choose_c_sqr)
					//							break;
				}) : UList.empty();
		//		printf("MXN %s%n", max_num_consts_constraints2.isNotEmpty());
		UList<Constraint> max_num_consts_constraints2 = max_num_consts_constraints3; //.concat(exp_kludge);

		String fracnm = expInstance == null ? null : expInstance.nm;
		if (fracnm != null)
			myassert(inpx.all_base_e); // haven't done the others

		if (min_prod_exp == 0 && max_prod_exp.orElse(1) == 0) {
			return getExprAndConstraints(hasConstantMult ? c00 : eONE, max_num_consts_constraints2);
		}

		if (inpx.mono_by_mul) {
			//			varexps.map(exp -> {
			//				rangeClosed(exp.lbound, exp.ubound).map(i -> {
			//					BVar bv = getBVar("exp" + nodeNo + exp.var + "_" + i);
			//					if (i>0)
			//						bv+"*"+nCopies(i,exp.var).join("+")
			//				});
			//			});
			die();
		} else if (inpx.all_var_combs) {
			die("review");
			//			if (inpx.relaxUSE_I && inpx.numInVars > 1)
			//				die("have to force all exps to be multiples");
			//			UList<OList<InVar>> allVcombs = new OList<>(ivars.keySet()).allSubsets().remove(OList.emptyOUL());
			//			String rval = "(" + c0 + "(use#_0+"
			//					+ allVcombs
			//							.mapI((seti1, nms) -> "use#_" + (1 + seti1) + "*"
			//									+ nms.map(nm -> String.format("(%s^exp#%s)", ivars.get(nm), nm)).join("*"))
			//							.join("+")
			//					+ "))";
			//			//		printf("VLA %s %s%n", nodeNo, rval);
			//			return eac(rval.replaceAll("#", "" + nodeNo));

		} else if (inpx.all_base_e) {
			myassert(inpx.drop_usex);
			//				UMap<String, Double> ivars1 = (intexpvals.isNotEmpty() ? ivars.removeAll(intexpvals.keySet()) : ivars);
			//				printf("FORM %s %s %s%n", nodeNo, ivars.keySet(), intexpvals.keySet());
			myassert(intexpvals.isEmpty()); // FOR NOW

			final boolean relaxUSE_I_add_int = use_max_num_consts && inpx.relaxUSE_I_add_int;

			// have to think about this
			//			myassert(!(use_neg_exps && relaxUSE_I_add_int));

			//			Optional<BVar> expmul = (!inpx.relaxUSE_I || inpx.numInVars == 1) ? Optional.empty()
			// arbitrary bounds to exp var
			//					: Optional.of(getBVar(relaxed_expmul(), 0, 1, false));
			//			if (expmul.isPresent())
			//				max_num_vars = max_num_vars.add(expmul.get());
			//			String expmulstr = expmul.isPresent() ? "*" + expmul.get().nm : "";
			//			BExpr rval = ivars.bimap((nm, jval) -> String.format("(%s*exp#%s)", Math.log(jval), nm// , expmulstr
			//			)).filter(x -> !x.equals("0")) // won't work anymore...
			BExpr rval = addAll(ivars.bimap((vr, jval) -> getBExpr(Math.log(jval)).mul(varexps_map.get(vr))));

			//		printf("VLA %s %s%n", nodeNo, rval);
			BExpr expon = rval; // .replaceAll("#", "" + nodeNo);
			//			if (fracnm != null)
			//				expon = "(" + expon + ")";
			BExpr rval2 = c0.mul(getBExpr(INTERNAL_EXP, //+ "exp(" + (fracnm == null ? "" : fracnm + "*") + expon + ")").replaceAll("#",
					rval));

			if (relaxUSE_I_add_int) {
				die("review");

				//				UList<Constraint> new_constraints = flatten(ivars.bimap((nm, jval) -> {
				//					int lb = varexps.get(nm.pos).lbound;
				//					int ub = varexps.get(nm.pos).ubound;
				//					//		nonconst_int_0x: (-2 + 4*USE_C0) - exp0x <= 0;
				//					//		foo0: 4*(1-USE_C0) - add0 >= 0;
				//
				//					return mkUList1(
				//							getConstraint("nonconst_int_" + nodeNo + nm,
				//									String.format("(%s + %s*USE_C#) - exp#%s <= 0", -lb, ub - lb, nm).replaceAll("#",
				//											"" + nodeNo),
				//									getBVar("addxi" + nodeNo + nm, 0, ub - lb, true)),
				//							getConstraint("nonconst_int_add" + nodeNo + nm,
				//									String.format("%s*(1-USE_C#) - addxi#%s >= 0", ub - lb, nm).replaceAll("#",
				//											"" + nodeNo)));
				//				}));
				//				die();
				//				return getExprAndConstraints(rval2, new_constraints.concat(max_num_consts_constraints2));
			} else {

				// may not use use these, can't hurt
				// assume ABS_VALUE
				UList<Pair<BVar, UList<Constraint>>> csx = varexps.filter(x -> x.lbound < 0).map(vexp -> {
					BVar bv = getBVar(vexp + "_abs", 0, Math.max(vexp.ubound, -vexp.lbound), exp_integral);
					return newPair(bv,
							getGeConstraint(bv + "_ub", bv, vexp).add(getLeConstraint(bv + "_lb", bv.neg(), vexp)));
				});
				UList<Constraint> cs = csx.flatMap(x -> x.right);
				UList<BVar> ub_lb_ivars = csx.map(x -> x.left);

				//				UList<String> nms_for_bnds = allow_neg_exps ? ub_lb_ivars.map(x -> x.nm) : exp_inms;

				//				UList<Constraint> fexp_const = maybeList(
				//						(fractional && inpx.numInVars > 1 && inpx.use_dimensional_analysis), () -> {
				//							BVar bv = getBVar(expInstance.nm + "i", 0, 1, true);
				//							Constraint lb = getConstraint("lb_fint" + nodeNo,
				//									"-100(" + nms.join("+") + ") + 1 -" + bv.nm + " <= 0", bv);
				//							Constraint ub = getConstraint("ub_fint" + nodeNo,
				//									" 100(" + nms.join("+") + ") + 1 -" + bv.nm + " >= 0");
				//							//							//max_num_consts_constraints2 = max_num_consts_constraints2.add(lb).add(ub);
				//							return null;
				//							//							return mkUList(lb, ub);
				//						});
				//				printf("MXx %s %s%n", max_num_consts_constraints2.isNotEmpty(),
				//						max_num_consts_constraints2.flatMap(x -> x.newvars));

				if (max_prod_exp.orElse(0) > 0 && inpx.numInVars > 1) {
					//String lbstr = (min_prod_exp == 0 ? "" : min_prod_exp + " <= ");
					int lbstr = (min_prod_exp == 0 ? 0 : min_prod_exp);
					if (!allow_neg_exps) {
						UList<Constraint> new_constraints = mkUList1(
								getConstraint("expbnd" + nodeNo, lbstr, addAll(varexps), (int) max_prod_exp.get()));
						return getExprAndConstraints(rval2, new_constraints.concat(max_num_consts_constraints2));

					} else {
						UList<Constraint> new_constraints = cs
								.add(getConstraint("expbnd" + nodeNo, lbstr, addAll(ub_lb_ivars), max_prod_exp.get()));

						return rval2.eac(new_constraints).add(max_num_consts_constraints2);
					}
				}

				return getExprAndConstraints(rval2, max_num_consts_constraints2);
			}

		} else if (inpx.drop_usex) {
			die("review");
			//			if (inpx.relaxUSE_I && inpx.numInVars > 1)
			//				die("have to force all exps to be multiples");

			//				UMap<String, Double> ivars1 = (intexpvals.isNotEmpty() ? ivars.removeAll(intexpvals.keySet()) : ivars);
			//				printf("FORM %s %s %s%n", nodeNo, ivars.keySet(), intexpvals.keySet());
			String rval = "("
					+ c0 + ivars
							.bimap((nm, jval) -> inpx.numInVars == 1 && min_prod_exp == max_prod_exp.orElse(-1)
									? "" + ipow(jval, min_prod_exp)
									: intexpvals.containsKey(nm) ? "" + ipow(jval, intexpvals.get(nm).expval)

											//								: fractional 
											//										? (inpx.neg_exps ? String.format("(%s^(exp#%s-exp#%sn))", jval, nm, nm)
											//												: String.format("(%s^(%sexp#%s))", jval, frac_power, nm))
											//
											: String.format("(%s^exp#%s)", jval, nm))
							.filter(x -> !x.equals("1")).join("*")
					+ ")";
			//		printf("VLA %s %s%n", nodeNo, rval);
			//			return eac(rval.replaceAll("#", "" + nodeNo));
			die();

		} else if (inpx.multiply_exp2) {
			die("review");
			//			if (inpx.relaxUSE_I && inpx.numInVars > 1)
			//				die("have to force all exps to be multiples");

			String rval = "(" + c0 + ivars
					.bimap((nm, jval) -> String.format("((1-use#%s)+use#%s*(%s^exp#%s))", nm, nm, jval, nm)).join("*")
					+ ")";
			//		printf("VLA %s %s%n", nodeNo, rval);
			//			return eac(rval.replaceAll("#", "" + nodeNo));
			die();

		} else {
			die("review");
			//			if (inpx.relaxUSE_I && inpx.numInVars > 1)
			//				die("have to force all exps to be multiples");

			String exp = inpx.multiply_exp ? "use#%s*exp#%s" : "exp#%s";
			String rval = "(" + c0
					+ ivars.bimap((nm, jval) -> String.format("(%s^(" + exp + "))", jval, nm, nm)).join("*") + ")";
			//		printf("VLA %s %s%n", nodeNo, rval);
			die(); // REVIEW
			//			return eac(rval.replaceAll("#", "" + nodeNo));
		}
		die();
		return null;
	}

	public static double ipow(double x, int e) {
		myassert(e >= 0);
		if (x == 0.0)
			myassert(e > 0);
		double y = 1;
		for (int i = 0; i < e; ++i)
			y *= x;
		return y;
	}

}
