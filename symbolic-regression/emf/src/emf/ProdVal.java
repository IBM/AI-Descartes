// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.UList.sumi;
import static utils.VRAUtils.*;

import java.io.PrintStream;
import java.util.LinkedHashMap;

import utils.UList;

public class ProdVal {
	public final double cval;
	public final double raw_cval;
	public final UList<Double> varexps;
	// can't use InVar, used before InVars created
	public final UList<String> varnms;

	public static boolean raw_hack;

	private ProdVal(double cval, double raw_cval, UList<Double> varexps, UList<String> varnms) {
		this.cval = cval;
		this.raw_cval = raw_cval;
		this.varexps = varexps;
		this.varnms = varnms;
		myassert(varexps.size() == varnms.size(), varexps, varnms);
	}

	private ProdVal(double cval, UList<Double> varexps, UList<String> varnms) {
		this(cval, cval, varexps, varnms);
	}

	private static UList<Double> initvarexps(RunResultVal result, MLInstance mli) {
		Input inpx = result.baronJob.inpx;
		myassert(inpx.drop_usex);
		//UList<String> vs_used = result.var_exp_map.get(nodeNo).filterByValues(vl -> vl > 1e-6).keySet();

		//		return vs_used.map(nm -> {
		//		double vl = result.var_exp_map.get(nodeNo).get(nm);		
		return result.var_exp_map.get(mli.nodeNo).mapI((i, vlx) -> {
			double vl = vlx;
			//			printf("FRAC %s %s %s%n", mli.fractional, nm, vl);
			double vli = Math.round(vl);
			myassert(Math.abs(vl - vli) < /*1e-3*/5e-3, vl, vli, Math.abs(vl - vli), result, mli, // nm,
					RunResultVal.runresult_expr_for_debugging, i);
			return vli;
		});
	}

	public static ProdVal getProdVal(RunResultVal result, MLInstance mli) {
		//				printf("PRODVAL %s %s %s%n", mli, mli.allowConst, result.c_map.get(nodeNo));
		int nodeNo = mli.nodeNo;
		double cnst = mli.mconst.allowConst ? result.c_map.get(nodeNo)
				: mli.mconst.plusMinus ? result.baronJob.inpx.nonconst_val(result.ci_map.get(mli)) : 1;
		ProdVal pval = getProdVal(!mli.mconst.allowConst && mli.mconst.plusMinus ? checkToInt(cnst, mli.Cinm()) : cnst,
				cnst, initvarexps(result, mli), result.baronJob.inpx.invarnms);
		//dbg.printf("PRODVAL %s %.3f %s%n", mli.nodeNo, pval.cval, pval.varexps);
		return pval;
	}

	private static int checkToInt(double x, String nm) {
		if (Math.abs(x - Math.round(x)) > 1e-3)
			die("The value for %s isn't an int: %s", nm, x);
		return (int) Math.round(x);
	}

	private static LinkedHashMap<UList<Object>, ProdVal> univ = new LinkedHashMap<>();

	public static ProdVal getProdVal(double cval, double raw_cval, UList<Double> varexps, UList<String> varnms) {
		return univ.computeIfAbsent(mkUList1(cval, raw_cval, varexps, varnms),
				x -> new ProdVal(cval, raw_cval, varexps, varnms));
	}

	public static ProdVal getProdVal(double cval, UList<Double> varexps, UList<String> varnms) {
		return getProdVal(cval, cval, varexps, varnms);
	}

	public static ProdVal getProdVal(double cval) {
		ProdVal hopefully_there = univ.values().iterator().next();
		return getProdVal(cval, hopefully_there.varexps, hopefully_there.varnms);
	}

	private static double prodeps = 1e-5;
	private static double expeps = 1e-2;

	public ProdVal getProdValSetConst(double cval) {
		return getProdVal(cval, varexps, varnms);
	}

	public ProdVal getProdValSubExps(UList<Double> exps) {
		return getProdVal(cval, varexps.mapI((i, x) -> x - exps.get(i)), varnms);
	}

	public ProdVal getProdValMul(double cvalmul) {
		double cval2 = cval * cvalmul;
		if (Math.abs(1 - cval2) < prodeps)
			cval2 = 1;

		return getProdVal(cval2, varexps, varnms);
	}

	public int complexity() {
		return (cval == 1.0 ? 0 : 1) + sumi(varexps.map(x -> x == 1.0 || x == 0.0 ? 0 : 1));
	}

	//	public static PrintStream dbg = System.out;
	public static PrintStream dbg = nullout;

	public ProdVal getProdValExp(double expadd) {
		double exp2 = varexps.get(0) + expadd;
		//if (Math.abs(1 - exp2) < expeps)
		//		exp2 = 1;
		if (Math.abs(exp2) < expeps)
			exp2 = 0;
		dbg.printf("EXPSIM %s %s%n", varexps.get(0), exp2);
		return getProdVal(cval, cons(exp2, varexps.rest()), varnms);
	}

	private static int roundThresh = 1000; // nearest 100th, 1000ths, etc

	private static double roundExpThresh = 1e-5;

	private static double roundExp(double x) {
		double y = Math.round(x);
		myassert(Math.abs(x - y) < roundExpThresh, x, y);
		return y;
	}

	private static double roundConst(double x) {
		//		printf("rnd %f %10f %n", x, Math.round(100 * x) / 100.0);
		if (x == 0)
			return x;
		if (x < 0)
			return -roundConst(-x);
		if (x < roundThresh) {
			double mul = 1;
			while (x * (mul * 10) < roundThresh)
				mul *= 10;
			//			printf("RNDLESS %s %s %s %s%n", x, mul, Math.round(x * mul), Math.round(x * mul) / mul);
			return Math.round(x * mul) / mul;
		} else {
			double mul = 1;
			while (x / (mul * 10) > roundThresh)
				mul *= 10;
			//			printf("RNDGRET %s %s %s %s%n", x, mul, Math.round(x * mul), Math.round(x * mul) / mul);
			return Math.round(x / mul) * mul;
		}
	}

	public ProdVal getProdValRound() {
		//		printf("RND %s%n", this);
		//				double rexp = round(varexps.get(0));
		//				dbg.printf("EXPrnd %s %s%n", varexps.get(0), rexp);
		//		return getProdVal(nodeNo, round(cval), cons(rexp, varexps.rest()), varnms, Fexp);
		return getProdVal(roundConst(cval), varexps.map(x -> roundExp(x)), varnms);
	}

	// just for grouping
	public ProdVal getProdValDropConst() {
		return getProdVal(1, varexps, varnms);
	}

	private static double redundantZeroThresh = 1e-10;
	private static double redundantZeroThreshOne = 1e-5;

	public ProdVal dropRedundantExps() {
		if (Math.abs(cval) < redundantZeroThresh)
			return null; // ???
		else
			return this;
	}

	public boolean isOne() {
		return (Math.abs(cval - 1) < redundantZeroThreshOne && varexps.all(x -> Math.abs(x) < redundantZeroThresh));
	}

	public boolean isConst() {
		return (varexps.all(x -> Math.abs(x) < redundantZeroThresh));
	}

	//	public static ProdVal getProdVal(int nodeNode, int expbnd, double cbnd, Random rand, UList<String> varnms) {
	//		return univ.computeIfAbsent(mkUList1(nodeNode, expbnd, cbnd
	//				, varexps, varnms),
	//				x -> new ProdVal(nodeNode, cval, varexps, varnms));
	//	}
	//
	//	private ProdVal(int nodeNode, int expbnd, double cbnd, Random rand, UList<String> varnms) {
	//		this.nodeNo = nodeNode;
	//		this.varnms = varnms;
	//		double cv = rand.nextDouble() * cbnd;
	//		this.cval = rand.nextBoolean() ? -cv : cv;
	//		this.varexps = varnms.map(nm -> (double) rand.nextInt(expbnd));
	//	}

	public static boolean javascript_pow_hack = false;

	public String toString() {
		String s = "";
		if (cval != 1 || varexps.all(exp -> exp == 0))
			s += cval;
		for (int i = 0; i < varnms.size(); ++i) {
			double exp = varexps.get(i);
			if (exp != 0) {
				if (!s.isEmpty())
					s += "*";
				String es = exp == (int) exp ? "" + (int) exp : "" + exp;
				if (javascript_pow_hack)
					s += "Math.pow(" + varnms.get(i) + "," + es + ")";
				else
					s += varnms.get(i) + (exp == 1 ? "" : "^" + es);
			}
		}
		return "(" + s + ")";
	}

	public double eval(DataLine invals) {
		return eval(invals, null);
	}

	public double eval(DataLine invals, MLInstance mli) {
		double x = raw_hack ? raw_cval : cval;
		for (int i = 0; i < varexps.size(); ++i)
			x *= Math.pow(invals.invals.get(i), varexps.get(i));
		//				x = Math.pow(x, Fexp);
		if (mli != null)
			dbg.printf("PRODVAL %s: %s == %s ", mli.nodeNo, x, cval);
		for (int i = 0; i < varexps.size(); ++i)
			dbg.printf("* %s^%s ", invals.invals.get(i), varexps.get(i));
		dbg.printf("%n");
		return x;
	}
}
