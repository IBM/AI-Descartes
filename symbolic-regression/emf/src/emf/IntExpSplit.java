// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

public class IntExpSplit extends Split {
	public final int expval;
	public final VarExp vexp;
	//public final String varnm;
	public final InVar var;

	public IntExpSplit(VarExp vexp, int expval) {
		//		super(vexp.inst);
		super(null);
		die();
		this.expval = expval;
		this.vexp = vexp;
		//this.varnm = vexp.var.nm;

		die();
		//		this.var = vexp.var;
		this.var = null;
	}

	public String toString() {
		return "exp" + mlsplit.nodeNo + var + "=" + expval;
	}

	protected int compareToAux(IntExpSplit that) {
		//return chainCompare(this.mlsplit.mn.compareTo(that.mlsplit.mn), this.splitPos, that.splitPos);
		return chainCompare(this.mlsplit, that.mlsplit, this.var, that.var, this.expval, that.expval);
	}
}
