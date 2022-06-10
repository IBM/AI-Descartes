// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.*;

import java.util.Comparator;

public class Pair<TL, TR> {
	public final TL left;
	public final TR right;

	public Pair(TL left, TR right) {
		this.left = left;
		this.right = right;
		myassert(left != null);
		myassert(right != null);
	}

	public String toString() {
		return "Pair<" + left + "," + right + ">";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		Pair<TL, TR> other = (Pair<TL, TR>) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}

	public static <TL extends Comparable<TL>, TR extends Comparable<TR>> Comparator<Pair<TL, TR>> pairComparator() {
		return new Comparator<Pair<TL, TR>>() {
			@Override
			public int compare(Pair<TL, TR> e1, Pair<TL, TR> e2) {
				return comparePairs(e1, e2);
			}
		};
	}

	public static <TL extends Comparable<TL>, TR extends Comparable<TR>> int comparePairs(Pair<TL, TR> x,
			Pair<TL, TR> y) {
		return chainCompare(x.left, y.left, x.right, y.right);
	}

	static {
		assert newPair(1, 1).equals(newPair(1, 1));
	}
}
