// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

import utils.UList;

public class DimUnit {
	public final String nm;

	private DimUnit(String nm) {
		this.nm = nm;
	}

	private static boolean inited = false;

	public static UList<DimUnit> mkUnits(UList<String> nms) {
		myassert(!inited);
		inited = true;
		return nms.map(nm -> new DimUnit(nm));
	}

	public String toString() {
		return nm;
	}
}
