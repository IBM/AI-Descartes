// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

public abstract class Split implements Comparable<Split> {
	public final MLInstance mlsplit;

	protected Split(MLInstance mlsplit) {
		this.mlsplit = mlsplit;
	}

	@Override
	public int compareTo(Split that) {
		if (this instanceof ConstSplit) {
			if (that instanceof ConstSplit)
				return ((ConstSplit) this).compareToAux((ConstSplit) that);
		} else if (this instanceof ExpSplit) {
			if (that instanceof ExpSplit)
				return ((ExpSplit) this).compareToAux((ExpSplit) that);
		} else if (this instanceof IntExpSplit) {
			if (that instanceof IntExpSplit)
				return ((IntExpSplit) this).compareToAux((IntExpSplit) that);
		} else if (this instanceof DropConstSplit) {
			if (that instanceof DropConstSplit)
				return ((DropConstSplit) this).compareToAux((DropConstSplit) that);
		} else {
			die();
		}
		return this.getClass().getName().compareTo(that.getClass().getName());
	}
}
