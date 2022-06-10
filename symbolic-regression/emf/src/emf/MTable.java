// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.VRAUtils.*;

import java.util.Arrays;

public class MTable extends Table {

	public final int ncombs;

	// 2D array: (ncombs * nLines)
	protected final double[] comb_vals;

	private final short[] comb_inds;
	protected final long[] comb_dims_mask;

	//	public final UMap<Integer, Pair<Integer, Integer>> comb_dims_range;

	public final short[] comb_inds() {
		return Arrays.copyOf(comb_inds, comb_inds.length);
	};

	//	private final byte[] comb_dim_masks;

	public static MTable monoTable(Input inpx) {
		return new MTable(inpx);
	}

	public static MTable yTable(Input inpx, double off) {
		return new MTable(inpx, off);
	}

	private void initInv() {
		myassert(ncombs >= 0);
		myassert(comb_vals.length == ncombs * nLines);
		myassert(comb_inds.length == ncombs);
		myassert(comb_dims_mask.length == ncombs);
	}

	protected MTable(MTable that) {
		super(that.inpx);
		this.ncombs = that.ncombs;
		this.comb_inds = that.comb_inds;
		this.comb_vals = that.comb_vals;
		this.comb_dims_mask = that.comb_dims_mask;
		//		this.comb_dims_range = that.comb_dims_range;
		initInv();
	}

	// retain only those entries with dims matching these
	//	protected MTable(MTable that, int msk) {
	//		super(that.inpx);
	//		final int ndimunits = inpx.ndimunits;
	//
	//		double[] comb_vals = new double[that.comb_vals.length];
	//		short[] comb_inds = new short[that.comb_inds.length];
	//		int[] comb_dims_mask = new int[that.comb_dims_mask.length];
	//		int ncs = 0;
	//		for (int i = 0; i < that.ncombs; ++i) {
	//			if (that.comb_dims_mask[i] == msk) {
	//				for (int j = 0; j < nLines; ++j)
	//					comb_vals[ncs * nLines + j] = that.comb_vals[i * nLines + j];
	//				comb_inds[ncs] = that.comb_inds[i];
	//				comb_dims_mask[ncs] = comb_dims_mask[i];
	//
	//				ncs++;
	//			}
	//		}
	//		this.ncombs = ncs;
	//		this.comb_inds = Arrays.copyOf(comb_inds, ncs * combsz);
	//		this.comb_vals = Arrays.copyOf(comb_vals, ncs * nLines);
	//		this.comb_dims_mask = Arrays.copyOf(comb_dims_mask, ncs);
	//		//		this.comb_dim_masks = Arrays.copyOf(that.comb_dim_masks, ncs * nLines);
	//
	//		initInv();
	//
	//	}

	// for dropSmall()
	//	protected MTable(MTable that, double tol) {
	//		super(that.inpx);
	//
	//		double[] comb_vals = new double[that.comb_vals.length];
	//		short[] comb_inds = new short[that.comb_inds.length];
	//
	//		this.combsz = that.combsz;
	//		int ncs = 0;
	//		for (int i = 0; i < that.ncombs; ++i) {
	//			boolean someNotSmall = false;
	//
	//			for (int j = 0; j < nLines; ++j)
	//				if (that.comb_vals[i * nLines + j] > tol)
	//					someNotSmall = true;
	//			if (someNotSmall) {
	//				for (int j = 0; j < nLines; ++j)
	//					comb_vals[ncs * nLines + j] = that.comb_vals[i * nLines + j];
	//				for (int j = 0; j < combsz; ++j)
	//					comb_inds[ncs * combsz + j] = that.comb_inds[i * combsz + j];
	//				ncs++;
	//			}
	//		}
	//		this.ncombs = ncs;
	//		this.comb_inds = Arrays.copyOf(comb_inds, ncs * combsz);
	//		this.comb_vals = Arrays.copyOf(comb_vals, ncs * nLines);
	//		this.comb_dims = that.comb_dims;		
	//		//		this.comb_dim_masks = Arrays.copyOf(that.comb_dim_masks, ncs * nLines);
	//
	//		initInv();
	//
	//	}

	private MTable(Input inpx, double offset) {
		super(inpx);
		this.comb_vals = new double[nLines];
		this.comb_inds = new short[1];
		//		this.comb_dim_masks = new byte[nLines];
		//		this.comb_inds = null;
		this.ncombs = 1;

		inputx.forEachI((i, line) -> {
			comb_vals[i] = line.outval + offset;
			//			comb_inds[i] = -1; // ??
		});
		printf("Y %s%n", mkUList(comb_vals));

		//this.comb_dims_mask = inpx.invar_dim_masks;
		this.comb_dims_mask = null; // THINK
		//		this.comb_dims_range = null; // THINK

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
		initInv();
	}

	//	final static int dim_msk_sz = 4;
	//	final static int min_dim = -4;
	//	final static int max_dim = 4;
	//
	//	static int mkDimMask(UList<Integer> dims) {
	//		int rval = 0;
	//		for (int vi = 0; vi < dims.size(); vi++) {
	//			rval <<= dim_msk_sz;
	//			int dim = dims.get(vi);
	//			myassert(min_dim <= dim && dim <= max_dim);
	//			rval |= (dim + (-min_dim));
	//		}
	//		return rval;
	//	}
	//
	//	static int mkDimList(int mask) {
	//		int []xs = new int[]
	//		GrowList<Integer> rval = 0;
	//		for (int vi = 0; vi < dims.size(); vi++) {
	//			rval <<= dim_msk_sz;
	//			int dim = dims.get(vi);
	//			myassert(min_dim <= dim && dim <= max_dim);
	//			rval |= (dim + (-min_dim));
	//		}
	//		return rval;
	//	}

	// single mono
	protected MTable(Input inpx) {
		super(inpx);

		boolean checkMonos = false;

		this.ncombs = (int) Math.pow(2 * inpx.max_var_exp + 1, numInVars);
		myassert((short) ncombs == ncombs);

		this.comb_vals = new double[ncombs * nLines];
		this.comb_inds = new short[ncombs];
		this.comb_dims_mask = new long[ncombs];

		myassert(ncombs <= Short.MAX_VALUE);
		for (short i = 0; i < ncombs; ++i) {
			comb_inds[i] = i;
			enx(comb_vals, i, inputx, inpx.max_var_exp, comb_dims_mask);

			//			for (int j = 0; j < nLines; ++j)
			//				inpx.dimensionUnits.forEach(unit -> {
			//					inpx.invars.filter(iv -> inpx.dimensionVarsByDimUnit.get(iv).get(unit) != 0)
			//				});
			//
			//			DimAnal da = new DimAnal(inpx, unit, use_max_num_consts, numPsums() > 1);
			//			UList<Constraint> rval = da.dimAnal(this, getBExpr(inpx.outvarDimUnits.get(unit)), getBExpr(1));

			if (checkMonos)
				for (int j = 0; j < nLines; ++j) {
					//					dump(monoList(i));
					double xv = eval_monoList(monoList(i), j);
					myassertTolEqual("", xv, comb_vals[i * nLines + j], xv * 0.0001, monoList(i));
				}
		}

		myassert(comb_vals.length == ncombs * nLines);
		initInv();
	}

	public static MTable pTable(Input inpx) {
		return new MTable(inpx);
	}

	//	public MTable add(MTable that) {
	//		return new MTable(this, that, false);
	//	}
	//
	//	public MTable sub(MTable that) {
	//		return new MTable(this, that, true);
	//	}

	//	public MTable dropSmall(double tol) {
	//		return new MTable(this, tol);
	//	}

	// P + P
	//	private MTable(MTable x, MTable y, boolean sub) {
	//		super(x.inpx);
	//
	//		int nx = x.ncombs;
	//		int ny = y.ncombs;
	//		this.ncombs = nx * ny;
	//		int nx1 = nx - 3000;
	//		//		dump(ncombs, nx, ny, nLines, ncombs * nLines, nx1 * nx1 * nLines);
	//		this.comb_vals = new double[ncombs * nLines];
	//		this.comb_inds = new short[ncombs];
	//		this.comb_dims_mask = new int[ncombs];
	//		die("init comb_dims");
	//
	//		double[] xcomb_vals = x.comb_vals;
	//		double[] ycomb_vals = y.comb_vals;
	//		for (int ix = 0; ix < nx; ++ix)
	//			for (int iy = 0; iy < ny; ++iy) {
	//				int cindx = ix * cszx;
	//				int cindy = iy * cszy;
	//				int cind = ((ix * ny) + iy) * combsz;
	//
	//				if (cszx == 0)
	//					;
	//				else if (cszx == 1)
	//					comb_inds[cind++] = x.comb_inds[cindx];
	//				else if (cszx == 2) {
	//					comb_inds[cind++] = x.comb_inds[cindx];
	//					comb_inds[cind++] = x.comb_inds[cindx];
	//				} else
	//					die(cszx);
	//
	//				if (cszy == 1)
	//					comb_inds[cind++] = y.comb_inds[cindy];
	//				else if (cszy == 2) {
	//					comb_inds[cind++] = y.comb_inds[cindy];
	//					comb_inds[cind++] = y.comb_inds[cindy];
	//				} else
	//					die();
	//
	//				int basex = ix * nLines;
	//				int basey = iy * nLines;
	//				int base3 = ((ix * ny) + iy) * nLines;
	//				if (sub)
	//					for (int j = 0; j < nLines; ++j) {
	//						comb_vals[base3 + j] = xcomb_vals[basex + j] - ycomb_vals[basey + j];
	//					}
	//				else
	//					for (int j = 0; j < nLines; ++j) {
	//						comb_vals[base3 + j] = xcomb_vals[basex + j] + ycomb_vals[basey + j];
	//					}
	//			}
	//		initInv();
	//	}

	public double[] copyElems() {
		return Arrays.copyOf(comb_vals, comb_vals.length);
	}

	public void copyElems(int source_pt, double[] out) {
		for (int i = 0; i < ncombs; ++i)
			out[i] = comb_vals[i * nLines + source_pt];
	}

	public short[] copyInds() {
		return Arrays.copyOf(comb_inds, comb_inds.length);
	}

}
