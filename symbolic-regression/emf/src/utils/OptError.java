// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.System_err;
import static utils.VRAUtils.System_out;
import static utils.VRAUtils.newArrayList;
import static utils.VRAUtils.newLinkedHashSet;
import static utils.VRAUtils.newlist;
import static utils.VRAUtils.printf;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * These are some haphazardly-written classes to try to make errors and warnings a little clearer.
 * Two things:
 *    we print to stdout/stderr and to a logging file
 *    we save messages in order to reprint them at the end of the run, so they don't get overlooked as easily (hopefully)
 */
@SuppressWarnings("serial")
public class OptError extends Exception {
	/** This is the string that is printed on stdout.  If this exception causes opti to fail, it will be put in the DB. */
	public final String errMsg;

	public OptError(String errMsg) {
		this.errMsg = errMsg;
	}

	public static class OptErrorState {
		private ArrayList<String> inputErrors = newArrayList();
		private LinkedHashSet<String> inputErrorFmts = newLinkedHashSet();
	}
	
	// the formats summarize the errors in a way
//	public static String inputErrors() {
//		StringBuilder sb = new StringBuilder();
//		for (String s : gx.opterrorstate.inputErrors)
//			sb.append(s).append("\n");
//		return sb.toString();
//	}

	/** There are many assertions in the code, but an inputError is something that
	 * is officially recognized as a minimal requirement of the input for opti to run.
	 * There may in fact be other, more difficult to diagnose input issues that cause failure,
	 * but we try to detect problems as soon as we can to avoid confusion.
	 * 
	 * This is an older interface that should be replaced by inputError1
	 * */
	public static void inputError(String fmt, Object... s) {
		inputError2("INPUT ERROR: ", fmt, s);
	}

	// this will eventually replace the one above
	public static void inputError1(InputErrorCode code, String fmt, Object... s) {
		inputError2("INPUT ERROR " + code + ": ", fmt, s); 
	}

	// almost identical - nuts
	public static void inputError1(MalformedInputErrorCode code, String fmt, Object... s) {
		inputError2("MALFORMED INPUT ERROR " + code + ": ", fmt, s); 
	}

	
	private static void inputError2(String errKind, String fmt, Object... s) {
		String str = errKind + String.format(fmt, s);
//			gx.opterrorstate.inputErrors.add(str);
//			gx.opterrorstate.inputErrorFmts.add(fmt);
			System_out.printf(fmt, s);
			System_out.println();
	}

	
	/** not doing anything with the code yet 
	 * @throws InputError */
	public static void inputError(InputErrorCode code, String fmt, Object... s) throws InputError {
		if (nonFatalErrors.contains(code))
			inputError1(code, fmt, s);
		else
			throw new InputError(code, fmt, s);
	};

	public static void malformedInputError(MalformedInputErrorCode code, String fmt, Object... s) {
		if (nonFatalErrors.contains(code))
			inputError1(code, fmt, s);
		else
			throw new MalformedInput(code, fmt, s);
	};

	public static final List<InputErrorCode> nonFatalErrors = newlist();

//	public static List<String> inputWarnings() {
//		return unmodifiableList(inputWarnings);
//	}
//
//	private static ArrayList<String> inputWarnings = newArrayList();
//	private static LinkedHashSet<String> inputWarningFmts = newLinkedHashSet();

	/** opti will run even in the presence of a warning, but
	 *    the result may have poor quality
	 *    the warnings may lead to failure.
	 *    
	 *  I don't claim that I've put a lot of thought into classifying messages as warnings.
	 *  Some may not actually be important, and many have been there since the beginning of time.
	 *  Ignore at your own risk.
	 * */
	public static void inputWarning(String fmt, Object... s) {
		String str = "INPUT WARNING: " + String.format(fmt, s);
		System_out.printf(fmt, s);
		System_out.println();
	}

	/**
	 * As we discover new issues with the input, I like to add checks, but can't enforce them right away
	 * until the data has been cleaned up. 
	 * In the meantime, I use this shorthand to allow me to selectively permit or disallow things.
	 * */
	public static void inputError(boolean ignorable, String fmt, Object... s) throws InputError {
		if (ignorable)
			throw new SkipInput(fmt, s);
		else
			throw new InputError(fmt, s);
	}

	public static void inputErrorNoThrow(boolean ignorable, String fmt, Object... s) throws InputError {
		if (ignorable)
			inputWarning(fmt, s);
		else
			throw new InputError(fmt, s);
	}

	@SuppressWarnings("serial")
	public static class OptInfeasible extends OptError {
		public OptInfeasible(String fmt, Object... s) {
			super(String.format(fmt, s));
			printf("The problem is infeasible: ");
			printf(fmt, s);
		}
	};

	// THIS SHOULD NEVER HAPPEN AND WE NEVER TRY TO RECOVER FROM SUCH AN ERROR.  So we don't even treat it as an exception.
	// referential integrity
	// null-string ids
	@SuppressWarnings("serial")
	public static class MalformedInput extends RuntimeException {
		public MalformedInput(String fmt, Object... s) {
			inputError(fmt, s);
		}

		public MalformedInput(MalformedInputErrorCode code, String fmt, Object... s) {
			inputError1(code, fmt, s);
		}
	};

	@SuppressWarnings("serial")
	public static class InputError extends OptError {
		public InputError(String fmt, Object... s) {
			super(String.format(fmt, s));			
			inputError(fmt, s);
		}

		public InputError(InputErrorCode code, String fmt, Object... s) {
			super(String.format(fmt, s));						
			inputError1(code, fmt, s);
			System_out.flush();
			System_err.flush();
		}
	};

	@SuppressWarnings("serial")
	public static class SkipInput extends InputError {
		public SkipInput(String fmt, Object... s) {
			super(fmt, s);
			//inputError(fmt, s);
		}
	};

	// there is a built-in Java class called InternalError...
	@SuppressWarnings("serial")
	public static class OptInternalError extends RuntimeException {
		public OptInternalError(String fmt, Object... s) {
			System_out.printf(fmt, s);
			System_out.println();
		}
	}

	// a hack for a debugging feature.
	@SuppressWarnings("serial")
	public static class OptReplayReturn extends RuntimeException {
		public OptReplayReturn() {
			System_out.printf("End of replay%n");
		}
	}
}
