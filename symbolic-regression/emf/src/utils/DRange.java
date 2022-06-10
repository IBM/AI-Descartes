// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.*;

public class DRange implements Comparable<DRange> {
	public final boolean nullset;
	public final double lbound;
	public final double ubound;

	public static DRange newDRange(double lbound, double ubound) {
		myassert(lbound <= ubound, lbound, ubound);
		return new DRange(lbound, ubound);
	}

	private static DRange nullrng = new DRange(1, 0);

	private DRange(double lbound, double ubound) {
		this.lbound = lbound;
		this.ubound = ubound;
		this.nullset = lbound > ubound;
	}

	public final String toString() {
		//		return nullset ? "DRN" : "DR(" + lbound + "," + ubound + ")";
		return nullset ? "DRN" : String.format("DRs(%.1f,%.1f)", lbound, ubound);
	}

	@Override
	public int compareTo(DRange that) {
		return chainCompare(this.nullset, that.nullset, this.lbound, that.lbound, this.ubound, that.ubound);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = nullset ? 1 : 2;
		result = prime * result + (int) (Math.abs(lbound) < 1 ? lbound * 10000 : lbound);
		result = prime * result + (int) (Math.abs(ubound) < 1 ? ubound * 10000 : ubound);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return equals((DRange) obj);
	}

	public boolean equals(DRange that) {
		if (nullset || that.nullset)
			return nullset && that.nullset;
		return this.lbound == that.lbound && this.ubound == that.ubound;
	}

	public final boolean contains(double x) {
		return lbound <= x && x < ubound;
	}

	public final boolean intersects(DRange that) {
		if (this.nullset || that.nullset)
			return false;
		return this.ubound >= that.lbound && that.ubound >= this.lbound;
	}

	public final boolean subrange(DRange that) {
		if (this.nullset)
			return true;
		if (that.nullset)
			return false;
		return (this.lbound >= that.lbound && this.ubound <= that.ubound);
	}

	public final DRange div(double x) {
		if (this.nullset)
			return this;
		return new DRange(lbound / x, ubound / x);
	}

	public final DRange neg() {
		if (this.nullset)
			return this;
		return new DRange(-ubound, -lbound);
	}

	// x - this
	public final DRange rsub(double x) {
		//		if (this.nullset)
		//			return this; ??		
		return new DRange(x - ubound, x - lbound);
	}

	public final DRange div(DRange x) {
		if (this.nullset)
			return this;
		if (x.nullset)
			die();
		return new DRange(lbound / x.ubound, ubound / x.lbound);
	}

	public final DRange mul(DRange x) {
		if (this.nullset)
			return this;
		if (x.nullset)
			return x;
		return new DRange(lbound * x.lbound, ubound * x.ubound);
	}

	public final DRange intersect(DRange x) {
		if (this.nullset || x.nullset)
			return nullrng;
		//		printf("INTER %s %s %s%n", this, x, new DRange(Math.max(lbound, x.lbound), Math.min(ubound, x.ubound)));
		return new DRange(Math.max(lbound, x.lbound), Math.min(ubound, x.ubound));
	}

	public final static DRange plusMinus(double x, double tol) {
		return new DRange(x - tol, x + tol);
	}

}
