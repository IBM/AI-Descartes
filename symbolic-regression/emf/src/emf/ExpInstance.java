// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

import utils.Univ2;

public class ExpInstance implements Comparable<ExpInstance> {
	public final char code; // for all other expn
	//	public final int nodeNo; // for fractional P

	public final String nm;
	public final Double powexp;
	public final boolean powIsFixed;

	public static final String expmul_prefix = "F";

	public String toString() {
		return nm;
	}

	public String expnm() {
		//return (code == '?' ? expmul_prefix + nodeNo : expmul_prefix + code);
		return nm;
	}

	private ExpInstance(char code, Double powexp) {
		this.code = code;
		//		this.nodeNo = -1;
		this.nm = expmul_prefix + code;
		this.powexp = powexp;
		this.powIsFixed = powexp != null;
	}

	//	private ExpInstance(int nodeNo) {
	//		this.code = '?';
	//		this.nodeNo = nodeNo;
	//		this.nm = expmul_prefix + nodeNo;
	//		this.powexp = null;
	//	}

	public int compareTo(ExpInstance that) {
		return chainCompare(this.nm, that.nm);
	}

	//	private static Univ1<Character, ExpInstance> cuniv = new Univ1<>(nodeNo -> new ExpInstance(nodeNo));
	private static Univ2<Character, Double, ExpInstance> cuniv = new Univ2<>(
			(nodeNo, powexp) -> new ExpInstance(nodeNo, powexp));
	//	private static Univ1<Integer, ExpInstance> iuniv = new Univ1<>(nodeNo -> new ExpInstance(nodeNo));

	public static ExpInstance getExpInstance(char nodeNo, Double powexp) {
		return cuniv.get(nodeNo, powexp);
	}

	//	public static ExpInstance getExpInstance(int nodeNo) {
	//		return iuniv.get(nodeNo);
	//	}
}
