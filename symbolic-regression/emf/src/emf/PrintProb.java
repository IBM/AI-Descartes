// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.BExpr.*;
import static emf.Constraint.*;
import static emf.INode.*;
import static emf.Operator.*;
import static utils.UList.*;
import static utils.VRAUtils.*;

import java.io.PrintStream;
import java.util.Random;

import emf.BExpr.BVar;
import utils.UList;
import utils.UMap;

public class PrintProb extends PrintProb0 {
	final boolean zimpl = inpx.zimpl;
	final String extension = inpx.extension;
	final String comment = inpx.comment;

	final boolean use_max_num_consts = !nullOrZero(inpx.max_num_consts)
			&& nodeInstance.mlInstances().size() > inpx.max_num_consts && nodeInstance.numPsums() > 1;
	{
		//		printf("USE MAX NC %s %s %s%n", use_max_num_consts, nodeInstance, nodeInstance.mlInstances().size());
	}
	final NormNode normNode = node0.arithNorm();
	final NormNode node = normNode;

	//final boolean special_div_process = inpx.special_div_process && node.op == DIV && node.left().isML;
	final boolean special_div_process = inpx.special_div_process && node.op == DIV;

	final boolean div_denom_constraints = inpx.div_denom_constraints && node.op == DIV;

	final boolean div_Pinv = inpx.div_Pinv && node.op == DIV && node.right().isML;

	//	final NormNode dcNode = normNode.maybeDropConsts(inpx.max_num_consts);
	final int nleaves = node.leaves().size();

	//	final PrintNode pnode = new PrintNode(dcNode, numInVars, inpx);

	final UList<IntExpSplit> isplits = splits.filterSubType(IntExpSplit.class);

	private INode pinv(INode n) {
		myassert(n.op == DIV);
		return getINode(MUL, n.lefti(), n.righti());
	}

	final UList<ExprAndConstraints> expcs = input.mapI((i, inLine) -> {
		if (div_Pinv)
			// XX/P ==> XX*Pinv
			// seems more effective just to drop denom and allow neg exponents in all numerator Ps  
			return pinv(nodeInstance).toBaronString(inpx, inpx.invarMap(inLine.invals),
					isplits.groupingBy(sp -> sp.mlsplit), use_max_num_consts);

		ExprAndConstraints eac = nodeInstance.toBaronString(inpx, inpx.invarMap(inLine.invals),
				isplits.groupingBy(sp -> sp.mlsplit), use_max_num_consts);
		//		printf("EAC %s %s%n", node, eac.expr);
		//		if (inpx.drop_all_constraints)
		//		eac = new ExprAndConstraints(eac.expr);
		if (inpx.diff_in_obj)
			if (inpx.diff_in_obj_y_sub_fx)
				// y-f(x): f(x) <= y				
				return eac.add(getLeConstraint("pos" + i, eac.expr, inLine.outval));
			else
				// f(x)-y: f(x) >= y
				return eac.add(getGeConstraint("neg" + i, eac.expr, inLine.outval));
		return eac;
	});

	UList<Constraint> dimcnsts = !inpx.use_dimensional_analysis ? UList.empty()
			: nodeInstance.dimAnal(inpx, use_max_num_consts);

	final UList<MLInstance> multiLeaves = nodeInstance.mlInstances();

	final UList<MLInstance> multiLeavesFixedV0 = isplits.map(sp -> sp.mlsplit).distinct();

	final UList<MLInstance> multiLeavesFixed_minmax = multiLeaves
			.filter(ml -> inpx.numInVars == 1 && ml.min_prod_exp == ml.max_prod_exp.orElse(-1));

	final UList<MLInstance> multiLeavesNotFixed = multiLeaves.diff(multiLeavesFixedV0).diff(multiLeavesFixed_minmax);
	final UList<MLInstance> multiLeavesNotFixedNegExps = multiLeaves.filter(x -> x.allow_neg_exps);

	final UList<MLInstance> multiLeavesNotFixedWithConst = multiLeavesNotFixed.union(multiLeavesFixed_minmax)
			.filter(x -> x.mconst.allowConst);

	final UList<ExpInstance> expInsts = nodeInstance.expInstances();
	final UList<INode> psums = nodeInstance.allSubNodes().filter(x -> x.isPsum);

	final UList<BVar> expvars = multiLeavesNotFixed.flatMap(x -> x.varexps);

	//	final UList<MultiNode> multiLeavesC = multiLeaves.filter(mln -> mln.allowConst);
	//
	//	final UList<MultiNode> multiLeavesML = multiLeaves.filter(x -> !x.is1Var);
	//	final UList<MultiNode> multiLeavesDiv = multiLeaves.filter(x -> x.isDivMN);
	//	final UList<MultiNode> multiLeavesNonDiv = multiLeaves.filter(x -> !x.isDivMN);
	//
	//	final boolean has1vars = !multiLeaves.filter(x -> x.is1Var).isEmpty();
	//	final boolean hasML_vars = !multiLeaves.filter(x -> !x.is1Var).isEmpty();

	final UList<Integer> inputSeq = seqTo(input.size());
	//	final UList<Integer> ivarSeq = seqTo(numInVars);

	//	public PrintProb(Input2 inpx2, NormNode node, UList<Split> splits, int redo, PrintStream out) {
	//		this(inpx2, node, node.mkinstance(inpx2.inpx), splits, redo, out);
	//	}

	public PrintProb(Input2 inpx2, NormNode node, INode nodeInstance, UList<Split> splits, int redo, PrintStream out) {
		super(inpx2, node, nodeInstance, splits, redo, out);
	}

	public void print(String resout, String timout, double ubound_squared) {
		final boolean printBound = (ubound_squared < inpx.infinity);

		//		printf("USE %s%n", inpx.use_dimensional_analysis);
		out.printf("%s NODE %s %s (%s) %s%n", comment, node.complexity, nleaves, node, nodeInstance.toYamlString());

		//		out.printf("// %s%n", pnode.form);
		//		multiLeaves.forEach(mn -> out.printf("// %s == %s%n", mn.nodeNm, mn.form));
		out.printf("//use_max_num_consts %s%n", use_max_num_consts);
		out.println();

		if (!zimpl) {
			out.printf("OPTIONS {%n");
			//out.printf(" CutOff: 1.0;%n");
			out.printf(" ResName: \"%s\";%n", resout);
			out.printf(" TimName: \"%s\";%n", timout);
			if (!inpx.baron_license.isEmpty())
				out.printf(" LicName: \"%s\";%n", inpx.baron_license);

			//out.printf(" MaxTime: %s;%n", (int) inpx.max_time);
			inpx.baronOpts.forEach(x -> out.printf("  %s%n", x));
			out.printf("}%n%n");
		}

		//printf("// %s %n", multiLeaves);

		{
			//			UList<String> mlints = multiLeavesNotFixed.flatMap(ml -> ml.exp_inms);
			UList<BVar> newvars = expcs.first().newvars().concat(dimcnsts.flatMap(x -> x.newvars)).concat(expvars)
					.distinct();
			//			printf("NVS %s %s%n", node, newvars);

			// NOTE: if you print INTEGER_VARIABLES after VARIABLES, the int vars will silently be treated as continuous!
			//			if (multiLeavesNotFixed.isNotEmpty())
			printBinaries(newvars.filter(x -> x.binary()));
			printIntegers(newvars.filter(x -> x.integral && !x.binary()).map(z -> z.nm));
			printContinuous(newvars.filter(x -> !x.integral).map(x -> x.nm));
		}

		class CBounds {
			public final MLInstance mlinst;
			public final double lbound;
			public final double ubound;

			public CBounds(MLInstance mlinst, double lbound, double ubound) {
				super();
				myassert(lbound <= ubound);
				this.mlinst = mlinst;
				this.lbound = lbound;
				this.ubound = ubound;
			}

		}

		//		class ExpBounds {
		//			public final MLInstance mlinst;
		//			public final int lbound;
		//			public final int ubound;
		//
		//			public ExpBounds(MLInstance mlinst, int lbound, int ubound) {
		//				super();
		//				myassert(lbound <= ubound);
		//				this.mlinst = mlinst;
		//				this.lbound = lbound;
		//				this.ubound = ubound;
		//			}
		//
		//		}
		if (!zimpl) {
			UList<ConstSplit> csplits = splits.filterSubType(ConstSplit.class);
			UMap<MLInstance, ConstSplit> cspmap = csplits.mkrevmap(csp -> csp.mlsplit);
			//			final double min_const_val = inpx.const_in_exp ? Math.log(inpx.min_const_val) : inpx.min_const_val;
			//			final double max_const_val = inpx.const_in_exp ? Math.log(inpx.max_const_val) : inpx.max_const_val;
			UList<CBounds> cbounds = multiLeavesNotFixedWithConst.map(ml -> {
				if (cspmap.containsKey(ml))
					die();
				//					return cspmap.get(ml).splitPositive ? new CBounds(ml, 0, inpx.max_const_val)
				//							: new CBounds(ml, inpx.min_const_val, 0);

				if (special_div_process && ml == nodeInstance.nodes.get(0).thisAsMLI) {
					die(); // ubnd
					// have to keep the const from going to 0
					return new CBounds(ml, 1.0, inpx.max_const_val);
				}
				return new CBounds(ml, ml.mconst.cnst_lbnd, ml.mconst.cnst_ubnd);
			});

			if (multiLeavesNotFixed.isNotEmpty()) {
				MLInstance divP = div_Pinv ? nodeInstance.righti().thisAsMLI : null;
				final int mve = inpx.max_var_exp;
				out.printf("%nLOWER_BOUNDS {%n");
				int negmve = -mve;
				//				if (inpx.numInVars == 1) {
				//					multiLeavesNotFixed.forEach(ml -> ml.exp_inms.forEach(
				//							nm -> out.printf("  %s: %s;%n", nm, ml.allow_neg_exps ? negmve : ml.min_prod_exp)));
				//				} else {
				//					multiLeavesNotFixed.forEach(ml -> ml.exp_inms
				//							.forEach(nm -> out.printf("  %s: %s;%n", nm, ml.allow_neg_exps ? negmve : 0)));
				//				}
				UList<BVar> cvars = multiLeavesNotFixedWithConst.map(x -> x.c);
				UList<BVar> newvars = expcs.first().newvars().concat(expvars).concat(cvars).distinct()
						.filter(x -> !x.binary());
				newvars.forEach(x -> out.printf("  %s: %s;%n", x.nm, x.lbound));

				//				cbounds.forEach(cbnd -> out.printf("  %s: %s;%n", cbnd.mlinst.cnm, cbnd.lbound));

				expInsts.forEach(x -> out.printf("  %s: %s;%n", x.nm, 0));
				out.printf("}%n");

				out.printf("%nUPPER_BOUNDS {%n");
				//				if (inpx.numInVars == 1) {
				//					multiLeavesNotFixed.forEach(ml -> ml.exp_inms
				//							.forEach(nm -> out.printf("  %s: %s;%n", nm, ml.allow_neg_exps ? mve : inpx.max_var_exp // ml.max_prod_exp
				//					)));
				//				} else {
				//
				//					multiLeavesNotFixed.forEachEx(ml -> ml.exp_inms.forEach(nm -> out.printf("  %s: %s;%n", nm, mve)));
				//				}

				newvars.forEach(x -> out.printf("  %s: %s;%n", x.nm, x.ubound));

				//				cbounds.forEach(cbnd -> out.printf("  %s: %s;%n", cbnd.mlinst.cnm, cbnd.ubound));

				expInsts.forEach(x -> out.printf("  %s: %s;%n", x.nm, inpx.expn_ubound));

				out.printf("}%n");
			}
		}

		out.println();
		UList<Constraint> eqs = printEqs().distinct();
		eqs.map(x -> x.nm).checkDistinct("These constraints have the same name, but differ.");
		if (eqs.isNotEmpty() || inpx.max_error >= 0) {
			out.printf("EQUATIONS %s", eqs.map(x -> x.nm).join(","));
			if (inpx.max_error >= 0) {
				if (eqs.isNotEmpty())
					out.printf("  ,");
				out.printf("  max_error%n");
			}
			out.println(";");
			out.println();

			eqs.forEach(x -> out.printf("%s: %s;%n", x.nm, x.eq));

			if (inpx.max_error >= 0) {
				out.printf("max_error:%n");
				printObj(); // JobPool ASSUMES THAT THE LAST LINE CONTAINS 'OBJ:'; if you change this, change that
				out.printf(" <= %s;%n", inpx.max_error);
			}

			out.println();
		}

		{
			{
				if (div_denom_constraints) {
					die();
					//					out.print(comma);
					out.print(inputSeq.map(i -> "denom" + i).join(","));
				}

				//			if (printBound)
				//				out.printf("  BND,");
				//			out.printf("  %s;%n", inputSeq.map(li -> "diff_" + li).join(","));
				//				out.printf(";%n");
				out.println();
			}

			out.println();
			out.println();
		}

		// constraints slow down baron
		//		if (0.0 < ubound_squared && ubound_squared < inpx.infinity)
		//			out.printf("BND: %s <= %s;%n%n", objective, ubound_squared);

		if (inpx.constraint_instead_of_objective)

		{
			out.printf("OBJ: minimize 0;%n");
		} else {
			out.printf("OBJ: minimize%n");
			printObj(); // JobPool ASSUMES THAT THE LAST LINE CONTAINS 'OBJ:'; if you change this, change that
			out.printf(";%n");
		}
	}

	private void printListComma(UList<String> ss) {
		if (ss.isNotEmpty()) {
			out.printf(" ");
			ss.forEach(s -> out.printf("%s,", s));
			out.println();
		}
	}

	private void printBinaries(UList<BVar> vs) {
		if (zimpl) {
			die();
			//			multiLeaves.forEach(ml -> out.printf(" var %s binary;%n", ml.use_cnm));
			return;
		}

		//if (use_max_num_consts) {
		if (vs.isNotEmpty()) {
			out.printf("BINARY_VARIABLES %n");

			// quick hack as an experiment - was worse
			//			if (inpx.all_var_combs)
			//				rangeClosed(0, 3).forEach(mni -> rangeClosed(0, 3).forEach(vi -> out.printf(" use%s_%s,%n", mni, vi)));

			//			if (!inpx.drop_usex)
			//				multiLeaves.forEach(ml -> out.printf(" %s,%n", ml.use_inms_map.values().join(",")));

			//			out.printf("  %s,%n", multiLeaves.map(ml -> "choose_c" + ml.nodeNo).join(","));
			//			out.printf("  %s;%n", multiLeaves.map(ml -> ml.use_cnm()).join(","));
			out.printf("  %s;%n", vs.join(","));
		}
	}

	private void printIntegers(UList<String> mlints) {
		UList<String> expnints = !inpx.expn_integral ? UList.empty() : expInsts.map(x -> x.nm);
		UList<String> nms = mlints.concat(expnints);

		if (nms.isNotEmpty())
			out.printf("%nINTEGER_VARIABLES %s;%n", nms.distinct().join(","));
	}

	private void printContinuous(UList<String> mlints) {
		if (zimpl) {
			multiLeavesNotFixedWithConst.forEach(ml -> out.printf(" var %s ;%n", ml.cnm));
			inputSeq.forEach(li -> out.printf(" var diff_%s ;%n", li));
		} else {
			myassert(!inpx.use_special_zero_tol);
			//			if (inpx.use_special_zero_tol)
			//				out.printf("  diff_z,%n");

			//			multiLeaves.forEach(ml -> out.printf(" %s,%n", ml.exp_inms_map.values().join(",")));
			UList<String> mlvs = multiLeavesNotFixedWithConst.map(ml -> ml.cnm);

			UList<String> expnints = inpx.expn_integral ? UList.empty() : expInsts.map(x -> x.nm);

			UList<String> all = concatULists1(mlvs, expnints, mlints);
			if (all.isNotEmpty())
				out.printf("%nVARIABLES %s;%n", all.distinct().join(","));
		}
	}

	private UList<Constraint> printEqs() {
		String subto = zimpl ? "subto " : "";

		myassert(!inpx.use_special_zero_tol);
		if (inpx.use_special_zero_tol) {
			die();
			//			out.printf("// use_special_zero_tol: value of function at 0 must be 0%n");
			//			out.printf("%sdiff_z_def: diff_z - (%s - %s) == 0;%n", subto, 0.0,
			//					node.toBaronString(inpx.invarMap(nCopies(inpx.numInVars, 0.0))));
			//			out.printf("diff_z:  diff_z*diff_z <= %s; // %s ^ 2%n", inpx.special_zero_tol * inpx.special_zero_tol,
			//					inpx.special_zero_tol);
			//			out.println();
		}

		UList<Constraint> max_num_consts_constraints = use_max_num_consts ? mkUList1(getLeConstraint("max_num_consts",
				addAll(multiLeavesNotFixedWithConst.map(ml -> ml.use_c)), inpx.max_num_consts)) : UList.empty();

		UList<Constraint> denom_constraints = UList.empty();
		if (inpx.C0_C1) {
			myassert(!div_denom_constraints);
			die();
			//			denom_constraints = mkUList1(getConstraint("foo1", "C1 - C0 >= 0"));

		}
		if (div_denom_constraints) {
			denom_constraints = input.mapI((i, inLine) -> {
				INode denom = nodeInstance.nodes.get(1);

				//				out.printf("denom%s: ", i);
				ExprAndConstraints denomec = denom.toBaronString(inpx, inpx.invarMap(inLine.invals),
						isplits.groupingBy(sp -> sp.mlsplit), use_max_num_consts);
				die();
				//				return getGeConstraint("denom" + i, denomec.expr + "^2 >= 0.001");
				return null;

				//				out.printf(" %s^2 >= 0.001;%n", denomec.expr);
			});
		}

		if (inpx.max_error >= 0)
			out.printf("%smax_error:  %s <= %s;%n", subto,
					inputSeq.map(i -> String.format("diff_%s*diff_%s", i, i)).join("+"),
					inpx.max_error * inpx.max_error);

		UList<Constraint> posderive = UList.empty();
		if (inpx.positive_deriviative_constraints) {
			posderive = rangeOpen(1, input.size()).map(i -> {
				BExpr thisval = expcs.get(i).expr;
				BExpr prevval = expcs.get(i - 1).expr;
				return getGeConstraint("posderiv" + i, thisval, prevval);
			});
		}

		// all expcs constraints are same
		return concatULists1(max_num_consts_constraints, expcs.first().constraints, denom_constraints, posderive,
				// drop 0 == 0 etc, at least one char must appear
				dimcnsts).filter(x -> x.eq.matches(".*\\p{Alpha}.*"));
	}

	private void printObj() {
		//				if (!inpx.drop_all_constraints) 
		//		{
		//			expcs.first().constraints.forEach(cnst -> out.printf("%s: %s;%n", cnst.nm, cnst.eq));
		//			out.println();
		//		}

		if (zimpl) {
			//out.printf("minimize nothing: (");
			return;
		} else
		//		if (!inpx.minimize_error)
		//			out.printf("1");
		//		else

		if (special_div_process) {
			printf("IGNORING ANY CONSTRAINTS ON VARS!%n");
		}

		if (false) {
			// doesn't work
			// don't actually have code that prints out vars instead of consts as base of exps
			// hand edited, used cpp instead of m4
			out.printf("//define(diff(%s,%s) ", inpx.outvarnm, inpx.invarnms.join(","));
			out.printf("(%s - %s)^2", inpx.outvarnm, nodeInstance.toString());
			out.println();

			//			for (int i = 0; i < input.size(); ++i) {
			//				DataLine inLine = input.get(i);
			//				double outvar = inLine.outval;
			//				String nval = expcs.get(i).expr;
			//				out.printf("//define(diff_%s, diff(%s, %s))%n", i, outvar, inLine.invals.join(","));
			//			}
			out.printf("//");
			for (int i = 0; i < input.size(); ++i)
				out.printf("diff_%s+", i);
			out.printf("0");
			out.println();
		}

		//for (int i = 0; i < input.size(); ++i) {
		UList<Integer> rng = rangeOpen(input.size());
		if (redo > 0)
			rng = rng.shuffle(new Random(redo));
		for (int i : rng) {
			DataLine inLine = input.get(i);

			double outvar = inLine.outval;
			BExpr nval = expcs.get(i).expr;

			// this ends up with LBs < 0
			//out.printf(" (%s - %s*%s + %s*%s) +%n", outvar * outvar, 2 * outvar, nval, nval, nval);

			if (inpx.multiply_out_obj) {
				notnow(!inpx.relative_squared_error);
				//				out.printf(" %s - %s*%s +%n    %s*%s +%n%n", outvar * outvar, 1.88 * outvar, nval, nval, nval);
				//out.printf(" %s - %s*%s +%n    %s*%s +%n%n", outvar * outvar, 1.89 * outvar, nval, nval, nval);
				out.printf(" %s - %s*%s +%n    %s*%s +%n%n", outvar * outvar, 2.0 * outvar, nval, nval, nval);

				//				out.printf(" %s - %s*%s +%n    %s*%s +%n%n", outvar * outvar, outvar, nval, nval, nval);
			} else if (special_div_process) {
				notnow(!inpx.relative_squared_error);

				INode num = nodeInstance.nodes.get(0);
				INode denom = nodeInstance.nodes.get(1);
				ExprAndConstraints numec = num.toBaronString(inpx, inpx.invarMap(inLine.invals),
						isplits.groupingBy(sp -> sp.mlsplit), use_max_num_consts);
				ExprAndConstraints denomec = denom.toBaronString(inpx, inpx.invarMap(inLine.invals),
						isplits.groupingBy(sp -> sp.mlsplit), use_max_num_consts);
				//				myassert(numec.constraints.isEmpty());
				//				myassert(denomec.constraints.isEmpty(), denomec.constraints);

				die();
				//				String numval = numec.expr;
				//				String denomval = denomec.expr;

				// y==n/d ==> y*d == n
				// just ignore scaling issues....
				die();
				//				out.printf(" (%s*%s - %s) *%n", outvar, denomval, numval);
				//				out.printf(" (%s*%s - %s) +%n", outvar, denomval, numval);

				out.println();

			} else if (inpx.diff_in_obj) {
				notnow(!inpx.relative_squared_error);

				if (inpx.diff_in_obj_y_sub_fx)
					// y-f(x): f(x) <= y
					out.printf(" %s - %s +%n", outvar, nval);
				else
					// f(x)-y: f(x) >= y
					out.printf(" -%s + %s +%n", outvar, nval);

				// this seems faster...
			} else if (!inpx.square_diffs_in_obj) {
				notnow(!inpx.relative_squared_error);
				out.printf(" (%s - %s) *%n (%s - %s) +%n", outvar, nval, outvar, nval);
			} else
			// ...than this
			// or maybe not...
			if (inpx.relative_squared_error)
				out.printf(" (1- %s*(%s))^2 +%n", 1.0 / outvar, nval);
			else
				// do it this way to avoid wrapping our expr in parens
				out.printf(" (%s + %s)^2 +%n", -outvar, nval);
		}
		out.printf(" 0");

		// sqrt is slow

	}
}
