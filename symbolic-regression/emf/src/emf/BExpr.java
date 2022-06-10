// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.Operator.*;
import static utils.UList.*;
import static utils.UList.sumi;
import static utils.VRAUtils.*;

import java.util.LinkedHashMap;

import utils.UList;

public class BExpr {
	public final boolean isVar;
	public final BVar bvar;
	public final Operator op;
	public final ExpInstance expinst;
	public final UList<BExpr> nodes;
	private final int prec;

	private BExpr() {
		this.bvar = (BVar) this;
		this.isVar = (bvar != null);
		this.op = null;
		this.nodes = null;
		this.expinst = null;

		this.prec = 20;
		inv();
	}

	private BExpr(Operator op, ExpInstance expinst, UList<BExpr> nodes) {
		this.bvar = null;
		this.isVar = (bvar != null);
		this.op = op;
		this.nodes = nodes;
		this.expinst = expinst;

		if (op == INTERNAL_EXP)
			myassert(nodes.size() == 1);

		// ????
		if (op.arity == 0)
			this.prec = 20;
		else if (op == MUL || op == DIV)
			this.prec = 10;
		else if (op == SUB || op == ADD)
			this.prec = 5;
		else if (op == POW || op == INTERNAL_EXP)
			this.prec = 20; // same as lits/vars
		else {
			die();
			this.prec = 0;
		}

		inv();
	}

	private void inv() {
		myassert(isVar == (op == null));
		myassert(isVar == (nodes == null));
		myassert((op == POW) == (expinst != null));
		if (op == SUB)
			myassert(nodes.size() == 2);
	}

	private int numAllSubnodes() {
		if (nodes == null)
			return 0;
		return sumi(nodes.map(x -> x.numAllSubnodes()));
	}

	public static class BVar extends BExpr {
		public final String nm;
		public final int lbound;
		public final int ubound;
		public final boolean integral;

		public final boolean binary() {
			return integral && lbound == 0 && ubound == 1;
		}

		public String toString() {
			//		die();
			//			String.format("CNST(%s,%s,%s,%s)", nm, lbound, ubound, integral);
			return nm;
		};

		private BVar(String nm, int lbound, int ubound, boolean integral) {
			super();
			this.nm = nm;
			//		if (nm.startsWith("exp") && !integral)
			//			die();

			this.lbound = lbound;
			this.ubound = ubound;
			this.integral = integral;
			if (integral)
				FORNOW(ubound >= 0, this); // see ABS_VALUE
		}
	}

	public String toString() {
		myassert(!isVar);
		return toStringAux(0);
	}

	private String maybeParens(int inprec, String rval) {

		//if (inprec > this.prec)
		// IS BETTER, but problems with x - (y+z)
		if (inprec >= this.prec)
			return parens(rval);
		return rval;
	}

	public String toStringAux(int inprec) {
		if (isVar)
			return bvar.nm;
		if (op.arity == 0)
			return op.toString();

		if (op.arity == 2) {
			String sep = " " + op.name + " ";
			if (op.addOrMul) {
				BExpr left = nodes.get(0);
				BExpr right = nodes.get(1);
				if (op == MUL && left.isConst() && nodes.size() == 2)
					if (left == negONE)
						return "-" + right.toStringAux(this.prec);
					else
						sep = op.name;
			}
			return maybeParens(inprec, nodes.map(x -> x.toStringAux(this.prec)).join(sep));
		}

		myassert(nodes.size() == 1, op, nodes);
		if (op == POW) {
			if (expinst != null)
				return maybeParens(inprec, nodes.get(0).toStringAux(this.prec)) + "^" + expinst.powexp;
			else
				die();
		}

		myassert(op == INTERNAL_EXP);
		return op + parens(nodes.get(0).toString());
	}

	private final static LinkedHashMap<UList<Object>, BVar> bv_univ = new LinkedHashMap<>();

	// boolean
	public static BVar getBVar(String nm) {
		return getBVar(nm, 0, 1, true);
	}

	public static BVar getBVar(String nm, int lbound, int ubound, boolean integral) {
		return bv_univ.computeIfAbsent(mkUList1(nm, lbound, ubound, integral),
				lst -> new BVar(nm, lbound, ubound, integral));
	}

	private final static LinkedHashMap<UList<Object>, BExpr> univ = new LinkedHashMap<>();

	private boolean isLeaf() {
		return isVar || op.arity == 0;
	}

	private boolean isConst() {
		return isVar ? false : op.arity == 0;
	}

	private boolean isNegConst() {
		return isConst() && op.isNegOp();
	}

	private BExpr negateConst() {
		myassert(isNegConst());
		return getBExpr(op.negateOp(), UList.empty());
	}

	private static BExpr getBExpr(Operator op, ExpInstance expinst, UList<BExpr> nodes0) {
		myassert(op != IS_LEAF);

		UList<BExpr> nodes1 = op == MUL ? nodes0.filter(x -> x != eONE)
				: op == ADD ? nodes0.filter(x -> x != eZERO) : nodes0;
		if (nodes1.isEmpty())
			if (op == MUL)
				return eONE;
			else if (op == ADD)
				return eZERO;
		if (expinst != null)
			myassert(op == POW);
		else
			myassert(op != POW);

		//		UList<BExpr> cnst_nodes = nodes1.filter(x -> x.isConst());
		//		double vl = op == ADD ? sumd(cnst_nodes.map(x -> x.cnst_val()))
		//				: op == MUL ? muld(cnst_nodes.map(x -> x.cnst_val())) : 0;
		UList<BExpr> nodes = op.addOrMul ? nodes1.filter(x -> x.isConst()).concat(nodes1.filter(x -> !x.isConst()))
				: nodes1;

		if (op.addOrMul) {
			BExpr first = nodes.first();

			if (nodes.size() == 1 && op.addOrMul)
				return first;

			if (op == MUL && first == eONE)
				return getBExpr(op, nodes.rest());

			if (op == ADD && first == eZERO)
				return getBExpr(op, nodes.rest());
		}
		//		if (op == SUB && nodes.get(1).isNeg())
		//			return getBExpr(ADD, nodes.first(), nodes.get(1).negReduce());

		return univ.computeIfAbsent(mkUList1(expinst == null ? op : expinst, nodes),
				lst -> new BExpr(op, expinst, nodes));
	}

	public UList<BExpr> cons(BExpr x) {
		return mkUList1(this, x);
	}

	public ExprAndConstraints eac(UList<Constraint> cs) {
		return ExprAndConstraints.getExprAndConstraints(this, cs);
	}

	public ExprAndConstraints eac(Constraint c) {
		return ExprAndConstraints.getExprAndConstraints(this, c);
	}

	public static BExpr getBExpr(Operator op, UList<BExpr> nodes) {
		return getBExpr(op, null, nodes);
	}

	public static BExpr getBExpr(Operator op, BExpr x, BExpr y) {
		return getBExpr(op, x.cons(y));
	}

	public static BExpr getBExpr(Operator op, BExpr node) {
		return getBExpr(op, mkUList1(node));
	}

	public static BExpr getBExpr(ExpInstance expinst, BExpr node) {
		return getBExpr(POW, expinst, mkUList1(node));
	}

	public static BExpr getBExpr(Operator op) {
		myassert(op.isLit);
		return getBExpr(op, UList.empty());
	}

	public static BExpr getBExpr(int val) {
		return getBExpr(getConst(val), UList.empty());
	}

	public static BExpr getBExpr(double val) {
		if (val == (int) val)
			return getBExpr((int) val);
		return getBExpr(getConst(val), UList.empty());
	}

	public UList<BVar> bvars() {
		if (isVar)
			return mkUList1(bvar);
		else if (op.isLit)
			return UList.empty();
		else
			return nodes.flatMap(x -> x.bvars());
	}

	private BExpr op_in(Operator op, UList<BExpr> xs) {
		return getBExpr(op, mkUList1(this).concat(xs));
	}

	private BExpr op_in(Operator op, BExpr x) {
		return op_in(op, mkUList1(x));
	}

	private BExpr op_in(Operator op, int x) {
		return op_in(op, mkUList1(getBExpr(x)));
	}

	private BExpr op_in(Operator op, double x) {
		return op_in(op, mkUList1(getBExpr(x)));
	}

	public static BExpr eONE = getBExpr(1);
	public static BExpr negONE = getBExpr(-1);
	public static BExpr eZERO = getBExpr(0);
	public static BExpr eTWO = getBExpr(2);

	public BExpr mul(UList<BExpr> xs) {
		return op_in(MUL, xs);
	}

	public BExpr mul(BExpr x) {
		return op_in(MUL, x);
	}

	public BExpr mul(int x) {
		return op_in(MUL, x);
	}

	public BExpr mul(double x) {
		return op_in(MUL, x);
	}

	public BExpr add(BExpr x) {
		return op_in(ADD, x);
	}

	public BExpr add(int x) {
		return op_in(ADD, x);
	}

	public BExpr sub(BExpr x) {
		return op_in(SUB, x);
	}

	public BExpr sub(int x) {
		return op_in(SUB, x);
	}

	public static <T extends BExpr> BExpr addAll(UList<T> xs) {
		return getBExpr(ADD, conv(xs));
	}

	public BExpr negReduce() {
		BExpr n = neg();
		myassert(n.numAllSubnodes() <= numAllSubnodes());
		return n;
	}

	private boolean isNeg() {
		if (isNegConst())
			return true;
		return op == MUL && nodes.first().isNegConst();
	}

	public BExpr neg() {
		if (this.isNegConst())
			return negateConst();

		if (isNeg())
			return getBExpr(op, nodes.first().negateConst(), nodes.get(1));

		return negONE.mul(this);
	}
}
