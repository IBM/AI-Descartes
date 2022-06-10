// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static emf.FormCode.*;
import static utils.UList.*;
import static utils.VRAUtils.*;

import java.util.Arrays;

import utils.GrowList;
import utils.UList;
import utils.UMap;

public class Dumb {
	public final Input inpx;
	public final UList<DataLine> inputx;
	public final int nLines;
	public final int nLines1;
	public final int numInVars;
	public final int monosz;
	public final boolean allow1Const;

	private final RunningSoln rsoln;

	// 2D array (nmono * nLines)
	private final double[] pt_monovals;

	private final UList<Integer> unsampledInputRng;
	private MonoLookup lookup0, lookup1;

	private final double tol;
	private double y0[];
	private double invy0[];
	private double invy0addTol[];
	private double invy0subTol[];

	public Dumb(Input inpx) {
		this.inpx = inpx;
		this.inputx = inpx.inputSampleSize == 0 ? inpx.unsampled_input : inpx.unsampled_input.subList(0, 10);
		this.numInVars = inpx.numInVars;
		this.nLines = inputx.size();
		this.nLines1 = nLines - 1;
		this.unsampledInputRng = rangeOpen(inputx.size());
		this.rsoln = new RunningSoln(this);

		this.allow1Const = inpx.max_num_consts == 1;
		//		this.allow1Const = false;
		if (inpx.max_num_consts > 1)
			die("more than 1 const not yet supported");

		inputx.dump("unsamp");

		this.tol = chooseTol();
		this.y0 = new double[nLines];
		this.invy0 = new double[nLines];
		this.invy0addTol = new double[nLines];
		this.invy0subTol = new double[nLines];
		inputx.forEach(line -> y0[line.index] = line.outval);
		inputx.forEach(line -> invy0[line.index] = 1.0 / line.outval);
		inputx.forEach(line -> invy0addTol[line.index] = 1.0 / (line.outval + tol));
		inputx.forEach(line -> invy0subTol[line.index] = 1.0 / (line.outval - tol));
		printf("Y %s %s%n", tol, mkUList(y0));

		codesizeOK();

		MTable ptbl = MTable.pTable(inpx);
		{
			boolean checkMonos = false;

			this.monosz = (int) Math.pow(2 * inpx.max_var_exp + 1, numInVars);
			this.pt_monovals = new double[monosz * nLines];
			for (int i = 0; i < monosz; ++i) {
				enx(pt_monovals, i, inputx, i, inpx.max_var_exp, numInVars);
				if (checkMonos)
					for (int j = 0; j < nLines; ++j) {
						dump(monoList(i));
						double xv = eval_monoList(monoList(i), j);
						myassertTolEqual("", xv, pt_monovals[i * nLines + j], xv * 0.0001);
					}
			}
			myassert(pt_monovals.length == monosz * nLines);
			myassert(Arrays.equals(ptbl.comb_vals, pt_monovals));

			this.lookup0 = new MonoLookup(inpx, 0, rsoln);
			this.lookup1 = new MonoLookup(inpx, 1, rsoln);
		}

		final int sz = monosz;

		//		sort(rangeOpen(monosz).map(i ->
		//		pt_monovals[i * nLines] / pt_monovals[i * nLines + 1])).dump("");
		//		die();

		if (false) {
			rangeOpen(sz).forEach(i -> printf("%x %s%n", code(pt_monovals, i), slice(pt_monovals, i)));

			UMap<Long, Integer> mp = rangeOpen(sz).map(i -> code(pt_monovals, i)).popCount();

			mp.forEachEntry((key, val) -> printf("%12x %x%n", key, val));

			//		mp.reverseMap().mapValues(x -> x.size()).sortByKeys(compareInts).dump();
			mp.reverseMap().sortByKeys(compareInts).forEachEntry(
					(key, val) -> printf("%12s %6s %s%n", key, val.size(), val.map(x -> String.format("%x", x))));
		}

		//		monovals.map(x-> )
		//		for (int j = 0; j < numInVars; ++j) {
		//			double vmx = monovals.first()[j];
		//			double vmn = vmx;
		//			GrowList<Double> outx = new GrowList<>();
		//			for (int i = 0; i < monovals.size(); ++i) {
		//				double x[] = monovals.get(i);
		//				vmx = Math.max(vmx, x[j]);
		//				vmn = Math.min(vmn, x[j]);
		//				outx.add(x[j]);
		//			}
		//			sort(outx.elems());
		//			//			printf("M %s %s %s%n", j, vmn, vmx, )
		//		}

		//		Collections.max(null);

		starttm = System.currentTimeMillis();
	}

	private final static int codeSz = 6;
	//	private final int codeVal = Math.pow(2, codeSz);

	private void codesizeOK() {
		long p = 1;
		for (int j = 0; j < nLines; ++j) {
			//long p1 = p << (2 * codeSz);
			long p1 = p << codeSz;
			// no overflow
			myassert(p1 > p);
			p = p1;
		}
	}

	private UList<Double> slice(double[] data, int mono_ind) {
		int base = mono_ind * nLines;
		return unsampledInputRng.map(j -> data[base + j]);
	}

	private final int invp = 1;

	private double nextcode_c;

	private long nextcode(double[] data, double[] origdata, double c) {
		die();
		final double inf = 1000000;
		double delta_c = inf;
		int delta_j = -1;
		for (int j = 0; j < nLines; ++j) {
			double x = data[j];

			//			printf("x %s%n", x);
			if (x < 0.5) {
				double p = 0.5;
				; // 0
				double c0 = (p - x) / origdata[j];
				if (delta_c > c0) {
					delta_c = c0;
					delta_j = j;
				}
			} else {
				long p = 1; // p==2^pi
				//				int pi = 0;
				int pi = invp;
				for (; pi < codeSz - 1; ++pi, p *= 2)
					if (x < p)
						break;
				//				printf("%s %s%n", x, p);
				if (x < p) {
					double c0 = (p - x) / origdata[j];
					if (delta_c > c0) {
						delta_c = c0;
						delta_j = j;
					}
				}
			}
		}
		if (delta_j == -1)
			return -1;

		double new_c = c + delta_c;
		for (int j = 0; j < nLines; ++j)
			data[j] = origdata[j] * new_c;
		nextcode_c = new_c;
		return code(data, 0);
	}

	private final static long codeMask = (1 << codeSz) - 1;

	private UList<Integer> codeList(long cd) {
		die();
		return unsampledInputRng.map(j -> (int) ((cd >> (j * codeSz)) & codeMask));
	}

	private long code(double[] data, int mono_ind) {
		die();
		int base = mono_ind * nLines;
		long code = 0L;
		//		int invp = 2;
		for (int j = 0; j < nLines; ++j) {
			double x = data[base + j];
			// p==2^(pi-1)
			//
			//			if (x < 0.25) {
			//				; // 0
			//			} else if (x < 0.5) {
			//				; // 0
			//				long p = 1; // p==2^pi
			//				code |= p << (j * codeSz);

			if (x < 0.5) {
				; // 0
			} else {
				long p = 1; // p==2^pi
				//				int pi = 0;
				int pi = invp;
				for (; pi < codeSz - 1; ++pi, p *= 2)
					if (x < p)
						break;
				long oldcode = code;
				code |= p << (j * codeSz);
			}
			//			printf("xxcode %x %x %x %s%n", oldcode, p, code, x);
			//			if (oldcode > 0)  works if codeSz==4
			//				myassertEqual(String.format("%x%x", p, oldcode), String.format("%x", code), pi, p, oldcode, code, j);
		}
		//		printf("xx %x %s%n", code, slice(mono_ind).map(x -> (int) (double) x));
		return code;

	}

	//	private double evalMonoCde(int mono_ind) {
	//		int base = mono_ind * nLines;
	//		double xx = 0;
	//		int code = mono_ind;
	//		for (int j = 0; j < nLines; ++j) {
	//			int p = (int) ((code >> (j * codeSz)) & codeMask);
	//			double x = 1.0;
	//			if (p == 0) {
	//				;
	//			} else {
	//				//				int pi = 0;
	//				int pi = invp;
	//				for (; pi < codeSz - 1; ++pi, p *= 2)
	//					if (x < p)
	//						break;
	//				long oldcode = code;
	//				code |= p << (j * codeSz);
	//			}
	//			double x = pt_monovals[base + j];
	//
	//			//			printf("xxcode %x %x %x %s%n", oldcode, p, code, x);
	//			//			if (oldcode > 0)  works if codeSz==4
	//			//				myassertEqual(String.format("%x%x", p, oldcode), String.format("%x", code), pi, p, oldcode, code, j);
	//		}
	//		//		printf("xx %x %s%n", code, slice(mono_ind).map(x -> (int) (double) x));
	//		return code;
	//
	//	}

	private static double ipow(double x, int exp) {
		double out = 1;
		for (; exp > 0; exp--)
			out *= x;
		for (; exp < 0; exp++)
			out /= x;
		return out;
	}

	private double eval_monoList(UList<Integer> lst, int pti) {
		return prodd(lst.mapI((j, exp) -> ipow(inputx.get(pti).invals.get(j), exp)));
	}

	public UList<Integer> monoList(int monoi) {
		int maxexp = inpx.max_var_exp; // exponent range is [-max_var_exp,max_var_exp]
		int div = 2 * maxexp + 1;
		long ec = monoi;
		GrowList<Integer> xs = new GrowList<>();
		for (int vi = 0; vi < numInVars; vi++) {
			int e = (int) (ec % div) - maxexp;
			//			myassert(-inpx.max_var_exp <= e, ec, vi);
			ec /= div;
			xs.add(e);
		}
		return xs.elems();
	}

	private int monoInd(Integer... xs) {
		return monoInd(mkUList(xs));
	}

	public int monoInd(UList<Integer> xs) {
		int maxexp = inpx.max_var_exp;
		int div = 2 * maxexp + 1;
		int ec = 0;
		//for (int vi = 0; vi < numInVars; vi++)
		for (int vi = numInVars - 1; vi >= 0; vi--) {
			int e = xs.get(vi);
			ec = ec * div + (e + maxexp);
			//			printf("%s %s %s%n", e, (e + maxexp), ec);
		}
		myassertEqual(monoList(ec), xs, ec);
		return ec;
	}

	private void enx(double[] out, int outind, UList<DataLine> xs, int expcomb, int maxexp, int numInVars) {
		int base = outind * nLines;
		int div = 2 * maxexp + 1;
		for (DataLine line : xs) {
			double xv = 1;
			long ec = expcomb;
			for (int vi = 0; vi < numInVars; vi++) {
				int e = (int) (ec % div) - maxexp;
				ec /= div;
				double x = line.invals.get(vi);
				switch (e) {
				case -2:
					xv /= (x * x);
					break;
				case -1:
					xv /= x;
					break;
				case 0:
					break;
				case 1:
					xv *= x;
					break;
				case 2:
					xv *= (x * x);
					break;
				default:
					die();
				}
			}
			out[base + line.index] = xv;
		}
	}

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

	//	private double best_errsq;
	//	private double best_C0;
	//
	//	private int ntry1, ntry2, nediv1, nediv2;

	private double[] mvals(int monoi) {
		double[] xs = new double[nLines];
		for (int j = 0; j < nLines; ++j)
			xs[j] = pt_monovals[monoi * nLines + j];
		return xs;
	}

	private double[] mul(double c, double inxs[]) {
		double[] xs = new double[nLines];
		for (int j = 0; j < nLines; ++j)
			xs[j] = c * inxs[j];
		return xs;
	}

	private double chooseTol() {
		double mx = maxd(inputx.map(dl -> dl.outval));
		double mn = mind(inputx.map(dl -> dl.outval));
		//?? what was this about?
		//		if (Math.abs(mx - mn) < 0.5 * mx)
		//			die("careful about tol - large diff between outvar min/max");
		FORNOW(mn >= 0);
		//		return 0.1 * mx;
		return 0.01 * mx;
	}

	final long starttm, init_starttm = System.currentTimeMillis();

	private String tm() {
		final long tm = System.currentTimeMillis();
		return "" + (tm - starttm) / 1000.0 + "(" + (starttm - init_starttm) / 1000.0 + ")";
	}

	//	boolean halfHack = false;
	int diemsk() {
		die();
		return 0;
	}

	private void doP3(boolean halfHack2) {
		double halfHack2Mul = halfHack2 ? 0.5 : 1.0;

		double[] p_errs;
		{
			double[] yx = new double[nLines];
			int eind = lookup0.search(diemsk(), yx, tol);
			p_errs = lookup0.save_errSq();
		}

		// y = P + P + P			
		double[] ylo = new double[nLines];
		double[] yhi = new double[nLines];

		double[] yx = new double[nLines];

		int ngot = 0;
		double b_C0 = 0;
		double b_errsq = 1e10;
		int b_monoi1 = -1;
		int b_monoi2 = -1;
		int b_monoi3 = -1;

		//		 [1, 2, 0, 0]
		//				 [1, 0, 0, 2]
		//				 [1, 0, 2, 0]
		//			for (int round = 0; round < monosz; ++round)
		//			for (int round = 0; round < monosz; ++round)
		//				for (int round2 = 0; round2 < monosz; ++round2)
		//			for (int round3 = 0; round3 < monosz; ++round3)
		int nskip = 0;
		if (false) {
			//boolean halfHacky = true;
			boolean halfHacky = false;
			boolean save = true;
			if (false) {
				for (int j = 0; j < nLines; ++j)
					yx[j] = 0;
				//				int eind = lookup0.tryall(yx, y0, save);
			} else {
				double best_errsq = 1e10;
				for (int monoi1 = 0; monoi1 < monosz; ++monoi1) {
					if (monoi1 % 100 == 0)
						printf("m %s%n", monoi1);
					for (int j = 0; j < nLines; ++j) {
						yx[j] = pt_monovals[monoi1 * nLines + j];
						if (halfHacky)
							yx[j] = 0.5 * yx[j];
					}
					double sm = 0;
					for (int j = 0; j < nLines; ++j)
						if (yx[j] > y0[j])
							sm += (yx[j] - y0[j]) * (yx[j] - y0[j]);
					if (sm > best_errsq) {
						nskip++;
						//						lookup0.tryall(yx, y0, save);
						die();

						//						myassert(lookup2.best_C0 >= 0);
						myassertLE(best_errsq, lookup0.best_errsq(), monoi1);
					} else {
						//						sm = 0;
						//						for (int j = 0; j < nLines; ++j)
						//							if (yx[j] < y0[j])
						//								sm += (yx[j] - y0[j]) * (yx[j] - y0[j]);
						//						if (sm > best_errsq)
						//							nskip++;
						//						else 
						{
							//					int	eind = 
							//							lookup0.tryall(yx, y0, save);
							die();
							best_errsq = Math.min(best_errsq, lookup0.best_errsq());
						}
					}
				}
			}

			//			double[] save_errTol = lookup0.save_errTol();
			//			double[] save_errSq = lookup0.save_errSq();
			//			Table.quickSort2(save_errTol, save_errSq);
			//			for (int k = 0; k < save_errSq.length && k < 10; ++k)
			//				printf("%15.5f %15.5f%n", save_errTol[k], save_errSq[k]);
			//				printf("%s %s %s %s%n", save_errTol[k], save_errSq[k], save_errTol[k] < save_errTol[k + 1],
			//						save_errSq[k] < save_errSq[k + 1]);

			//			dumpdie(save_errSq.length, tm(), nskip);
			die();
		}

		if (false) {
			int nnegpos = 0;
			int nLines = this.nLines;
			//			double[] pt_monovals = this.pt_monovals;
			double[] vs = new double[nLines];
			for (int monoi1 = 0; monoi1 < monosz; ++monoi1) {
				//				double mv1 = pt_monovals[monoi1 * nLines + pti];
				//				for (int pti = 0; pti < nLines; ++pti)
				//					vs[pti] = pt_monovals[monoi1 * nLines + pti];

				for (int monoi2 = monoi1; monoi2 < monosz; ++monoi2) {
					boolean someNeg = false;
					boolean somePos = false;
					int pti = 0;
					double msm0 = pt_monovals[monoi1 * nLines + pti] + pt_monovals[monoi2 * nLines + pti];
					double msm1;
					for (pti = 1; pti < nLines; ++pti) {
						//						double msm = mv1 + pt_monovals[monoi2 * nLines + pti];
						//						double msm = vs[pti] + pt_monovals[monoi2 * nLines + pti];
						msm1 = pt_monovals[monoi1 * nLines + pti] + pt_monovals[monoi2 * nLines + pti];
						if (msm0 >= /*lbnd0*/y0[pti - 1] + tol)
							someNeg = true;
						else if (msm0 <= y0[pti - 1] - tol)
							somePos = true;
						msm0 = msm1;
						//						if (someNeg && somePos) {
						//							nnegpos++;
						//							break;
						//						}
					}
					if (msm0 >= /*lbnd0*/y0[pti - 1] + tol)
						someNeg = true;
					else if (msm0 <= y0[pti - 1] - tol)
						somePos = true;

					if (someNeg && somePos)
						nnegpos++;
				}
			}
			dumpdie(tm(), nnegpos, 0.5 * 100.0 * nnegpos / (monosz * monosz));
		}

		{
			myassert((1 << nLines) == (((short) 1) << nLines));
			//			short[] larges = new short[monosz];
			//			short[] smalls = new short[monosz];
			//			short[] larges1a = new short[monosz];
			//			short[] larges1b = new short[monosz];
			//			short[] smalls1a = new short[monosz];
			//			short[] smalls1b = new short[monosz];
			//
			//			short[] larges2a = new short[monosz];
			//			short[] larges2b = new short[monosz];
			//			short[] smalls2a = new short[monosz];
			//			short[] smalls2b = new short[monosz];
			//			
			//			short[] larges3a = new short[monosz];
			//			short[] larges3b = new short[monosz];
			//			short[] smalls3a = new short[monosz];
			//			short[] smalls3b = new short[monosz];

			int nratios = 5;
			int ratioMul = nratios;
			short[] largesA = new short[ratioMul * monosz];
			short[] largesB = new short[ratioMul * monosz];
			short[] smallsA = new short[ratioMul * monosz];
			short[] smallsB = new short[ratioMul * monosz];

			for (int monoi1 = 0; monoi1 < monosz; ++monoi1) {
				//				int large = 0;
				//				int small = 0;
				//
				//				for (int pti = 0; pti < nLines; ++pti) {
				//					double vx = pt_monovals[monoi1 * nLines + pti];
				//
				//					double ratio = 0.5;
				//
				//					if (vx <= ratio * y0[pti])
				//						small |= 1 << pti;
				//					if (vx >= ratio * y0[pti])
				//						large |= 1 << pti;
				//				}
				//				largesA[monoi1 * ratioMul + 0] = (short) large;
				//				largesB[monoi1 * ratioMul + 0] = (short) large;
				//				smallsA[monoi1 * ratioMul + 0] = (short) small;
				//				smallsB[monoi1 * ratioMul + 0] = (short) small;

				double ratio = 0.5;
				for (int ri = 0; ri < nratios; ++ri, ratio *= 0.25) {
					int largeA = 0;
					int smallA = 0;
					int largeB = 0;
					int smallB = 0;

					for (int pti = 0; pti < nLines; ++pti) {
						double vx = pt_monovals[monoi1 * nLines + pti];

						if (vx <= ratio * y0[pti])
							smallA |= 1 << pti;
						if (vx >= ratio * y0[pti])
							largeA |= 1 << pti;

						ratio = 1 - ratio;
						if (vx <= ratio * y0[pti])
							smallB |= 1 << pti;
						if (vx >= ratio * y0[pti])
							largeB |= 1 << pti;
					}
					largesA[monoi1 * ratioMul + ri] = (short) largeA;
					largesB[monoi1 * ratioMul + ri] = (short) largeB;
					smallsA[monoi1 * ratioMul + ri] = (short) smallA;
					smallsB[monoi1 * ratioMul + ri] = (short) smallB;
				}
			}

			int nlarges = 0;
			int nsmalls = 0;
			for (int monoi1 = 0; monoi1 < monosz; ++monoi1) {
				if ((largesA[monoi1 * ratioMul]) != 0)
					nlarges++;
				if ((smallsA[monoi1 * ratioMul]) != 0)
					nsmalls++;
			}

			boolean check = false;
			int nbad = 0;
			int ngood = 0;

			//			boolean[] allsmall = new boolean[monosz];
			double tolFromPP = 1.0; // roughly, for I.11.19

			//			if (false) {
			//				int monoi1 = monoInd(1, 0, 0, 1, 0, 0);
			//				int monoi2 = monoInd(0, 1, 0, 0, 1, 0);
			//
			//				for (int j = 0; j < nLines; ++j) {
			//					yx[j] = (pt_monovals[monoi1 * nLines + j] + pt_monovals[monoi2 * nLines + j]);
			//					//						if (halfHack)
			//					//							yx[j] *= 0.5;
			//					// y0[j] - tol <= mono + yx[j] <= y0[j] + tol
			//					double val = y0[j] - yx[j];
			//					ylo[j] = val - tol;
			//					yhi[j] = val + tol;
			//				}
			//				int monoi3 = monoInd(0, 0, 1, 0, 0, 1);
			//				lookup0.monoind3 = monoi3;
			//				int eind = lookup0.search(ylo, yhi, yx, y0);
			//
			//				for (int j = 0; j < nLines; ++j) {
			//					yx[j] = (pt_monovals[monoi1 * nLines + j] + pt_monovals[monoi2 * nLines + j]
			//							+ pt_monovals[monoi3 * nLines + j]);
			//					//						if (halfHack)
			//					//							yx[j] *= 0.5;
			//					// y0[j] - tol <= mono + yx[j] <= y0[j] + tol
			//					double val = y0[j] - yx[j];
			//					printf("v %s%n", val);
			//				}
			//
			//				dumpdie(eind, lookup0.best_errsq());
			//			}

			double minErr = nLines * (tol * tol);
			double[] mono1 = new double[nLines];
			int nallsmall = 0;
			int nsmallSkip = 0;
			double best_errsq = 1e10;
			double best_C0 = 0;
			int best_monoi1 = -1, best_monoi2 = 0, best_eind = 0;
			for (int monoi1 = 0; monoi1 < monosz; ++monoi1) {
				boolean allsmall = true;
				for (int pti = 0; pti < nLines; ++pti) {
					double vx = pt_monovals[monoi1 * nLines + pti];
					mono1[pti] = vx;
					if (vx > tolFromPP)
						allsmall = false;
				}
				if (allsmall) {
					nallsmall++;
					nsmallSkip += monosz - monoi1;
					continue;
				}
				for (int monoi2 = monoi1; monoi2 < monosz; ++monoi2) {
					boolean bad = false;
					for (int ri = 0; ri < nratios; ++ri)
						if ((largesA[monoi1 * ratioMul + ri] & largesB[monoi2 * ratioMul + ri]) != 0
								&& (smallsA[monoi1 * ratioMul + ri] & smallsB[monoi2 * ratioMul + ri]) != 0)
							bad = true;
					if (bad)
						nbad++;
					else
						ngood++;
					if (check || !bad) {
						for (int j = 0; j < nLines; ++j) {
							yx[j] = (pt_monovals[monoi1 * nLines + j] + pt_monovals[monoi2 * nLines + j]);
							//						if (halfHack)
							//							yx[j] *= 0.5;
							// y0[j] - tol <= mono + yx[j] <= y0[j] + tol
							double val = y0[j] - yx[j];
							ylo[j] = val - tol;
							yhi[j] = val + tol;
						}
						int eind = lookup0.search(ylo, yhi, yx, y0);
						if (check)
							myassert(lookup0.best_errsq() > minErr, monoi1, monoi2, eind, lookup0.best_errsq());
					}
				}
			}
			dump(nallsmall, ngood + nbad, monosz * monosz / 2);
			//					double[] large0_small1 = new double[monosz];
			dump(100.0 * nsmallSkip / (0.5 * monosz * monosz), 100.0 * nlarges / monosz, 100.0 * nsmalls / monosz,
					100.0 * nbad / (nbad + ngood));
			//(0.5 * monosz * monosz));

			dump(best_errsq, best_C0, best_monoi1, best_monoi2, best_eind);
			dumpdie(tm(), 100.0 * (ngood + nbad) / (0.5 * monosz * monosz), 100.0 * ngood / (0.5 * monosz * monosz));
		}

		{
			//			int[] large0 = new int[monosz];
			int best_nlarge_small = 0;
			int best_small = 0;
			int best_ptj = -1;

			//			for (UList<Integer> subset : allSubsets_int(nLines, nLines / 2)) 
			double ratio = 0.5;
			for (int ptj = 0; ptj < nLines; ++ptj) {
				int nsmall = 0;
				int nsmall_l = 0;
				int nsmall_s = 0;
				int nlarge_small = 0;
				outerloop: for (int monoi1 = 0; monoi1 < monosz; ++monoi1)
					if (pt_monovals[monoi1 * nLines + ptj] <= /*ubnd1*/ratio * y0[ptj]) {
						nsmall++;

						for (int pti = 0; pti < nLines; ++pti)
							if (pti != ptj) {
								if (pt_monovals[monoi1 * nLines + pti] >= /*lbnd0*/y0[pti]) {
									nlarge_small++;

									if (pt_monovals[monoi1 * nLines + ptj] <= /*ubnd1*/(1 - ratio) * y0[ptj])
										nsmall_s++;
									else
										nsmall_l++;
									continue outerloop;
								}
							}
					}
				if (best_nlarge_small < nlarge_small) {
					best_nlarge_small = nlarge_small;
					best_small = nsmall;
					best_ptj = ptj;
				}
				dump(ptj, 100.0 * nsmall / monosz, 100.0 * nlarge_small / monosz,
						//						100.0 * nsmall_l / monosz,
						//						100.0 * nsmall_s / monosz,
						100.0 * (nsmall_s * nlarge_small) / (monosz * monosz));
			}

			//					double[] large0_small1 = new double[monosz];
			dump(best_ptj, 100.0 * best_small / monosz, 100.0 * best_nlarge_small / monosz,
					100.0 * (best_nlarge_small * best_nlarge_small) / (monosz * monosz));

			die();
		}

		boolean tryall = false;
		//		if (false)
		{
			ylo = new double[3];
			yhi = new double[3];

			//		double mulfac = 1.0;
			double mulfac = 1.5;
			//		double mulfac = 2.0;
			UList<Double> mvals0 = sort(rangeOpen(monosz).map(i -> mulfac * pt_monovals[i * nLines + 0]));
			UList<Double> mvals1 = sort(rangeOpen(monosz).map(i -> mulfac * pt_monovals[i * nLines + 1]));
			UList<Double> mvals2 = sort(rangeOpen(monosz).map(i -> mulfac * pt_monovals[i * nLines + 2]));
			int gsz = 32;
			//			for (int i = 0; i < monosz; ++i)
			//				printf("%12.5f %12.5f %12.5f %n", mvals0.get(i), mvals1.get(i), mvals2.get(i));
			//			die();

			//			UList<UList<Double>> groups0 = mvals0.partition(gsz);
			//			UList<UList<Double>> groups1 = mvals1.partition(gsz);
			//			UList<UList<Double>> groups2 = mvals2.partition(gsz);
			int gmult = 1;
			UList<UList<Double>> groups0 = rangeOpen(gsz)
					.map(i -> mkUList1((double) i * gmult, (double) (i + 1) * gmult));
			UList<UList<Double>> groups1 = groups0;
			UList<UList<Double>> groups2 = groups0;

			printf("SZ %s %s %s %s%n", monosz, mvals0.count(i -> i < gsz * gmult), mvals1.count(i -> i < gsz * gmult),
					mvals2.count(i -> i < gsz * gmult));

			//			groups0.forEach(x -> printf("%s%n", x));
			//			groups1.forEach(x -> printf("%s%n", x));
			//			groups2.forEach(x -> printf("%s%n", x));
			//			die();

			int ngood = 0, nbad = 0, nneg = 0;
			int limit = 100078;
			boolean newsearch = false;
			int[] tbl0 = new int[lookup0.ncombs];
			int[] tbl1 = new int[lookup1.ncombs];
			boolean[] btbl = new boolean[lookup1.ncombs];
			int smx = 0;
			for (int i0 = 0; i0 < limit && i0 < groups0.size(); ++i0) {
				for (int i1 = 0; i1 < limit && i1 < groups1.size(); ++i1)
					for (int i2 = 0; i2 < limit && i2 < groups2.size(); ++i2) {
						UList<Double> grp0 = groups0.get(i0);
						UList<Double> grp1 = groups1.get(i1);
						UList<Double> grp2 = groups1.get(i2);
						ylo[0] = y0[0] - grp0.first();
						yhi[0] = y0[0] + grp0.last();
						ylo[1] = y0[1] - grp1.first();
						yhi[1] = y0[1] + grp1.last();
						ylo[2] = y0[2] - grp2.first();
						yhi[2] = y0[2] + grp2.last();
						double limit2 = -15.0;
						//						if (ylo[0] < limit2 || ylo[1] < limit2 || ylo[2] < limit2)
						//							continue;

						//						printf("%12.5f %12.5f  %12.5f %12.5f  %12.5f %12.5f%n", grp0.first(), grp0.last(), grp1.first(),
						//								grp1.last(), grp2.first(), grp2.last());
						//						printf("%12.5f %12.5f %12.5f%n", ylo[0], ylo[1], ylo[2]);
						//						printf("%12.5f %12.5f %12.5f%n", yhi[0], yhi[1], yhi[2]);
						if (yhi[0] < 0 || yhi[1] < 0 || yhi[2] < 0)
							nneg++;
						else {
							int eind = -1;
							//							if (newsearch)
							//							if (false) {
							//								//								1.361 32768 0 0 0.0
							//								int n0 = lookup0.qsearch(ylo, yhi, tbl0);
							//								if (n0 > 0) {
							//									int n1 = lookup1.qsearch(ylo, yhi, tbl1);
							//									for (int ti = 0; ti < n1; ti++)
							//										btbl[tbl1[ti]] = true;
							//									for (int ti = 0; ti < n0; ti++)
							//										if (btbl[tbl0[ti]]) {
							//											eind = tbl0[ti];
							//											break;
							//										}
							//									for (int ti = 0; ti < n0; ti++)
							//										btbl[tbl0[ti]] = false;
							//								}
							//							}
							int oeind;
							//							else
							// 0.459 29600 3168 0 9.66796875
							//							int oeind = eind;
							//							if (false) {
							//								oeind = lookup0.qsearch3(ylo, yhi);
							//								smx += oeind;
							//								eind = oeind;
							//							} else
							{
								//								eind = lookup0.search(ylo, yhi, null, null, true);
								eind = lookup0.search(ylo, yhi, yx, y0);
								//								if (oeind != eind)
								//									dumpdie(oeind, eind, i0, i1, i2, lookup0.tryvals());
							}
							//						if (eind < 0)
							//							println("bad");
							//						else
							//							println();

							//					printf("E %s%n", eind);
							if (eind < 0)
								nbad++;
							else
								ngood++;
						}
					}
			}
			dumpdie(tm(), ngood, nbad, nneg, 100.0 * nbad / (ngood + nbad), smx, lookup0.tryvals());
		}

		double maxErr = nLines * (tol * tol);

		printf("MAXER %s %s%n", maxErr, tol);
		lookup0.tolx = tol;
		lookup0.maxTolErr = maxErr;
		int ntried = 0;
		for (int monoi1 = 0; monoi1 < monosz; ++monoi1) {
			for (int monoi2 = monoi1; monoi2 < monosz; ++monoi2) {
				int eind;
				if (tryall) {
					for (int j = 0; j < nLines; ++j) {
						yx[j] = pt_monovals[monoi1 * nLines + j] + pt_monovals[monoi2 * nLines + j];
						//						if (halfHack)
						//							yx[j] *= 0.5;
					}
					eind = lookup0.tryall(yx, y0);
				} else {
					for (int j = 0; j < nLines; ++j) {
						yx[j] = halfHack2Mul * (pt_monovals[monoi1 * nLines + j] + pt_monovals[monoi2 * nLines + j]);
						//						if (halfHack)
						//							yx[j] *= 0.5;
						// y0[j] - tol <= mono + yx[j] <= y0[j] + tol
						double val = y0[j] - yx[j];
						ylo[j] = val - tol;
						yhi[j] = val + tol;
					}
					eind = lookup0.search(ylo, yhi, yx, y0);
					ntried++;
				}
				if (eind >= 0) {
					ngot++;
				}
			}
			dump(monoi1, lookup0.tryvals(), ngot, 100.0 * ngot / ntried, b_C0, b_errsq);
			if (monoi1 == 20)
				dumpdie(tm());
		}
		//		if (tryall) {
		//			for (int key : lookup2.blocks.keySet())
		//				printf("BL %12s %s%n", Integer.toBinaryString(key), lookup2.blocks.get(key));
		//			printf("SZ %s%n", lookup2.blocks.keySet().size());
		//		}

		dump(monoList(b_monoi1));
		dump(monoList(b_monoi2));
		dump(monoList(b_monoi3));
		double C0 = 1.0;
		double errsq = 0.0;
		for (DataLine dl : inputx) {
			int j = dl.index;
			double p0 = eval_monoList(monoList(b_monoi1), dl.index);
			double p1 = eval_monoList(monoList(b_monoi2), dl.index);
			double p2 = eval_monoList(monoList(b_monoi3), dl.index);
			double yxx = C0 * p0 + p1 + p2;
			//			if (halfHack)
			//				yxx *= 0.5;
			double diff = y0[j] - yxx;
			errsq += diff * diff;
			printf("%15.5f %15.5f %15.5f %15.5f%n", y0[j], yxx, diff, errsq);
		}
		dump("SWITCHKLUDGE", tm(), monosz, lookup0.switchkludge, allow1Const, lookup0.tryvals(), ngot, b_monoi1,
				b_monoi2, b_monoi3, b_C0, b_errsq);
		die();
	}

	boolean checkNegPos = true;

	static double vecsz(double[] xs) {
		double d = 0;
		for (double x : xs)
			d += x * x;
		return Math.sqrt(d);
	}

	private void doPaddPdivPP() {
		int ngot = 0;
		double b_C0 = 0;
		double b_errsq = 1e10;
		int b_monoi0 = -1;
		int b_monoi1 = -1;
		int b_monoi2 = -1;
		int b_monoi3 = -1;
		double[] ylo = new double[nLines];
		double[] ytmp = new double[nLines];
		double[] yhi = new double[nLines];
		double[] p3vals = new double[nLines];

		double[] yx = new double[nLines];
		for (int monoi0 = 0; monoi0 < monosz; ++monoi0)
			for (int monoi1 = 0; monoi1 < monosz; ++monoi1)
				for (int monoi2 = 0; monoi2 < monosz; ++monoi2) {

					// y=P0+P1/(P2+CP3) ==> y-P0 = P1/(P2+CP3) ==> (P2+CP3) = P1/(y-P0) ==> CP3 = P1/(y-P0) - P2
					for (int j = 0; j < nLines; ++j) {
						double p0 = pt_monovals[monoi0 * nLines + j];
						double p1 = pt_monovals[monoi1 * nLines + j];
						double p2 = pt_monovals[monoi2 * nLines + j];
						ylo[j] = p1 / ((y0[j] - tol) - p0) - p2;
						yhi[j] = p1 / ((y0[j] + tol) - p0) - p2;
						if (!(ylo[j] <= yhi[j])) {
							double tmp = ylo[j];
							ylo[j] = yhi[j];
							yhi[j] = tmp;
						}
						myassert(ylo[j] <= yhi[j]);
						yx[j] = p1 / ((y0[j]) - p0) - p2;
					}

					//					int eind = lookup0.search(ylo, yhi, yx, y0);
					lookup0.alteval = true;
					lookup0.ae_monoi0 = monoi0;
					lookup0.ae_monoi1 = monoi1;
					lookup0.ae_monoi2 = monoi2;
					int eind = lookup0.tryall(yx, y0);
					double[] errsq = lookup0.save_errSq();
					for (double esq : errsq)
						if (esq < 0.001)
							printf("LOW ERR %s%n", esq);

					if (false)
						for (int monoi3 = 0; monoi3 < monosz; ++monoi3) {
							double esq = 0;
							for (int j = 0; j < nLines; ++j) {
								double p0 = pt_monovals[monoi0 * nLines + j];
								double p1 = pt_monovals[monoi1 * nLines + j];
								double p2 = pt_monovals[monoi2 * nLines + j];
								double p3 = pt_monovals[monoi3 * nLines + j];
								double vl = p0 + p1 / (p2 + (-1.0 / 3) * p3);
								double e = y0[j] - vl;
								esq += e * e;
							}
							if (esq < 1e-10) {
								for (int j = 0; j < nLines; ++j)
									p3vals[j] = pt_monovals[monoi3 * nLines + j];

								dump("LO", toList(ylo));
								dump("YT", toList(ytmp));
								dump("P3", toList(p3vals));
								dump("HI", toList(yhi));
								dump("TOL", tol);

								for (int j = 0; j < nLines; ++j) {
									double p0 = pt_monovals[monoi0 * nLines + j];
									double p1 = pt_monovals[monoi1 * nLines + j];
									double p2 = pt_monovals[monoi2 * nLines + j];
									double p3 = pt_monovals[monoi3 * nLines + j];
									dump(p0, p1, p2, p3);
									// y=P0+P1/(P2+CP3) ==> y-P0 = P1/(P2+CP3) ==> (P2+CP3) = P1/(y-P0) ==> CP3 = P1/(y-P0) - P2
									double c = -1.0 / 3;
									double y = y0[j];
									myassertEqualToTol(y - p0, p1 / (p2 + c * p3), 1e-3);
									myassertEqualToTol((p2 + c * p3), p1 / (y - p0), 1e-3);
									myassertEqualToTol((c * p3), p1 / (y - p0) - p2, 1e-3);
									myassertEqualToTol(yx[j], p1 / (y - p0) - p2, 1e-3);
								}
								dumpdie("LOW ERR2 ", esq, monoi0, monoi1, monoi2, monoi3);
							}
						}

					if (eind >= 0) {
						//					dump("GOT ONE", eind, monoList(eind), best_C0, best_errsq, errsq(mul(best_C0, mvals(eind)), y0));
						ngot++;
					}
				}
		dumpdie("BEST", tm(), b_errsq);

	}

	private Soln doPP(int fno) {
		double[] ylo = new double[nLines];
		double[] yhi = new double[nLines];
		double[] yx = new double[nLines];
		double[] tmp0 = new double[nLines];

		double[] y0sq = new double[nLines];

		int ngot = 0;

		for (int j = 0; j < nLines; ++j)
			y0sq[j] = y0[j] * y0[j];

		//		# Feynman I.27.6   1/(1/d1+n/d2)
		//		** d1 d2 n        foc
		//       ==	d1 + d2/n
		// WRONG
		//		int p0ind = monoInd(1, 0, 0);
		//		int p1ind = monoInd(0, 1, -1);

		//		# Feynman II.37.1   mom*(1+chi)*B
		//		** mom B chi        E_n

		int p0ind = monoInd(1, 1, 0);
		int p1ind = monoInd(1, 1, 1);
		boolean dbg = false;
		lookup0.foodebugind = p0ind;
		//		printf("DBGIND %s%n", p0ind);

		//		lookup.dumpLookup();

		for (int monoi0 = 0; monoi0 < monosz; ++monoi0) {

			switch (fno) {
			case b_2P:
				for (int j = 0; j < nLines; ++j) {
					tmp0[j] = pt_monovals[monoi0 * nLines + j];
					double val = y0[j] - tmp0[j];
					yx[j] = val;
					ylo[j] = val - tol;
					yhi[j] = val + tol;
				}
				break;

			case b_pow2P:
				for (int j = 0; j < nLines; ++j) {
					tmp0[j] = pt_monovals[monoi0 * nLines + j];
					double val = y0sq[j] - tmp0[j]; // NOT y0[j]
					yx[j] = val;
					ylo[j] = val - tol;
					yhi[j] = val + tol;
				}
				break;

			default:
				die();
			}

			if (dbg && monoi0 == p1ind)
				lookup0.foodebug = true;

			int eind = lookup0.search(diemsk(), ylo, yhi, yx, y0, fno, monoi0, -1, -1, tmp0, -1, false);

			if (dbg && monoi0 == p1ind) {
				//				for (int j = 0; j < nLines; ++j) {
				//					double p0 = pt_monovals[p0ind * nLines + j];
				//					double p1 = pt_monovals[p1ind * nLines + j];
				//					double d1 = inpx.unsampled_input.get(j).invals.get(0);
				//					double d2 = inpx.unsampled_input.get(j).invals.get(1);
				//					double n = inpx.unsampled_input.get(j).invals.get(2);
				//					double c = 1.0;
				//					double y = y0[j];
				//					dump(p0, p1, y, 1 / ((1 / d1) + (n / d2)), p0 + p1);
				//					//										dump(d1, d2, n, y, 1 / ((1 / d1) + (d2 / n)), 1 / d1, d2 / n, 1 / d1 + d2 / n);
				//					//					printf("d1: %6.4s    1/d1: %6.4s%n", d1, 1 / d1);
				//					//					printf("d2: %6.4s    1/d2: %6.4s%n", d2, 1 / d2);
				//					//					printf(" n: %6.4s    n/d2: %6.4s%n", n, n / d2);
				//				}
				die();
			}

			if (eind >= 0) {
				//					dump("GOT ONE", eind, monoList(eind), best_C0, best_errsq, errsq(mul(best_C0, mvals(eind)), y0));
				ngot++;
			}
		}

		printf("doPP %s %s %s%n", fno, tm(), rsoln.soln());
		return rsoln.soln();
	}

	void theEnd() {
		printf("tm %s %s%n", tm(), lookup0.nneg_chis);
	}

	private Soln doP4(int fno, int caseno, boolean negone) {
		double[] ylo = new double[nLines];
		double[] yhi = new double[nLines];
		double[] yx = new double[nLines];
		double[] tmp0 = new double[nLines];

		int ngot = 0;
		int ntried = 0;

		//# Feynman I.15.3t   (t-u*x/c**2)/sqrt(1-u**2/c**2)
		//			** x c u t       t1
		int p0ind = monoInd(0, 0, 0, 1);
		int p1ind = monoInd(0, -2, 2, 0); //negated
		int p2ind = monoInd(0, 0, 0, 0);
		int p3ind = monoInd(1, -2, 1, 0);

		boolean break_symmetry1 = mkUList1(b_4P).contains(fno);
		boolean break_symmetry2 = negone ? mkUList1(b_4P).contains(fno) : mkUList1(b_4P, b_2Pdivpow2P).contains(fno);

		printf(">>>> NOT CHECKING NEGS %s%n", monosz);

		for (int mono0 = 0; mono0 < monosz; ++mono0)
			for (int mono1 = break_symmetry1 ? mono0 : 0; mono1 < monosz; ++mono1)
				for (int mono2 = break_symmetry2 ? mono1 : 0; mono2 < monosz; ++mono2) {
					ntried++;
					//
					//					mono0 = p0ind;
					//					mono1 = p1ind;
					//					mono2 = p2ind;
					//					caseno = 0;

					switch (fno) {
					case b_4P:
						for (int j = 0; j < nLines; ++j) {
							tmp0[j] = (pt_monovals[mono0 * nLines + j] + pt_monovals[mono1 * nLines + j]
									+ pt_monovals[mono2 * nLines + j]);
							double val = y0[j] - tmp0[j];
							yx[j] = val;
							ylo[j] = val - tol;
							yhi[j] = val + tol;
						}
						break;

					case b_2Pdivpow2P:
						//  ((P+P)/pow((P+P)))
						if (caseno == 0)
							//  (CP+P)/pow((P+P))  y=(CP3+P0)/pow(P1+P2) ==> y*pow(P1+P2)=(CP3+P0) ==> y*pow(P1+P2)-P0=CP3
							// negone ==> P1 negated
							for (int j = 0; j < nLines; ++j) {
								double p0 = pt_monovals[mono0 * nLines + j];
								double p1 = pt_monovals[mono1 * nLines + j];
								double p2 = pt_monovals[mono2 * nLines + j];

								double tmp0x = POW((negone ? -p1 : p1) + p2);
								tmp0[j] = 1.0 / tmp0x;
								double y0x = y0[j];
								yx[j] = y0x * tmp0x - p0;
								ylo[j] = (y0x - tol) * tmp0x - p0;
								yhi[j] = (y0x + tol) * tmp0x - p0;
								//								if (!(ylo[j] <= yx[j] && yx[j] <= yhi[j]))
								//									myassert(yhi[j] <= yx[j] && yx[j] <= ylo[j]);
							}
						else if (caseno == 1)
							//  (P+P)/pow((CP+P))
							for (int j = 0; j < nLines; ++j) {
								tmp0[j] = (pt_monovals[mono0 * nLines + j] + pt_monovals[mono1 * nLines + j]
										+ pt_monovals[mono2 * nLines + j]);
								double val = y0[j] - tmp0[j];
								yx[j] = val;
								ylo[j] = val - tol;
								yhi[j] = val + tol;
								die();
							}
						else
							die();
						break;

					default:
						die();
					}

					//					if (mono0 == p0ind && mono1 == p1ind && mono2 == p2ind && caseno == 0) {
					//						lookup0.foodebug = true;
					//						lookup0.foodebugind = p3ind;
					//						dump(yx, ylo, yhi);
					//					}
					int eind = lookup0.search(diemsk(), ylo, yhi, yx, y0, fno, mono0, mono1, mono2, tmp0, caseno,
							negone);

					//					printf("EIND %s%n", eind);
					if (false && mono0 == p0ind && mono1 == p1ind && mono2 == p2ind && caseno == 0) {
						lookup0.foodebug = true;
						lookup0.foodebugind = p3ind;
						printf("EIND %s%n", eind);
						die();
						//						lookup0.search(ylo, yhi, yx, y0, fno, mono0, mono1, mono2, tmp0, caseno, negone);

						double C0 = -1.0;
						double errsq = 0.0;
						for (DataLine dl : inputx) {
							int j = dl.index;
							double p0 = eval_monoList(monoList(p0ind), dl.index);
							double p1 = eval_monoList(monoList(p1ind), dl.index);
							double p2 = eval_monoList(monoList(p2ind), dl.index);
							double p3 = eval_monoList(monoList(p3ind), dl.index);
							//double yxx = C0 * p2 / Math.sqrt(p1 + p2);
							//double yxx = p0 / Math.sqrt(C0 * p2 + p1); // b_Pdivpow2P case 1
							double yxx = (C0 * p3 + p0) / POW((negone ? -p1 : p1) + p2); // b_2Pdivpow2P case 0
							double yxx2 = y0[j] * POW((negone ? -p1 : p1) + p2) - p0; // b_2Pdivpow2P case 0
							double diff = y0[j] - yxx;
							errsq += diff * diff;
							printf("xy %15.5f %15.5f %15.5f %15.5f %15.5f%n", y0[j], yxx, diff, errsq, yxx2);
						}

						dumpdie(yx, ylo, yhi);
					}

					if (eind >= 0) {
						//					dump("GOT ONE", eind, monoList(eind), best_C0, best_errsq, errsq(mul(best_C0, mvals(eind)), y0));
						ngot++;
					}
				}
		Soln soln = rsoln.soln();
		printf("doP4 %s %s %s%n", tm(), ntried, soln);
		myassertEqual(lookup0.nneg_chis, 0, 100.0 * lookup0.nneg_chis / ntried);

		return soln;
	}

	private Soln doP2_P2(int fno, int caseno, boolean negone) {
		double[] ylo = new double[nLines];
		double[] yhi = new double[nLines];
		double[] yx = new double[nLines];
		double[] tmp0 = new double[nLines];
		double[] tmp0x = new double[nLines];

		int ngot = 0;
		int ntried = 0;

		//# Feynman I.15.3t   (t-u*x/c**2)/sqrt(1-u**2/c**2)
		//			** x c u t       t1
		int p0ind = monoInd(0, 0, 0, 1);
		int p1ind = monoInd(0, -2, 2, 0); //negated
		int p2ind = monoInd(0, 0, 0, 0);
		int p3ind = monoInd(1, -2, 1, 0);

		boolean break_symmetry1 = mkUList1(b_4P).contains(fno);
		boolean break_symmetry2 = negone ? mkUList1(b_4P).contains(fno) : mkUList1(b_4P, b_2Pdivpow2P).contains(fno);

		printf(">>>> NOT CHECKING NEGS %s%n", monosz);
		myassert(!break_symmetry1);
		for (int mono1 = /*break_symmetry1 ? mono0 :*/ 0; mono1 < monosz; ++mono1)
			for (int mono2 = break_symmetry2 ? mono1 : 0; mono2 < monosz; ++mono2) {
				switch (fno) {
				case b_4P:
					die();
					//					for (int j = 0; j < nLines; ++j) {
					//						tmp0[j] = (pt_monovals[mono0 * nLines + j] + pt_monovals[mono1 * nLines + j]
					//								+ pt_monovals[mono2 * nLines + j]);
					//						double val = y0[j] - tmp0[j];
					//						yx[j] = val;
					//						ylo[j] = val - tol;
					//						yhi[j] = val + tol;
					//					}
					break;

				case b_2Pdivpow2P:
					//  ((P+P)/pow((P+P)))
					if (caseno == 0)
						//  (CP+P)/pow((P+P))  y=(CP3+P0)/pow(P1+P2) ==> y*pow(P1+P2)=(CP3+P0) ==> y*pow(P1+P2)-P0=CP3
						// negone ==> P1 negated
						for (int j = 0; j < nLines; ++j) {
							//							double p0 = pt_monovals[mono0 * nLines + j];
							double p1 = pt_monovals[mono1 * nLines + j];
							double p2 = pt_monovals[mono2 * nLines + j];

							//double tmp0x = POW((negone ? -p1 : p1) + p2);
							double tmp = (negone ? -p1 : p1) + p2;
							if (negone && tmp < 0)
								continue;
							tmp0x[j] = POW(tmp);
							tmp0[j] = 1.0 / tmp0x[j];
							//							double y0x = y0[j];
							//							yx[j] = y0x * tmp0x - p0;
							//							ylo[j] = (y0x - tol) * tmp0x - p0;
							//							yhi[j] = (y0x + tol) * tmp0x - p0;
							//								if (!(ylo[j] <= yx[j] && yx[j] <= yhi[j]))
							//									myassert(yhi[j] <= yx[j] && yx[j] <= ylo[j]);
						}
					else
						die();
					break;

				default:
					die();
				}

				for (int mono0 = 0; mono0 < monosz; ++mono0) {
					ntried++;

					switch (fno) {
					case b_4P:
						die();
						for (int j = 0; j < nLines; ++j) {
							tmp0[j] = (pt_monovals[mono0 * nLines + j] + pt_monovals[mono1 * nLines + j]
									+ pt_monovals[mono2 * nLines + j]);
							double val = y0[j] - tmp0[j];
							yx[j] = val;
							ylo[j] = val - tol;
							yhi[j] = val + tol;
						}
						break;

					case b_2Pdivpow2P:
						//  ((P+P)/pow((P+P)))
						if (caseno == 0)
							//  (CP+P)/pow((P+P))  y=(CP3+P0)/pow(P1+P2) ==> y*pow(P1+P2)=(CP3+P0) ==> y*pow(P1+P2)-P0=CP3
							// negone ==> P1 negated
							for (int j = 0; j < nLines; ++j) {
								double p0 = pt_monovals[mono0 * nLines + j];
								//								double p1 = pt_monovals[mono1 * nLines + j];
								//								double p2 = pt_monovals[mono2 * nLines + j];
								//
								//								double tmp0x = POW((negone ? -p1 : p1) + p2);
								//								tmp0[j] = 1.0 / tmp0x;
								double y0x = y0[j];
								yx[j] = y0x * tmp0x[j] - p0;
								ylo[j] = (y0x - tol) * tmp0x[j] - p0;
								yhi[j] = (y0x + tol) * tmp0x[j] - p0;
								//								if (!(ylo[j] <= yx[j] && yx[j] <= yhi[j]))
								//									myassert(yhi[j] <= yx[j] && yx[j] <= ylo[j]);
							}
						else if (caseno == 1)
							//  (P+P)/pow((CP+P))
							for (int j = 0; j < nLines; ++j) {
								tmp0[j] = (pt_monovals[mono0 * nLines + j] + pt_monovals[mono1 * nLines + j]
										+ pt_monovals[mono2 * nLines + j]);
								double val = y0[j] - tmp0[j];
								yx[j] = val;
								ylo[j] = val - tol;
								yhi[j] = val + tol;
								die();
							}
						else
							die();
						break;

					default:
						die();
					}

					//					if (mono0 == p0ind && mono1 == p1ind && mono2 == p2ind && caseno == 0) {
					//						lookup0.foodebug = true;
					//						lookup0.foodebugind = p3ind;
					//						dump(yx, ylo, yhi);
					//					}
					int eind = lookup0.search(diemsk(), ylo, yhi, yx, y0, fno, mono0, mono1, mono2, tmp0, caseno,
							negone);

					//					printf("EIND %s%n", eind);
					if (false && mono0 == p0ind && mono1 == p1ind && mono2 == p2ind && caseno == 0) {
						lookup0.foodebug = true;
						lookup0.foodebugind = p3ind;
						printf("EIND %s%n", eind);
						die();
						//						lookup0.search(ylo, yhi, yx, y0, fno, mono0, mono1, mono2, tmp0, caseno, negone);

						double C0 = -1.0;
						double errsq = 0.0;
						for (DataLine dl : inputx) {
							int j = dl.index;
							double p0 = eval_monoList(monoList(p0ind), dl.index);
							double p1 = eval_monoList(monoList(p1ind), dl.index);
							double p2 = eval_monoList(monoList(p2ind), dl.index);
							double p3 = eval_monoList(monoList(p3ind), dl.index);
							//double yxx = C0 * p2 / Math.sqrt(p1 + p2);
							//double yxx = p0 / Math.sqrt(C0 * p2 + p1); // b_Pdivpow2P case 1
							double yxx = (C0 * p3 + p0) / POW((negone ? -p1 : p1) + p2); // b_2Pdivpow2P case 0
							double yxx2 = y0[j] * POW((negone ? -p1 : p1) + p2) - p0; // b_2Pdivpow2P case 0
							double diff = y0[j] - yxx;
							errsq += diff * diff;
							printf("xy %15.5f %15.5f %15.5f %15.5f %15.5f%n", y0[j], yxx, diff, errsq, yxx2);
						}

						dumpdie(yx, ylo, yhi);
					}

					if (eind >= 0) {
						//					dump("GOT ONE", eind, monoList(eind), best_C0, best_errsq, errsq(mul(best_C0, mvals(eind)), y0));
						ngot++;
					}
				}
			}
		Soln soln = rsoln.soln();
		printf("doP4 %s %s %s%n", tm(), ntried, soln);
		myassertEqual(lookup0.nneg_chis, 0, 100.0 * lookup0.nneg_chis / ntried);

		return soln;
	}

	private static double POW(double x) {
		return Math.sqrt(x);
	}

	private static double INVPOW(double x) {
		return x * x;
	}

	private Soln doP3(int fno, int caseno) {
		double[] ylo = new double[nLines];
		double[] yhi = new double[nLines];
		double[] yx = new double[nLines];
		double[] tmp0 = new double[nLines];

		int ngot = 0;
		int ntried = 0;
		double b_C0 = 0;
		double b_errsq = 1e10;
		int b_mono0 = -1;
		int b_mono1 = -1;
		int b_mono2 = -1;

		boolean break_symmetry = mkUList1(b_3P, b_Pdiv2P, b_2Pdivpow2P).contains(fno);
		//		boolean two_cases = mkUList1(b_Pdiv2P, b_2Pdivpow2P).contains(fno);
		int p0ind = monoInd(1, 0, 2);
		int p1ind = monoInd(0, 0, 0);
		int p2ind = monoInd(0, 2, -2);

		for (int mono0 = 0; mono0 < monosz; ++mono0)
			for (int mono1 = break_symmetry ? mono0 : 0; mono1 < monosz; ++mono1) {
				switch (fno) {
				case b_3P:
					for (int j = 0; j < nLines; ++j) {
						tmp0[j] = (pt_monovals[mono0 * nLines + j] + pt_monovals[mono1 * nLines + j]);
						double val = y0[j] - tmp0[j];
						yx[j] = val;
						ylo[j] = val - tol;
						yhi[j] = val + tol;
					}
					break;

				case b_Pdiv2P:
					// P2/(P0+P1)

					// y=CP/(P+P) ==> y*(P+P)=CP
					if (caseno == 0)
						for (int j = 0; j < nLines; ++j) {
							double denom = (pt_monovals[mono0 * nLines + j] + pt_monovals[mono1 * nLines + j]);
							tmp0[j] = denom;
							double y0j = y0[j];
							yx[j] = y0j * denom;
							ylo[j] = (y0j - tol) * denom;
							yhi[j] = (y0j + tol) * denom;
							myassert(ylo[j] <= yhi[j]);
						}
					else {
						// other case:
						// y=P/(CP+P) ==> (CP+P)=P/y ==> CP=P/y-P
						die();
					}
					break;

				case b_Pdivpow2P:
					// case 0:
					// P2/pow(P0+P1)

					if (caseno == 0)
						for (int j = 0; j < nLines; ++j) {
							// y=CP/pow(P+P) ==> y*pow(P+P)=CP    P2/(P0+P1)
							double denom = POW(pt_monovals[mono0 * nLines + j] + pt_monovals[mono1 * nLines + j]);
							tmp0[j] = 1.0 / denom;
							double y0j = y0[j];
							yx[j] = y0j * denom;
							ylo[j] = (y0j - tol) * denom;
							yhi[j] = (y0j + tol) * denom;
							myassert(ylo[j] <= yhi[j]);
						}
					else
						for (int j = 0; j < nLines; ++j) {
							// other case:
							// y=P/pow(CP+P) ==> pow(CP+P)=P/y ==> CP+P=invpow(P/y) ==> CP=invpow(P0/y)-P1   P0/pow(P2+P1)
							double p0 = pt_monovals[mono0 * nLines + j];
							double p1 = pt_monovals[mono1 * nLines + j];
							yx[j] = INVPOW(p0 * invy0[j]) - p1;
							ylo[j] = INVPOW(p0 * invy0addTol[j]) - p1;
							yhi[j] = INVPOW(p0 * invy0subTol[j]) - p1;

							myassert(ylo[j] <= yhi[j]);
						}
					break;

				default:
					die();
				}

				//				if (mono0 == p0ind && mono1 == p1ind && caseno == 1) {
				//					lookup0.foodebug = true;
				//					lookup0.foodebugind = p2ind;
				//					//						lookup0.search(ylo, yhi, yx, y0, fno, mono0, mono1, -1, tmp0);
				//
				//					double C0 = -1.0;
				//					double errsq = 0.0;
				//					for (DataLine dl : inputx) {
				//						int j = dl.index;
				//						double p0 = eval_monoList(monoList(p0ind), dl.index);
				//						double p1 = eval_monoList(monoList(p1ind), dl.index);
				//						double p2 = eval_monoList(monoList(p2ind), dl.index);
				//						//double yxx = C0 * p2 / Math.sqrt(p1 + p2);
				//						double yxx = p0 / Math.sqrt(C0 * p2 + p1); // b_Pdivpow2P case 1
				//						double diff = y0[j] - yxx;
				//						errsq += diff * diff;
				//						printf("%15.5f %15.5f %15.5f %15.5f%n", y0[j], yxx, diff, errsq);
				//					}
				//
				//					dumpdie(ylo, yhi, yx);
				//				}

				int eind = lookup0.search(diemsk(), ylo, yhi, yx, y0, fno, mono0, mono1, -1, tmp0, caseno, false);

				if (eind >= 0) {
					//					dump("GOT ONE", eind, monoList(eind), p2ind, lookup0.best_C0, lookup0.best_errsq()); //							errsq(mul(best_C0, mvals(eind)), y0));
					//					
					//					if (mono0 == p0ind && mono1 == p1ind && caseno == 1) {
					//						lookup0.foodebug = true;
					//						lookup0.foodebugind = p2ind;
					//						lookup0.search(ylo, yhi, yx, y0, fno, mono0, mono1, -1, tmp0, caseno);
					//						//						FD    14       0.3345 XX
					//						//						M     153.9212       1.0000       0.0254     152.0043     155.9124
					//						//						M      51.1343       1.0000       0.0759      49.2984      53.1920
					//						//						M     189.2273       1.0000       0.0479     184.8484     193.9329
					//						//						M     113.7853       1.0000       0.0312     112.0534     115.6002
					//						//						M      55.4828       1.0000       0.2922      48.8075      65.9501
					//						//						M      94.3103       1.0000       0.0478      92.1354      96.6469
					//						//						M     150.9500       1.0000       0.0316     148.6177     153.3956
					//						//						M      99.0269       1.0000       0.0331      97.4265     100.7088
					//						//						M     118.3928       1.0000       0.1016     112.7996     124.9101
					//						//						M     173.5942       1.0000       0.0302     171.0271     176.2805
					//
					//						double C0 = -1.0;
					//						double errsq = 0.0;
					//						for (DataLine dl : inputx) {
					//							int j = dl.index;
					//							double p0 = eval_monoList(monoList(p0ind), dl.index);
					//							double p1 = eval_monoList(monoList(p1ind), dl.index);
					//							double p2 = eval_monoList(monoList(p2ind), dl.index);
					//							//double yxx = C0 * p2 / Math.sqrt(p1 + p2);
					//							double yxx = p0 / POW(C0 * p2 + p1); // b_Pdivpow2P case 1
					//							double diff = y0[j] - yxx;
					//							errsq += diff * diff;
					//							printf("M %12.4f %12.4f %12.4f %12.4f %12.4f     ", p0, p1, p2, yxx, y0[j]);
					//							printf("%15.5f %15.5f %15.5f %15.5f%n", y0[j], yxx, diff, errsq);
					//						}
					//
					//						//						dumpdie(ylo, yhi, yx, caseno, lookup0.best_errsq());
					//					}

					ngot++;
				}
				ntried++;
			}

		//		for (int k = 0; k < nLines; ++k) {
		//			double d1 = inpx.unsampled_input.get(k).invals.get(0);
		//			double d2 = inpx.unsampled_input.get(k).invals.get(1);
		//			double n = inpx.unsampled_input.get(k).invals.get(2);
		//			double out = inpx.unsampled_input.get(k).outval;
		//
		//			printf("%s %s%n", out, (1 / d1) / ((n / d2) + 1));
		//		}
		Soln soln = ngot == 0 ? null : new Soln(this, fno, caseno, b_C0, b_errsq, b_mono0, b_mono1, b_mono2);
		printf("doP3 %s %s %s err: %s %s %s%n", fno, ngot, 100.0 * ngot / ntried, b_errsq, b_mono0, b_mono1, tm(),
				soln);
		return soln;
	}

	private void doPP(boolean quarterHack) { // y = P + P
		if (false) {

			double[] yx = new double[nLines];
			int eind = lookup0.tryall(yx, y0);
			double[] p_errs = lookup0.save_errSq();

			double[][] pp_errs = new double[monosz][];

			for (int monoi = 0; monoi < monosz; ++monoi) {
				boolean someNeg = false;
				boolean somePos = false;

				for (int j = 0; j < nLines; ++j)
					yx[j] = pt_monovals[monoi * nLines + j];
				eind = lookup0.tryall(yx, y0);
				// p1err >= monoi+p2err
				pp_errs[monoi] = lookup0.save_errSq();
				double ptsz = vecsz(yx);
				for (int monoj = 0; monoj < monosz; ++monoj) {
					double p1e = p_errs[monoj];
					double p2e = pp_errs[monoi][monoj] + ptsz;
					//					if (ptsz > 1)
					//						printf("%5s %5s %15.5f %15.5f %15.5f %15.5f%n", monoi, monoj, p1e, p2e, pp_errs[monoj], ptsz);
					if (!(p2e >= p1e))
						printf("BAD %s %s %s %s%n", p2e, p1e, pp_errs[monoi][monoj], ptsz);
				}
			}
			die();

			double[][][] ppp_errs = new double[monosz][][];

			for (int monoi0 = 0; monoi0 < monosz; ++monoi0) {
				ppp_errs[monoi0] = new double[monosz][];

				for (int monoi1 = monoi0 /*not 0*/; monoi1 < monosz; ++monoi1) {
					//					for (int monoi = 0; monoi < monosz; ++monoi) {

					for (int j = 0; j < nLines; ++j)
						yx[j] = pt_monovals[monoi0 * nLines + j] + pt_monovals[monoi1 * nLines + j];
					eind = lookup0.tryall(yx, y0);
					// p1err >= monoi+p2err
					pp_errs[monoi1] = lookup0.save_errSq();
					double ptsz = vecsz(yx);
					for (int monoj = 0; monoj < monosz; ++monoj) {
						double p0e = p_errs[monoj];
						double p2e = pp_errs[monoi1][monoj] + ptsz;
						if (ptsz > 1)
							printf("%5s %5s %5s %15.5f %15.5f %15.5f %15.5f%n", monoi0, monoi1, monoj, p0e, p2e,
									pp_errs[monoi1][monoj], ptsz);
						if (!(p2e >= p0e))
							printf("BAD %s %s%n", p2e, p0e);
					}
				}
			}
			die();
		}

		double[] ylo = new double[nLines];
		double[] yhi = new double[nLines];
		double[] yx = new double[nLines];

		printf("QMUL HACK %s%n", quarterHack);
		double quarterHackMul = quarterHack ? 0.25 : 1.0;
		int ngot = 0;
		double b_C0 = 0;
		double b_errsq = 1e10;
		int b_monoi = -1;
		int b_monoj = -1;

		//		boolean tryall = false;
		boolean tryall = true;
		//		double tol2 = 10 * tol;
		double tol2 = tol;
		for (int monoi = 0; monoi < monosz; ++monoi) {
			boolean someNeg = false;
			boolean somePos = false;

			for (int j = 0; j < nLines; ++j) {
				yx[j] = quarterHackMul * pt_monovals[monoi * nLines + j];
				double val = y0[j] - yx[j];
				ylo[j] = val - tol2;
				yhi[j] = val + tol2;
				if (yhi[j] < y0[j])
					someNeg = true;
				else if (ylo[j] > y0[j])
					somePos = true;
			}
			int eind = -1;

			if (tryall)
				//				6.128(0.346) 15625 15625 11466 1.9421013091893389 15.101395645092815 [-1, 1, 1, -1, 1, 1] [1, 0, 0, 1, 0, 0]
				//				eind = lookup0.tryall(yx, y0, 0, null, null, null);
				die();
			else {
				if (someNeg && somePos) {
					if (checkNegPos)
						myassertEqual("", -1, lookup0.search(ylo, yhi, yx, y0));
				} else
					eind = lookup0.search(ylo, yhi, yx, y0);
			}

			if (eind >= 0) {
				//					dump("GOT ONE", eind, monoList(eind), best_C0, best_errsq, errsq(mul(best_C0, mvals(eind)), y0));
				ngot++;
			}
		}
		//		lookup0.switchkludge,
		dump("SWITCHKLUDGE", tm(), ngot, monosz, b_monoi, b_C0, b_errsq, monoList(b_monoi), monoList(b_monoj));
		die();
	}

	private Soln doP() {
		// y = P
		//			double[] ydivslo = new double[nLines - 1];
		//			double[] ydivshi = new double[nLines - 1];
		//			for (int j = 0; j < nLines - 1; ++j) {
		//				ydivshi[j] = (y0[j] + tol) / (y0[j + 1] - tol);
		//				ydivslo[j] = (y0[j] - tol) / (y0[j + 1] + tol);
		//				myassert(ydivshi[j] >= ydivslo[j]);
		//			}
		//			printf("yd %s%n", mkUList(ydivshi));
		//			printf("yd %s%n", mkUList(ydivslo));
		//		double[] ylo = new double[nLines];
		//		double[] yhi = new double[nLines];
		double[] yx = new double[nLines];
		//
		//		for (int j = 0; j < nLines; ++j) {
		//			ylo[j] = y0[j] - tol;
		//			yhi[j] = y0[j] + tol;
		//		}
		//		printf("ylo %s%n", mkUList(ylo));
		//		printf("yhi %s%n", mkUList(yhi));
		//		myassert(!halfHack);
		myassert(allow1Const);
		// yx all 0.0

		//		# Feynman I.12.2   q1*q2*r/(4*pi*epsilon*r**3)
		//		** q1 q2 epsilon r       F		
		//		int p0ind = monoInd(1, 1, -1, -2);
		//		lookup0.foodebugind = p0ind;
		//		lookup0.foodebug = true;

		//		# Feynman I.29.4   omega/c
		//		** omega c         k
		int p0ind = monoInd(1, -1);
		//		lookup0.foodebugind = p0ind;
		//		lookup0.foodebug = true;
		//		printf("DBGIND %s%n", p0ind);
		lookup0.dumpLookup();

		int eind = lookup0.search(0, yx, tol);

		if (eind >= 0)
			rsoln.gotone();

		Soln soln = rsoln.soln();
		printf("doP %s %s %s%n", eind, tm(), soln);
		if (true)
			return soln;

		if (false) {
			//			double C0 = 1.09941;
			//			UList<Integer> mlst = mkUList1(1, 0, 0, 1, 1, 0);
			//			if (false) 
			{
				// I.12.2
				double C0 = 0.079737;
				UList<Integer> mlst = mkUList1(1, 1, -1, -2);
				for (int monoi = 0; monoi < monosz; ++monoi) {
					double err = errsq(mul(C0, mvals(monoi)), y0);
					if (err < 1 || monoList(monoi).equals(mlst))
						printf("%5s %25s: %15.5f %s%n", monoi, monoList(monoi), err,
								monoList(monoi).equals(mlst) ? "XXX" : "");
				}
				double esq = 0;
				if (false) {
					int mi = 43;
					for (DataLine dl : inputx) {
						double q1 = dl.getx(0);
						double q2 = dl.getx(1);
						double epsilon = dl.getx(2);
						double r = dl.getx(3);
						double xv = q1 * q2 / (r * r * epsilon);
						printf("xv %s %s%n", xv, pt_monovals[mi * nLines + dl.index]);
						double dx = (dl.outval - C0 * xv);
						esq += dx * dx;
						printf("%15.5f %15.5f %15.5f%n", dl.outval, dx, esq);
					}
				} else if (false) {
					int mi = 8563;
					for (DataLine dl : inputx) {
						double x1 = dl.getx(0);
						double y1 = dl.getx(3);
						double y2 = dl.getx(4);
						double xv = x1 * y1 * y2;
						printf("xv %s %s%n", xv, pt_monovals[mi * nLines + dl.index]);
						double dx = (dl.outval - C0 * xv);
						esq += dx * dx;
						printf("%15.5f %15.5f %15.5f   %s%n", dl.outval, dx, esq, dl.outval / xv);
					}
				}
			}
		}
		die("SWITCHKLUDGE %s %s", lookup0.switchkludge, tm());
		return null;
	}

	void run() {

		//		int maxpow = inpx.max_var_exp;
		//		UList<Integer> pows = rangeClosed(-maxpow, maxpow);
		//
		//		//		int numInVars = inpx.numInVars;
		//		printf("cy %s%n", code(y0, 0));

		final int P = 0;
		final int PP = 1;
		final int PPP = 2;

		printf("USE DIM? %s%n", inpx.use_dimensional_analysis);

		doP();
		die();

		doPP(b_2P);
		die();

		doP2_P2(b_2Pdivpow2P, 0, true);
		printf("NT %s%n", rsoln.ntries());
		for (int i = 0; i < monosz; ++i)
			printf("%5s %s%n", i, lookup0.range_checked[i]);
		die();

		doP();
		doPP(b_2P);
		doPP(b_pow2P);
		doP3(b_3P, 0);
		doP3(b_Pdiv2P, 0);
		doP4(b_4P, 0, true);

		//  b_4P
		//  b_pow3P
		//  b_Ppluspow2P
		//  b_Pmulpow2P
		//  b_pow2PdivP
		//  b_Pdiv3P
		doP3(b_Pdivpow2P, 0);
		//  b_pow4P
		//  b_2Pdiv2P
		//  b_2Ppluspow2P
		//  b_pow2Pmul2P
		//  b_Pdiv4P
		//  b_3Pdiv2P
		//  b_pow2Pdiv2P
		//  b_2Pdiv3P
		doP4(b_2Pdivpow2P, 0, false);
		//  b_3Ppluspow2P
		//  b_pow2Ppluspow2P
		//  b_pow2Pmul3P
		//  b_3Pdiv3P
		//  b_3Pdivpow2P
		//  b_4Pdiv2P
		//  b_pow2Pdiv3P
		//  b_2Pdiv4P
		//  b_4Ppluspow2P
		//  b_pow2Pmul4P
		//  b_3Pdiv4P
		//  b_4Pdiv3P
		//  b_4Pdivpow2P
		//  b_pow2Pdiv4P
		//  b_4Pdiv4P

		die();

		//		doPaddPdivPP();

		switch (PP) {
		case PPP:
			doP3(inpx.infile.contains("I.13.4"));
			break;

		case PP:
			// 1/4 hack for I.24.6
			//doPP(inpx.infile.contains("I.24.6"));
			doPP(b_2P);
			break;

		case P:
			// I.12.2
			doP();
			break;

		default:
			long ycode = code(y0, 0);
			//		double y[] = new double[nLines];
			//		Arrays.copyOf(y0, y0.length, y);
			double y[] = Arrays.copyOf(y0, y0.length);
			double c = 0.01;
			for (int j = 0; j < nLines; ++j)
				y[j] = y0[j] * c;
			while (true) {
				ycode = nextcode(y, y0, c);
				c = nextcode_c;
				if (ycode == -1)
					break;
				printf("%15.4f %20s %s%n", c, ycode, codeList(ycode));
			}
			//
			//		double minC = 0; // inpx.min_const_val;
			//		double maxC = inpx.max_const_val;
			//
			//		double lowest[] = new double[numLines];
			//		for (int i = 0; i < numLines; ++i) {
			//			lowest[i] = pt_monovals[i][0];
			//			if (lowest[i] == 0)
			//				printf("PT %s%n", pt_monovals[i][1]);
			//			myassert(lowest[i] > 0);
			//		}
			//
			//		double c = 100000;
			//		for (int i = 0; i < numLines; ++i) {
			//			double ci = y0[i] / lowest[i];
			//			//			printf("ci %e %f %e%n", y0[i], lowest[i], ci);
			//			c = Math.min(c, ci);
			//			if (c > maxC) {
			//
			//			}
			//		}
			//
			//		double targ[] = new double[numLines];
			//		c = 1 / maxC;
			//		for (int i = 0; i < numLines; ++i)
			//			targ[i] = y0[i] * c;
			//		printf("C %f  Y: %s%n", c, mkUList(targ));
			//
			//		double tol = 1.0;
			//		int nmono = pt_monovals[0].length / 2;
			//		LinkedHashMap<Double, Integer> mp = new LinkedHashMap<>();
			//		int[] low_inds = new int[numLines];
			//		int[] hi_inds = new int[numLines];
			//		for (int lineNo = 0; lineNo < numLines; ++lineNo) {
			//			double[] monovals = pt_monovals[lineNo];
			//			int ind = 0;
			//			double tg = targ[lineNo];
			//			while (ind < nmono && monovals[2 * ind] < tg - tol)
			//				ind++;
			//			low_inds[lineNo] = ind;
			//			while (ind < nmono && monovals[2 * ind] < tg + tol) {
			//				mp.merge(monovals[2 * ind + 1], 1, (vl, indx) -> indx + 1);
			//				ind++;
			//			}
			//			hi_inds[lineNo] = ind;
			//		}
			//
			//		for (int iter = 0; iter < 100; ++iter) {
			//			c *= 1.01;
			//			int bad = -200000000;
			//			for (int lineNo = 0; lineNo < numLines; ++lineNo) {
			//				targ[lineNo] = y0[lineNo] * c;
			//
			//				double[] monovals = pt_monovals[lineNo];
			//				int ind = low_inds[lineNo];
			//				double tg = targ[lineNo];
			//				while (ind < nmono && monovals[2 * ind] < tg - tol) {
			//					boolean is1 = mp.getOrDefault(monovals[2 * ind + 1], 10) == 1;
			//					myassert(mp.containsKey(monovals[2 * ind + 1]));
			//
			//					myassert(mp.getOrDefault(monovals[2 * ind + 1], -1) != bad);
			//					myassert(mp.getOrDefault(monovals[2 * ind + 1], -1) != bad - 1);
			//					int oldval = (mp.getOrDefault(monovals[2 * ind + 1], -1));
			//					mp.merge(monovals[2 * ind + 1], bad, (vl, indx) -> indx == 1 ? null : indx - 1);
			//					myassert(mp.getOrDefault(monovals[2 * ind + 1], -1) != bad);
			//					myassert(mp.getOrDefault(monovals[2 * ind + 1], -1) != bad - 1, oldval);
			//					if (is1)
			//						myassert(!mp.containsKey(monovals[2 * ind + 1]));
			//					ind++;
			//				}
			//				low_inds[lineNo] = ind;
			//
			//				ind = hi_inds[lineNo];
			//				while (ind < nmono && monovals[2 * ind] < tg + tol) {
			//					myassert(mp.getOrDefault(monovals[2 * ind + 1], -1) != bad);
			//					myassert(mp.getOrDefault(monovals[2 * ind + 1], -1) != bad - 1);
			//					mp.merge(monovals[2 * ind + 1], 1, (vl, indx) -> indx + 1);
			//					myassert(mp.getOrDefault(monovals[2 * ind + 1], -1) != bad);
			//					myassert(mp.getOrDefault(monovals[2 * ind + 1], -1) != bad - 1);
			//					ind++;
			//				}
			//				hi_inds[lineNo] = ind;
			//			}
			//			for (Map.Entry<Double, Integer> entry : mp.entrySet()) {
			//				myassert(entry.getValue() <= numLines);
			//				myassert(entry.getValue() != bad - 1);
			//				myassert(entry.getValue() > 0, entry.getValue(), entry.getKey());
			//				if (entry.getValue() == numLines)
			//					printf("GOT ONE %s%n", entry.getKey());
			//			}
			//		}
			//
		}
		final long tm = System.currentTimeMillis();
		printf("TM %.5f%n", (tm - starttm) / 1000.0);
		die();
	}

	//https://www.baeldung.com/java-quicksort
	// end is the LAST INDEX in the array, NOT its length!
	private void quickSort2(double arr[], int begin, int end) {
		if (begin < end) {
			int partitionIndex = partition(arr, begin, end);

			quickSort2(arr, begin, partitionIndex - 1);
			quickSort2(arr, partitionIndex + 1, end);
		}
	}

	private int partition(double arr[], int begin, int end) {
		double pivot = arr[2 * end];
		int i = (begin - 1);

		//		printf("par %s %s%n", begin, end);
		myassert(arr[0] != 0);

		for (int j = begin; j < end; j++) {
			if (arr[2 * j] <= pivot) {
				i++;

				double swapTemp = arr[2 * i];
				myassert(swapTemp != 0);
				myassert(arr[2 * j] != 0);

				arr[2 * i] = arr[2 * j];
				arr[2 * j] = swapTemp;

				swapTemp = arr[2 * i + 1];
				arr[2 * i + 1] = arr[2 * j + 1];
				arr[2 * j + 1] = swapTemp;
			}
		}
		myassert(arr[0] != 0);
		myassert(arr[2 * end] != 0);

		double swapTemp = arr[2 * (i + 1)];
		arr[2 * (i + 1)] = arr[2 * end];
		arr[2 * end] = swapTemp;

		myassert(arr[0] != 0, i, swapTemp);

		swapTemp = arr[2 * (i + 1) + 1];
		arr[2 * (i + 1) + 1] = arr[2 * end + 1];
		arr[2 * end + 1] = swapTemp;

		myassert(arr[0] != 0);

		return i + 1;
	}

	public static void en(GrowList<Double> out, UList<Double> xs, double xin, int dim) {
		double x = xs.get(dim);
		if (dim > 0) {
			en(out, xs, xin, dim - 1);
			en(out, xs, xin * x, dim - 1);
			en(out, xs, xin * x * x, dim - 1);
			en(out, xs, xin / x, dim - 1);
			en(out, xs, xin / (x * x), dim - 1);
		} else {
			out.add(xin);
			out.add(xin * x);
			out.add(xin * x * x);
			out.add(xin / x);
			out.add(xin / (x * x));
		}
	}
}

//private final UList<double[]> monovals;
//private final UList<Long> monobuckets;
//
//public Dumb(Input inpx) {
//	super();
//	this.inpx = inpx;
//
//	int numInVars = inpx.numInVars;
//	{
//		GrowList<double[]> outx = new GrowList<>();
//		double x[] = new double[numInVars];
//		Arrays.fill(x, 1.0);
//		en(outx, inputx, x, 0, inpx.max_var_exp, numInVars);
//		this.monovals = outx.elems();
//	}
//	inputx.forEach(x -> myassert(x.outval >= 0 && x.invals.all(y -> y >= 0)));
//
//	//		monovals.map(x-> )
//	for (int j = 0; j < numInVars; ++j) {
//		double vmx = monovals.first()[j];
//		double vmn = vmx;
//		GrowList<Double> outx = new GrowList<>();
//		for (int i = 0; i < monovals.size(); ++i) {
//			double x[] = monovals.get(i);
//			vmx = Math.max(vmx, x[j]);
//			vmn = Math.min(vmn, x[j]);
//			outx.add(x[j]);
//		}
//		sort(outx.elems());
//		//			printf("M %s %s %s%n", j, vmn, vmx, )
//	}
//
//	//		Collections.max(null);
//
//}
