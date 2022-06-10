// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAFileUtils.*;
import static utils.VRAUtils.*;

import java.util.List;

public class SSLine {
	public final String ssName;
	public final int lineInSpreadsheet;
	public final String line;
	public final String[] slots; // old code
	public final List<String> cells;
	public final Boolean remapped;

	public String toString() {
		return "SSL(" + lineInSpreadsheet + "," + line + ")";
	}

	public String fileAndLine() {
		return ssName + ":" + lineInSpreadsheet;
	}

	static String[] trim_all(String[] ss) {
		String[] ss2 = new String[ss.length];
		for (int i = 0; i < ss.length; ++i)
			ss2[i] = ss[i].trim();
		return ss2;
	}

	public SSLine(String ssName, String line, int lineno) {
		this.ssName = ssName;
		this.line = line;
		this.slots = trim_all(cvs_split(line));
		this.cells = asList(line.split(","));

		this.lineInSpreadsheet = lineno;
		this.remapped = false;
	}

	public SSLine(String ssName, String line, String[] slots, int lineno) {
		this.ssName = ssName;
		this.line = line;
		this.slots = trim_all(slots);
		this.cells = asList(line.split(","));

		this.lineInSpreadsheet = lineno;
		this.remapped = true;
	}
}
