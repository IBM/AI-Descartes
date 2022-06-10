// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

public class ExpSplit extends Split {
	public final boolean splitGT1;

	public ExpSplit(MLInstance mlsplit, boolean splitPos) {
		super(mlsplit);
		this.splitGT1 = splitPos;
	}

	public String toString() {
		return mlsplit.cnm + (splitGT1 ? "ge1" : "le1");
	}

	protected int compareToAux(ExpSplit that) {
		//return chainCompare(this.mlsplit.mn.compareTo(that.mlsplit.mn), this.splitPos, that.splitPos);
		return chainCompare(this.mlsplit, that.mlsplit, this.splitGT1, that.splitGT1);
	}
}
