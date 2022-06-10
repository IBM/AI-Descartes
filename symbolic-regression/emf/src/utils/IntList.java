// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

public class IntList extends UList<Integer> implements Comparable<IntList> {

	public IntList() {

	}

	//	public IntList subList(int fromIndex) {
	//		//return new UList<>(this, fromIndex, toIndex);
	//		return super.subList(fromIndex);
	//	}

	// modified from OList
	@Override
	public int compareTo(IntList x) {
		if (size() < x.size())
			return -1;
		if (size() > x.size())
			return 1;
		for (int i = 0; i < size(); ++i) {
			int cmp = get(i).compareTo(x.get(i));
			if (cmp != 0)
				return cmp;
		}
		return 0;
	}

}
