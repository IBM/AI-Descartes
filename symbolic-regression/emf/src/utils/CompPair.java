// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.*;

public class CompPair<TL extends Comparable<TL>, TR extends Comparable<TR>> extends Pair<TL, TR>
		implements Comparable<CompPair<TL, TR>> {
	public CompPair(TL left, TR right) {
		super(left, right);
	}

	@Override
	public int compareTo(CompPair<TL, TR> that) {
		return chainCompare(this.left, that.left, this.right, that.right);
	}
}
