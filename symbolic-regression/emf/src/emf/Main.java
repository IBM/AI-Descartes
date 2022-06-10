// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.NormNode.*;
import static utils.DRange.*;
import static utils.MDRange.*;
import static utils.UList.*;
import static utils.UList.sumi;
import static utils.VRAUtils.*;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sourceforge.yamlbeans.YamlException;
import utils.CPair;
import utils.DRange;
import utils.GrowList;
import utils.MDRange;
import utils.OList;
import utils.UList;
import utils.UMap;
import utils.Univ1;
import utils.Univ2;
import utils.VRAFileUtils;

public class Main {
	public static void main(String[] args) throws Exception {
		new Main(args).main1();
	}

	final static long starttm = System.currentTimeMillis();

	public final Input inpx;

	public final int numInVars;

	public static class Bucket {
		public final int ptind;
		public final int ind;
		public final UList<Integer> members;
		public final DRange rng;
		//		public final GrowList<Bucket> partners = new GrowList<>();

		public String toString() {
			return "BKT(" + ptind + ":" + ind + ":" + rng + ")";
		}

		public Bucket(int ptind, int ind, UList<Integer> members, DRange rng) {
			this.ptind = ptind;
			this.ind = ind;
			this.members = members;
			this.rng = rng;
			myassert(!rng.nullset, this);
			myassert(members.isNotEmpty());
		}

	};

	public static class PntBkts {
		public final int pointi;
		public final DataLine line;
		public final UList<Double> ptvals;
		public final UList<Bucket> bkts;
		public final UList<Combo2P> combs;
		public final MDRange mrng;

		public PntBkts(Input inpx, int pointi, int nbkts, int nPs) {
			this.pointi = pointi;
			//			this.bkts = inpx.unsampled_input.mapI((pointi, line0) -> {
			DataLine line0 = inpx.unsampled_input.get(pointi);
			this.line = line0;
			//			UList<Double> xs0;

			GrowList<Double> outx = new GrowList<>();
			Dumb.en(outx, line0.invals, 1, inpx.numInVars - 1);
			this.ptvals = outx.elems();
			//			Comparator<Pair<Double, Integer>> comp = (x, y) -> Pair.comparePairs(x, y);
			UList<CPair<Double, Integer>> xs = sort(ptvals.mapI((i, x) -> new CPair<>(x, i)));
			int n = xs.size() / nbkts;
			//			println();
			//			for (int i = 0; i < nbkts; ++i)
			//				printf("%2s: %5.2f%n", i, xs.get(n * i));
			//			printf("%2s: %s%n", "", xs.last());
			this.bkts = rangeOpen(nbkts).map(i -> {
				int i0 = n * i;
				int i1 = i == nbkts ? xs.size() - 1 : n * (i + 1);
				//				printf("Is %s %s %s %s%n", i0, i1, nbkts, xs.size());
				return new Bucket(pointi, i, xs.map(x -> x.right).subList(i0, i1),
						newDRange(xs.get(i0).left, xs.get(i1).left));
			});

			this.combs = bkts.allchoices(nPs).map(comb -> new Combo2P(inpx, pointi, comb, nPs));
			myassert(combs.isNotEmpty());

			this.mrng = newMDRange(combs.map(x -> x.rng));
		}
	}

	public static final int nPs = 3;

	public static class Combo2P {
		public final UList<Bucket> bcomb;
		public final DRange rng;
		public final double outval;

		public String toString() {
			return "C" + bcomb.map(b -> b.ind).join("x");
		}

		public Combo2P(Input inpx, int pointi, UList<Bucket> bkts, int nPs) {
			myassert(bkts.size() == nPs);
			this.bcomb = bkts;
			this.outval = inpx.unsampled_input.get(pointi).outval;
			DRange r = bcomb.get(0).rng;
			if (nPs == 2)
				this.rng = r.nullset ? r : bcomb.get(1).rng.rsub(outval).div(r).intersect(inpx.crng);
			else if (nPs == 3)
				this.rng = r.nullset ? r
						: bcomb.get(1).rng.mul(bcomb.get(2).rng).rsub(outval).div(r).intersect(inpx.crng);
			else {
				this.rng = null;
				die();
			}
			//				DRange crng = newDRange(inpx.min_const_val, inpx.max_const_val);
			//				UList<UList<PntBkts>> bcombs = 
			//				for (Bucket bkt0 : bkts) {
			//					UList<DRange> drs = bcombs.filter(bcomb -> bcomb.first().equals(bkt0))
			//							.map(bcomb -> bcomb.get(0).rng.rsub(outval).div(bcomb.get(1).rng).intersect(crng));
			//					printf("%s %s %s%n", bkt0, drs.count(x -> !x.nullset), drs);
			//				}
		}

	}

	public static class VarPow {
		public final String var;
		public final Integer pow;

		public VarPow(String var, Integer pow) {
			this.var = var;
			this.pow = pow;
		}

		public String toString() {
			return var + "^" + pow;
		}
	};

	static final Univ2<String, Integer, VarPow> vpu = new Univ2<>((s, p) -> new VarPow(s, p));

	public static class VarComb {
		public static boolean printJustPowers = false;

		public final UList<VarPow> vars;

		public VarComb(UList<VarPow> vars) {
			this.vars = vars;
			myassert(vars.map(x -> x.var).equals(allvars));
		}

		public String toString() {
			if (printJustPowers)
				return "VC" + vars.map(x -> x.pow < 0 ? x.pow + "" : " " + x.pow).toString();
			else
				return vars.join("*");
		}

		public VarComb mult(VarComb x) {
			return vpc.get(vars.zipmap(x.vars, (xv, yv) -> {
				myassert(xv.var.equals(yv.var));
				return vpu.get(xv.var, xv.pow + yv.pow);
			}));
		}

		public VarComb pow(int n) {
			return vpc.get(vars.map(xv -> vpu.get(xv.var, xv.pow * n)));
		}

		public UList<Integer> pows() {
			return vars.map(x -> x.pow);
		}
	}

	static Univ1<UList<VarPow>, VarComb> vpc = new Univ1<>(l -> new VarComb(l));
	static UList<String> allvars = mkUList1("x", "y", "z", "w", "q", "r").subList(0, 3);
	//	static UList<String> allvars = mkUList1("x", "y", "z");
	public static final VarComb vco = new VarComb(allvars.map(s -> vpu.get(s, 0)));

	static VarComb getvc(VarPow x) {
		return vpc.get(allvars.map(xv -> vpu.get(xv, xv.equals(x.var) ? x.pow : 0)));
	}

	static VarComb getvc(String x, String y) {
		return vpc.get(allvars.map(xv -> vpu.get(xv, xv.equals(x) ? 1 : xv.equals(y) ? -1 : 0)));
	}

	static VarComb getvc(OList<String> xl, OList<String> yl) {
		return multall1(xl.map(x -> vpu.get(x, 1))).mult(multall1(yl.map(y -> vpu.get(y, -1))));
	}

	static VarComb getvc(UList<Integer> ps) {
		return multall1(allvars.zipmap(ps, (x, pow) -> vpu.get(x, pow)));
	}

	//	public final UList<String> vars;
	public static VarComb multall(UList<VarComb> vcs, UList<Integer> ps) {
		myassertEqual(vcs.size(), ps.size());
		if (vcs.isEmpty())
			return vco;
		return vcs.first().pow(ps.first()).mult(multall(vcs.rest(), ps.rest()));
	}

	public static VarComb multall1(UList<VarPow> vcs) {
		if (vcs.isEmpty())
			return vco;
		return getvc(vcs.first()).mult(multall1(vcs.rest()));
	}

	public static VarComb multall(UList<VarComb> vcs) {
		if (vcs.isEmpty())
			return vco;
		return vcs.first().mult(multall(vcs.rest()));
	}

	int testit(UList<VarComb> vcs, UList<UList<Integer>> pcs, UList<VarComb> goodps, boolean show) {
		myassert(vcs.size() == pcs.size());
		UList<VarComb> outcs = allCombs(pcs).map(ps -> multall(vcs, ps));
		if (goodps.subset(outcs)) {
			if (show) {
				UMap<UList<Integer>, VarComb> outcs2 = allCombs(pcs).mkmap(ps -> multall(vcs, ps));
				for (VarComb goodp : goodps) {
					printf("%15s: %s ^ %s%n", goodp, vcs, outcs2.reverseMap().get(goodp));
				}
				//				outcs.diff(goodps).dump("bad");
				printf("bad:%n");
				for (VarComb goodp : outcs.diff(goodps)) {
					printf("%15s: %s ^ %s%n", goodp, vcs, outcs2.reverseMap().get(goodp));
				}
				vcs.dump("v");
				pcs.dump("p");
			}
			return outcs.diff(goodps).size();
		}
		//		if (goodps.diff(outcs).size() == 286) {
		//			goodps.diff(outcs).dump("");
		//			outcs.dump("");
		//			vcs.dump("v");
		//			pcs.dump("p");
		//			die(outcs.size());
		//		}
		return -goodps.diff(outcs).size();
	}

	public void heur1() {
		//UList<VarComb> facs = mkUList1(getvc("y", "x"), getvc("y", "z"));
		int maxpow = 2;
		UList<Integer> pows = rangeClosed(-maxpow, maxpow);
		int nvs = allvars.size();
		UList<UList<Integer>> goodps = pows.allchoices(nvs).filter(ps -> sumi(ps) == 0);
		UList<VarComb> goodvcs = goodps.map(ps -> getvc(ps));
		//		UList<VarComb> goodvcs = goodps.filter(ps -> 2 == sumi(ps.map(x -> Math.abs(x)))).map(ps -> getvc(ps));

		VarComb.printJustPowers = true;
		goodps.dump("goodps");
		printf("TOT %s%n", Math.pow(pows.size(), allvars.size()));

		//		goodvcs.dump("goodvcs");
		//		goodps.filter(x -> x.contains(2) || x.contains(-2)).dump("goodps");
		//		die();

		//UList<OList<String>> allvcs0 = allvars.allchoices(maxpow * 2).map(x -> sort(x)).distinct();
		//		UList<VarComb> allvcs = allCombs(mkUList1(allvcs0, allvcs0)).map(l -> getvc(l.first(), l.last()));
		UList<VarComb> allvcs = allCombs(mkUList1(allvars, allvars)).filter(x -> x.distinct().size() == 2)
				.map(l -> getvc(l.first(), l.last()));
		//		if (false)
		{
			//			int sz = allvars.size() - 1;
			//			for (int ncs : rangeClosed(sz, sz))
			int ncs = allvars.size() - 1;
			{
				printf("P %s%n", pows);
				//UList<UList<Integer>> pcombs = allSubseqs(pows);
				UList<UList<Integer>> pcombs = mkUList1(pows);
				//			pcombs.dump("");
				//			die();
				UList<UList<UList<Integer>>> npcombs = pcombs.allchoices(ncs);
				//			npcombs.dump("");
				//			die();
				//npcombs.map(pc -> allCombs(pc)).dump("");//.map(vcs -> printf("%s%n", vcs)));
				UList<VarComb> winning_vcs = null;
				myassert(pcombs.size() == 1);
				for (UList<VarComb> vcs : allvcs.allchoices(ncs))
					if (vcs.areDistinct())
						for (UList<UList<Integer>> pc : npcombs) {
							//						printf("%s %s%n", vcs, pc);
							int vl = testit(vcs, pc, goodvcs, false);
							if (vl >= 0) {// && vl < 6) 
								printf("%s %s %s%n", vl, vcs.map(x -> x.pows()), pc);
								for (int pi : pc.indexRange()) {
									//UList<UList<Integer>> pc1 = pc.set(pi, pc.get(pi).subList(1));
									for (boolean fromFront : trueFalse) {
										UList<UList<Integer>> pc1 = pc.set(pi, pc.get(pi).subList(fromFront ? 1 : 0,
												pows.size() - (fromFront ? 0 : 1)));
										//									UList<UList<Integer>> pc1 = pc.set(pi, pc.get(pi).subList(0, 4));
										vl = testit(vcs, pc1, goodvcs, false);
										if (vl >= 0) {// && vl < 6) 
											die(">> %s %s %s%n", vl, pc1, vcs.map(x -> x.pows()));
										}
									}
								}
								winning_vcs = vcs;
								//								testit(winning_vcs, npcombs.get(0), goodvcs, true);
							}
						}
				if (winning_vcs != null)
					testit(winning_vcs, npcombs.get(0), goodvcs, true);
			}
			die();
		}

		//		UList<VarComb> allfacs = allCombs(allvars) rangeOpen(allvars.size() - 1).map(i -> getvc(allvars.get(i + 1), allvars.get(i)));
		int nvars = allvars.size();
		UList<VarComb> facs0 = rangeOpen(allvars.size() - 1).map(i -> getvc(allvars.get(i + 1), allvars.get(i)));
		UList<String> allvarsp = allvars.getList(0, 2, 1);
		UList<VarComb> facs1 = rangeOpen(allvarsp.size() - 1).map(i -> getvc(allvarsp.get(i + 1), allvarsp.get(i)));
		//		UList<VarComb> facs1 = mkUList1(getvc(allvars.get(2), allvarsp.get(0)));
		//				UList<VarComb> facs1 = UList.empty();

		//		UList<VarComb> facs = facs0.concat(facs1);
		UList<VarComb> facs = rangeOpen(nvars)
				.flatMap(i -> rangeOpen(nvars).mapDropNulls(j -> i == j ? null : getvc(allvars.get(i), allvars.get(j))))
				.rest().rest();

		printf("BASIS %s%n", facs.map(x -> x.pows()));
		//UList<Integer> pows1 = rangeClosed(0, maxpow);
		UList<Integer> pows1 = rangeClosed(0, 1);
		int nbasis = facs.size(); // nvars-1

		UMap<UList<Integer>, VarComb> allcs = pows1.allchoices(nbasis).mkmap(ps -> multall(facs, ps));
		allcs.forEachEntry((ps, mavcs) -> printf("%8s %8s %8s%n", ps, mavcs.vars.map(x -> x.pow),
				mavcs.vars.some(x -> Math.abs(x.pow) > maxpow) ? "XXXX" : ""));
		printf("sz: %s%n%n", allcs.size());

		goodps.dump("goodps");
		printf("%s %s%n", goodps.size(), allcs.size());
		UList<UList<Integer>> gotps = allcs.values().map(x -> x.vars.map(y -> y.pow));
		if (!gotps.areDistinct())
			printf("DUPLICATE ENTRIES!%n%n");
		printf("SUB? %s%n", goodps.subset(gotps));
		goodps.forEach(x -> {
			if (!gotps.contains(x))
				printf("%s%n", x);
		});
		die();
	}

	public void heur() {
		int nbkts = 10;
		UList<Integer> allpts = rangeOpen(inpx.unsampled_input.size());
		//		UList<PntBkts> allbkts = allpts.map(i -> new PntBkts(inpx, i, nbkts, nPs));

		{
			UList<PntBkts> pbkts = allpts.map(pointi -> new PntBkts(inpx, pointi, nbkts, nPs));
			//			pbkts.map(pbkt -> allchoices(pbkt.bkts, nPs).map(comb -> new Combo(inpx, pbkt.pointi, comb, nPs)));

			{
				String pathname = "/home/austel/emf/emf.lp";
				PrintStream out = VRAFileUtils.newPrintStream(pathname);
				out.printf("Minimize obj:%n");
				out.printf(" %s%n", allpts.map(pti -> "diff" + pti).join("+"));
				out.printf("Subject To%n");
				int ncs = pbkts.first().ptvals.size();
				for (PntBkts ptbkt : pbkts) {
					out.printf("diff%s:  ", ptbkt.pointi);
					//ptbkt.ptvals.forEachI((i, x) -> out.printf("%s c%s %s %n", x, i, (i == ncs - 1 ? "-" : "+")));
					ptbkt.ptvals.forEachI((i, x) -> out.printf("%s c%s %s %n", x, i, "+"));
					//					ptbkt.ptvals.forEachI((i, x) -> out.printf("%s x%s %s %n", x, i, "+"));
					ptbkt.ptvals.forEachI((i, x) -> out.printf("%s y%s %s %n", x, i, (i == ncs - 1 ? "-" : "+")));
					out.printf(" diff%s = %s%n", ptbkt.pointi, ptbkt.line.outval);
				}
				out.println();

				rangeOpen(ncs).forEach(i -> out.printf("ci%s: 100.0 ci%s- c%s >= 0%n", i, i, i));

				allpts.forEach(i -> out.printf("diffx%s: diff%s >= 0%n", i, i));

				out.printf(" %s <= 1%n", rangeOpen(ncs).map(i -> "ci" + i).join("+"));
				//				out.printf(" %s <= 1%n", rangeOpen(ncs).map(i -> "x" + i).join("+"));
				out.printf(" %s <= 1%n", rangeOpen(ncs).map(i -> "y" + i).join("+"));

				out.printf("Bounds%n");

				rangeOpen(ncs).forEach(i -> out.printf(" 0 <= c%s <= 100%n", i));

				out.printf("Binaries%n");

				rangeOpen(ncs).forEach(i -> out.printf(" ci%s%n", i));
				//				rangeOpen(ncs).forEach(i -> out.printf(" x%s%n", i));
				rangeOpen(ncs).forEach(i -> out.printf(" y%s%n", i));

				out.printf("End%n");
				out.close();
				die();
			}
			pbkts.forEach(px -> pbkts.forEach(py -> {
				if (px.pointi < py.pointi)
					printf("NINT %s%n", px.combs.sumi(cmbx -> py.combs.count(cmby -> cmbx.rng.intersects(cmby.rng))));
			}));
			printf("%n%s%n", pbkts.first().combs.filter(x -> !x.rng.nullset).size());

			//			UList<UList<Bucket>> bcombsx = allbkts.flatMap(bkts1 -> allchoices(bkts1, 2));

			//			pbkts.forEach(x -> printf("R %s%n", x.mrng));
			//			rangeOpen(100).forEach(i -> printf("%s%n", pbkts.map(x -> x.combs.get(i).rng).join(" ")));
			die();

			UList<Integer> cmems = pbkts.first().combs.first().bcomb.map(x -> x.members.first());

			printf("MEM: %s%n", cmems);
			printf("CS %s%n", pbkts.map(pbkt -> pbkt.combs
					.findFirst(comb -> comb.bcomb.alli((i, bkt) -> bkt.members.contains(cmems.get(i)))).get()));
			//			UList<UList<Bucket>> bcombs1 = bcombsx
			//					.filter(comb 
			//			bcombs1.forEachI((i, x) -> printf("%2s: %s%n", i, x));
			//				for (UList<Bucket> bcomb : bcombs)
			//					if (bcomb.first().equals(bkt0)) {
			//						DRange cmb_crng = bcomb.get(0).rng.rsub(outval).div(bcomb.get(1).rng).intersect(crng);
			//						if (!cmb_crng.equals(crng))
			//							printf("%s %s%n", cmb_crng, bcomb);
			//					}
		}

		die();

		//		int nbkts = 20;
		//		for (int pointi : rangeOpen(nbkts))
		//			for (int pointj : rangeOpen(nbkts))
		//				if (pointi < pointj) {
		//					UList<DRange> rng0, rng1;
		//					UList<Double> xs0, xs1;
		//					DataLine line0 = inpx.unsampled_input.get(pointi);
		//					DataLine line1 = inpx.unsampled_input.get(pointj);
		//					{
		//						GrowList<Double> outx = new GrowList<>();
		//						en(outx, inpx.unsampled_input.get(pointi).invals, 1, inpx.numInVars - 1);
		//						xs0 = outx.elems();
		//						UList<Double> xs = sort(outx.elems());
		//						int n = xs.size() / nbkts;
		//						println();
		//						for (int i = 0; i < nbkts; ++i)
		//							printf("%2s: %5.2f%n", i, xs.get(n * i));
		//						printf("%2s: %s%n", "", xs.last());
		//						rng0 = rangeClosed(0, nbkts)
		//								.map(i -> newDRange(xs.get(n * i), xs.get(i == nbkts ? xs.size() - 1 : n * (i + 1))));
		//						GrowList<Double> out = new GrowList<>();
		//						en(out, inpx.unsampled_input.get(1).invals, 1, inpx.numInVars - 1);
		//					}
		//					{
		//						//			UList<Double> ys = sort(out.elems());
		//						//			xs = sort(out.elems());
		//						GrowList<Double> outx = new GrowList<>();
		//						en(outx, inpx.unsampled_input.get(pointj).invals, 1, inpx.numInVars - 1);
		//						xs1 = outx.elems();
		//						UList<Double> xs = sort(outx.elems());
		//						int n = xs.size() / nbkts;
		//						println();
		//						for (int i = 0; i < nbkts; ++i)
		//							printf("%2s: %5.2f%n", i, xs.get(n * i));
		//						printf("%2s: %s%n", "", xs.last());
		//						rng1 = rangeClosed(0, nbkts)
		//								.map(i -> newDRange(xs.get(n * i), xs.get(i == nbkts ? xs.size() - 1 : n * (i + 1))));
		//						GrowList<Double> out = new GrowList<>();
		//						en(out, inpx.unsampled_input.get(1).invals, 1, inpx.numInVars - 1);
		//					}
		//					int n = xs0.size();
		//					int nsame = 0;
		//					for (int i = 0; i < n; ++i) {
		//						double x0 = xs0.get(i);
		//						double x1 = xs1.get(i);
		//						int ri0 = rng0.findFirstIndexI((ii, rng) -> rng.contains(x0));
		//						int ri1 = rng1.findFirstIndexI((ii, rng) -> rng.contains(x1));
		//						if (ri1 == ri0)
		//							nsame++;
		//						//							printf("xx %s %s %s%n", ri0, ri1, ri0 == ri1 ? "==" : "");
		//					}
		//					printf("nsame: %d %d  %s %.2f %n", pointi, pointj, nsame, nsame / (double) xs0.size());
		//
		//					printf("bkts:  %d %d ", pointi, pointj);
		//
		//					rangeOpen(nbkts).forEach(
		//							i -> printf(" %s", xs0.count(x -> rng0.get(i).contains(x) & rng1.get(i).contains(x))));
		//					println();
		//
		//					double tol = 1;
		//					//						printf("DR %s%n", rangeOpen(nbkts).map(i -> rng0.get(i).div(line0.outval + tol)
		//					//								.intersects(rng1.get(i).div(line1.outval + tol))));
		//
		//					printf("DR %s %s%n",
		//							rangeOpen(nbkts).flatMap(i -> rangeOpen(nbkts).map(j -> rng0.get(i)
		//									.div(DRange.plusMinus(line0.outval, tol)).intersects(rng1.get(j)
		//											.div(DRange.plusMinus(line1.outval, tol)))))
		//									.count(true),
		//							rangeOpen(
		//									nbkts).map(
		//											i -> rangeOpen(nbkts)
		//													.map(j -> rng0.get(i).div(DRange.plusMinus(line0.outval, tol))
		//															.intersects(rng1.get(j)
		//																	.div(DRange.plusMinus(line1.outval, tol))))
		//													.count(true)));
		//
		//					//crng0, crng1, crng0.intersects(crng1));
		//
		//					//						UList<Integer> bkt0 = rng0.map(rng -> xs0.count(x -> rng.contains(x)));
		//					//						UList<Integer> bkt1 = rng1.map(rng -> xs1.count(x -> rng.contains(x)));
		//					//						for (int bi : rangeOpen(nbkts))
		//					//							printf("bkts: %d %d %s %s%n", pointi, pointj, nsame, nsame / (double) xs0.size());
		//				}
		printf("Time %s%n", (System.currentTimeMillis() - starttm) / 1000);
		die();
	}

	public Main(String[] args) throws IOException, YamlException {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Current relative path is: " + s);

		this.inpx = new Input(args, true);

		//		new Dumb(inpx).run();

		if (false) {
			UList<String> js = runShellCmd("ps x|grep bin/baron|sed 's/pts.*//')");

			if (inpx.kill_existing_baron_jobs) {
				printf("KILLING EXISTING BARON JOBS%n");
				// ps doesn't work, dunno why
				//			runShellCmd("ls");
				//			UList<String> js = runShellCmd("kill -9 $(ps x|grep baron|sed 's/pts.*//')");
				//			runShellCmd("ps");
				//			UList<String> js = runShellCmd("ls /proc/*/cmdline");
				//UList<String> js = runCmd("/usr/bin/sh", "-c", "ls /home/austel");
				//						UList<String> js = runCmd("/usr/bin/sh", "-c", "ls /proc/17152");
				//			UList<String> js = runCmd("/usr/bin/ls");
				//			UList<String> js = runCmd("/usr/bin/ls");
				//						UList<String> js = VRAFileUtils.allFiles("/proc").filter(d-> VRAFileUtils.allFiles("/proc/"+d.contains("cmdLine")))
				//			printf("BJS %s%n", js);
				//			die();
				runShellCmd("kill -9 " + js.join(" "));
			} else if (js.size() > 100)
				printf("There are %s baron jobs running - perhaps you should kill them.%n", js.size());
		}
		this.numInVars = inpx.numInVars;
		//"input_10.dat"
	}

	public UList<NormNode> genNodes() throws Exception {

		//			UList<NormNode> ns0 = addLevel(nlevels, Operator.all);
		final UList<NormNode> ns00 = concatULists(addLevel(mkUList1(inpx.mlleaves), 1, inpx.max_tree_depth));
		final UList<NormNode> ns0 =
				// 	BUG?  n.norm.arithNorm().mergeDivP() produced different results, unnormalized different
				ns00.map(n -> n.mergeDivP().arithNorm());
		//		ns0.forEach(n -> printf("NN %s %s%n", n, n.arithNorm()));
		//		final UMap<NormNode, Integer> pcnt = ns0.map(n -> n.arithNorm()).popCount();
		//		final UList<NormNode> ns1 = ns0
		//				.filter(n -> pcnt.get(n.norm.arithNorm()) == 1 && n.norm.arithNorm().noBadCombs());
		final UList<NormNode> ns1 = ns0.maybeFilter(inpx.discard_redundant_exprs, n -> !n.isRedundant());
		final UList<NormNode> ns1b = ns1.maybeFilter(inpx.relaxUSE_I, n -> !n.hasExpP());

		final UList<NormNode> ns2 = ns1b.filter(n -> n.allSubNodes().count(x -> x.op == Operator.INTERNAL_EXP) <= 1);
		// .filter(n -> !n.allSubNodes().some(m -> m.isPsum && m.nodes.size() > mpsum));

		if (false)
			ns0.forEach(x -> {
				printf("-- %s", x);
				if (!ns1.contains(x))
					printf(" redund%n", x);
				else if (!ns1b.contains(x))
					printf(" hasExpP%n", x);
				else if (!ns1b.contains(x))
					printf(" mpsum%n", x);
				else
					printf("%n");
			});
		//		final UList<NormNode> ns2 = ns1;
		final UList<NormNode> nsx = ns2.distinct().sortByFieldAscending(n -> n.complexity);

		final UList<NormNode> ns = inpx.max_num_forms > 0 ? nsx.subList(0, Integer.min(nsx.size(), inpx.max_num_forms))
				: nsx;
		//		final UList<NormNode> ns = nsx;
		//		final UList<NormNode> ns = ns1.map(n -> n.norm.arithNorm().maybeDropConsts());
		ns.map(n -> n.toString()).checkDistinct();

		//			ns.forEach(n -> printf("%s %s %s%n", n, n.norm.allSubNodes(),
		//					n.norm.allSubNodes().map(sn -> sn.op != Operator.DIV)));
		//
		//			die();

		if (inpx.generateAllBNodes) {
			ns.groupingBy(n -> n.arithNorm()).forEachEntryEx((nn, bns) -> printf("%-40s %s%n", nn, bns));
			printf("NUM TREES grouped: %s   raw: %s%n", ns.groupingBy(n -> n.arithNorm()).size(), ns.size());
			//			die();
		}

		printf("NUM TREES %s with max depth %s%n", ns.size(), inpx.max_tree_depth);
		//			die();
		printf("%6s  %-40s %-40s%n", "", "ArithNormTree", "Unnormalized (blank if same)");

		UList<NormNode> only_process = ns.filter(x -> inpx.only_process.contains(x.toString()));
		if (!inpx.only_process.equals(only_process.map(x -> x.toString())))
			die("BAD VALUE for yaml 'only_process': %s%n  These have no corresponding entry: %s%n", inpx.only_process,
					inpx.only_process.diff(only_process.map(x -> x.toString())));

		//UList<NormNode> numberedNodes = (!inpx.ignoreUnlikely ? ns : ns.filter(n -> n.isLikely()));
		UList<NormNode> numberedNodes = ns;
		if (!only_process.subset(numberedNodes))
			only_process.forEachI((i, n) -> n.nodeNum.set(i));
		else
			numberedNodes.forEachI((i, n) -> n.nodeNum.set(i));

		if (only_process.isEmpty()) {
			int ntrees = 0;

			//			int nleaves = (int) Math.round(Math.pow(2, inpx.max_tree_depth));
			//			for (boolean likely : mkUList1(true, false)) 
			{
				//				if (inpx.ignoreUnlikely)
				//					printf("Likely: %s%n", likely);
				for (NormNode n : ns) // .filter(n -> n.isLikely() == likely))
				//				if (n.leaves().size() == nleaves) 
				{
					printf("%6s %3s: %-40s", ntrees, n.complexity, n); // .norm.arithNorm().maybeDropConsts());
					if (n.nodeNum.isPresent())
						myassertEqual(ntrees, n.nodeNum.get());

					if (inpx.discard_redundant_exprs)
						printf("%1s ", "");
					else
						printf("%1s ", n.isRedundant() ? "X" : "");

					if (n != n.arithNorm())
						printf(" %-40s", n);
					else
						printf(" %-40s", "");

					//				if (n.norm.arithNorm().maybeDropConsts() != n.norm.arithNorm())
					//					printf("%-40s", n.norm.arithNorm());

					ntrees++;
					printf("%n");
					assert (!n.isRedundant());
					//					printf(" xx %s%n", n.norm.arithNorm().nodes.count(MultiNode.mlprod));
				}
				printf("%n");

				printf("----------------------- %n");
			}
		} else {
			printf("Only processing the following (because of only_process setting):%n");
			only_process.forEach(x -> printf("  %s%n", x));
		}

		printf("%n");

		// this was to generate lots of variants in order to test the effectiveness of all_base_e
		//		if (inpx.obj_variations > 0) {
		//			(only_process.isNotEmpty() ? only_process : numberedNodes)
		//					.forEach(n -> new BaronJob(inpx2, n, n.depth).obj_variations());
		//			die();
		//		}

		//		EVALLED 413.87526310327956 15814.59433125291
		//		OPT objval by   JOB(0,P):     413.88 P (38.2598*x^0.20623)
		//		if (false) {
		//			//			PVALS: [{P=12.352077276118816}, {P=35.14552215591456}, {P=100.0}, {P=284.53126846821147}, {P=809.5804273612941}]
		//			double accum = 0;
		//			for (DataLine line : inpx.input) {
		//				double x = line.invals.get(0);
		//				double y = line.outval;
		//				double val = ((-55.7386 * Math.pow(x, 0.477125)) + (88.5239 * Math.pow(x, 1.99814))
		//						+ (98.7147 * Math.pow(x, 0.389664)) + (-88.6402 * Math.pow(x, 1.997854)));
		//				//double val = ((-60.5127 * Math.pow(x, 0.536983)) + (100.0 * Math.pow(x, 0.45413)));
		//				double diff = y - val;
		//				accum += diff * diff;
		//				printf("%s %s %s %s%n", x, val, diff * diff, accum);
		//			}
		//			die();
		//		}
		return only_process.isNotEmpty() ? only_process : numberedNodes;
	}

	static boolean isMMSnode(NormNode n) {
		String s = n.toString();
		String orig = s;
		//		s = s.replaceAll("(P[+*/]P)", "P");
		s = s.replaceAll("\\(P[\\+\\*\\/]P\\)", "P");
		//		s = s.replaceAll("[\\+\\*\\/]", "W");
		s = s.replaceAll("\\(P\\*P\\*P\\)", "(P*P)");
		String want = "(pow(P)/(P*P))";
		//		printf("REPL %s %s %s%n", s, orig, s.equals(want));

		return s.equals(want);
		//		return true;
	}

	private void main1() throws Exception {
		//		heur1();
		UList<NormNode> numberedNodes = genNodes();

		Input2 inpx2 = new Input2(inpx, inpx.unsampled_input);

		if (inpx.inputSampleSize > 0) {
			if (inpx.inputSampleSize > inpx.unsampled_input.size()) {
				printf("IGNORING inputSampleSize, since too large (%s >= %s).%n", inpx.inputSampleSize,
						inpx.unsampled_input.size());
			} else {
				inpx2 = inpx2.input2(inpx.inputSampleSize, inpx.inputSampleSeed);
				printf("Sampled data:%n");
				inpx2.input.forEachI((i, line) -> printf("%2s: %s -> %s%n", i, line.invals, line.outval));

			}
		}

		JobPool jpool = new JobPool(inpx2, numberedNodes);
		//jpool.addShutdownHook();  // triggers on all exceptions, not useful

		//jpool = jpool.splitOnDropConst(3)
		if (inpx.printBaronFilesUpfront) {
			printf("Printing all jobs upfront - ubound is 0.0%n");
			jpool.jobs_by_id.forEach(x -> x.printBaron(0.0));
			//			die("printed all jobs");
		}

		UList<RunResult> res = inpx.reuse_baron_files ? jpool.reuse_baron_files() : jpool.run(false);
		//		printf("RES %s%n", res);
		report(res, true);

		if (inpx.print_timing_info)
			jpool.print_timing_info();

		//		jpool = new JobPool(inpx, report(res, true));
		//		res = jpool.run(true);
		//		report(res, true);

		final long endtm = System.currentTimeMillis();
		printf("Wall clock runtime: %.2f (%.2f over all procs)%n", (endtm - starttm) / 1000.0,
				sumd(jpool.jobs_by_id.map(x -> (double) x.runTime())) / 1000.0);
		System.exit(0);
		if (!inpx.kill_existing_baron_jobs)
			printf("DIDN'T KILL BARON JOBS - you may need to clean them up.%n");
	}

	public UList<NormNode> report(UList<RunResult> res0, boolean nobound) {
		UList<RunResultVal> res = res0.filterSubType(RunResultVal.class);

		double bestRes = mindOrElse(res.map(x -> x.val), 1e10);
		if (bestRes < 1e10) {
			double ubnd = inpx.focusOnBest ? (bestRes < 1e-2 ? 0.1 : 10 * bestRes) : (nobound ? 1e10 : 10 * bestRes);
			UList<RunResultVal> candres = res.filter(x -> x.val <= ubnd);

			//					: x.lbound.isPresent() ? x.lbound.get() <= ubnd : true);

			printf("%n------%n");
			int nunreported = res.size() - candres.size();
			if (nunreported > 0)
				printf("BOUND: %s (%s >= than this bound and so not reported)%n", ubnd, nunreported);

			//trueFalse.forEach(opt -> {
			candres.sort((x, y) -> chainCompare(String.format("%10.5f", x.val), String.format("%10.5f", y.val),
					x.baronJob.expr.nodeNum.get(), y.baronJob.expr.nodeNum.get())).forEach(r -> {
						int nn = r.baronJob.expr.nodeNum.get();
						boolean opt = r.optimal;
						if (inpx.unsampled_input != null)
							;
						ResForm simpform = r.resform.simpform();
						if (opt && (simpform == null || simpform.form() != r.resform.form())) {
							printf("%3s CAND %3s: REDUNDANT (%8.5f", opt ? "OPT" : "", nn, r.val);
							if (r.baronJob.inpx2.input.size() < r.baronJob.inpx.unsampled_input.size())
								printf(" %10.5f", r.evalObjective(r.baronJob.inpx.unsampled_input));
							printf(") %s %s%n", simpform == null ? "?" : simpform.form(), simpform);
						} else {
							printf("%3s CAND %3s: %10.5f", opt ? "OPT" : "", nn, r.val);
							//							else
							//								printf("%3s CAND %3s: %10s", opt ? "OPT" : "", nn, "??");

							//							if (r.lbound.isPresent() && r.lbound.get() > 0.0)
							//								printf(" >= %10.4f", r.lbound.get());
							//							else
							printf("    %10s", "");

							if (r.baronJob.inpx2.input.size() < r.baronJob.inpx.unsampled_input.size()) {
								//								if (r.val.isPresent())
								printf(" (%10.5f)", r.evalObjective(r.baronJob.inpx.unsampled_input));
								//								else
								//									printf("    %10s", "");
							}

							if (simpform != null)
								printf(" %20s S %s%n", r.baronJob.expr, simpform);
							else
								printf(" %20s %s%n", r.baronJob.expr, r.resform);
						}
					});
			//			});

			UList<RunResultVal> nonoptres = candres.filter(x -> !x.optimal);
			return nonoptres.map(x -> x.baronJob.expr);
		} else
			printf("NO RESULTS AT ALL - what now?%n");
		return UList.empty();
	}

	private UList<UList<NormNode>> addLevel(UList<UList<NormNode>> prevlevels, int depth, int maxdepth) {
		UList<NormNode> allprevlevels = concatULists(prevlevels);
		if (depth > maxdepth)
			return prevlevels;

		if (inpx.trace_treegen)
			printf("%n------- ADDLEVEL %s%n", depth);

		UList<Operator> ops = depth == maxdepth && inpx.root_only_operators.isNotEmpty() ? inpx.operators
				: inpx.operators.diff(inpx.root_only_operators);
		//		printf("OPS %s%n", ops);
		UList<NormNode> nodes1 = ops.flatMap(op -> {
			if (op.arity == 1)
				return addOp1(op, prevlevels.last());
			else if (op.arity == 2)
				return addOp2(op, prevlevels.last(), allprevlevels);
			//			else if (op.arity == 2) {
			//			else if (op == Operator.DIV) {
			//				return addOp2(prevlevels.last().filter(x -> x.op == Operator.MUL));
			//			}
			die("op %s", op);
			return null;
		});
		//		printf("NODES1 %s%n", nodes1);

		if (inpx.generateAllBNodes)
			return addLevel(prevlevels.add(nodes1), depth + 1, maxdepth);

		UMap<NormNode, UList<NormNode>> grouped = nodes1.groupingBy(n -> n.arithNorm());
		// choose the NormNode with smallest depth as the group representative
		UList<NormNode> outnodes = grouped.values().map(gp -> gp.min(nd -> nd.depth));

		//nuts
		UList<NormNode> outnodes1 = outnodes.map(n -> n.arithNorm()).maybeFilter(inpx.discard_redundant_exprs,
				n -> !n.isRedundant());

		//		printf("LEVEL %s %s%n", depth, outnodes);

		return addLevel(prevlevels.add(outnodes1), depth + 1, maxdepth);
	}

	//	private UList<NormNode> addOpPow(UList<NormNode> nodes) {
	//		return concatULists(inpx.power_operator_exponents.map(ex -> nodes.map(nd -> new NormNode(nd, ex))));
	//	}

	private UList<NormNode> addOp1(Operator op, UList<NormNode> nodes) {
		return nodes.map(nd -> getNormNode(op, mkUList1(nd)));
	}

	private UList<NormNode> addOp2(Operator op, UList<NormNode> last_prevlevels, UList<NormNode> allprevlevels) {
		//		UList<TNode> nonop = leafNodes.concat(UList.conv(nodes.filter(n -> n.op != op)));
		//		UList<TNode> opnodes = leafNodes.concat(UList.conv(nodes.filter(n -> n.op == op)));

		if (inpx.trace_treegen) {
			printf("ALLCOMBS %s %s%n", op, last_prevlevels);
			printf("            %s%n", allprevlevels);
		}

		UList<UList<NormNode>> combs0 = allCombs(mkUList1(last_prevlevels, allprevlevels)).distinct();

		// If you don't filter out P*P and P/P right away, you end up with lots of pointless expressions
		//XXXXXXX
		UList<UList<NormNode>> combs = // op == MUL || op == DIV ? combs0.filter(ns -> !ns.equals(inpx.twoprods)) :
				//op == ADD || op == SUB ? combs0.filter(ns -> !ns.equals(MultiNode.twosums)) : 
				combs0;
		if (inpx.trace_treegen)
			combs.dump("combs");

		UList<NormNode> ns1a = combs.map(ns -> getNormNode(op, ns));
		UList<NormNode> ns1 = op.isCommutative ? ns1a : ns1a.concat(combs.map(ns -> getNormNode(op, ns.reverse())));
		//		printf("DS5%n");
		//		allCombs(mkUList1(allprevlevels, last_prevlevels).diff(combs).distinct()).dump();

		if (inpx.generateAllBNodes) {
			//			die("haven't done this in a while - review");
			UList<UList<NormNode>> combs2 = allCombs(mkUList1(allprevlevels, last_prevlevels)).diff(combs).distinct();
			if (inpx.trace_treegen)
				combs2.dump("combs2");
			// ridiculous, but must be done
			//			if (mms_hack) {
			//				return ns1.concat(combs2.map(ns -> getNormNode(op, ns)));
			//			}
			return ns1.concat(combs2.map(ns -> getNormNode(op, ns)));
		}

		UMap<NormNode, UList<NormNode>> grouped = ns1.groupingBy(n -> n /*.norm*/);

		//		printf("%s %s%n", ns1.size(), grouped.size());

		return grouped.values().map(gp -> gp.get(0));
	}
}
