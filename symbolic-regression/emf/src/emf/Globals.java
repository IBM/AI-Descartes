// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

public class Globals {

	//	public final int M_complexity;
	//	public final int addsub_complexity;
	//	public final int mul_complexity;
	//	public final int div_complexity;

	public static void makeGlobals(Input inpx) {
		myassert(!alreadyCalled);
		alreadyCalled = true;
		myassert(gx == null);
		gx = new Globals(inpx);
	}

	private Globals(Input inpx) {
		//		this.M_complexity = inpx.M_complexity;
		//		this.mul_complexity = inpx.mul_complexity;
		//		this.addsub_complexity = inpx.addsub_complexity;
		//		this.div_complexity = inpx.div_complexity;
	}

	public static Globals gx;

	private static boolean alreadyCalled;
}
