// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

public class ConstSplit extends Split {
	public final boolean splitPositive;

	public ConstSplit(MLInstance mlsplit, boolean splitPos) {
		super(mlsplit);
		this.splitPositive = splitPos;
	}

	public String toString() {
		return mlsplit.cnm + (splitPositive ? "pos" : "neg");
	}

	protected int compareToAux(ConstSplit that) {
		//return chainCompare(this.mlsplit.mn.compareTo(that.mlsplit.mn), this.splitPos, that.splitPos);
		return chainCompare(this.mlsplit, that.mlsplit, this.splitPositive, that.splitPositive);
	}
}
