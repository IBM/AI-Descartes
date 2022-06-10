// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import utils.UList;

public class RunResultInfeasible extends RunResult {
	public RunResultInfeasible(BaronJob baronJob) {
		super(baronJob, UList.empty());
		//		printf("RESEULT INFE %s%n", baronJob.expr);
	}

}
