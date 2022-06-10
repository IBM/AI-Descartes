// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.BExpr.*;
import static emf.Constraint.*;
import static emf.ExprAndConstraints.*;
import static emf.Operator.*;
import static utils.UList.*;
import static utils.UList.sum;
import static utils.UList.sumi;
import static utils.VRAUtils.*;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Predicate;

import emf.BExpr.BVar;
import utils.UList;
import utils.UMap;

public class INode extends Node0DoNotUse implements Comparable<INode> {
	public final MLInstance thisAsMLI; // yuck
	public final ExpInstance expInstance; // yuck

	public final UList<INode> nodes;

	@SuppressWarnings("unchecked")
	protected <T extends Node0DoNotUse> UList<T> getNodes() {
		return (UList<T>) nodes;
	}

	public final UList<INode> oneList;

	// YUCK
	public boolean kludgeMul = false;

	protected INode(MLInstance inst) {
		super(IS_LEAF, UList.empty());

		myassert(this.op == IS_LEAF);
		myassert(this.isLeaf == true);
		myassert(op.arity == 0);
		myassert(this.isML == true);
		myassert(this.isLit == false);
		myassert(!op.isLit);

		//		myassert(this.isPsum == (op == ADD && nodes.all(n -> n.isML)));
		myassert(this.isPsumorP == (isPsum || isML));

		myassert(this.numML == 1);

		myassert(this.depth == 0);

		this.nodes = UList.empty();
		this.oneList = mkUList1(this);

		this.thisAsMLI = nonNull(inst);
		this.expInstance = null;
		invariant();
	}

	protected INode(ExpInstance inst, UList<INode> nodes) {
		super(Operator.POW, nodes);

		myassert(this.isLeaf == (op.arity == 0));
		myassert(this.isML == false);
		myassert(this.isLit == op.isLit);

		myassert(this.numML == (isML ? 1 : sum(nodes.map(n -> n.numML))));
		myassert(this.isPsum == (op == ADD && nodes.all(n -> n.isML)));
		myassert(this.isPsumorP == (isPsum || isML));

		myassert(this.depth == (op.arity == 0 ? 0 : 1 + max(nodes.map(n -> n.depth))));

		this.nodes = nodes;
		this.oneList = mkUList1(this);

		this.thisAsMLI = null;
		this.expInstance = inst;
		invariant();
	}

	protected INode(Operator op, UList<INode> nodes) {
		super(op, nodes);

		myassert(this.op == op);
		myassert(this.isLeaf == (op.arity == 0));
		myassert(this.isML == (op == IS_LEAF));
		myassert(this.isLit == op.isLit);

		myassert(this.numML == (isML ? 1 : sum(nodes.map(n -> n.numML))));
		myassert(this.isPsum == (op == ADD && nodes.all(n -> n.isML)));
		myassert(this.isPsumorP == (isPsum || isML));

		myassert(this.depth == (op.arity == 0 ? 0 : 1 + max(nodes.map(n -> n.depth))));

		if (op.addOrMul) {
			myassert(nodes.size() > 1);
			// no
			//			myassert(nodes.all(n -> n.op != op), this);
		}

		this.nodes = nodes;
		this.oneList = mkUList1(this);

		this.thisAsMLI = null;
		this.expInstance = null;
		invariant();
	}

	private void invariant() {
		myassertEqual(op == POW, expInstance != null, this);
	}

	protected String monoToString() {
		return thisAsMLI.toString();
	}

	private String mlnm() {
		myassert(isML);
		return thisAsMLI.nodeNm;
	}

	@Override
	public int compareTo(INode that) {
		//		return chainCompare(this.op, that.op, UList.chainCompare(this.nodes, that.nodes, 0));
		int cmp = chainCompare(this.op, that.op);
		if (cmp == 0) {
			if (this.isML) {
				myassert(that.isML);
				return this.mlnm().compareTo(that.mlnm());
			}
			return chainCompare(this.nodes, that.nodes);
		}
		return cmp;
	}

	public UList<ExpInstance> expInstances() {
		//return allSubNodes().filter(x -> x.op == EXPN).map(x -> x.expInstance);
		return allSubNodes().filter(x -> x.expInstance != null).map(x -> x.expInstance);
	}

	public UList<MLInstance> mlInstances() {
		return mlInstances(UList.empty());
	}

	private UList<MLInstance> mlInstances(UList<MLInstance> accum) {
		if (isML)
			return accum.add(thisAsMLI);
		if (op.arity == 0)
			return accum;

		{
			UList<MLInstance> res = accum;
			for (INode n : nodes)
				res = n.mlInstances(res);
			//			printf("ML INST %s %s%n", nodes, res);
			return res;
		}
	}

	public UList<INode> allSubNodes() {
		if (isLeaf)
			return oneList;
		return oneList.concat(nodes.flatMap(n -> n.allSubNodes()));
	}

	public UList<INode> allSubNodes(Predicate<INode> predicate) {
		return allSubNodes().filter(predicate);
	}

	public ExprAndConstraints toBaronString(Input inpx, UMap<InVar, Double> ivars,
			UMap<MLInstance, UList<IntExpSplit>> intexpvals, boolean use_max_num_consts) {
		ToBaronString tbs = new ToBaronString(inpx, ivars, intexpvals, use_max_num_consts);
		ExprAndConstraints rval = tbs.toBaronString(this);
		//		printf("MX4 %s%n", tbs.constraints.flatMap(x -> x.newvars));
		return rval.add(tbs.constraints);
	}

	private static class ToBaronString {
		private UList<Constraint> constraints = UList.empty();
		public final UMap<InVar, Double> ivars;
		public final Input inpx;

		public final UMap<MLInstance, UList<IntExpSplit>> intexpvals;
		public final boolean use_max_num_consts;

		public ToBaronString(Input inpx, UMap<InVar, Double> ivars, UMap<MLInstance, UList<IntExpSplit>> intexpvals,
				boolean use_max_num_consts) {
			this.ivars = ivars;
			this.inpx = inpx;
			this.intexpvals = intexpvals;
			this.use_max_num_consts = use_max_num_consts;
		}

		public ExprAndConstraints toBaronString(INode n) {
			if (n.isML) {
				ExprAndConstraints eac = n.thisAsMLI.form(inpx, ivars, n.thisAsMLI.nodeNo,
						n.thisAsMLI.mconst.allowConst,
						intexpvals.getOrElse(n.thisAsMLI, UList.empty()).mkrevmap(sp -> sp.var), n.expInstance,
						use_max_num_consts);
				//				printf("MX3 %s%n", eac.constraints.flatMap(x -> x.newvars));
				constraints = constraints.concat(eac.constraints);
				return eac;
			}
			Operator op = n.op;
			if (op.arity == 0)
				return getExprAndConstraints(op);
			//		if (op.isPow)
			//			//			if (exponent() == Operator.cbrt_exp)
			//			//				return "cbrt(" + base().toString() + ")";
			//			//			else if (exponent() == Operator.sqrt_exp)
			//			//				return "sqrt(" + base().toString() + ")";
			//			//			else if (exponent() == Operator.cont_exp)
			//			//				return "(" + base().toString() + ") ^ " xxx + exponent().litval();
			//			//			else
			//			//				return "(" + base().toString() + ") ^ " + exponent().litval();
			//
			//			return "(" + base().toBaronString(ivars, nodeNo) + ") ^ exp";
			if (op == DIV || op == MUL || op == ADD) {
				INode n0 = n.nodes.get(0);
				ExprAndConstraints res = toBaronString(n0);
				if (op == DIV) {
					myassert(n.nodes.size() == 2);
					res = res.op(op, toBaronString(n.nodes.get(1)));
				} else
					for (INode n1 : n.nodes.subList(1))
						res = res.op(op, toBaronString(n1));

				{
					UList<INode> prods = n.nodes.filter(x -> x.isML);
					if (!inpx.drop_usex) {
						die();
						//						for (int i = 0; i + 1 < prods.size(); ++i)
						//							for (int j = i + 1; j < prods.size(); ++j)
						//								for (InVar v : ivars.keySet()) {
						//									MLInstance mni = prods.get(i).thisAsMLI;
						//									MLInstance mnj = prods.get(j).thisAsMLI;
						//									constraints = constraints.add(getConstraint(
						//											"prod_dist_" + mni.nodeNo + "_" + mnj.nodeNo + "_" + v,
						//											mni.use_inms_map.get(v) + " + " + mnj.use_inms_map.get(v) + " <= 1"));
						//					}
					}
				}
				//				printf(">>%s %s%n", n, n.isPsum);
				if (inpx.force_lex_order && n.isPsum) {
					UList<INode> prods = n.nodes;
					die();
					//					for (int i = inpx.max_num_consts // because of ONE CONST PER PSUM; consts must be treated separately (not actually bothering with const case, probably just 1)
					//					; i + 1 < prods.size(); ++i) {
					//						int j = i + 1;
					//						MLInstance mni = prods.get(i).thisAsMLI;
					//						MLInstance mnj = prods.get(j).thisAsMLI;
					//
					//						constraints = constraints.add(getConstraint("lex_ord_" + mni.nodeNo + "_" + mnj.nodeNo,
					//								mnj.lexOrder(inpx) + " - (" + mni.lexOrder(inpx) + ") >= 0"));
					//				}
				} else {
					// don't allow two Ps with same exponents.
					// for univariate case, can just require them to be ordered.
					// say max_exp==2; then P+P+P+P will cause infeasibility, since max Ps is 3 (0,1,2).
					UList<INode> prods = n.isPsum ? n.nodes
							: n.kludgeMul ? n.nodes.map(x -> x.righti()) : UList.empty();
					//				if (false)
					//					if (/*!inpx.relaxUSE_I &&*/ inpx.numInVars == 1 && prods.isNotEmpty())
					//						for (int i = 0; i + 1 < prods.size(); ++i) {
					//							int j = i + 1;
					//							MLInstance mni = prods.get(i).thisAsMLI;
					//							MLInstance mnj = prods.get(j).thisAsMLI;
					//							InVar v = ivars.keySet().first();
					//							constraints = constraints
					//									.add(getConstraint("exp_incr_" + mni.nodeNo + "_" + mnj.nodeNo + "_" + v,
					//											mnj.exp_inms.first() + " - (" + mni.exp_inms.first() + " + 1) >= 0"));
					//						}
				}

				return res;
			}

			myassert(n.nodes.size() == 1);
			//return op + "(" + nodes.join(",") + ")";

			//			if (op == ADD1)
			//				return "(" + op + nodes.first().toBaronString(ivars, nodeNo) + ")";
			//			if (op == EXPN && n.nodes.first().isML) {
			//				INode m = n.nodes.first();
			//				return m.thisAsMLI.form(inpx, ivars, m.thisAsMLI.nodeNo, hasConstMul,
			//						intexpvals.getOrElse(m.thisAsMLI, UList.empty()).mkrevmap(sp -> sp.var), n.expInstance.nm,
			//						neg_exps);
			//			}
			ExprAndConstraints arg = toBaronString(n.nodes.first());
			if (op == POW) {
				if (n.expInstance.powIsFixed)
					return arg.pow(n.expInstance);
				else {
					die();
					//					return "exp(" + n.expInstance.nm + " * log(" + arg + "))";
				}
			}

			if (op == INTERNAL_EXP) {
				return arg.op(op);
			}
			die();
			//			return "(" + inpx.unary_operators.map(uop -> String.format("%s(%s)", uop, arg)).join(" + ") + ")";
			return null;
		}

	};

	public ResForm resultForm(RunResultVal result) {
		if (isML)
			return thisAsMLI.mkres(result);
		if (op.arity == 0) {
			die();
			//return op.name;
		}

		//			if (exponent() == Operator.cbrt_exp)
		//				return "cbrt(" + base().toString() + ")";
		//			else if (exponent() == Operator.sqrt_exp)
		//				return "sqrt(" + base().toString() + ")";
		//			else if (exponent() == Operator.cont_exp)
		//				return "(" + base().toString() + ") ^ " xxx + exponent().litval();
		//			else
		//				return "(" + base().toString() + ") ^ " + exponent().litval();

		//return "(" + nodes.map(n -> n.resultForm(result)).join(op.toString()) + ")";
		return ResForm.getResForm(result, this, nodes.map(n -> n.resultForm(result)));
	}

	public UList<MLInstance> dropConstsCands() {
		if (isML)
			return mkUList1(thisAsMLI);
		if (op.arity == 0)
			return UList.empty();

		if (op == ADD && nodes.some(x -> x.isML))
			return cons(nodes.filter(x -> x.isML).first().thisAsMLI, // only the first one
					nodes.filter(x -> !x.isML).flatMap(x -> x.dropConstsCands()));

		return nodes.flatMap(x -> x.dropConstsCands());
	}

	public INode dropConsts(UList<MLInstance> mlis) {
		if (mlis.contains(thisAsMLI)) {
			MLInstance mli = thisAsMLI;
			return getMLInstance(mli.mconst.onlyOne ? MConst.mconstOne : MConst.mconstPlusMinus, mli.min_prod_exp,
					mli.max_prod_exp);
		}
		if (op.arity == 0)
			return this;

		return getINode(nodes.map(n -> n.dropConsts(mlis)));
	}

	public INode tightedPsumBndst() {
		if (op.arity == 0)
			return this;

		if (isPsum && nodes.all(x -> !x.thisAsMLI.allow_neg_exps)) {
			// could only allow first to be plain const, but probably isn't worth adding the constraints
		}

		if (isPsum) {
			int pel = nodes.first().thisAsMLI.max_prod_exp.orElse(-1);
			int ninvars = nodes.first().thisAsMLI.varexps.size();
			if (ninvars == 1 && !nodes.all(x -> x.thisAsMLI.allow_neg_exps)
					&& nodes.all(x -> x.thisAsMLI.mconst.allowConst) && nodes.all(x -> x.thisAsMLI.min_prod_exp == 0)
					&& nodes.all(x -> x.thisAsMLI.max_prod_exp.orElse(-2) == pel)) {

				UList<INode> res = nodes.mapI((i, subn) -> {
					int lb = i == 0 ? 0 : 1;
					int ub = /*inpx.numInVars > 1 ? pel :*/ (i + 1 == nodes.size() ? pel : pel - 1);
					MLInstance subi = subn.thisAsMLI;
					return subn.getMLInstance(subi.mconst, lb, Optional.of(ub));
				});
			}
		}

		return getINode(nodes.map(n -> n.tightedPsumBndst()));
	}

	private final INode getINode(UList<INode> ns) {
		if (op == POW)
			return getExpInstance(expInstance.code, ns, expInstance.powexp);
		return getINode(op, ns);
	}

	public final INode lefti() {
		myassert(nodes.size() == 2);
		return nodes.get(0);
	}

	public final INode righti() {
		myassert(nodes.size() == 2, this);
		return nodes.get(1);
	}

	public INode simplify() {
		if (op.arity == 0)
			return this;

		if (op == MUL) {
			int addind = nodes.findFirstIndex(n -> n.op == ADD);
			if (addind >= 0)
				return getINode(ADD, nodes.get(addind).nodes.map(x -> getINode(MUL, nodes.set(addind, x)))).simplify();
			myassert(nodes.all(n -> n.isML), this); // normed?
			// eventually simplify
		} else if (op == ADD) {
			myassert(nodes.all(n -> n.isML), this); // normed?
		} else if (op == DIV) {
			myassert(!(lefti().nodes.some(n -> n.isML) && righti().nodes.some(n -> n.isML)), this);
		} else {
			die();
		}

		return getINode(op, nodes.map(n -> n.simplify()));
	}

	public double eval(UMap<MLInstance, Double> pvals, UMap<ExpInstance, Double> expn_map) {
		if (op.arity == 0)
			if (isML)
				return pvals.get(thisAsMLI);
			else {
				myassert(op == ONE);
				return 1;
			}
		if (op == MUL)
			return prodd(nodes.map(n -> n.eval(pvals, expn_map)));
		if (op == ADD)
			return sumd(nodes.map(n -> n.eval(pvals, expn_map)));
		if (op == DIV)
			return lefti().eval(pvals, expn_map) / righti().eval(pvals, expn_map);
		if (op == POW) {
			return Math.pow(nodes.get(0).eval(pvals, expn_map),
					expInstance.powIsFixed ? expInstance.powexp : expn_map.get(expInstance));
		}
		if (op == INTERNAL_EXP)
			return Math.exp(nodes.get(0).eval(pvals, expn_map));

		die();
		return 0;
	}

	UList<Constraint> dimAnal(Input inpx, boolean use_max_num_consts) {
		return inpx.dimensionUnits.flatMap(unit -> {
			DimAnal da = new DimAnal(inpx, unit, use_max_num_consts, numPsums() > 1);
			UList<Constraint> rval = da.dimAnal(this, getBExpr(inpx.outvarDimUnits.get(unit)), getBExpr(1));
			//			printf("DIM ANAL: %s %s %s %s%n", unit, this, rval.expr, rval.constraints.size());
			return rval;
		}).maybeConcat(inpx.max_const_dim_sum > 0, mlInstances().filter(x -> x.mconst.allowConst)
				// tried combining as lb <= x <= ub, was slower in that one case, doing this for now
				.flatMap(x -> trueFalse.map(isTrue -> getConstraint(x.cnm + "dimlim" + (isTrue ? "_le" : "_ge"),
						(!isTrue ? -inpx.max_const_dim_sum : Double.NEGATIVE_INFINITY),
						getBExpr(ADD, inpx.dimensionUnits.map(unit -> inpx.dimConst(x, unit))),
						(isTrue ? inpx.max_const_dim_sum : Double.POSITIVE_INFINITY)))));
		//				
	}

	public int numPsums() {
		if (op.arity == 0)
			return 1;
		if (isPsum)
			return 1;
		//		if (op == ADD || op == MUL || op == DIV)
		return sumi(nodes.map(x -> x.numPsums()));
		//		die();
		//		return 0;
	}

	private static class DimAnal {
		//		private UList<Constraint> constraints = UList.empty();
		//		private UList<BVar> intvars = UList.empty();
		//		public final UMap<InVar, Double> ivars;
		private int num = 0;
		public final Input inpx;
		public final DimUnit dv;
		//		public final UMap<MLInstance, UList<IntExpSplit>> intexpvals;
		public final boolean use_max_num_consts;
		public final boolean constrainConsts;

		public DimAnal(Input inpx, DimUnit v, boolean use_max_num_consts, boolean constrainConsts) {
			//			this.ivars = ivars;
			this.inpx = inpx;
			this.dv = v;
			//			this.intexpvals = intexpvals;
			this.use_max_num_consts = use_max_num_consts;
			this.constrainConsts = constrainConsts;
		}

		public UList<Constraint> dimAnal(INode n, BExpr indim, BExpr expDivDim) {
			if (n.isML) {
				return n.thisAsMLI.dimAnal(inpx, dv, use_max_num_consts, indim, constrainConsts, expDivDim,
						n.expInstance);
			}
			Operator op = n.op;
			if (op.arity == 0) {
				// constant -> unitless.  but wait until test case.
				die();
			}

			if (op == ADD) {
				UList<Constraint> cs = n.nodes.flatMap(sn -> dimAnal(sn, indim, expDivDim));
				//				UList<Constraint> cs0 = vals.flatMap(x -> x.constraints);
				//				UList<String> xs = vals.map(x -> x.expr);
				//				UList<Constraint> cs1 = indim.isPresent() || xs.size() < 2 ? UList.empty()
				//						: xs.rest().mapI((i, x) -> getConstraint("add_" + dv + num + "_" + i,
				//								xs.first() + " - (" + x + ") == 0"));
				//				if (cs1.isNotEmpty())
				//					num++;
				//				UList<Constraint> cs = cs0.concat(cs1);
				//				if (xs.isEmpty())
				//					return new UList<Constraint>(cs);
				//				return new UList<Constraint>(xs.min(x -> x.length()), cs);
				return cs;
			}
			if (op == MUL) {
				UList<BVar> bvs = n.nodes.mapI((i, sn) -> getBVar("damul" + num + "_" + i + dv, inpx.min_dimanal_units,
						inpx.max_dimanal_units, true));
				UList<Constraint> cs = n.nodes.flatMapI((i, sn) -> dimAnal(sn, bvs.get(i), expDivDim));
				return cs.add(getEqConstraint("mul_" + num++ + dv, indim, expDivDim.mul(addAll(bvs))));
			}
			if (op == DIV) {
				myassert(n.nodes.size() == 2);
				UList<BVar> bvs = n.nodes.mapI((i, sn) -> getBVar("dadiv" + num + "_" + i + dv, inpx.min_dimanal_units,
						inpx.max_dimanal_units, true));
				UList<Constraint> cs = n.nodes.flatMapI((i, sn) -> dimAnal(sn, bvs.get(i), expDivDim));
				return cs.add(getEqConstraint("divl_" + num++ + dv, indim, expDivDim.mul(bvs.get(0).sub(bvs.get(1)))));
			}
			myassert(n.nodes.size() == 1);
			if (op == POW) {
				INode n0 = n.nodes.first();
				myassert(!n0.isML);

				if (n.expInstance.powIsFixed)
					return dimAnal(n0, indim, expDivDim.mul(getBExpr(n.expInstance.powexp)));

				die();
				//				return dimAnal(n0, indim, expDivDim.mul(n.expInstance.expnm()));
				return null;
			}

			die();
			//return op + "(" + nodes.join(",") + ")";

			//			if (op == ADD1)
			//				return "(" + op + nodes.first().toBaronString(ivars, nodeNo) + ")";
			//			if (op == EXPN && n.nodes.first().isML) {
			//				INode m = n.nodes.first();
			//				return m.thisAsMLI.form(inpx, ivars, m.thisAsMLI.nodeNo, hasConstMul,
			//						intexpvals.getOrElse(m.thisAsMLI, UList.empty()).mkrevmap(sp -> sp.var), n.expInstance.nm,
			//						neg_exps);
			//			}
			return null;
		}
	};

	//	private static LinkedHashMap<Integer, INode> leaf_univ = new LinkedHashMap<>();
	//
	//	public static INode getINode(MLInstance inst, MConst mconst) {
	//		myassert(allowConst);
	//		return leaf_univ.computeIfAbsent(inst.nodeNo, nn -> new INode(IS_LEAF, inst));
	//	}

	//private static LinkedHashMap<Pair<Operator, UList<INode>>, INode> univ = new LinkedHashMap<>();
	private static LinkedHashMap<UList<Object>, INode> univ = new LinkedHashMap<>();

	//	private static Univ2<Integer, UList<INode>, INode> exp_univ = new Univ2<>(
	//			(inst, nodes) -> new INode(ExpInstance.getExpInstance(inst), nodes));

	private static INode getINode(Operator op, UList<INode> nodes) {
		myassert(op.arity > 0);
		myassert(op != POW);
		//		return univ.computeIfAbsent(new Pair<>(op, nodes), nn -> new INode(op, nodes));
		return univ.computeIfAbsent(consx(op, nodes), nn -> new INode(op, nodes));
	}

	public static INode getINode(Operator op, INode left, INode right) {
		return getINode(op, mkUList1(left, right));
	}

	//	private static LinkedHashMap<UList<Object>, INode> mono_univ = new LinkedHashMap<>();

	private static INode getMLInstance(Input inpx, int nodeNo, MConst mconst, boolean neg_exps, int min_prod_exp,
			Optional<Integer> max_prod_exp) {
		UList<Object> key = mkUList1(nodeNo, mconst, neg_exps, min_prod_exp, max_prod_exp);
		//		printf("GIN %s%n", key);
		return univ.computeIfAbsent(key, nn -> {
			MLInstance mon = MLInstance.getMLInstance(inpx, nodeNo, mconst, neg_exps, min_prod_exp, max_prod_exp);
			//			printf(" NEW%n", key);
			return new INode(mon);
		});
	}

	private INode getMLInstance(MConst mconst, int min_prod_exp, Optional<Integer> max_prod_exp) {
		UList<Object> key = mkUList1(thisAsMLI.nodeNo, mconst, min_prod_exp, max_prod_exp);
		return univ.computeIfAbsent(key, nn -> {
			return new INode(thisAsMLI.getMLInstance(mconst, min_prod_exp, max_prod_exp));
		});
	}

	// just represents that node's constant
	private static INode getMLInstanceConst(Input inpx, INode n, double cnst_lbnd, double cnst_ubnd) {
		MLInstance mli = n.thisAsMLI;
		return getMLInstance(inpx, mli.nodeNo, MConst.getMConst(cnst_lbnd, cnst_ubnd), false, 0, Optional.of(0));
	}

	public static INode getMLInstanceFromString(Input inpx, String specString) {
		double cnst_lbnd = inpx.min_const_val;
		double cnst_ubnd = inpx.max_const_val;

		if (specString.equals("1")) {
			boolean neg_exps = false;
			int nodeNo = -2;
			int min_prod_exp = 0;
			int max_prod_exp = 0;
			return getMLInstance(inpx, nodeNo, MConst.mconstPlusMinus, neg_exps, min_prod_exp,
					Optional.of(max_prod_exp));
		}
		UList<String> spec = split(specString, "_");
		String s = spec.get(0);
		boolean allowConst = s.charAt(0) == 'P';
		if (!allowConst)
			myassert(s.charAt(0) == 'p');
		boolean neg_exps = s.contains("n");
		int i = neg_exps ? 2 : 1;
		//		int lb = s.indexOf('[');
		//		myassert(lb >= 0);
		//		myassert(s.charAt(s.length() - 1) == ']');
		//		if (!allow_neg_exps)
		//			myassert(s.charAt(1) == '[');
		int nodeNo = toInt(s.substring(i));
		int min_prod_exp = toInt(spec.get(1));
		int max_prod_exp = toInt(spec.get(2));

		die();
		//return getMLInstance(inpx, nodeNo, allowConst, neg_exps, min_prod_exp, Optional.of(max_prod_exp), cnst_lbnd,
		//		cnst_ubnd);
		return null;

		//		UList<Object> key = mkUList1(nodeNo, allowConst, neg_exps, min_prod_exp, max_prod_exp);
		//
		//		return univ.computeIfAbsent(key, nn -> {
		//			return new INode(MLInstance.getMLInstance(inpx, nodeNo, allowConst, neg_exps, min_prod_exp, max_prod_exp),
		//					inpx.fractional_mli ? ExpInstance.getExpInstance(nodeNo) : null);
		//		});
	}

	// Note:  we don't create INodes for expn(P), only for more complex objects, which are identified by letter.
	private static INode getExpInstance(char letter, UList<INode> ns, Double powexp) {
		myassert(Character.isAlphabetic(letter));
		UList<Object> key = consx(letter, consx(powexp, ns));

		return univ.computeIfAbsent(key, nn -> {
			return new INode(ExpInstance.getExpInstance(letter, powexp), ns);
		});
	}

	//	private static INode getExpInstance(ExpInstance expinst, UList<INode> ns) {
	//		UList<Object> key = mkUList1(expinst);
	//		return univ.computeIfAbsent(key, nn -> new INode(expinst, ns));
	//	}

	public static class MkInstance {
		public final Input inpx;
		private int nodeNo = 0;
		private char expLetter = 'a';

		public MkInstance(Input inpx) {
			this.inpx = inpx;
		}

		public INode mkinstanceAux(NormNode n) {
			return mkinstanceAux(n, !inpx.izero.equals(inpx.max_num_consts), inpx.neg_exps);
		}

		public INode mkinstanceAux(NormNode n, boolean hasConstMul, boolean neg_exps0) {
			boolean neg_exps = inpx.always_neg_exps || neg_exps0;

			if (n.isML)
				return getMLInstance(inpx, nodeNo++,
						hasConstMul ? inpx.mconst : inpx.nonconst_plusminus ? MConst.mconstPlusMinus : MConst.mconstOne,
						neg_exps, 0, inpx.prod_exp_limit);

			if (n.op.arity == 0)
				//				return n; // independent of input
				return getINode(n.op, UList.empty());

			//		if (op.isPow)
			//			return base().mlDepths(inpx, accum, maxDepth - 1, nodeNo);

			//			if (n.isPsum) {
			//				int maxn = inpx.max_var_exp;
			//				UList<INode> res = n.nodes.mapI((i, ignore) -> MLInstance.getMLInstance(inpx, nodeNo++, i == 0 ? 0 : 1,
			//						i + 1 == n.nodes.size() ? maxn - 1 : maxn));
			//				printf("MINP %s%n", res.map(x -> x.thisAsMLI.min_prod_exp));
			//				return getINode(n.op, res);kin
			//			}

			//			printf("ISP %s %s %s%n", n.op == DIV, n.lefti().isPsumorP, n.righti().isPsumorP);
			if (n.op == DIV && n.left().isPsumorP && n.right().isPsumorP) {
				if (n.left().isML && inpx.numerator_const_in_denom) {
					//					myassert(hasConstMul); //wait for example
					//					myassert(mconst == inpx.mconst);
					//					MConst old_mconst = mconst;
					//					if (mconst.cnst_lbnd < 0)
					//						cnst_lbnd = -1.0;
					//					myassert(cnst_ubnd >= 1.0);
					//					cnst_ubnd = 1.0;
					//					INode l = mkinstanceAux(n.left(), false, false); // no const in num
					//					myassert(inpx.max_num_consts != 0);
					//					INode cnst = getMLInstanceConst(inpx, l, 1.0, old_cnst_ubnd);
					//					INode r0 = mkinstanceAux(n.right(), hasConstMul, false);
					//					INode r = getINode(r0.op, r0.nodes.map(subn -> getINode(MUL, cnst, subn)));
					//					r.kludgeMul = true; // YUCK
					//
					//					UList<INode> res = mkUList1(l, r);
					//					this.cnst_lbnd = old_cnst_lbnd;
					//					this.cnst_ubnd = old_cnst_ubnd;
					//					return getINode(n.op, res);
					die();
				}

				//				myassert(!hasConstMul); //wait for example
				INode l = mkinstanceAux(n.left(), hasConstMul, false);
				//INode r = mkinstance(n.righti(), false, false);
				INode r = mkinstanceAux(n.right(), hasConstMul, false);

				UList<INode> res = mkUList1(l, r);
				return getINode(n.op, res);
			}

			UList<INode> res = n.nodes.mapI((i, subn) -> {
				//				printf("z %s %s%n", nodeNo, subn);
				return mkinstanceAux(subn,
						hasConstMul && (inpx.max_num_consts == null || !(n.isPsum && i >= inpx.max_num_consts)),
						neg_exps);
			});
			//			printf("yy  %s %s%n", n, res.map(z -> z.allSubNodes().filter(x -> x.isML).map(x -> x.thisAsMLI.nodeNo)));
			res.flatMap(x -> x.allSubNodes()).filter(x -> x.isML).map(x -> x.thisAsMLI.nodeNo).checkDistinct();
			if (n.op == POW) {
				myassert(!(res.size() == 1 && res.first().isML));

				if (inpx.pow_values.isNotEmpty()) {
					myassert(inpx.pow_values.size() == 1);
					return getExpInstance(expLetter++, res, inpx.pow_values.first());
				}
				//				if (res.size() == 1 && res.first().isML)
				//					return getExpInstance(ExpInstance.getExpInstance(res.first().thisAsMLI.nodeNo), res);
				//return getExpInstance(ExpInstance.getExpInstance(expLetter++), res);
				return getExpInstance(expLetter++, res, null);
			}

			//			printf("MKIr %s %s%n", res, nodeNo);
			return getINode(n.op, res);
		}
	}
}
