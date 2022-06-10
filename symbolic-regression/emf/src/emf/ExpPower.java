// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

public class ExpPower {
	public final boolean isLiteral;
	public final int index;
	private final Double litval_dont_use;

	public ExpPower(int index, double lit) {
		this.isLiteral = true;
		this.index = index;
		this.litval_dont_use = lit;
	}

	public ExpPower(int index) {
		this.isLiteral = false;
		this.index = index;
		this.litval_dont_use = -666666.0;
	}

	public double litval() {
		myassert(isLiteral);
		return litval_dont_use;
	}

	public String toString() {
		if (isLiteral)
			return "" + litval_dont_use;
		return "ContEx";
	}

}
