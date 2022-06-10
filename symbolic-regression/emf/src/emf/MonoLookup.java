// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.FormCode.*;
import static utils.UList.*;
import static utils.VRAUtils.*;

import utils.UList;

public class MonoLookup extends MTable {
	private final boolean has_neg_divs;
	private final boolean has_neg_vals;
	private final boolean has_zero_vals;
	private final int lookup1_sz = 1024;
	private final int[] comb_vals_divs_lookup_1;
	private final int lookup2_sz = 1024;
	//	private final double lookup2_max = 64.0;
	//	private final double lookup2_mul = lookup2_sz / lookup2_max;
	private final double lookup2_max;
	private final double lookup2_mul;

	private final int[] comb_vals_divs_lookup_2;

	// in sorted order  	 2D array (nmono * nLines1)
	private final double[] comb_vals_divs;
	// indices of the sorted comb_vals_divs
	private final int[] comb_vals_divs_inds;
	private final long[] comb_vals_divs_masks;

	// the actual comb_ind code, which is different from divs_inds using dims
	//	private final short[] comb_vals_divs_comb_inds;

	private final double[] comb_vals_divs1;
	private final double[] comb_vals_invsq;

	private double[] myy0 = new double[nLines];
	private double[] myyx = new double[nLines];
	private double[] myylo = new double[nLines];
	private double[] myyhi = new double[nLines];

	public final int pti_for_lookup;

	public final RunningSoln rsoln;

	public MonoLookup(Input inpx, int pti, RunningSoln rsoln) {
		super(inpx);

		this.rsoln = rsoln;

		inputx.forEach(line -> myy0[line.index] = line.outval);

		this.pti_for_lookup = pti;

		UList<Integer> rng2 = rangeOpen(nLines + 1);

		{
			// sort by: mask, then div1, then comb index
			// using a UList<Double> is actually easier than hassling with Pair.
			// NO REAL REASON FOR THE SORT ORDER BY VALUES.
			//			UList<UList<Double>> buf = rangeOpen(ncombs)
			//					.map(i -> rng2.map(j -> j == nLines ? (double) i
			//							: j == 0 ? (double) comb_dims_mask[i]
			//									: comb_vals[i * nLines + (j - 1)] / comb_vals[i * nLines + (j - 1) + 1]))
			//					.sort((xs, ys) -> chainCompare(xs, ys));

			// FOR NOW, sort by div
			UList<UList<Double>> buf = rangeOpen(ncombs).map(combind -> mkUList1(//(double) comb_dims_mask[combind],
					comb_vals[combind * nLines + pti] / comb_vals[combind * nLines + pti + 1],
					(double) comb_dims_mask[combind],
					//							comb_vals[i * nLines + pti2] / comb_vals[i * nLines + pti2 + 1],
					(double) combind)).sort((xs, ys) -> chainCompare(xs, ys));

			this.comb_vals_divs = new double[ncombs * nLines1];
			this.comb_vals_divs_inds = new int[ncombs];
			this.comb_vals_divs_masks = new long[ncombs];
			this.comb_vals_divs1 = new double[ncombs];
			for (int mdvi = 0; mdvi < ncombs; ++mdvi) {
				comb_vals_divs_masks[mdvi] = (int) (double) buf.get(mdvi).get(1);
				comb_vals_divs1[mdvi] = buf.get(mdvi).get(0);
				final int comb_ind = (int) (double) buf.get(mdvi).get(2);
				comb_vals_divs_inds[mdvi] = comb_ind;
				for (int j = 0; j < nLines1; ++j)
					comb_vals_divs[mdvi * nLines1 + j] = comb_vals[comb_ind * nLines + j]
							/ comb_vals[comb_ind * nLines + j + 1];
				// wasted div
				myassert(comb_vals_divs[mdvi * nLines1 + pti] == comb_vals_divs1[mdvi]);
			}
			//			this.comb_vals_divs_comb_inds = this.comb_inds();
		}
		// indexed by combind
		this.comb_vals_invsq = new double[ncombs];
		for (int i = 0; i < ncombs; ++i) {
			double sm2 = 0;
			for (int j = 0; j < nLines; ++j) {
				double F = comb_vals[i * nLines + j];
				sm2 += F * F;
			}
			comb_vals_invsq[i] = 1 / sm2;
		}

		{
			boolean neg = false;
			boolean zero = false;
			for (int ij = 0; ij < comb_vals.length; ++ij)
				if (comb_vals[ij] < 0)
					neg = true;
				else if (comb_vals[ij] == 0)
					zero = true;
			this.has_neg_vals = neg;
			this.has_zero_vals = zero;
			// SAVE THIS HEADACHE FOR LATER
			FORNOW(!has_neg_vals);
			FORNOW(!has_zero_vals);
		}

		{
			this.has_neg_divs = rangeOpen(ncombs).some(i -> comb_vals_divs[i * nLines1 + pti] < 0);
			FORNOW(!has_neg_divs);

			int lsz = lookup1_sz;
			final double lookup1_sz_inv = 1.0 / lookup1_sz;
			this.comb_vals_divs_lookup_1 = new int[lsz];
			//			myassert(pt <= comb_vals_divs[pt]);
			for (int divind = 0; divind < lsz; ++divind) {
				double divval = divind * lookup1_sz_inv;
				int i = 0;
				for (; i < ncombs && comb_vals_divs[i * nLines1 + pti] < divval; ++i)
					;
				//				dump("NC", ncombs, lookup1_sz, i, pti, divval, comb_vals_divs[0 * nLines1 + pti]);
				//				FORNOW(i < ncombs, comb_vals_divs[i * nLines + pti]);
				// with dims, fail to find a smaller div
				if (!(i <= ncombs))
					dumpdie(comb_vals_divs[i * nLines + pti]);
				comb_vals_divs_lookup_1[divind] = i;
			}
		}
		{
			int lsz = lookup2_sz;
			this.comb_vals_divs_lookup_2 = new int[lsz];
			double mx = 0;
			for (int i = 0; i < ncombs; ++i)
				mx = Math.max(mx, comb_vals_divs[i * nLines1 + pti]);
			this.lookup2_max = mx;
			this.lookup2_mul = lookup2_sz / lookup2_max;
			// values for divval<1 not actually used
			for (int divind = 0; divind < lsz; ++divind) {
				double divval = divind * (1.0 / lookup2_mul);
				int i = 0;
				for (; i < ncombs && comb_vals_divs[i * nLines1 + pti] < divval; ++i)
					;
				//				myassertEqual(divind, (int) (divval * lookup2_mul)); fails, only mostly true
				//				printf("divv %s %s==%s (%s %s)%n", i, divind, (int) (divval * lookup2_mul), divval, lookup2_mul);
				comb_vals_divs_lookup_2[divind] = i;
			}
		}
	}

	// crude lookup
	int lookup(double divlo) {
		if (divlo < 1) {
			int ind = (int) (divlo * lookup1_sz);
			return comb_vals_divs_lookup_1[ind];
		} else {
			int ind = (int) (divlo * lookup2_mul);
			return ind >= lookup2_sz ? ncombs : comb_vals_divs_lookup_2[ind];
		}
	}

	public void dumpLookup() {
		//		printf("divs%n");
		//		for (int i = 0; i < ncombs; ++i)
		//			printf("%5s: %10.4f %20s %s%n", i, comb_vals_divs1[i], inpx.mask_dims(comb_vals_divs_masks[i]),
		//					comb_vals_divs_masks[i]);
		;

		printf("divlo<1%n");
		for (int i = 0; i < lookup1_sz; ++i)
			printf("%5s: %5s%n", i, comb_vals_divs_lookup_1[i]);
		printf("divlo>=1%n");
		for (int i = 0; i < lookup2_sz; ++i)
			printf("%5s: %5s%n", i, comb_vals_divs_lookup_2[i]);
	}

	boolean switchkludge = false;

	double errsq(double[] xs, double[] y0) {
		myassert(xs.length == nLines);
		myassert(y0.length == nLines);
		double errsq = 0;
		for (int j = 0; j < nLines; ++j) {
			double val = y0[j] - xs[j];
			errsq += val * val;
		}
		return errsq;
	}

	public double tolx;
	public double maxTolErr;
	private int ntry0, ntry0b, ntry1, ntry2, ntry3, ntry4, ntry5, nediv1, nediv2, nnegh, nnegl, nnoneg, ntolviol;

	public UList<Number> tryvals() {
		return mkUList1(ntry0, ntry0b, ntry1, ntry2, ntry3, ntry4, ntry5, -100, nnoneg, nnegh, nnegl, ntolviol,
				//nediv1, nediv2,
				(double) (ntry2) / (ncombs * ncombs));
	}

	private static double POW(double x) {
		return Math.sqrt(x);
	}

	private static double INVPOW(double x) {
		return x * x;
	}

	// otherP is a value being subtracted from y0
	public int search(int msk, double[] otherP, double tol) {
		for (int j = 0; j < nLines; ++j) {
			myyx[j] = myy0[j] - otherP[j];
			myylo[j] = myyx[j] - tol;
			myyhi[j] = myyx[j] + tol;
		}
		return search(msk, myylo, myyhi, myyx, myy0, b_P, -1, -1, -1, otherP, -1, false);
	}

	int search(double[] clo, double[] chi, double[] yx, double[] y0) {
		int msk = 12345; // FIX FIX
		die();
		return search(msk, clo, chi, yx, y0, b_P, -1, -1, -1, null, -1, false);
	}

	public int nneg_chis = 0;

	private double[] cloNeg = new double[nLines], chiNeg = new double[nLines], yxNeg = new double[nLines];

	final boolean checkNegs = false;

	public int[] range_checked = new int[ncombs + 1];

	int search(int dimmsk, double[] cloIn, double[] chiIn, double[] yxIn, double[] y0, int fno, int p0ind, int p1ind,
			int p2ind, double[] tmp0, int caseno, boolean negone) {
		ntry0++;
		double[] clo = cloIn, chi = chiIn, yx = yxIn;
		long which_soln = rsoln.nupdates();
		//		for (int pti = 0; pti < nLines - 1; ++pti)
		boolean neglo0 = false;
		int best_monoi = -1;
		boolean negated = false; // whether we actually modified clo/chi/yx
		{
			boolean mustNotBeNegated = false; // whether some pairs required us not to negate
			boolean straddles = false;
			int last_straddle_ind = -2;
			for (int pti = 0; pti + 1 < nLines; ++pti) {
				int branch_taken = -1;
				boolean this_straddles = false;

				if (chi[pti] <= 0 || chi[pti + 1] <= 0) { // one high neg/zer
					if (chi[pti] <= 0 && chi[pti + 1] <= 0) { // both highs neg/zero
						if (negated || mustNotBeNegated) {
							// already negated, or had to be as-is - impossible
							if (checkNegs)
								myassertEqual(tryall(yx, y0, fno, caseno, negone, p0ind, p1ind, p2ind, tmp0), -1, yx,
										y0, fno, p0ind, p1ind, p2ind, tmp0, pti, negated, mustNotBeNegated, clo, chi,
										cloIn, chiIn);
							if (foodebug)
								println("ret x1");
							return best_monoi;
						}

						// [..... 0] 
						// [..... 0]
						myassert(clo == cloIn);
						clo = cloNeg;
						chi = chiNeg;
						yx = yxNeg;
						for (int j = 0; j < nLines; ++j) {
							clo[j] = -chiIn[j];
							chi[j] = -cloIn[j];
							yx[j] = -yxIn[j];
						}
						negated = true;
						branch_taken = 1;
					} else { // ==> one high neg/zero, one high pos
						//						int pthineg = (chi[pti] <= 0) ? pti : pti + 1;
						int pthipos = (chi[pti] <= 0) ? pti + 1 : pti;

						if (clo[pthipos] >= 0) {
							// [....0] 
							//     [0.....]
							myassert(!has_neg_vals);
							myassert(!has_zero_vals); // lazy, doing both together
							if (foodebug) {
								printf("ret x2 %s %s %10.5f%n", pti, pthipos, clo[pthipos]);
								dump(clo);
							}
							return best_monoi;
						} else {
							if (negated || mustNotBeNegated) {
								// already negated, or had to be as-is - impossible
								if (checkNegs)
									myassertEqual(tryall(yx, y0, fno, caseno, negone, p0ind, p1ind, p2ind, tmp0), -1,
											yx, y0, fno, p0ind, p1ind, p2ind, tmp0);
								if (foodebug)
									println("ret x3");
								return best_monoi;
							}

							// [......0] 
							//    [...0...]
							// cut off high bound to:
							// [......0] 
							//    [...0]
							// then negate.  or rather, first negate, then cut off lbound.

							// note that others may fail for the same reason, checked in a later iteration
							myassert(clo == cloIn);
							clo = cloNeg;
							chi = chiNeg;
							yx = yxNeg;
							for (int j = 0; j < nLines; ++j) {
								clo[j] = -chiIn[j];
								chi[j] = -cloIn[j];
								yx[j] = -yxIn[j];
							}
							clo[pthipos] = 0;
							negated = true;
							branch_taken = 2;
						}
					}
				} else if (clo[pti] <= 0 || clo[pti + 1] <= 0) { // one+ lows neg/zero		

					if (clo[pti] <= 0 && clo[pti + 1] <= 0) { // both lows neg/zero			
						// [.....0....] 
						//   [...0...]
						if (negated || mustNotBeNegated) {
							if (clo == cloIn) {
								// first copy.  may not for mustNoBeNegated
								clo = cloNeg;
								chi = chiNeg;
								yx = yxNeg;
								for (int j = 0; j < nLines; ++j) {
									clo[j] = cloIn[j];
									chi[j] = chiIn[j];
									yx[j] = yxIn[j];
								}
							}

							clo[pti] = 0;
							clo[pti + 1] = 0;
							branch_taken = 3;
						} else {
							// try both??
							//							dumpdie(pti, clo[pti], chi[pti], clo[pti + 1], chi[pti + 1]);
							this_straddles = true;
							branch_taken = 4;
						}
					} else { // exactly one low neg/zero
						// [...0....] 
						//    [0...]
						// cut off to:
						//    [0.....] 
						//    [0...]
						// do NOT negate, so this case be done more than once
						if (clo == cloIn) {
							clo = cloNeg;
							chi = chiNeg;
							yx = yxNeg;
							for (int j = 0; j < nLines; ++j) {
								clo[j] = cloIn[j];
								chi[j] = chiIn[j];
								yx[j] = yxIn[j];
							}
						}

						clo[pti] = 0;
						clo[pti + 1] = 0;
						if (!negated) // if this was negated, we just truncated, no special case
							mustNotBeNegated = true;
						branch_taken = 5;
					}
				} else {
					// this pair didn't require negation, so now we CAN'T negate 
					// do NOT copy, no changes yet
					if (!negated) // if this was negated, we just truncated, no special case
						mustNotBeNegated = true;
					branch_taken = 6;
				}

				if (this_straddles) {
					if (last_straddle_ind < 0)
						// only do this once for an unbroken series of this_straddles
						straddles = true;
				} else if (straddles && last_straddle_ind < 0) {
					last_straddle_ind = pti;
					straddles = false;
				}
				myassert(!(negated && mustNotBeNegated));
				//				dump(pti, branch_taken, clo);
			}
			//			nneg_chis++;
			if (straddles) {
				// try both
				myassert(clo == cloIn);
				boolean lo0 = true;
				boolean hi0 = true;
				for (int pti = 0; pti < nLines; ++pti) {
					if (clo[pti] != 0)
						lo0 = false;
					if (chi[pti] != 0)
						hi0 = false;
				}
				// lo0/hi0 can be result of recursive call
				if (!lo0 && !hi0) {
					double[] ctmp = new double[nLines]; // inited to 0s

					int ind1 = search(dimmsk, ctmp, chiIn, yxIn, y0, fno, p0ind, p1ind, p2ind, tmp0, caseno, negone);
					int ind2 = search(dimmsk, cloIn, ctmp, yxIn, y0, fno, p0ind, p1ind, p2ind, tmp0, caseno, negone);

					// second search info in place
					// JUST RETURN SECOND
					return ind2;
				}
			}
			//				FORNOW(!straddles, clo, chi);

			// doesn't completely straddle - must have been set positive, just truncate lbounds 
			for (int pti = 0; pti < last_straddle_ind; ++pti)
				clo[pti] = 0;
		}

		int pti = 0;
		if (foodebug)
			println("here");

		ntry0b++;

		// assuming no neg divs, don't need to check neg
		FORNOW(!has_neg_divs);
		{
			int init_mdvi;

			final double inf = 1e10;
			int mdvi;
			double divhi;
			double divlo = -11111;
			if (clo[pti] < 0) {
				init_mdvi = mdvi = 0;
				//				ntry0b++;
				neglo0 = true;
				if (clo[pti + 1] < 0) {
					divhi = inf;
					//					ntry0b++;
				} else {
					divhi = chi[pti] / clo[pti + 1];
				}
			} else {
				//				double 
				divlo = clo[pti] / chi[pti + 1];
				init_mdvi = mdvi = lookup(divlo);
				if (clo[pti + 1] < 0) {
					divhi = inf;
					//					ntry0b++;
				} else
					divhi = chi[pti] / clo[pti + 1];
				//for (; mdvi < ncombs && comb_vals_divs[mdvi * nLines1 + pti] < divlo; ++mdvi)
				for (; mdvi < ncombs && comb_vals_divs1[mdvi] < divlo; ++mdvi)
					;
			}

			//			if (false) {
			//				printf("clo %s%n", mkUList(clo));
			//				printf("chi %s%n", mkUList(chi));
			//				printf("divlo %s%n", divlo);
			//				printf("divhi %s%n", divhi);
			//			}

			//			int mdvi = lookup.lookup(divlo, divhi);
			//			myassert(mdvi == lookup2.lookup(divlo, divhi));
			//			printf("LOK %s%n", mdvi);

			if (mdvi == ncombs) {
				if (foodebug)
					printf("ret -1%n");
				return -1;
			}
			//			printf("GOT? %s %s %.10f %s%n", mdvi, divlo, comb_vals_divs[mdvi * nLines1 + pti], divhi);

			//if (mdvi < ncombs && comb_vals_divs[mdvi * nLines1 + pti] <= divhi)
			if (true) {
				if (mdvi < ncombs && comb_vals_divs1[mdvi] <= divhi)
					ntry1++;
			}
			if (foodebug) {
				printf("DV %s %12.4f %12.4f%n", init_mdvi, divlo, divhi);
				dump(yx, clo, chi);
				for (int mi = 0; mi <= mdvi; ++mi) {
					int ind = comb_vals_divs_inds[mi];
					printf("dx %5s %5s %12.4f %8s %s%n", mi, ind, comb_vals_divs1[mi], comb_vals_divs_masks[mi],
							comb_vals_divs_inds[mi] == foodebugind ? (foodebugind < init_mdvi ? "X????" : "X")
									: comb_vals_divs_inds[mi] == init_mdvi ? "START" : "");
					//					myassert(comb_vals_divs_inds[mi] != foodebugind, mi, mdvi);
				}
				printf("clo %s%n", mkUList(clo));
				printf("chi %s%n", mkUList(chi));
				printf("yx %s%n", mkUList(yx));
				printf("divlo %s%n", divlo);
				printf("divhi %s%n", divhi);
				//				dumpdie(divlo, divhi, clo[pti]);
			}
			boolean use_dims = inpx.use_dimensional_analysis;

			int first_mdvi_checked = mdvi;
			//for (; mdvi < ncombs && comb_vals_divs[mdvi * nLines1 + pti] <= divhi; ++mdvi) {
			//myassert(comb_vals_divs1[mdvi] == comb_vals_divs[mdvi * nLines1 + pti]);
			for (; mdvi < ncombs && comb_vals_divs1[mdvi] <= divhi; ++mdvi) {
				ntry2++;
				if ((!use_dims || dimmsk == comb_vals_divs_masks[mdvi]) && search_rest(clo, chi, 1, mdvi)) {
					ntry4++;
					int monoind = comb_vals_divs_inds[mdvi];

					double C0;
					//					if (halfHack)
					//						C0 = 0.5;

					if (allow1Const) {
						//										https://en.wikipedia.org/wiki/Least_squares   Regression analysis and statistics
						//						C0 = (0.5 * (clo[0] + chi[0])) / comb_vals[monoind * nLines + 0];// FOR NOW
						double sm1 = 0;
						double rsm2 = comb_vals_invsq[monoind];
						if (negated)
							rsm2 = -rsm2;
						//						double sm2 = 0;
						for (int j = 0; j < nLines; ++j) {
							double y = yx[j]; //  y0[j] - yx[j];
							double F = comb_vals[monoind * nLines + j];
							sm1 += F * y;
							//							sm2 += F * F;
							//							if (foodebug && comb_vals_divs1_inds[mdvi] == foodebugind)
							//								printf("F %s %s %s%n", y, F, sm1);
						}
						//						C0 = sm1 / sm2;
						//						if (foodebug && comb_vals_divs1_inds[mdvi] == foodebugind)
						//							printf("C0 %s %s %s%n", sm1, rsm2, sm1 * rsm2);
						C0 = sm1 * rsm2;
					} else
						C0 = negated ? -1.0 : 1.0;

					double errsq = 0.0;
					switch (fno) {
					case b_P:
					case b_2P:
					case b_3P:
					case b_4P:
						for (int j = 0; j < nLines; ++j) {
							//				double fval = C0 * comb_vals[monoind * nLines + j] + ev.apply(monoind, j);
							double fval = C0 * comb_vals[monoind * nLines + j] + tmp0[j];
							double xv = y0[j] - fval;
							errsq += xv * xv;
						}
						if (fno == b_P)
							rsoln.update(errsq, fno, caseno, C0, monoind);
						else if (fno == b_2P)
							rsoln.update(errsq, fno, caseno, C0, p0ind, monoind);
						else if (fno == b_3P)
							rsoln.update(errsq, fno, caseno, C0, p0ind, p1ind, monoind);
						else if (fno == b_4P)
							rsoln.update(errsq, fno, caseno, C0, p0ind, p1ind, p2ind, monoind);
						else
							die();
						break;

					case b_pow2P:
						for (int j = 0; j < nLines; ++j) {
							//				double fval = C0 * comb_vals[monoind * nLines + j] + ev.apply(monoind, j);
							double fval = POW(C0 * comb_vals[monoind * nLines + j] + comb_vals[p0ind + nLines + j]);
							double xv = y0[j] - fval;
							errsq += xv * xv;
						}
						rsoln.update(errsq, fno, caseno, C0, p0ind, monoind);
						break;

					case b_Pdiv2P:

						//CP/(P+P)
					case b_Pdivpow2P:
						//CP/pow(P+P)
						if (caseno == 0) {
							for (int j = 0; j < nLines; ++j) {
								//								double fval = C0 * comb_vals[monoind * nLines + j] / tmp0[j];
								double fval = C0 * comb_vals[monoind * nLines + j] * tmp0[j];
								double xv = y0[j] - fval;
								errsq += xv * xv;
							}
							rsoln.update(errsq, fno, caseno, C0, p0ind, p1ind, monoind);
						} else {
							myassert(caseno == 1);
							for (int j = 0; j < nLines; ++j) {
								// other case:
								// y=P/pow(CP+P) ==> pow(CP+P)=P/y ==> CP+P=invpow(P/y) ==> CP=invpow(P0/y)-P1   P0/pow(P2+P1)
								double p0 = comb_vals[p0ind * nLines + j];
								double p1 = comb_vals[p1ind * nLines + j];
								double p2 = comb_vals[monoind * nLines + j];
								double fval = p0 / POW(C0 * p2 + p1);

								double xv = y0[j] - fval;
								//								if (mdvi == 14)
								//									printf("Mx %12.4f %12.4f %12.4f %12.4f %12.4f %12.4f %s %s%n", xv, p0, p1, p2, fval,
								//											y0[j], C0, negated);
								errsq += xv * xv;
								if (errsq > inpx.hopelessJobThresh) {
									range_checked[mdvi - first_mdvi_checked]++;
									return best_monoi;
								}
							}
							rsoln.update(errsq, fno, caseno, C0, p0ind, p1ind, monoind);
						}
						break;

					case b_2Pdivpow2P:
						//  ((P+P)/pow((P+P)))
						if (caseno == 0)
							//  (CP+P)/pow((P+P))  y=(CP3+P0)/pow(P1+P2) ==> y*pow(P1+P2)=(CP3+P0) ==> y*pow(P1+P2)-P0=CP3
							// negone ==> P1 negated
							for (int j = 0; j < nLines; ++j) {
								double p0 = comb_vals[p0ind * nLines + j];
								//								double p1 = comb_vals[p1ind * nLines + j];
								//								double p2 = comb_vals[p2ind * nLines + j];
								double p3 = comb_vals[monoind * nLines + j];

								//								double fval = (C0 * p3 + p0) / tmp0[j];
								double fval = (C0 * p3 + p0) * tmp0[j];

								double xv = y0[j] - fval;
								//								if (comb_vals_divs1_inds[mdvi] == foodebugind)
								//									printf(" 2p %2s %12.4f %12.4f %12.4f %12.4f %n", j, p0, p3, fval, xv);
								errsq += xv * xv;
							}
						else if (caseno == 1) {
							die();
						} else
							die();
						rsoln.update(errsq, fno, caseno, C0, p0ind, p1ind, monoind);
						break;
					default:
						die();
					}

					if (foodebug)
						printf("FD %5s %5s %12.4f %s %s%n", mdvi, comb_vals_divs_inds[mdvi], comb_vals_divs1[mdvi],
								comb_vals_divs_inds[mdvi] == foodebugind ? "XX" : "",
								comb_vals_divs_inds[mdvi] == foodebugind ? errsq : "");

					//					double foox = 0;
					//					double fooy = 0;
					//					for (int j = 0; j < nLines; ++j) {
					//						double a = comb_vals[monoind * nLines + j];
					//						double x0 = y0[j] - yx[j];
					//						foox += a * x0;
					//						fooy += a * a;
					//					}
					//					printf("F %s %s%n", errsq, foox * foox / fooy);

					if (false) {
						int negh = 0;
						int negl = 0;
						boolean tolviol = false;
						double negtolviol = 0;
						for (int j = 0; j < nLines; ++j) {
							double fval = C0 * comb_vals[monoind * nLines + j] + yx[j];
							//							if (fval < 0)
							if (chi[j] < 0) {
								negh++;
								//								negtolviol += chi[j] * chi[j];
							} else if (clo[j] < 0)
								negl++;
							else if (Math.abs(y0[j] - fval) > tolx)
								tolviol = true;
						}
						//if (negh == nLines)
						if (negh > 0)
							nnegh++;
						//else if (negl == nLines)
						else if (negl > 0)
							nnegl++;
						else
							nnoneg++;
						if (tolviol)
							//						if (negtolviol > nLines * (tolx * tolx))
							ntolviol++;
						if (negh == 0 && negl == 0 && tolviol) {
							foodebug = true;
							search_rest(clo, chi, 1, mdvi);
							//							die();
						}
					}

					//					printf("tried %s %s %s%n", monoind, C0, errsq);
					//					dump("GO?", monoind, C0, errsq);

				}
			}

			//			if (neglo0)
			//				ntry0b += mdvi;
			range_checked[mdvi - first_mdvi_checked]++;
		}
		//	return best_monoi;
		return rsoln.nupdates() > which_soln ? 0 : -1;
	}

	private final boolean doEDiv = false;
	private final boolean checkEDiv = false;
	double foo, foo2;
	int fooind;
	boolean foobool;
	boolean foodebug;
	int foodebugind;

	private boolean search_rest(double[] clo, double[] chi, int pti0, int mdvi) {
		FORNOW(!has_neg_divs);

		if (true)
			return true;

		double negtolviol = 0;
		for (int pti = pti0; pti < nLines1; ++pti) {
			ntry3++;
			//			if (foodebug)
			//				dump("DIVS", clo[pti], chi[pti], clo[pti + 1], chi[pti + 1], comb_vals_divs[mdvi * nLines1 + pti]);

			//			if (chi[pti] < 0 || chi[pti + 1] < 0)
			//				return false;
			//				continue;
			if (chi[pti] < 0) {
				negtolviol += chi[pti] * chi[pti];
				continue;
			}
			if (chi[pti + 1] < 0)
				continue;

			double divhi, divlo = 0;
			//			ntry4++;

			FORNOW(!has_neg_divs);
			final double inf = 1e10;
			boolean allok = true;
			if (checkEDiv || doEDiv) {
				if (clo[pti] < 0) {
					divlo = -inf;
					if (clo[pti + 1] < 0) {
						continue;
					} else {
						divhi = chi[pti] / clo[pti + 1];
					}
				} else {
					divlo = clo[pti] / chi[pti + 1];
					myassert(divlo >= 0, clo[pti], chi[pti + 1]);

					if (clo[pti + 1] < 0)
						divhi = inf;
					else
						divhi = chi[pti] / clo[pti + 1];
				}

				double dval = comb_vals_divs[mdvi * nLines1 + pti];

				int monoind = comb_vals_divs_inds[mdvi];
				double monox = comb_vals[monoind * nLines + pti];
				double monoy = comb_vals[monoind * nLines + pti + 1];
				if (checkEDiv)
					myassertEqual(dval, monox / monoy);

				if (checkEDiv) {
					if (!(clo[pti] < 0)) {
						myassertLE_GE(divlo <= dval, clo[pti] / chi[pti + 1], monox / monoy);
						myassertLE_GE(chi[pti + 1] < 0 ? !(divlo <= dval) : divlo <= dval, clo[pti] * monoy,
								chi[pti + 1] * monox, comb_vals[monoind * nLines + pti + 1], chi[pti + 1],
								comb_vals[monoind * nLines + pti]);
					}

					if (!(clo[pti + 1] < 0)) {
						myassertLE_GE(dval <= divhi, monox / monoy, chi[pti] / clo[pti + 1]);
						myassertLE_GE(clo[pti + 1] < 0 ? !(dval <= divhi) : dval <= divhi, clo[pti + 1] * monox,
								chi[pti] * monoy, comb_vals[monoind * nLines + pti + 1], chi[pti + 1],
								comb_vals[monoind * nLines + pti]);
					}
				}
				//				printf("DIV %s %s %s %s %s%n", divlo <= dval, dval <= divhi, divlo, dval, divhi);
				if (!(divlo <= dval && dval <= divhi))
					allok = false;
			}
			if (doEDiv) {

				continue;
			}

			boolean allok2 = true;

			int monoind = comb_vals_divs_inds[mdvi];
			double monox = comb_vals[monoind * nLines + pti];
			double monoy = comb_vals[monoind * nLines + pti + 1];

			if (clo[pti] < 0) {

				if (clo[pti + 1] < 0) {
					continue;
				} else {
					if (!(clo[pti + 1] * monox <= chi[pti] * monoy))
						allok2 = false;
				}
			} else {
				if (!(clo[pti] * monoy <= chi[pti + 1] * monox))
					allok2 = false;

				if (clo[pti + 1] < 0)
					;
				else if (!(clo[pti + 1] * monox <= chi[pti] * monoy))
					allok2 = false;
				if (false) {
					foo = monoy;
					foo2 = monox;
					fooind = monoind;
					foobool = allok2;
					//					99 [1195739, 575209847, 0, 504524218, 0, 0, 2.356059533312]

					return false;

					//					 SWITCHKLUDGE 9.203 15625 false true [1195739, 575209847, 0, 511951258, 0, 0, 2.356059533312] 0 -1 -1 -1 0.0 1.0E10

				}
			}

			//			if (checkEDiv) {
			//				double dval = comb_vals_divs[mdvi * nLines1 + pti];
			//				if (!(clo[pti] < 0)) {
			//					myassertLE_GE(divlo <= dval, clo[pti] / chi[pti + 1], monox / monoy);
			//					myassertLE_GE(chi[pti + 1] < 0 ? !(divlo <= dval) : divlo <= dval, clo[pti] * monoy,
			//							chi[pti + 1] * monox, comb_vals[monoind * nLines + pti + 1], chi[pti + 1],
			//							comb_vals[monoind * nLines + pti]);
			//				}
			//
			//				myassertEqual(allok, allok2, clo[pti], clo[pti + 1],
			//						// lowcheck2
			//						(clo[pti] * monoy <= chi[pti + 1] * monox),
			//						// vals
			//						clo[pti], monoy, chi[pti + 1], monox,
			//						// hicheck2
			//						(clo[pti + 1] * monox <= chi[pti] * monoy),
			//
			//						dval,
			//						//lowcheck
			//						clo[pti] / chi[pti + 1] <= dval,
			//						//hicheck
			//						dval <= chi[pti] / clo[pti + 1]);
			//			}
			if (!allok2)
				return false;

			//			double dval0 = comb_vals_divs[mdvi * nLines1 + 0];
			//			double dval = comb_vals_divs[mdvi * nLines1 + pti];
			//			dump("DIVS", dval0, dval, divlo, divhi, monoind);
		}
		//		printf("fit%n");
		if (chi[nLines1] < 0)
			negtolviol += chi[nLines1] * chi[nLines1];
		//		if (negtolviol > maxTolErr)
		//			return false;

		return true;
	}

	//	private double[] save_errTol2 = new double[ncombs];
	private double[] save_errSq = new double[ncombs];
	private int nsave_err = 0;

	//	public double[] save_errTol() {
	//		double[] rval = save_errTol;
	//		save_errTol = new double[0];
	//		return rval;
	//	}

	public double[] save_errSq() {
		double[] rval = save_errSq;
		save_errSq = new double[ncombs];
		return rval;
	}

	public boolean alteval = false;
	int ae_monoi0 = -1;
	int ae_monoi1 = -1;
	int ae_monoi2 = -1;

	int tryall(double[] yx, double[] y0) {
		die();
		return 0;
	}

	int ntryall = 0;

	int tryall(double[] yx, double[] y0, int fno, int caseno, boolean negone, int p0ind, int p1ind, int p2ind,
			double[] tmp0) {
		nsave_err = 0;

		if ((ntryall++ & 127) == 0)
			printf("tryall%n");
		int best_monoi = -1;

		//		if (alteval)
		//			myassert(!pipeline);

		double preC0 = 1.0;
		//			if (halfHack)
		//				C0 = 0.5;
		for (int mdvi = 0; mdvi < ncombs; ++mdvi) {
			double C0 = 1.0;
			int monoind = mdvi;

			if (allow1Const) {
				//										https://en.wikipedia.org/wiki/Least_squares   Regression analysis and statistics
				//						C0 = (0.5 * (clo[0] + chi[0])) / comb_vals[monoind * nLines + 0];// FOR NOW
				double sm1 = 0;
				//						double sm2 = 0;
				for (int j = 0; j < nLines; ++j) {
					double y = yx[j]; //  y0[j] - yx[j];
					double F = comb_vals[monoind * nLines + j];
					sm1 += F * y;
					//							sm2 += F * F;
				}
				//						C0 = sm1 / sm2;
				C0 = sm1 * comb_vals_invsq[monoind];
			}

			double errsq = 0.0;
			switch (fno) {
			case b_P:
			case b_2P:
			case b_3P:
			case b_4P:
				for (int j = 0; j < nLines; ++j) {
					//				double fval = C0 * comb_vals[monoind * nLines + j] + ev.apply(monoind, j);
					double fval = C0 * comb_vals[monoind * nLines + j] + yx[j];
					double xv = y0[j] - fval;
					errsq += xv * xv;
				}
				if (fno == b_P)
					rsoln.update(errsq, fno, caseno, C0, monoind);
				else if (fno == b_2P)
					rsoln.update(errsq, fno, caseno, C0, p0ind, monoind);
				else if (fno == b_3P)
					rsoln.update(errsq, fno, caseno, C0, p0ind, p1ind, monoind);
				else if (fno == b_4P)
					rsoln.update(errsq, fno, caseno, C0, p0ind, p1ind, p2ind, monoind);
				else
					die();
				break;

			case b_pow2P:
				for (int j = 0; j < nLines; ++j) {
					//				double fval = C0 * comb_vals[monoind * nLines + j] + ev.apply(monoind, j);
					double fval = POW(C0 * comb_vals[monoind * nLines + j] + comb_vals[p0ind + nLines + j]);
					double xv = y0[j] - fval;
					errsq += xv * xv;
				}

				rsoln.update(errsq, fno, caseno, C0, p0ind, monoind);
				break;

			case b_Pdiv2P:

				//CP/(P+P)
			case b_Pdivpow2P:
				//CP/pow(P+P)
				for (int j = 0; j < nLines; ++j) {
					double fval = C0 * comb_vals[monoind * nLines + j] / //(comb_vals[p0ind * nLines + j] + comb_vals[p1ind * nLines + j]);
							tmp0[j];
					double xv = y0[j] - fval;
					errsq += xv * xv;
				}

				die();
				rsoln.update(errsq, fno, caseno, C0, p0ind, p1ind, monoind);
				break;

			case b_2Pdivpow2P:
				//  ((P+P)/pow((P+P)))
				if (caseno == 0)
					//  (CP+P)/pow((P+P))  y=(CP3+P0)/pow(P1+P2) ==> y*pow(P1+P2)=(CP3+P0) ==> y*pow(P1+P2)-P0=CP3
					// negone ==> P1 negated
					for (int j = 0; j < nLines; ++j) {
						double p0 = comb_vals[p0ind * nLines + j];
						//								double p1 = comb_vals[p1ind * nLines + j];
						//								double p2 = comb_vals[p2ind * nLines + j];
						double p3 = comb_vals[monoind * nLines + j];

						//								double fval = (C0 * p3 + p0) / tmp0[j];
						double fval = (C0 * p3 + p0) * tmp0[j];

						double xv = y0[j] - fval;
						errsq += xv * xv;
					}
				else if (caseno == 1) {
					die();
				} else
					die();
				break;

			default:
				die();
			}

			//				if (alteval) {
			//					double p0 = comb_vals[ae_monoi0 * nLines + j];
			//					double p1 = comb_vals[ae_monoi1 * nLines + j];
			//					double p2 = comb_vals[ae_monoi2 * nLines + j];
			//					double p3 = comb_vals[basei + j];
			//					fval = p0 + p1 / (p2 + C0 * p3);
			//					if (ae_monoi0 == 12 && ae_monoi1 == 6 && ae_monoi2 == 0 && monoind == 6) {
			//						dump(p0, p1, p2, C0, p3);
			//						printf("MX %s %s %s%n", fval, y0[j], C0);
			//					}
			//
			//				}
			//				if (fval >= 1)
			//					blockind |= (1 << j);

			//				if (ae_monoi0 == 12 && ae_monoi1 == 6 && ae_monoi2 == 0 && monoind == 6)
			//					printf("MXe %s %s%n", xv, errsq);
			//				Math.abs(xv));
			//				if ()
			//				maxerr = Math.max(maxerr, 

			//						save_errTol2[nsave_err2] = maxerr;
			save_errSq[nsave_err] = errsq;
			nsave_err++;

			//					dump("GO?", monoind, C0, errsq);
		}

		return best_monoi;
	}

	public double best_errsq() {
		return rsoln.errsq();
	}
}
