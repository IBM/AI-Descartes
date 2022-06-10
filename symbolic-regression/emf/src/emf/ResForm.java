// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.Operator.*;
import static utils.UList.*;
import static utils.UList.sumi;
import static utils.VRAUtils.*;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.function.Function;

import utils.UList;
import utils.UMap;

public class ResForm {
	protected final Operator op;

	//	public final MLInstance inst;

	//	public final boolean isLeaf;
	public final boolean isML;
	public final boolean isPsum;

	public final UList<ResForm> nodes;

	public final ProdVal pval;
	public final Double powval; // if op==POW

	private static LinkedHashMap<UList<Object>, ResForm> univ = new LinkedHashMap<>();

	//	private ResForm(double x) {
	//		this.op = null;
	//
	//		this.isML = false;
	//		this.pval = null;
	//		this.nodes = null;
	//		this.isPsum = false;
	//		this.cval = x;
	//	}

	private ResForm(ProdVal pval) {
		this.op = IS_LEAF;

		this.isML = true;
		this.pval = pval;
		this.nodes = null;

		this.isPsum = op == ADD && nodes.all(n -> n.isML);
		this.powval = null;
	}

	private ResForm(Operator op, UList<ResForm> nodes) {
		this(op, null, nodes);
	}

	private ResForm(Operator op, Double powval, UList<ResForm> nodes) {
		this.op = op;

		this.isML = false;
		this.pval = null;
		this.nodes = nodes;

		//		this.oneList = mkUList1(this);
		this.isPsum = op == ADD && nodes.all(n -> n.isML);
		this.powval = powval;

		if (op.arity > 0 && nodes.isEmpty())
			die("EMPTY RESFORM! %s%n", this);

		myassertEqual(op == POW, powval != null, this);
	}

	public static ResForm getResForm(RunResultVal result, MLInstance mli) {
		ProdVal pval = ProdVal.getProdVal(result, mli);
		return getResForm(pval);
	}

	private static ResForm getResForm(ProdVal pval) {
		return univ.computeIfAbsent(mkUList1(pval), x -> new ResForm(pval));
	}

	public ResForm getResForm(Function<ResForm, ResForm> fn) {
		return getResForm(nodes.map(x -> fn.apply(x)));
	}

	public ResForm getResForm(UList<ResForm> nodes) {
		if (op == POW)
			return univ.computeIfAbsent(mkUList1(op, powval, nodes), x -> new ResForm(op, powval, nodes));
		return univ.computeIfAbsent(mkUList1(op, nodes), x -> new ResForm(op, powval, nodes));
	}

	public static ResForm getResForm(RunResultVal result, INode inode, UList<ResForm> nodes) {
		Operator op = inode.op;
		if (op == POW) {
			ExpInstance ei = inode.expInstance;
			double powval = ei.powIsFixed ? ei.powexp : result.Fexp_map.get(ei);
			return univ.computeIfAbsent(mkUList1(POW, powval, nodes), x -> new ResForm(POW, powval, nodes));
		}
		return univ.computeIfAbsent(mkUList1(op, nodes), x -> new ResForm(op, null, nodes));
	}

	//	public static ResForm getResForm(INode inode, UList<ResForm> nodes) {
	//		Operator op = inode.op;
	//		if (op == POW) {
	//			double powval = 
	//			return univ.computeIfAbsent(mkUList1(POW, powval, nodes), x -> new ResForm(POW, powval, nodes));
	//		}
	//		return univ.computeIfAbsent(mkUList1(op, nodes), x -> new ResForm(op, null, nodes));
	//	}

	public String toString() {
		//		if (cval != null)
		//			return Double.toString(cval);

		if (isML)
			return pval.toString();

		if (op == DIV || op == MUL || op == ADD)
			return "(" + nodes.join(op.toString()) + ")";

		myassert(nodes.size() == 1);
		if (op == POW)
			return "(" + nodes.first() + ")^" + powval;

		return op + "(" + nodes.first() + ")";
	}

	private ResForm mulform(double cval) {
		if (op == DIV || op == ADD)
			return getResForm(x -> x.mulform(cval));
		if (isML)
			return getResForm(pval.getProdValMul(cval));
		die();
		return null;
	}

	private ResForm mulformexp(double xval) {
		if (op == DIV || op == ADD)
			return getResForm(nodes.map(x -> x.mulformexp(xval)));
		if (isML)
			return getResForm(pval.getProdValExp(xval));
		die();
		return null;
	}

	public int complexity() {
		if (isML)
			return pval.complexity();

		return 1 + sumi(nodes.map(x -> x.complexity()));
	}

	public static ProdVal sumPs(UList<ProdVal> pvs) {
		myassert(pvs.size() > 0);
		if (pvs.size() == 1)
			return pvs.first();
		myassert(pvs.map(x -> x.varexps).distinct().size() == 1);
		ProdVal x0 = pvs.first();
		return x0.getProdValSetConst(sumd(pvs.map(x -> x.cval)));
	}

	NormNode form() {
		if (isML)
			return NormNode.getNormML();
		//		printf("GETNORNM %s%n", this);
		return NormNode.getNormNode(op, nodes.map(x -> x.form()));
	}

	ResForm dropRedundantExps() {
		if (isML)
			if (pval.dropRedundantExps() == null)
				return null;
			else
				return this;

		if (op == ADD) {
			if (true) {
				UList<ResForm> ns = nodes.mapDropNulls(x -> x.dropRedundantExps());
				if (ns.isEmpty()) {
					//					printf("DROPPED ALL! %s %s %s%n", this, op, nodes.size());
					return getResForm(ProdVal.getProdVal(0));
				}
				return ns.size() == 1 ? ns.first() : getResForm(ns);
			}
			//			printf("DROPRED %s%n", this);
			UMap<UList<Double>, UList<ProdVal>> ps = nodes.mapDropNulls(x -> {
				// not all x have x.pval - what was this doing???
				//				printf("DROP0 %s %s%n", x, x.pval);
				//				printf("DROP1 %s %s%n", x, x.pval.getProdValRound());
				//				printf("DROP2 %s %s%n", x, x.pval.getProdValRound().dropRedundantExps());
				return x.pval.getProdValRound().dropRedundantExps();
			}).mkrevmapList2(p -> p.varexps);
			if (ps.size() == nodes.size())
				return this;
			//			printf("DROPPED %s->%s %s%n", nodes.size(), ps.size(), this);
			UList<ResForm> ns = ps.values().mapDropNulls(pvl -> getResForm(sumPs(pvl)));
			return ns.size() == 1 ? ns.first() : getResForm(ns);
		}
		if (op == MUL) {
			UList<ResForm> ns = nodes.mapDropNulls(x -> x.dropRedundantExps());
			return ns.size() == 1 ? ns.first() : getResForm(ns);
		}

		if (op == DIV) {
			ResForm num = nodes.first().dropRedundantExps();
			ResForm den = nodes.last().dropRedundantExps();
			if (num == null) {
				return den;
//				printf("NULL NUMERATOR! %s%n", this);
//				return null; // 0/P
			}
			if (den == null)
				die("DENOM 0! %s", this);

			if (den.isML && den.pval.isOne())
				return num;
			//			if (den.isPsum && den.nodes.all(x -> x.pval.isOne()))
			//				return ?

			return getResForm(mkUList1(num, den));
		}

		if (op == POW || op == INTERNAL_EXP) {
			ResForm arg = nodes.first().dropRedundantExps();
			if (op == POW && arg.isConst()) {
				myassert(powval != null);
				return getResForm(ProdVal.getProdVal(Math.pow(arg.pval.cval, powval)));
			}
			return getResForm(mkUList1(arg));
		}

		die(); //???
		//return getResForm(op, nodes.distinct(x -> x.pval.getProdValRound().varexps));
		return null;
	}

	public boolean isConst() {
		return isML && pval.isConst();
	}

	public ResForm simpform() {
		ResForm x = dropRedundantExps();
		if (x == null) {
			printf("%s RED NULL!", this);
			return null;
		}
		ResForm rval = x.simpform2().simpformAux();
		if (rval.op.arity > 0 && rval.nodes.isEmpty())
			printf("EMPTY RESFORM! %s %s%n", this);
		return rval;
	}

	private ResForm simpformAux() {
		UList<Function<ResForm, ResForm>> fns = mkUList1(ResForm::simpformRound, ResForm::simpformexp,
				ResForm::simpformconst);

		ResForm best = this;
		for (Function<ResForm, ResForm> fn : fns) {
			ResForm simp = fn.apply(best);
			if (simp != null && simp.complexity() < best.complexity()) {
				best = simp;
			}
		}
		//return best == this ? null : best;
		return best;
		//		return best.simpformRound();
	}

	//public static PrintStream dbg = System.out;
	public static PrintStream dbg = nullout;

	private ResForm subExps(UList<Double> exps) {
		if (isML)
			return getResForm(pval.getProdValSubExps(exps));
		return getResForm(x -> x.subExps(exps));
	}

	private ResForm simpform2() {
		//		OPT CAND   2:     0.0000                      ((P+P)/(P+P)) S (((98.99848022172728*x)+(x))/((x)+(1.3330717652588155)))

		//		if (true)
		//			return this;

		if (op.arity == 0)
			return this;

		if (isPsum) {
			UMap<ProdVal, UList<ResForm>> byExps = nodes.groupingBy(x -> x.pval.getProdValDropConst());
			if (byExps.values().some(x -> x.size() > 1)) {
				//				printf("PS %s%n", this);
				return getResForm(byExps.values()
						.map(lst -> getResForm(lst.first().pval.getProdValSetConst(sumd(lst.map(x -> x.pval.cval))))));
			}
		}

		if (op == DIV && nodes.all(x -> x.isPsum || x.isML)) {
			UList<ProdVal> pvals = nodes.flatMap(x -> x.isML ? mkUList1(x) : x.nodes).map(x -> x.pval);
			if (pvals.all(x -> x.varexps.all(y -> y >= 0))) {
				//				int ninvars = pvals.first().varexps.size();
				//				UList<Pair<Double, Double>> uses = transpose(pvals.map(x -> x.varexps)).filter(xs -> !xs.contains(0.0))
				//						.map(xs -> new Pair<>(mind(xs), maxd(xs)));
				UList<Double> mins = transpose(pvals.map(x -> x.varexps)).map(xs -> mind(xs));
				myassert(transpose(transpose(pvals.map(x -> x.varexps))).equals(pvals.map(x -> x.varexps)));

				if (mins.some(x -> x > 0)) {
					//					printf("TRANS:%n%s%n%s%n", (pvals.map(x -> x.varexps)), transpose(pvals.map(x -> x.varexps)));
					ResForm y = getResForm(x -> x.subExps(mins));
					//					printf("SIMP %s %s%n", this, y);
					return y;
				}
			}
		}

		return getResForm(x -> x.simpform2());
	}

	private ResForm simpformexp() {
		if (op == DIV && nodes.some(x -> x.isML) && nodes.some(x -> x.isPsum)) {
			ProdVal mli = nodes.findFirst(x -> x.isML).get().pval;

			if (mli.varexps.size() == 1) {
				UList<ProdVal> mlis = nodes.findFirst(x -> x.isPsum).get().nodes.map(x -> x.pval);
				double smallestExp = mlis.sort((x, y) -> chainCompare(x.varexps, y.varexps)).get(0).varexps.get(0);

				ResForm simp = mulformexp(-smallestExp);
				dbg.printf("EXPCOMPx %s %s %s%n", simp.complexity(), complexity(), simp);
				if (simp.complexity() < complexity())
					return simp;
			}
		}

		return null;
	}

	private ResForm simpformRound() {
		if (isML)
			return getResForm(pval.getProdValRound());

		return getResForm(nodes.map(x -> x.simpformRound()));
	}

	private ResForm simpformconst() {
		//		if (nodes.all(x->x.isML && x.pval.isOne())
		//				return getResForm(ProdVal.ge

		if (op == DIV && nodes.some(x -> x.isML) && nodes.some(x -> x.isPsum)) {
			ProdVal mli = nodes.findFirst(x -> x.isML).get().pval;

			UList<ProdVal> mlis = nodes.findFirst(x -> x.isPsum).get().nodes.map(x -> x.pval);
			ResForm best = this;
			for (ProdVal pval : cons(mli, mlis)) {
				ResForm simp = mulform(1.0 / pval.cval);
				dbg.printf("EXPCOMPc %s %s%n", simp.complexity(), simp);
				if (simp.complexity() < best.complexity())
					best = simp;
			}
			return best;
		}

		return null;
	}

	public double eval(DataLine invals) {
		//			UMap<MLInstance, Double> pvals, UMap<ExpInstance, Double> expn_map) 
		if (op.arity == 0)
			if (isML)
				return pval.eval(invals);
			else {
				myassert(op == ONE);
				return 1;
			}

		if (op == MUL)
			return prodd(nodes.map(n -> n.eval(invals)));
		if (op == ADD)
			return sumd(nodes.map(n -> n.eval(invals)));
		if (op == DIV)
			return nodes.get(0).eval(invals) / nodes.get(1).eval(invals);
		if (op == POW) {
			return Math.pow(nodes.get(0).eval(invals), powval);
		}
		die();
		return 0;
	}

}
