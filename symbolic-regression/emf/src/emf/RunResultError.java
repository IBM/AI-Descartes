// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import utils.UList;

public class RunResultError extends RunResult {
	public RunResultError(BaronJob baronJob, UList<String> output) {
		super(baronJob, output);
	}

	public RunResultError(BaronJob baronJob) {
		super(baronJob, UList.empty());
		//		printf("RESEULT ERR %s%n", baronJob.expr);
	}
}
