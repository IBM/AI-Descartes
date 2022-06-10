// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.*;

/** inclusive bounds */
public class IntRange extends Pair<Integer, Integer> {
	public int lbound;
	public int ubound;

	public boolean sameBound;

	public IntRange(int lb, int ub) {
		super(lb, ub);
		this.lbound = lb;
		this.ubound = ub;
		this.sameBound = (lb == ub);
		myassert(lb <= ub);
	}

	public int size() {
		return ubound - lbound + 1;
	}

	// not bothering with iterable interface 
	public UList<Integer> elems() {
		return UList.rangeClosed(lbound, ubound);
	}

	public String toString() {
		return lbound + "," + ubound;
	}
}
