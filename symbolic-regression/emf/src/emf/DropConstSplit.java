// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

public class DropConstSplit extends Split {
	public DropConstSplit(MLInstance mlsplit) {
		super(mlsplit);
	}

	public String toString() {
		return "drp_" + mlsplit.cnm;
	}

	protected int compareToAux(DropConstSplit that) {
		//return chainCompare(this.mlsplit.mn.compareTo(that.mlsplit.mn), this.splitPos, that.splitPos);
		return chainCompare(this.mlsplit, that.mlsplit);
	}
}
