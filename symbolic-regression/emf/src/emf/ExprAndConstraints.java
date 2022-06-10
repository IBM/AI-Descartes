// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.BExpr.*;
import static utils.UList.*;

import java.util.LinkedHashMap;

import emf.BExpr.BVar;
import utils.UList;

public class ExprAndConstraints {
	public final BExpr expr;
	public final UList<Constraint> constraints;

	public ExprAndConstraints(BExpr expr) {
		this(expr, UList.empty());
	}

	public final UList<BVar> newvars() {
		return constraints.flatMap(x -> x.newvars);
	}

	private ExprAndConstraints(BExpr expr, UList<Constraint> constraints) {
		super();
		this.expr = expr;
		this.constraints = constraints;
	}

	private final static LinkedHashMap<UList<Object>, ExprAndConstraints> univ = new LinkedHashMap<>();

	public static ExprAndConstraints getExprAndConstraints(BExpr expr, UList<Constraint> constraints) {
		return univ.computeIfAbsent(mkUList1(expr, constraints), lst -> new ExprAndConstraints(expr, constraints));

	}

	public static ExprAndConstraints getExprAndConstraints(BExpr expr, Constraint constraints) {
		return univ.computeIfAbsent(mkUList1(expr, constraints),
				lst -> new ExprAndConstraints(expr, mkUList1(constraints)));

	}

	//	public ExprAndConstraints(String expr, UList<Constraint> constraints, UList<BVar> intvars) {
	//		super();
	//		this.expr = expr;
	//		this.constraints = constraints;
	//		this.newvars = intvars;
	//	}

	public static ExprAndConstraints getExprAndConstraints(BExpr expr) {
		return getExprAndConstraints(expr, UList.empty());
	}

	public static ExprAndConstraints getExprAndConstraints(Operator op) {
		return getExprAndConstraints(getBExpr(op), UList.empty());
	}

	public ExprAndConstraints add(Constraint cnst) {
		return getExprAndConstraints(expr, constraints.add(cnst));
	}

	public ExprAndConstraints add(UList<Constraint> cnsts) {
		return getExprAndConstraints(expr, constraints.concat(cnsts));
	}

	private ExprAndConstraints op(Operator op, UList<BExpr> nodes) {
		return getExprAndConstraints(getBExpr(op, nodes), constraints);
	}

	//	public ExprAndConstraints op(Operator op, BExpr node) {
	//		return op(op, mkUList1(node));
	//	}

	public ExprAndConstraints op(Operator op) {
		return op(op, mkUList1(expr));
	}

	public ExprAndConstraints op(Operator op, ExprAndConstraints ncs) {
		return op(op, mkUList1(expr, ncs.expr)).add(constraints);
	}

	public ExprAndConstraints pow(ExpInstance expinst) {
		return getExprAndConstraints(getBExpr(expinst, expr), constraints);
	}
}
