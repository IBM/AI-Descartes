// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.VRAUtils.*;

public class OutTable extends Table {

	// SHOULD NEVER CHANGE
	private final double[] y0;

	//	private final double[] y;
	//	private final double[] hi;
	//	private final double[] lo;

	//	private final int[] inds;

	private MTable lo_tbl, hi_tbl;

	public OutTable(Input inpx, double tol) {
		super(inpx);

		this.y0 = MTable.yTable(inpx, 0).copyElems();

		this.lo_tbl = MTable.yTable(inpx, -tol);
		this.hi_tbl = MTable.yTable(inpx, tol);

		printf("ylo %s%n", mkUList(lo_tbl.copyElems()));
		printf("yhi %s%n", mkUList(hi_tbl.copyElems()));
	}

	private OutTable(OutTable that, MTable lo, MTable hi) {
		super(that.inpx);
		this.y0 = that.y0;
		this.lo_tbl = lo;
		this.hi_tbl = hi;
	}

	//	public void doPnoC(MTable ptbl, double C0) {
	//
	//		int[] comb_inds;
	//		myassert(ptbl.combsz == 1);
	//		comb_inds = new int[ptbl.ncombs];
	//
	//		double[] comb_vals1;
	//		comb_vals1 = new double[ptbl.ncombs];
	//		int source_pt = 0;
	//		int ncombs = ptbl.ncombs;
	//
	//		ptbl.copyElems(source_pt, comb_vals1);
	//		quickSort2(comb_vals1, comb_inds);
	//		for (int i = 0; i < ncombs; ++i) {
	//			
	//		}
	//	}

	//	public OutTable(Input inpx) {
	//		super(inpx);
	//		this.y0 = new double[nLines];
	//		inputx.forEach(line -> y0[line.index] = line.outval);
	//		printf("Y %s%n", mkUList(y0));
	//
	//		double tol = chooseTol();
	//
	//		lo = new double[nLines];
	//		hi = new double[nLines];
	//
	//		// this represents one entry
	//		for (int j = 0; j < nLines; ++j) {
	//			lo[j] = y0[j] - tol;
	//			hi[j] = y0[j] + tol;
	//		}
	//	}

	public double[] copyElems() {
		die();
		return null;
	}

	public void clearDiv() {
		double[] lo = lo_tbl.copyElems();
		double[] hi = hi_tbl.copyElems();
		//int ncombs = lo.length / nLines;
		int ncombs = lo_tbl.ncombs;
		myassert(lo.length == ncombs * nLines, lo.length, ncombs);

		int[] divSizes = new int[100];
		byte[] sortOrder = new byte[ncombs * nLines];
		//		byte[] firstDiv = new byte[ncombs];
		byte[] divSz = new byte[ncombs];
		double[] buf = new double[2 * nLines];
		for (int i = 0; i < ncombs; ++i) {
			int base = i * nLines;
			for (int j = 0; j < nLines; ++j) {
				buf[2 * j] = hi[base + j];
				buf[2 * j + 1] = j;
			}
			die();
			//			quickSort2(buf);

			myassert(buf[2 * 0] <= buf[2 * 1]); // ascending
			int firstClearDiv = -1;
			for (int sortj = 0; sortj < nLines1; ++sortj) {
				int ind = (byte) (int) (buf[2 * sortj + 1]);
				if (hi[base + ind] <= lo[base + ind + 1]) {
					firstClearDiv = sortj;
					divSizes[firstClearDiv]++;
					break;
				}
			}
		}

		for (int dsz = 0; dsz < 10; ++dsz)
			printf("dsz %2s: %s%n", dsz, divSizes[dsz]);
	}

	//	public OutTable add(MTable that) {
	//		return new OutTable(this, lo_tbl.add(that), hi_tbl.add(that));
	//	}
	//
	//	public OutTable sub(MTable that) {
	//		return new OutTable(this, lo_tbl.sub(that), hi_tbl.sub(that));
	//	}
}
