// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

public class InVar implements Comparable<InVar> {
	public final String nm;
	public final int pos;
	//	public final UList<Double> vals;

	public InVar(String nm, int pos //, UList<Double> vals
	) {
		//		this.nm = nm.replaceAll(" ", "_");
		this.nm = nm;
		if (nm.contains(" "))
			die("'%s' is not a legal variable name.", nm);
		this.pos = pos;
		//		this.vals = vals;
	}

	public String toString() {
		return nm;
	}

	@Override
	public int compareTo(InVar that) {
		return chainCompare(this.pos, that.pos);
	}
}
