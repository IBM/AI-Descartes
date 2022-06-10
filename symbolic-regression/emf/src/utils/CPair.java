// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.*;

public class CPair<TL extends Comparable<//? super  for ? would have to modify chainCompare
		TL>, TR extends Comparable<//? super
				TR>>
		extends Pair<TL, TR> implements Comparable<CPair<TL, TR>> {

	public CPair(TL left, TR right) {
		super(left, right);
	}

	@Override
	public int compareTo(CPair<TL, TR> that) {
		return chainCompare(this.left, that.left, this.right, that.right);
	}
}
