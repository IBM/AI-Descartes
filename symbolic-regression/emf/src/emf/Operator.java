// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.VRAUtils.*;

import java.util.LinkedHashMap;
import java.util.Optional;

import utils.GrowList;
import utils.SingleAssign;
import utils.UList;

public final class Operator implements Comparable<Operator> {
	public final String name;
	public final int arity;
	public final boolean addOrMul;
	public final boolean isLit;
	public final boolean isCommutative;

	private final static GrowList<Operator> allops = new GrowList<>();

	public final SingleAssign<Integer> complexity = new SingleAssign<>();

	private Operator(String nm, int arity) {
		//		printf("creating operator %s\n", nm);
		this.name = nm;
		this.arity = arity;
		this.addOrMul = (nm.equals("+") || nm.equals("*"));
		this.isLit = (nm.equals("1"));
		this.isCommutative = addOrMul;
		allops.add(this);
	}

	public String toString() {
		//		if (arity == 0)
		//			die();
		return name;
	}

	public boolean isNegOp() {
		return name.startsWith("-"); // HACK
	}

	public Operator negateOp() {
		myassert(isNegOp());
		String newnm = name.substring(1);
		return univ.computeIfAbsent(mkUList1(newnm), lst -> new Operator(newnm, 0));
	}

	//	public final static Operator LEAF = new Operator("L not actually used", 0);
	public final static Operator IS_LEAF = new Operator("leaf or lit", 0);
	//	public final static Operator INVLEAF = new Operator("invL not actually used", 0);

	private final static LinkedHashMap<UList<Object>, Operator> univ = new LinkedHashMap<>();

	public final static Operator getConst(int val) {
		return univ.computeIfAbsent(mkUList1(val), lst -> new Operator(val + "", 0));
	}

	public final static Operator getConst(double val) {
		myassert(val != (int) val);
		return univ.computeIfAbsent(mkUList1(val), lst -> new Operator(val + "", 0));
	}

	public final static Operator ONE = getConst(1);
	public final static Operator ZERO = getConst(0);

	//	public final static Operator POW = new Operator("pow", 2);

	//	public final static ExpPower cbrt_exp = new ExpPower(0, 1.0 / 3);
	//	public final static ExpPower sqrt_exp = new ExpPower(1, 1.0 / 2);

	//	public final static ExpPower cont_exp = new ExpPower(2);

	//	public final static UList<ExpPower> exp_inds = mkUList1(cbrt_exp, sqrt_exp);
	public final static Operator ADD1 = new Operator("1+", 1);

	public final static Operator SIN = new Operator("sin", 1);
	public final static Operator COS = new Operator("cos", 1);
	public final static Operator TAN = new Operator("tan", 1);

	public final static Operator ADD = new Operator("+", 2);
	public final static Operator SUB = new Operator("-", 2);

	public final static Operator MUL = new Operator("*", 2);
	public final static Operator DIV = new Operator("/", 2);

	//	public final static Operator LN = new Operator("ln", 1);
	public final static Operator POW = new Operator("pow", 1);

	// implements monos
	public final static Operator INTERNAL_EXP = new Operator("exp", 1);

	public final static Operator lookupOperator(String nm) {
		Optional<Operator> x = allops.elems().findFirst(y -> nm.equals(y.name));

		//		if (nm.equals("+"))
		//			return ADD;
		//		if (nm.equals("*"))
		//			return MUL;
		//		if (nm.equals("/"))
		//			return DIV;
		//		//		if (nm.equals("1+"))
		//		//			return ADD1;
		//		if (nm.equals("ln"))
		//			return LN;
		//		if (nm.equals("expn"))
		//			return EXPN;
		//		//		if (nm.equals("sin"))
		//		//			return SIN;
		//
		//		// these are handled differently, don't call this routine with root names
		//		if (nm.equals("cbrt"))
		//			die();
		//		if (nm.equals("sqrt"))
		//			return SQRT;
		//
		if (!x.isPresent())
			die("SORRY, operator '%s' is not implemented.%n", nm);
		return x.get();
		//		die();
		//		return null;
	}
	//	public final static Operator DIV = new Operator("/", 2);

	@Override
	public int compareTo(Operator x) {
		return chainCompare(arity, x.arity, name, x.name);
	}
}
