// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

import utils.UList;

public class Soln {
	public final int fno;
	public final int caseno;
	public final UList<Integer> monoinds;
	public final double C0;

	public final double sqerr;

	public final Dumb dx;

	public Soln(Dumb dx, int fno, int caseno, double C0, double sqerr, Integer... monoinds) {
		this(dx, fno, caseno, C0, sqerr, UList.mkUList(monoinds));
	}

	public Soln(Dumb dx, int fno, int caseno, double C0, double sqerr, UList<Integer> monoinds) {
		this.dx = dx;
		this.fno = fno;
		this.caseno = caseno;
		this.monoinds = monoinds;
		this.C0 = C0;
		this.sqerr = sqerr;
		monoinds.forEach(ind -> myassert(ind >= 0));
		monoinds.forEach(mind -> myassert(dx.monoInd(dx.monoList(mind)) == mind));
	}

	public String toString() {
		return String.format("{%s %s %s %s %s %s %s}", sqerr, fno, caseno, FormCode.formStrs[fno], C0,
				monoinds.map(mind -> dx.monoList(mind)), dx.inpx.invarnms);
	}

}
