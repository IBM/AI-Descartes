// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.UList.*;
import static utils.VRAUtils.*;

import utils.UList;

public class OneSet<T> {

	// may be null, representing All.
	public final T item;

	public final boolean isAll;

	public String toString() {
		return isAll ? "ALL" : item.toString();
	}

	public OneSet(T team) {
		this.item = nonNull(team);
		this.isAll = false;
	}

	public OneSet() {
		this.item = null;
		this.isAll = true;
	}

	public UList<T> intersect(UList<T> xs) {
		if (isAll)
			return xs;
		if (xs.contains(item))
			return mkUList1(item);
		return UList.empty();
	}

	public boolean intersects(UList<T> xs) {
		if (isAll)
			return true;
		return xs.contains(item);
	}

	public boolean contains(T x) {
		return matches(x);
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;// if both of them points the same address in memory

		if (!(that instanceof OneSet<?>))
			return false; // if "that" is not a People or a childclass

		@SuppressWarnings("unchecked")
		OneSet<T> thatx = (OneSet<T>) that; // than we can cast it to People safely

		return isAll ? thatx.isAll : thatx.isAll ? false : this.item.equals(thatx.item);
	}

	@Override
	public int hashCode() {
		return isAll ? 1234 : item.hashCode();
	}

	public boolean matches(T x) {
		return isAll || x == item;
	}

	// for some debugging code
	public boolean matchesExactly(OneSet<T> x) {
		if (x.isAll)
			return true;
		return !isAll && x.item == item;
	}

	/** does NOT match if the set is All */
	public boolean matchesExactly(T x) {
		return !isAll && x == item;
	}

	public final static <T> OneSet<T> newOneSet(T x) {
		return new OneSet<>(x);
	}

}
