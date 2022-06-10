// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

public class RunningSoln {
	public final Dumb dx;

	private int fno, caseno;
	private double b_C0 = 0;
	private double b_errsq = 1e10;
	private int p0ind = -1;
	private int p1ind = -1;
	private int p2ind = -1;
	private int p3ind = -1;

	private long nupdates = 0;
	private long ntries = 0;

	public double errsq() {
		return b_errsq;
	}

	public RunningSoln(Dumb dx) {
		this.dx = dx;
	}

	public long nupdates() {
		return nupdates;
	}

	public long ntries() {
		return ntries;
	}

	public Soln soln() {
		if (b_errsq == 1e10)
			return null;
		if (p1ind == -1)
			return new Soln(dx, fno, caseno, b_C0, b_errsq, p0ind);
		else if (p2ind == -1)
			return new Soln(dx, fno, caseno, b_C0, b_errsq, p0ind, p1ind);
		else if (p3ind == -1)
			return new Soln(dx, fno, caseno, b_C0, b_errsq, p0ind, p1ind, p2ind);
		else
			return new Soln(dx, fno, caseno, b_C0, b_errsq, p0ind, p1ind, p2ind, p3ind);
	}

	public void gotone() {
		dump("GOT ONE", b_errsq, b_C0, p0ind, p1ind, p2ind, p3ind, dx.monoList(p0ind));

		//			errsq(mul(lookup0.best_C0, mvals(eind)), y0), tol);
	}

	public void update(double errsq, int fno, int caseno, double C0, int p0ind) {
		update(errsq, fno, caseno, C0, p0ind, -1, -1, -1);
	}

	public void update(double errsq, int fno, int caseno, double C0, int p0ind, int p1ind) {
		update(errsq, fno, caseno, C0, p0ind, p1ind, -1, -1);
	}

	public void update(double errsq, int fno, int caseno, double C0, int p0ind, int p1ind, int p2ind) {
		update(errsq, fno, caseno, C0, p0ind, p1ind, p2ind, -1);
	}

	public void update(double errsq, int fno, int caseno, double C0, int p0ind, int p1ind, int p2ind, int p3ind) {
		ntries++;
		if (this.b_errsq > errsq) {
			this.b_errsq = errsq;
			this.p0ind = p0ind;
			this.p1ind = p1ind;
			this.p2ind = p2ind;
			this.p3ind = p3ind;
			this.b_C0 = C0;
			this.fno = fno;
			this.caseno = caseno;
			nupdates++;
		}
	}
	//	public String info() {
	//		return String.format("%s %s %s %s %s", fno, sqerr, FormCode.formStrs[fno], C0,
	//				monoinds.map(mind -> dx.monoList(mind)));
	//	}

}
