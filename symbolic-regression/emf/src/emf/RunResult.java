// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import utils.UList;

public abstract class RunResult {
	public final BaronJob baronJob;

	public final boolean max_time_exceeded;
	public final boolean infeasible;

	public final boolean heuristic_completion;

	public final boolean isPresent() {
		//		printf("ISPRES? %s %s%n", !(this instanceof RunResultError), baronJob.expr);

		return !(this instanceof RunResultError);
	}

	public static int someSolutionFound(UList<String> resout) {
		return resout.findFirstIndex(s -> s.startsWith("The best solution found is:"));
	}

	public static boolean heuristicCompletion(UList<String> output) {
		return output.some(s -> s.contains("Heuristic termination"));
	}

	public static boolean resultInfeasible(UList<String> output) {
		return output.some(s -> s.contains("Problem is infeasible"));
	}

	public RunResult(BaronJob baronJob, UList<String> output) {
		this.baronJob = baronJob;

		this.max_time_exceeded = output.some(s -> s.contains("Max. allowable time exceeded"));
		//		Optional<String> preproc_val = output
		//				.findFirst(s -> s.contains("Preprocessing found feasible solution with value"));
		this.infeasible = resultInfeasible(output);

		this.heuristic_completion = heuristicCompletion(output);
	}

}
