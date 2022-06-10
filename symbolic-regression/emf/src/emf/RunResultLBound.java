// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.VRAUtils.*;

import utils.UList;

public class RunResultLBound extends RunResult {

	public final double lbound;

	public RunResultLBound(BaronJob baronJob, UList<String> output, UList<String> timout) {
		super(baronJob, output);

		myassert(heuristic_completion);
		if (timout.isEmpty()) {
			printf("BAD TIMOUT FILE! %s%n", timout);
			this.lbound = 0;
		} else {
			//[problem, 0, 6, 1291, 54, 0.00000000000, 72.5299661210, 12, 4, 0, 1350, 862, 430, 4.03, 4.15]
			//					printf("TIMFILE: %s%n", split(timout.first(), " +"));
			UList<String> info = split(timout.first(), " +");
			this.lbound = toDouble(info.get(5));
		}
	}
}
