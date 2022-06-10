// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.VRAUtils.*;

import java.util.LinkedHashMap;

import utils.UList;

public class MConst {
	public final boolean allowConst;
	public final boolean plusMinus;
	public final boolean onlyOne;
	public final double cnst_lbnd;
	public final double cnst_ubnd;

	private MConst(boolean allowConst, boolean plusMinus, double cnst_lbnd, double cnst_ubnd) {
		super();
		this.allowConst = allowConst;
		this.plusMinus = plusMinus;
		this.onlyOne = !allowConst && !plusMinus;
		this.cnst_lbnd = cnst_lbnd;
		this.cnst_ubnd = cnst_ubnd;

		myassert(!(allowConst && plusMinus));
		if (plusMinus) {
			myassert(cnst_lbnd == 0);
			myassert(cnst_ubnd == -1);
		}
	}

	private final static LinkedHashMap<UList<Object>, MConst> univ = new LinkedHashMap<>();

	public static MConst getMConst(double cnst_lbnd, double cnst_ubnd) {
		return getMConst(true, false, cnst_lbnd, cnst_ubnd);
	}

	public final static MConst mconstPlusMinus = getMConst(false, true, 0, -1);
	public final static MConst mconstOne = getMConst(false, false, 0, -1);

	public static MConst getMConst(boolean allowConst, boolean plusMinus, double cnst_lbnd, double cnst_ubnd) {
		return univ.computeIfAbsent(mkUList1(allowConst, plusMinus, cnst_lbnd, cnst_ubnd),
				x -> new MConst(allowConst, plusMinus, cnst_lbnd, cnst_ubnd));
	}

}
