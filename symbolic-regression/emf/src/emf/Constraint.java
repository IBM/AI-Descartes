// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.BExpr.*;
import static utils.UList.*;

import java.util.LinkedHashMap;

import emf.BExpr.BVar;
import utils.UList;

public class Constraint {
	public final String nm;
	public final String eq;

	// lhs <= expr <= rhs
	public final double lhs;
	public final BExpr expr;
	public final double rhs;

	public final UList<BVar> newvars;

	private Constraint(String nm, double lhs, BExpr expr, double rhs) {
		this.nm = nm;
		this.lhs = lhs;
		this.expr = expr;
		this.rhs = rhs;

		if (Double.isInfinite(lhs))
			this.eq = expr + " <= " + rhs;
		else if (Double.isInfinite(rhs))
			this.eq = expr + " >= " + lhs;
		else if (lhs == rhs)
			this.eq = expr + " == " + rhs;
		else
			this.eq = lhs + " <= " + expr + " <= " + rhs;

		this.newvars = expr.bvars();
	}

	private final static LinkedHashMap<UList<Object>, Constraint> univ = new LinkedHashMap<>();

	public static Constraint getConstraint(String nm, double lhs, BExpr expr, double rhs) {
		return univ.computeIfAbsent(mkUList1(nm, lhs, expr, rhs), lst -> new Constraint(nm, lhs, expr, rhs));
	}

	private static Constraint getConstraint(String nm, double lhs, BExpr expr) {
		double rhs = Double.POSITIVE_INFINITY;
		return univ.computeIfAbsent(mkUList1(nm, lhs, expr, rhs), lst -> new Constraint(nm, lhs, expr, rhs));
	}

	private static Constraint getConstraint(String nm, BExpr expr, double rhs) {
		double lhs = Double.NEGATIVE_INFINITY;
		return univ.computeIfAbsent(mkUList1(nm, lhs, expr, rhs), lst -> new Constraint(nm, lhs, expr, rhs));
	}

	public UList<Constraint> add(Constraint x) {
		return mkUList1(this, x);
	}

	// x<=y;   0 <= y-x
	public static Constraint getLeConstraint(String nm, BExpr x, BExpr y) {
		return getConstraint(nm, 0, y.sub(x));
	}

	public static Constraint getGeConstraint(String nm, BExpr x, BExpr y) {
		return getLeConstraint(nm, y,x);
	}

	public static Constraint getEqConstraint(String nm, BExpr expr, double rhs) {
		return getConstraint(nm, rhs, expr, rhs);
	}

	public static Constraint getEqConstraint(String nm, BExpr x, BExpr y) {
		return getEqConstraint(nm, x.sub(y), 0);
	}

	public static Constraint getConstraint(BVar bv, double lhs, BExpr expr, double rhs) {
		return getConstraint(bv.nm, lhs, expr, rhs);
	}

	public static Constraint getLeConstraint(String nm, BExpr x, double y) {
		return getLeConstraint(nm, x, getBExpr(y));
	}

	public static Constraint getGeConstraint(String nm, BExpr x, double y) {
		return getGeConstraint(nm, x, getBExpr(y));
	}

}
