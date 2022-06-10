// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static java.lang.Integer.*;
import static java.util.Collections.*;
import static utils.UList.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VRAUtils {
	public static int compareInts(int x, int y) {
		return x < y ? -1 : x == y ? 0 : 1;
	}

	public final static Comparator<Integer> compareInts = (x, y) -> x < y ? -1 : x == y ? 0 : 1;

	public static int compareDoubles(double x, double y) {
		return x < y ? -1 : x == y ? 0 : 1;
	}
	// public static int chainCompare(int x, int y) { return x==0 ? y : x; }

	// public static int chainCompare(String x0, String x1, int y) {
	// int n = x0.compareTo(x1);
	// return n==0 ? y : n;
	// }
	public static int chainCompare(int x, int y) {
		return x < y ? -1 : x == y ? 0 : 1;
	}

	public static int chainCompare(double x, double y) {
		return x < y ? -1 : x == y ? 0 : 1;
	}

	//	public static <T extends Comparable<T>> int chainCompare(T x0, T x1, int y) {
	//		int n = x0.compareTo(x1);
	//		return n == 0 ? y : n;
	//	}
	// hack - Optional doesn't have a compare
	public static <T extends Comparable<T>, T2 extends Comparable<T2>> int chainCompare(Optional<T> x0, Optional<T> x1,
			T2 y0, T2 y1) {
		if (x0.isPresent() && !x1.isPresent())
			return -1;
		if (!x0.isPresent() && x1.isPresent())
			return 1;
		int n = x0.get().compareTo(x1.get());
		return n == 0 ? chainCompare(y0, y1) : n;
	}

	public static <T extends Comparable<T>, T2 extends Comparable<T2>> int chainCompare(T x0, T x1, T2 y0, T2 y1) {
		int n = x0.compareTo(x1);
		return n == 0 ? chainCompare(y0, y1) : n;
	}

	public static <T extends Comparable<T>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> int chainCompare(T x0,
			T x1, T2 y0, T2 y1, T3 z0, T3 z1) {
		int n = x0.compareTo(x1);
		return n == 0 ? chainCompare(y0, y1, z0, z1) : n;
	}

	public static <T2 extends Comparable<T2>> int chainCompare(int x0, int x1, T2 y0, T2 y1) {
		int n = chainCompare(x0, x1);
		return n == 0 ? chainCompare(y0, y1) : n;
	}

	public static <T extends Comparable<T>> int chainCompare(T x0, T x1) {
		return x0.compareTo(x1);
	}

	public static <T extends Comparable<T>, T2 extends Comparable<T2>> int chainCompare(UList<T> x0, UList<T> x1, T2 y0,
			T2 y1) {
		int n = chainCompare(x0, x1);
		return n == 0 ? chainCompare(y0, y1) : n;
	}

	public static <T extends Comparable<T>, T2 extends Comparable<T2>> int chainCompare(T2 x0, T2 x1, UList<T> y0,
			UList<T> y1) {
		int n = chainCompare(x0, x1);
		return n == 0 ? chainCompare(y0, y1) : n;
	}

	public static <T extends Comparable<T>> int chainCompare(UList<T> x0ul, UList<T> x1ul) {
		List<T> x0 = x0ul.vals();
		List<T> x1 = x1ul.vals();

		int i = 0;
		int n = Math.min(x0.size(), x1.size());
		for (; i < n; ++i) {
			int cmp = x0.get(i).compareTo(x1.get(i));
			if (cmp != 0)
				return cmp;
		}
		if (x0.size() == x1.size())
			return 0;
		if (x0.size() < x1.size())
			return -1;
		return 1;
	}
	//	public static <T extends Comparable<T>> int chainCompare(List<T> x0, List<T> x1) {
	//		int i = 0;
	//		int n = Math.min(x0.size(), x1.size());
	//		for (; i < n; ++i) {
	//			int cmp = x0.get(i).compareTo(x1.get(i));
	//			if (cmp != 0)
	//				return cmp;
	//		}
	//		if (x0.size() == x1.size())
	//			return 0;
	//		if (x0.size() < x1.size())
	//			return -1;
	//		return 1;
	//	}

	//	public static <T extends Comparable<T>> int chainCompare(List<T> x0, List<T> x1, int y) {
	//		int cmp = chainCompare(x0, x1);
	//		if (cmp == 0)
	//			return y;
	//		return cmp;
	//	}

	// All comes last
	// need '? super TT' to use GSvar, base class of other classes that use its comparison method
	public static <TT extends Comparable<? super TT>> int chainCompareOS(OneSet<TT> x, OneSet<TT> y) {
		return x.isAll ? (y.isAll ? 0 : 1) : (y.isAll ? -1 : x.item.compareTo(y.item));
	}

	public static <TT extends Comparable<? super TT>> int chainCompareOS(OneSet<TT> x0, OneSet<TT> x1, int y) {
		int cmp = chainCompareOS(x0, x1);
		if (cmp == 0)
			return y;
		return cmp;
	}

	public static int compareBoolLess(boolean x, boolean y) {
		return !x && y ? -1 : x == y ? 0 : 1;
	}

	public static int compareStrings(String x, String y) {
		return x.compareTo(y);
	}

	public static String file_separator = System.getProperty("file.separator");

	// capture all output to a file in a production run.
	// note that is should always be ok to uses them, they are never null, but they
	// are sometimes closed and re-opened.
	// output will continue to be written until the files are redirected; they
	// aren't closed at the end of a run.
	public static PrintStream System_out = System.out;
	public static PrintStream System_err = System.err;

	public static class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
		}
	}

	public static final PrintStream nullout = new PrintStream(new NullOutputStream());

	public static final PrintStream dbgout = System.getenv().containsKey("DBGOUT") ? System.out
			//: OutputStream.nullOuputStream();
			: nullout;

	public static void badData(String fmt, Object... s) {
		die(fmt, s);
	}

	public static void badData(boolean fixup, String fmt, Object... s) {
		if (fixup) {
			printf(fmt, s);
			printf("\n");
		} else
			die(fmt, s);
	}

	public static boolean die_bool(String fmt, Object... s) {
		die(fmt, s);
		return false;
	}

	public static <T> T die_null(String fmt, Object... s) {
		die(fmt, s);
		return null;
	}

	public static void die(Object... x) {
		die("%s%n", x);
	}

	public static void dumpdie(Object... x) {
		println();
		dump(x);
		die();
	}

	public static void die() {
		println();
		println();
		System_out.flush();
		System_err.flush();

		die("AAAAARRRRRGGGGGHHHH...%n");
	}

	public static void die(String fmt, Object... s) {
		System_out.flush();
		System_err.flush();
		System_out.println();
		System_out.printf(fmt, s);
		System_out.println();
		System_out.flush();
		throw new Error();
	}

	public static void println() {
		System_out.println();
	}

	public static void println(Object x) {
		System_out.println(x.toString());
	}

	public static void println(String s) {
		System_out.println(s);
	}

	public static void printf(String fmt, Object... s) {
		System_out.printf(fmt, s);
		// System_out.flush();
	}

	private static GrowList<String> printed_fmts = new GrowList<>();

	public static void printf_once(String fmt, Object... s) {
		if (printed_fmts.elems().contains(fmt))
			return;
		printed_fmts.add(fmt);
		printf(fmt, s);
	}

	public static void write(Object... objs) {
		for (Object obj : objs)
			System_out.print(obj.toString());
	}

	public static void writeln(Object... objs) {
		for (Object obj : objs)
			System_out.print(obj.toString());
		System_out.println();
	}

	public static void notnow(boolean cond) {
		myassert(cond, "");
	}

	public static void notnow(boolean cond, Object... xs) {
		if (!cond)
			failedassert(xs);
	}

	public static void myassert(boolean cond) {
		myassert(cond, "");
	}

	public static void myassertEqualToTol(double x, double y, double tol, Object... xs) {
		if (!equalToTol(x, y, tol)) {
			dump(x, y, tol);
			dumpdie(xs);
		}
	}

	public static <T> void myassertSubset(UList<T> x, UList<T> y) {
		if (!x.subset(y))
			die("NOT SUBSET: %s %s %s%n", x.diff(y), x, y);
	}

	public static <T> void myassertEqual(String msg, T x, T y) {
		if (!x.equals(y))
			die("NOT EQUAL %s: >%s< >%s<%n", msg, x, y);
	}

	public static void myassertTolEqual(String msg, double x, double y, double tol, Object... dumps) {
		if (Math.abs(x - y) > tol) {
			dumpv(dumps);
			die("NOT EQUAL %s: %s %s (%s)%n", msg, x, y, tol);
		}
	}

	//public static <T extends Comparable<? super T>> void myassertSetEqual(String msg, UList<T> xs, UList<T> ys,
	public static <T extends Comparable<? super T>, TL extends UList<T>> void myassertSetEqual(String msg, TL xs, TL ys,
			Object... dumps) {
		myassertEqual(msg, sortDistinct(xs), sortDistinct(ys), dumps);
	}

	public static <T> void myassertEqual(T x, T y, Object... dumps) {
		if (x.equals(y))
			return;
		printf("%n%n>>> myassertEqual fails:%n");
		printf("%s%n", x);
		printf("%s%n", y);
		dumpv(dumps);
		die();
	}

	public static void myassertLE(double x, double y, Object... dumps) {
		if (x <= y)
			return;
		printf("%n%n>>> myassertLE fails:%n");
		printf("%s%n", x);
		printf("%s%n", y);
		dumpv(dumps);
		die();
	}

	public static void myassertGE(double x, double y, Object... dumps) {
		if (x >= y)
			return;
		printf("%n%n>>> myassertLE fails:%n");
		printf("%s%n", x);
		printf("%s%n", y);
		dumpv(dumps);
		die();
	}

	public static void myassertLE_GE(boolean le, double x, double y, Object... dumps) {
		if (le ? x <= y : x >= y)
			return;
		printf("%n%n>>> myassertLE_GE fails:%n");
		printf("%s%n", x);
		printf("%s%n", y);
		dumpv(dumps);
		die();
	}

	public static <T, TL extends UList<T>> void myassertEqual(String msg, TL xs, TL ys, Object... dumps) {
		//	public static <T> void myassertEqual(String msg, UList<T> xs, UList<T> ys, Object... dumps) {
		if (xs.size() != ys.size())
			printf("DIFF SIZES: %s %s%n", xs.size(), ys.size());
		int n = Math.min(xs.size(), ys.size());
		int ndiffs = 0;
		for (int i = 0; i < n; ++i)
			if (!xs.get(i).equals(ys.get(i))) {
				printf("DIFF %s: %s %s%n", i, xs.get(i), ys.get(i));
				if (ndiffs++ > 10)
					break;
			}
		if (ndiffs > 0 || xs.size() != ys.size()) {
			dumpv(dumps);
			die("myassertEqual fails for " + msg);
		}
	}

	public static void experimentalAssert(boolean cond) {
		// if (OptiDebugOptsNoSave.experimentalAssertions)
		myassert(cond, "");
	}

	private static Object dovec(Object x) {
		if (x instanceof double[])
			return mkUList((double[]) x);
		else if (x instanceof int[])
			return mkUList((int[]) x);
		else
			return x;
	}

	public static void FORNOW(boolean cond, Object... xs) { // was: ObjId
		if (!cond) {
			// maybe call 'dump' via reflection?
			// x.dump(System_out, "%nFAILED MYASSERT ");
			// doesn't help, need a stack trace
			System_out.flush();
			System_err.flush();
			for (Object x : xs)
				printf(" %s\n", dovec(x));
			printf("%n");
			throw new RuntimeException();
		}
	}

	public static void emergency(String fmt, Object... args) {
		System_out.println("\nEMERGENCY ACTION: " + String.format(fmt, args));
	}

	public static void FORNOW(boolean cond, String fmt, Object... args) {
		if (!cond) {
			System_out.println("\nFAILED FORNOW ASSERT: " + String.format(fmt, args));
			throw new RuntimeException();
		}
	}

	public static void myassert(boolean cond, String fmt) {
		if (!cond)
			failedassert(fmt);
	}

	public static void myassert(boolean cond, Object... fmt) {
		if (!cond)
			failedassert(fmt);
	}

	public static void myassert(boolean cond, String fmt, Object... args) {
		if (!cond)
			failedassert(fmt, args);
	}

	private static void failedassert(String fmt, Object... args) {
		System_out.flush();
		System_out.flush();
		System_out.println("\nFAILED MYASSERT: " + String.format(fmt, args));
		System_out.println();
		System_out.flush();
		throw new RuntimeException();
	}

	private static void failedassert(Object... xs) {
		System_out.flush();
		System_out.flush();
		System_out.println("\nFAILED MYASSERT: ");
		for (Object x : xs)
			printf(" %s\n", x);
		printf("%n");
		System_out.flush();
		throw new RuntimeException();
	}

	public static void checkassert(boolean cond, String fmt, Object... args) {
		if (!cond) {
			System_out.println("\nFAILED MYASSERT: " + String.format(fmt, args));
			System_out.println();
			System_out.flush();
			//			throw new RuntimeException();
		}
	}

	//	public static class ManageError {
	//	}
	//
	//	public static ArrayList<String> manageErrors = newArrayList();
	//
	//	public static void checkassert(boolean cond, String fmt, Object... args) {
	//		if (!cond) {
	//			manageErrors.add(String.format(fmt, args));
	//		}
	//	}

	public static void done() {
		done("");
	}

	public static void done(String msg) {
		System_out.println("// DONE " + msg);
		System.exit(0);
	}

	public static class GetId<T> {
		String getId(T x) {
			return "";
		}
	}

	public static <T> LinkedHashMap<String, T> getById(T[] xs, GetId<T> getid) {
		LinkedHashMap<String, T> retval = newLinkedHashMap();
		for (T x : xs)
			retval.put(getid.getId(x), x);
		return retval;
	}

	public static <T> boolean contains(Collection<T> xs, T x) {
		return xs != null && xs.contains(x);
	}

	// ///
	public static <T> T getLast(List<T> xs) {
		return xs.get(xs.size() - 1);
	}

	public static <T> T last(List<T> xs) {
		return xs.get(xs.size() - 1);
	}

	public static <T> T first(List<T> xs) {
		return xs.get(0);
	}

	public static <T> T oneof(Collection<T> xs) {
		return xs.iterator().next();
	}

	public static <T> boolean setcontains(LinkedHashSet<T> set, T x) {
		return set != null && set.contains(x);
	}

	public static double sum(Collection<Double> xs) {
		double sm = 0;
		for (Double x : xs)
			sm += x;
		return sm;
	}

	public static int sumi(Collection<Integer> xs) {
		int sm = 0;
		for (Integer x : xs)
			sm += x;
		return sm;
	}

	// public static <S,T,TS extends Collection<T>> boolean addMap(Map<S, TS> map, S
	// x, T y, TS empty) {
	// TS ys = map.get(x);
	// if (ys == null)
	// map.put(x, ys = empty.
	// return ys.add(y);
	// }

	// can't make this generic, since it creates new objects.
	public static <S, T> boolean addMapSet(Map<S, Set<T>> map, S x, T y) {
		Set<T> ys = map.get(x);
		if (ys == null)
			map.put(x, ys = new LinkedHashSet<T>());
		return ys.add(y);
	}

	public static <S, T> boolean addMapList(Map<S, List<T>> map, S x, T y) {
		List<T> ys = map.get(x);
		if (ys == null)
			map.put(x, ys = new ArrayList<T>());
		return ys.add(y);
	}

	public static <S, T> boolean addMapArrayList(Map<S, ArrayList<T>> map, S x, T y) {
		ArrayList<T> ys = map.get(x);
		if (ys == null)
			map.put(x, ys = new ArrayList<T>());
		return ys.add(y);
	}

	public static <S, T, U> U addMapMap(Map<S, Map<T, U>> map, S x, T y, U u) {
		Map<T, U> ys = map.get(x);
		if (ys == null)
			map.put(x, ys = new LinkedHashMap<T, U>());
		return ys.put(y, u);
	}

	public static <S, T> void addMapOnce(Map<S, T> map, S x, T y) {
		T prev = map.put(x, y);
		if (prev != null) {
			printf("FAILED ASSERTION in addMapOnce: %s %s %s\n", x, y, prev);
			myassert(false);
		}
	}

	// public static <S, T> void addMapOnce(Map<S, T> map, S x, T y, String loc) {
	// T prev = map.put(x, y);
	// if (prev != null)
	// malformedInputError(MalformedInputErrorCode.BAD_MAP, "a map has two values
	// for %s: %s AND %s (%s)", x,
	// prev, y, loc);
	// }

	public static <S, T> void addMapOnceOrEquals(Map<S, T> map, S x, T y) {
		T prev = map.put(x, y);
		assert prev == null || prev.equals(y);
	}

	public static <T> void addSetOnce(Set<T> set, T x) {
		boolean isnew = set.add(x);
		if (!isnew) {
			printf("NOW NEW: %s\n", x);
			assert isnew;
		}
	}

	public static <S> void addMapDouble(Map<S, Double> map, S x, double y) {
		Double ys = map.get(x);
		if (ys == null)
			map.put(x, y);
		else
			map.put(x, ys + y);
	}

	public static <S> void addMapInt(Map<S, Integer> map, S x, int y) {
		Integer ys = map.get(x);
		if (ys == null)
			map.put(x, y);
		else
			map.put(x, ys + y);
	}

	public static <S> void addMapMin(Map<S, Integer> map, S x, int y) {
		Integer ys = map.get(x);
		if (ys == null)
			map.put(x, y);
		else
			map.put(x, Math.min(ys, y));
	}

	public static <S> void addMapMax(Map<S, Integer> map, S x, int y) {
		Integer ys = map.get(x);
		if (ys == null)
			map.put(x, y);
		else
			map.put(x, Math.max(ys, y));
	}

	public static <S> void addMapMax(Map<S, Double> map, S x, double y) {
		Double ys = map.get(x);
		if (ys == null)
			map.put(x, y);
		else
			map.put(x, Math.max(ys, y));
	}

	public static <S, T> void addMapMultiple(Map<S, T> map, S x, T y) {
		T prev = map.put(x, y);
		assert prev == null || prev.equals(y);
	}

	public static <S, T> LinkedHashSet<T> getMapSet(LinkedHashMap<S, LinkedHashSet<T>> map, S x) {
		LinkedHashSet<T> ys = map.get(x);
		if (ys == null)
			map.put(x, ys = new LinkedHashSet<T>());
		return ys;
	}

	public static <T> T nonNull(T x) {
		if (x == null)
			die("THIS WAS NOT SUPPOSED TO BE NULL: " + x);
		return x;
	}

	public static <T> T nonNull(T x, Object msg) {
		if (x == null)
			die("%s%nTHIS WAS NOT SUPPOSED TO BE NULL: %s", msg, x);
		return x;
	}

	public static <T> T nonNull(T x, SSLine ssl) {
		if (x == null)
			die("%nTHIS WAS NOT SUPPOSED TO BE NULL: %s  (on row %s)", x, ssl);
		return x;
	}

	public static <S, T> T getNonNull(Map<S, T> map, S x) {
		T y = map.get(x);
		if (y == null) {
			// printf("null value for x: %s%n", x.toString());
			// System_out.flush();
			die("map null value for x: %s%n", x.toString());
		}
		return y;
	}

	public static <S> double getMapDouble(Map<S, Double> map, S x) {
		Double y = map.get(x);
		return y == null ? 0.0 : y;
	}

	public static <S> int getMapInt(Map<S, Integer> map, S x) {
		Integer y = map.get(x);
		return y == null ? 0 : y;
	}

	// public static <K> SingleInit<K> newSingleInit() {
	// return new SingleInit<K>();
	// }
	// public static <K> SingleInitList<K> newSingleInitList() {
	// return new SingleInitList<K>();
	// }
	// public static <K> SingleInitSet<K> newSingleInitSet() {
	// return new SingleInitSet<K>();
	// }
	// public static <K,T> SingleInitMap<K,T> newSingleInitMap() {
	// return new SingleInitMap<K,T>();
	// }

	public static class NonNullArrayList<K> extends ArrayList<K> {
		@Override
		public boolean add(K x) {
			return super.add(assertNonNull(x));
		}
	};

	public static <K> ArrayList<K> newNullableArrayList() {
		// return new ArrayList<K>();
		return new ArrayList<K>();
	}

	public static <K> ArrayList<K> newArrayList() {
		// return new ArrayList<K>();
		return new NonNullArrayList<K>();
	}

	public static <K, T> Pair<K, T> newPair(K x, T y) {
		return new Pair<K, T>(x, y);
	}

	// public static <K,T,W> Triple<K,T,W> newTriple(K x, T y, W z) {
	// return new Triple<K,T,W>(x,y,z);
	// }
	public static <K> Collection<K> newCCollection(Collection<K> l) {
		return Collections.unmodifiableCollection(l);
	}

	public static <K> List<K> newCList(List<K> l) {
		return Collections.unmodifiableList(l);
	}

	public static <T> boolean checkDistinct(T[] vals) {
		return vals.length == new LinkedHashSet<T>(Arrays.asList(vals)).size();
	}

	public static <T> void assertDistinct(Collection<T> vals) {
		assert vals.size() == new LinkedHashSet<T>(vals).size();
	}

	public static <T> LinkedHashSet<T> newLinkedHashSet(T[] vals) {
		return new LinkedHashSet<T>(Arrays.asList(vals));
	}

	public static <T> LinkedHashSet<T> allocLinkedHashSet(LinkedHashSet<T> vs) {
		assert vs == null;
		return new LinkedHashSet<T>();
	}

	// public static <K0,K1,T> TupleMap2<K0,K1,T> newRecStore2() {
	// return new utils.TupleMapL.TupleMap2<K0,K1,T>(null);
	// }
	// public static <K0,K1,T> TupleMap2<K0,K1,T> newRecStore2(Class<T> cls) {
	// return new utils.TupleMapL.TupleMap2<K0,K1,T>(cls);
	// }
	// public static <K0,K1,K2,T> TupleMap3<K0,K1,K2,T> newRecStore3() {
	// return new utils.TupleMapL.TupleMap3<K0,K1,K2,T>();
	// }
	// public static <K0,K1,K2,T> TupleMap3<K0,K1,K2,T> newRecStore3(Class<T> cls) {
	// return new utils.TupleMapL.TupleMap3<K0,K1,K2,T>(cls);
	// }
	//
	// public static <K0,T> TupleMap<K0,T> newRecStore(Class<T> cls) {
	// return new TupleMap<K0,T>(cls);
	// }
	// public static <K0> DoubleStore<K0> newDoubleStore() {
	// return new DoubleStore<K0>();
	// }
	// public static <K0,K1,T> TupleMap2Set<K0,K1,T> newRecStore2Set() {
	// return new utils.TupleMapL.TupleMap2Set<K0,K1,T>();
	// }
	// public static <K0,K1,T> TupleMap2List<K0,K1,T> newRecStore2List() {
	// return new utils.TupleMapL.TupleMap2List<K0,K1,T>();
	// }
	//
	// public static <K0,K1,K,T> TupleMap2Map<K0,K1,K,T> newRecStore2Map() {
	// return new utils.TupleMapL.TupleMap2Map<K0,K1,K,T>() {
	// public Map<K,T> newInstance() {
	// return new LinkedHashMap<K,T>();
	// }
	// };
	// }
	// public static <K0,K1,K2 , K,T> TupleMap3Map<K0,K1,K2,K,T> newRecStore3Map() {
	// return new utils.TupleMapL.TupleMap3Map<K0,K1,K2,K,T>() {
	// public Map<K,T> newInstance() {
	// return new LinkedHashMap<K,T>();
	// }
	// };
	// }
	//
	// public static <K0,K1,K2,T> TupleMap3Set<K0,K1,K2,T> newRecStore3Set() {
	// return new utils.TupleMapL.TupleMap3Set<K0,K1,K2,T>();
	// }

	public static <T extends Comparable<? super T>> ArrayList<T> sortedSet(Collection<T> xs) {
		ArrayList<T> idlst = new ArrayList<T>(xs);
		Collections.sort(idlst);
		return idlst;
	}

	// nuts
	public static <T extends Comparable<? super T>> ArrayList<List<T>> sortedSetL(Collection<List<T>> xs) {
		ArrayList<List<T>> idlst = new ArrayList<List<T>>(xs);
		Collections.sort(idlst, new Comparator<List<T>>() {

			@Override
			public int compare(List<T> o1, List<T> o2) {
				int i = 0;
				while (i < o1.size() && i < o2.size()) {
					int x = o1.get(i).compareTo(o2.get(i));
					if (x != 0)
						return x;
					i++;
				}
				if (o1.size() == o2.size())
					return 0;
				if (o1.size() < o2.size())
					return -1;
				return 1;
			}
		});
		return idlst;
	}

	public static <T0, T extends T0> ArrayList<T> sortedSet(Collection<T> xs, Comparator<T0> comp) {
		ArrayList<T> idlst = new ArrayList<T>(xs);
		Collections.sort(idlst, comp);
		return idlst;
	}

	public static <T> ArrayList<T> reverseList(List<T> xs) {
		ArrayList<T> idlst = new ArrayList<T>(xs);
		Collections.reverse(idlst);
		return idlst;
	}

	// public static <T extends Comparable<? super T>> ArrayList<T>
	// sort(Collection<T> xs) {
	// ArrayList<T> sxs = newArrayList();
	// sxs.addAll(xs);
	// Collections.sort(sxs);
	// return sxs;
	// }

	public static <T> LinkedHashSet<String> getIdSet(T[] xs, GetId<T> getid) {
		LinkedHashSet<String> ids = newLinkedHashSet();
		for (T x : xs)
			ids.add(getid.getId(x));
		return ids;
	}
	//
	//	public static <T> LinkedHashSet<T> intersection(Collection<T> s1, Collection<T> s2) {
	//		LinkedHashSet<T> inter = new LinkedHashSet<T>(s1);
	//		inter.retainAll(s2);
	//		return inter;
	//	}
	//
	//	public static <T, TSet extends Iterable<T>> LinkedHashSet<T> unionAll(Collection<TSet> s) {
	//		LinkedHashSet<T> union = new LinkedHashSet<T>();
	//		for (TSet ts : s)
	//			for (T t : ts)
	//				union.add(t);
	//		return union;
	//	}
	//
	//	public static <T> LinkedHashSet<T> union(Collection<T> s1, Collection<T> s2) {
	//		LinkedHashSet<T> union = new LinkedHashSet<T>(s1);
	//		union.addAll(s2);
	//		return union;
	//	}
	//
	//	public static <T> LinkedHashSet<T> diff(Collection<T> s1, Collection<T> s2) {
	//		LinkedHashSet<T> diff = new LinkedHashSet<T>(s1);
	//		diff.removeAll(s2);
	//		return diff;
	//	}
	//
	//	public static <T> ArrayList<T> concat(Collection<T> s1, Collection<T> s2) {
	//		ArrayList<T> union = new ArrayList<T>(s1);
	//		union.addAll(s2);
	//		return union;
	//	}
	//

	//
	//	public static <T> ArrayList<T> cons(T x, Collection<T> s2) {
	//		ArrayList<T> union = new ArrayList<T>();
	//		union.add(x);
	//		union.addAll(s2);
	//		return union;
	//	}

	public static String join_with_sep(String sep, String... ss) {
		// return join(ss, sep);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String x : ss) {
			if (first)
				first = false;
			else
				sb.append(sep);
			sb.append(x);
		}
		return sb.toString();
	}

	public static <T> String join(Collection<T> s, String sep) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (T x : s) {
			if (first)
				first = false;
			else
				sb.append(sep);
			sb.append(x);
		}
		return sb.toString();
	}

	public static boolean initMsg(String s) {
		printf("%s%n", s);
		return true;
	}

	public static <T> boolean intersects(Collection<T> s1, Collection<T> s2) {
		for (T x : s1)
			if (s2.contains(x)) {
				return true;
			}
		return false;
	}

	public static <T> boolean intersects(Set<T> s1, Set<T> s2) {
		for (T x : s1)
			if (s2.contains(x)) {
				return true;
			}
		return false;
	}

	public static <T> boolean intersects(UList<T> s1, UList<T> s2) {
		for (T x : s1)
			if (s2.contains(x)) {
				return true;
			}
		return false;
	}

	// avoids the cost of creating the hash set. I assume that it is faster if s2 is
	// a hash set. should test.

	//
	//
	//	public static <T> boolean completeSubset(Collection<T> s1, Collection<T> s2) {
	//		return setEqual(s1, intersection(s1, s2));
	//	}
	//
	//	public static <T> boolean setEqual(Collection<T> s1, Collection<T> s2) {
	//		return new HashSet<T>(s1).equals(new HashSet<T>(s2));
	//	}
	//
	//	public static <T> boolean setEqual(Set<T> s1, Collection<T> s2) {
	//		return s1.equals(new HashSet<T>(s2));
	//	}
	//
	//	public static <T> boolean setEqual(Collection<T> s1, Set<T> s2) {
	//		return new HashSet<T>(s1).equals(s2);
	//	}
	//
	//	public static <T> boolean setEqual(Set<T> s1, Set<T> s2) {
	//		return s2.equals(s1);
	//	}
	//
	//	public static <K, V> LinkedHashMap<K, List<V>> newLinkedHashMapList(Collection<K> dom) {
	//		LinkedHashMap<K, List<V>> rval = new LinkedHashMap<K, List<V>>();
	//		for (K k : dom)
	//			rval.put(k, new ArrayList<V>());
	//		return rval;
	//	}
	//
	//	public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(Collection<K> dom, V init) {
	//		LinkedHashMap<K, V> rval = newLinkedHashMap();
	//		for (K k : dom)
	//			rval.put(k, init);
	//		return rval;
	//	}

	// copied (with minor change)
	public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
		// return Collections.synchronizedMap(new LinkedHashMap<K,V>());
		// return new ConcurrentLinkedHashMap<K,V>();
		return new LinkedHashMap<K, V>();
	}

	public static class CheckedMap<K, T> extends LinkedHashMap<K, T> {
		// private final ObjId objid;
		private final String objid;

		// CheckedMap(ObjId x) {
		CheckedMap(String x) {
			this.objid = x;
		}

		@Override
		public T get(Object key) {
			T x = super.get(key);
			if (x == null) {
				printf("NO VALUE FOR KEY %s%n", key);
				dumpit(objid);
				assert false;
			}
			return x;
		}
	};

	// public static <K, V> CheckedMap<K, V> newCheckedMap(ObjId x) {
	public static <K, V> CheckedMap<K, V> newCheckedMap(String x) {
		// return Collections.synchronizedMap(new LinkedHashMap<K,V>());
		// return new ConcurrentLinkedHashMap<K,V>();
		return new CheckedMap<K, V>(x);
	}

	public static void dump(Object... xs) {
		dumpv(xs);
	}

	public static void dumpv(Object[] xs) {
		for (Object x : xs)
			printf(" %s%n", dovec(x));
		printf("%n");
	}

	public static void dumpit(Object x) {
		PrintStream out = System_out;

		for (Method m : x.getClass().getDeclaredMethods())
			if (m.getName().equals("dump") && m.getParameterTypes()[0] == out.getClass()) {
				try {
					printf("method: %s%n", mkUList(m.getParameterTypes()));
					Object[] xs = new Object[0];
					m.invoke(x, out, "", xs);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
		die("no dump method: %s", x);
	}

	public static <K> LinkedHashSet<K> newLinkedHashSet() {
		return new LinkedHashSet<K>();
	}

	public static void dataError(String fmt, Object... s) {
		// do something with this eventually
	}

	public static boolean equalToTol(double x, double y, double tol) {
		double min = Math.min(x, y);
		double max = Math.max(x, y);
		return max - min < tol;
	}

	public static <T> ArrayList<T> newlist(T... xs) {
		ArrayList<T> l = new ArrayList<T>();
		for (T x : xs)
			l.add(x);
		return l;
	}

	//	public static <T> List<T> toList(Collection<T> xs) {
	//		ArrayList<T> l = new ArrayList<T>();
	//		for (T x : xs)
	//			l.add(x);
	//		return l;
	//	}

	//	public static <T> List<T> toList(T[] xs) {
	//		ArrayList<T> l = new ArrayList<T>();
	//		for (T x : xs)
	//			l.add(x);
	//		return l;
	//	}

	public static <T> Set<T> toSet(Collection<T> xs) {
		Set<T> l = newLinkedHashSet();
		for (T x : xs)
			l.add(x);
		return l;
	}

	// public static class PairIterator implements Iterable<Pair<T,T>> {
	public static class PairIterator<T> implements Iterator<Pair<T, T>> {
		final Collection<T> xs;
		final Iterator<T> iter;
		T x;

		PairIterator(Collection<T> xs) {
			this.xs = xs;
			this.iter = xs.iterator();
			if (iter.hasNext()) {
				x = iter.next();
			} else
				x = null;
		}

		@Override
		public boolean hasNext() {
			return x != null && iter.hasNext();
		}

		@Override
		public Pair<T, T> next() {
			T y = iter.next();
			Pair<T, T> rval = new Pair<T, T>(x, y);
			x = y;
			return rval;
		}

		@Override
		public void remove() {
			die("unimplemented");
		}
	}

	public static class PairIterable<T> implements Iterable<Pair<T, T>> {
		final Collection<T> xs;

		PairIterable(Collection<T> xs) {
			this.xs = xs;
		}

		@Override
		public Iterator<Pair<T, T>> iterator() {
			return new PairIterator<T>(xs);
		}
	}

	public static <T> PairIterable<T> pairs(Collection<T> xs) {
		return new PairIterable<T>(xs);
	}

	public static <T> Map<T, Integer> indexMap(List<T> xs) {
		Map<T, Integer> xmap = newLinkedHashMap();
		int i = 0;
		for (T fi : xs)
			addMapOnce(xmap, fi, i++);
		return xmap;
	}

	public static String days2dayHour(double day) {
		int d = (int) day;
		int hours = (int) (24 * (day - d));
		if (d == 0)
			return String.format(":%02d", hours);
		return String.format("%d:%02d", d, hours);
	}

	public static String minutes2dayHour(double minutes) {
		return days2dayHour(minutes / 1440);
	}

	public static String secs2minsecs(double secs) {
		int m = (int) (secs / 60);
		int s = (int) (secs - m * 60);
		return String.format("%d:%02d", m, s);
	}

	public static String days2dayFracHour(double day) {
		int d = (int) day;
		double hours = (24 * (day - d));
		if (d == 0)
			return String.format(":%05.2f", hours);
		return String.format("%d:%05.2f", d, hours);
	}

	public static String minutes2dayFracHour(double minutes) {
		return days2dayFracHour(minutes / 1440);
	}
	// public static String days2dayHourMinutes(double day) {
	// int d = (int)day;
	// int hours = (int)(24*(day-d));
	// int minutes = (int)(day*24*60 - (d*24 +hours)*60);
	// printf("%n%d:%02d:%02d %f %f%n", d, hours, minutes, day,
	// Math.abs(minutes/1440 + hours/24 + d - day));
	// assert Math.abs(minutes + hours*60 + d*1440 - day*1440) < 1;
	// return String.format("%d:%02d:%02d", d, hours, minutes);
	// }
	// public static String minutes2dayHourMinute(double minutes) {
	// return days2dayHourMinutes(minutes*1440);
	// }

	public static String hoursAndMinutes(double minutes) {
		int hours = (int) (minutes / 60);
		int minutesLeft = (int) minutes - hours * 60;
		if (hours == 0)
			return String.format(":%02d", minutesLeft);
		return String.format("%d:%02d", hours, minutesLeft);
	}

	public static <T> T assertNonNull(T x) {
		myassert(x != null);
		return x;
	}

	public static <K, T> T lookupNonNull(Map<K, T> m, K key) {
		T x = m.get(key);
		if (x == null)
			printf("Didn't find value for key ><%s\n", key);
		myassert(x != null);
		return x;
	}

	public static <T> List<T> nonnullList(List<T> xs) {
		return xs == null ? new ArrayList<T>() : xs;
	}

	public static <T> List<T> nonnullListImm(List<T> xs) {
		List<T> l = Collections.emptyList();
		return xs == null ? l : xs;
	}

	public static <T> Set<T> nonnullSet(Collection<T> xs) {
		return xs == null ? new LinkedHashSet<T>() : xs instanceof Set<?> ? (Set<T>) xs : new LinkedHashSet<T>(xs);
	}

	public static <T> Set<T> nonnullSetImm(Set<T> xs) {
		Set<T> l = Collections.emptySet();
		return xs == null ? l : xs;
	}

	// static boolean cplex126= new File("/tmp/savesols").isDirectory();
	/**
	 * tries to set the deterministic tilim based on a time in seconds.
	 * 
	 * @throws IloException
	 */
	// public static void setCplexTilim(IloCplex cpx, double tilim) throws
	// IloException {
	//// double dtilim = 700*tilim;
	//// if (cplex126) {
	//// printf("tilim: %f :det %f%n", tilim, dtilim);
	//// cpx.setParam(IloCplex.DoubleParam.DetTiLim, dtilim);
	//// } else
	// cpx.setParam(IloCplex.DoubleParam.TiLim, tilim); // time limit in seconds -
	// FOR DEBUGGING
	// }

	public static <T> List<T> copyList(Collection<T> xs) {
		ArrayList<T> s = newArrayList();
		s.addAll(xs);
		return s;
	}

	public static <S, T> Map<S, T> copyMap(Map<S, T> xs) {
		Map<S, T> s = newLinkedHashMap();
		s.putAll(xs);
		return s;
	}

	public static <T> List<T> copy2unmodifiableList(Collection<T> xs) {
		ArrayList<T> s = newArrayList();
		s.addAll(xs);
		return unmodifiableList(s);
	}

	public static <T> Set<T> copy2unmodifiableSet(Collection<T> xs) {
		Set<T> s = newLinkedHashSet();
		s.addAll(xs);
		return unmodifiableSet(s);
	}

	public static <S, T> Map<S, List<T>> make_unmodifiableMapList(Map<S, List<T>> map) {
		return make_unmodifiableMapList(map.keySet(), map);
	}

	public static <S, T> Map<S, List<T>> make_unmodifiableMapList(Collection<S> dom, Map<S, List<T>> map) {
		Map<S, List<T>> res = newLinkedHashMap();
		for (Map.Entry<S, List<T>> entry : map.entrySet())
			res.put(entry.getKey(), unmodifiableList(entry.getValue()));

		final ArrayList<T> nl = new ArrayList<T>();
		for (S x : dom)
			if (res.get(x) == null)
				res.put(x, unmodifiableList(nl));
		return unmodifiableMap(res);
	}

	public static <S, T> Map<S, Set<T>> make_unmodifiableMapSet(Collection<S> dom, Map<S, Set<T>> map) {
		Map<S, Set<T>> res = newLinkedHashMap();
		for (Map.Entry<S, Set<T>> entry : map.entrySet())
			res.put(entry.getKey(), unmodifiableSet(entry.getValue()));
		for (S x : dom)
			if (res.get(x) == null)
				res.put(x, unmodifiableSet(new LinkedHashSet<T>()));
		return unmodifiableMap(res);
	}

	public static String newFileOutputStreamName(String path) {
		File dest = new File(path);
		if (path.contains("/"))
			dest.getParentFile().mkdirs();
		return path;
	}

	public static FileOutputStream newFileOutputStream(String path) {
		FileOutputStream fout = null;
		try {
			File dest = new File(path);
			if (path.contains("/"))
				dest.getParentFile().mkdirs();
			fout = new FileOutputStream(dest);
		} catch (FileNotFoundException e) {
			printf("Couldn't open %s for writing%n", path);
		}
		return fout;
	}

	public static long getSecs() {
		return Calendar.getInstance().getTimeInMillis() / 1000;
	}

	public static double getSecsD() {
		return Calendar.getInstance().getTimeInMillis() / 1000.0;
	}

	public static <T> double getOrZero(Map<T, Double> m, T key) {
		Double x = m.get(key);
		return x == null ? 0 : x;
	}

	public static <T> int getOrZeroInt(Map<T, Integer> m, T key) {
		Integer x = m.get(key);
		return x == null ? 0 : x;
	}

	// public static <T,X> double getOrNull(Map<T, List<X>> m, T key) {
	// List<X> x = m.get(key);
	// return x==null ? new List<X>() : x;
	// }

	public static <K, T> List<T> getAll(Map<K, T> map, Collection<K> keys) {
		List<T> vals = newArrayList();
		for (K key : keys)
			vals.add(map.get(key));
		return vals;
	}

	public static <K, T> Map<K, T> getSubmap(Map<K, T> map, Collection<K> keys) {
		Map<K, T> vals = newLinkedHashMap();
		for (K key : keys)
			vals.put(key, map.get(key));
		return vals;
	}

	public static class NullStream extends PrintStream {
		NullStream() {
			// just a handy thing to ignore
			super(System_out);
		}

		// I'm not overriding other PrintStream methods, so if you call them, the output
		// will NOT get ignored...
		@Override
		public PrintStream printf(String fmt, Object... s) {
			return this;
		}

		@Override
		public PrintStream format(String fmt, Object... s) {
			return this;
		}
	};

	public static final NullStream devnull = new NullStream();

	public static <T> boolean containsEq(Map<T, Integer> m, T x, int i) {
		return m.containsKey(x) && m.get(x) == i;
	}

	public static <T> boolean containsNeq(Map<T, Integer> m, T x, int i) {
		return m.containsKey(x) && m.get(x) != i;
	}

	public static <T> boolean containsEq(Map<T, Double> m, T x, double i) {
		return m.containsKey(x) && m.get(x) == i;
	}

	public static abstract class ElemCombiner<T> {
		abstract public T combine(T x, T y);
	};

	public static <T> List<T> combineElems(List<T> elems, ElemCombiner<T> combiner) {
		List<T> rval = newArrayList();
		T lastx = null;
		for (T x : elems) {
			if (lastx == null)
				lastx = x;
			else {
				T y = combiner.combine(lastx, x);
				if (y != null)
					lastx = y;
				else {
					rval.add(lastx);
					lastx = x;
				}
			}
		}
		if (lastx != null)
			rval.add(lastx);
		return rval;
	}

	public static String parens(String s) {
		return "(" + s + ")";
	}

	public static double toDouble(String nm, String errmsg) {
		try {
			return Double.parseDouble(nm);
		} catch (NumberFormatException x) {
			die("This string couldn't be converted to a double: %s %s\n", nm, errmsg == null ? "" : parens(errmsg));
			return 0;
		}
	}

	public static double toDouble(String nm) {
		return toDouble(nm, null);
	}

	public static boolean isDouble(String nm) {
		try {
			Double.parseDouble(nm);
			return true;
		} catch (NumberFormatException x) {
			return false;
		}
	}

	public static int toInt(String nm) {
		try {
			return Integer.parseInt(nm);
		} catch (NumberFormatException x) {
			die("This string couldn't be converted to an int: %s\n", nm);
			return 0;
		}
	}

	public static int parseWeek(String s) {
		int x = parseInt(s);
		myassert(1 <= x && x <= 17);
		return x;
	}

	public static boolean parseBool(String s) {
		int x = parseInt(s);
		myassert(0 <= x && x <= 1);
		return x == 1;
	}

	public static boolean parseBoolStr(String s) {
		if (s.equalsIgnoreCase("true"))
			return true;
		else if (s.equalsIgnoreCase("false"))
			return false;
		die("not true or false: %s", s);
		return false;
	}

	public static <T> void checkLen(List<T> xs, int n) {
		myassert(n == xs.size());
	}

	public static <T> T checkReturn(boolean b, T x) {
		myassert(b);
		return x;
	}

	public static <T> void checkNewItem(Set<T> xs, T x) {
		if (!xs.add(x)) {
			printf("This item is not new: " + x.toString());
			myassert(false);
		}
	}

	public static <T, U> void checkNewItem(Map<T, U> xs, T x, U y) {
		if (xs.put(x, y) != null) {
			printf("This item is not new: " + x.toString());
			myassert(false);
		}
	}

	public static <T> List<String> stringList(Collection<T> xs) {
		List<String> ss = newArrayList();
		for (T x : xs)
			ss.add(x.toString());
		return ss;
	}

	public static double percent(int i, int j) {
		return 100.0 * i / j;
	}

	public static String dirname(String path) {
		return new File(path).getParent();
	}

	public static String basename(String path) {
		return new File(path).getName();
	}

	//	public static void runCompress(String path) {
	//		int retval = -1;
	//		try {
	//			String[] args = { "/bin/tar", "-C", dirname(path), "-czf", path + ".tgz", basename(path) };
	//			Process p = Runtime.getRuntime().exec(args);
	//			retval = p.waitFor();
	//		} catch (InterruptedException e) {
	//			e.printStackTrace();
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//		if (retval == 0)
	//			new File(path).delete();
	//	}

	public static UList<String> runShellCmd(String cmd) {
		return runCmd("/usr/bin/sh", "-c", cmd);
	}

	public static UList<String> runCmd(String cmd) {
		return runCmd(cmd.split(" "));
	}

	public static UList<String> runCmd(String... args) {
		int retval = -1;
		try {
			Process p = Runtime.getRuntime().exec(args);
			retval = p.waitFor();
			InputStream is = p.getInputStream();
			//			printf("PROCESS OUTPUT: ");
			StringBuilder output = new StringBuilder();
			int c;
			while ((c = is.read()) != -1) {
				output.append((char) c);
				System_out.print((char) c);
			}
			return splitOnNewlines(output.toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		printf("SOME ERROR WITH runCmd!%n");
		return UList.empty();
	}

	/**
	 * return map m st: m.get(ks.get(i)) == vs.get(i) since ks is a set, there
	 * really isn't a method 'ks.get(i)', but for LinkedHashSet the set elements are
	 * enumerated in insertion order, so we can pretend that there is.
	 */
	public static <K, V> Map<K, V> zipMap(Set<K> ks, List<V> vs) {
		// WE RELY ON 'SET' BEING A LinkedHashSet,
		// BUT WE CAN'T TEST THIS, SINCE WE WRAP THEM IN AN UNMODIFIABLE SET.
		// what to do???
		// just hope that everything is ok for now.
		// assert(ks instanceof LinkedHashSet); // we rely on the insertion order

		Map<K, V> m = newLinkedHashMap();
		myassert(ks.size() == vs.size());
		// for (int i=0; i<ks.size(); ++i)
		// assert m.put(ks.get(i), vs.get(i)) == null;
		int i = 0;
		for (K k : ks) {
			addMapOnce(m, k, vs.get(i));
			i = i + 1;
		}
		return Collections.unmodifiableMap(m);
	}

	public static <K, V> Map<K, V> zipMap(String nm, Set<K> ks, List<V> vs) {
		Map<K, V> m = newLinkedHashMap();
		// assert (ks.size() == vs.size());
		int i = 0;
		for (K k : ks) {
			addMapOnce(m, k, vs.get(i));
			i = i + 1;
		}
		return Collections.unmodifiableMap(m);
	}

	public static <K, V> Map<K, V> zipMapL(List<K> ks, List<V> vs) {
		Map<K, V> m = newLinkedHashMap();
		myassert(ks.size() == vs.size());
		// for (int i=0; i<ks.size(); ++i)
		// assert m.put(ks.get(i), vs.get(i)) == null;
		int i = 0;
		for (K k : ks) {
			addMapOnce(m, k, vs.get(i));
			i = i + 1;
		}
		return Collections.unmodifiableMap(m);
	}

	//	public static <T> boolean nodups(Collection<T> xs) {
	//		return xs.size() == toSet(xs).size();
	//	}

	public static String enquote(String s) {
		return "\"" + s + "\"";
	}

	//	public static <T> LinkedHashMap<String, T> listToMap(List<T> xs, Function<T, String> getid) {
	//		LinkedHashMap<String, T> map = newLinkedHashMap();
	//		for (T x : xs)
	//			map.put(getid.apply(x), x);
	//		return map;
	//	}

	// public static <T> List<T> toList(Collection<T> xs) {
	// ArrayList<T> l = new ArrayList<T>();
	// for (T x : xs)
	// l.add(x);
	// return l;
	// }

	public static <T> List<T> asList(T[] xs) {
		return Arrays.asList(xs);
	}

	//	public static <S, T> T getOrDie(Map<S, T> map, S key) {
	//		T x = map.get(key);
	//		if (x == null)
	//			die("MAP LOOKUP FAILED!");
	//		return x;
	//	}

	// public static void printf(java.lang.String format, java.lang.Object... args)
	// {
	// System.out.printf(format, args);
	// }
	//
	// public static void println(String s) {
	// System.out.println(s);
	// }

	public static <T> ArrayList<T> toArrayList(Stream<T> stream) {
		return stream.collect(Collectors.toCollection(ArrayList::new));
	}

	public static <T> Set<T> toSet(Stream<T> stream) {
		return stream.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	// public static <T> ArrayList<T> toArrayList(Map<Integer,T> m) {
	// int mx =max(m.keySet());
	// ArrayList<T> xs=newArrayList();
	//
	// return
	// }

	//	public static <T> List<T> toUnmodifiableArrayList(Stream<T> stream) {
	//		return Collections.unmodifiableList(toArrayList(stream));
	//	}
	//
	//	public static <T> Set<T> toUnmodifiableSet(Stream<T> stream) {
	//		return Collections.unmodifiableSet(toSet(stream));
	//	}
	//
	//	public static <T> Stream<T> concatStreams(Collection<Stream<T>> streams) {
	//		Stream<T> outs = Stream.empty();
	//		for (Stream<T> s : streams)
	//			outs = Stream.concat(outs, s);
	//		return outs;
	//	}
	//
	//	public static <T> Stream<T> concatStreams(Stream<Stream<T>> streams) {
	//		return concatStreams(toArrayList(streams));
	//	}
	//
	//	public static <S, T> ArrayList<T> map(Collection<S> lst, Function<S, T> fn) {
	//		ArrayList<T> ys = newArrayList();
	//		for (S x : lst)
	//			ys.add(fn.apply(x));
	//		return ys;
	//	}
	//
	//	public static <S, T> ArrayList<T> flatMap(Collection<S> lst, Function<S, List<T>> fn) {
	//		ArrayList<T> ys = newArrayList();
	//		for (S x : lst)
	//			ys.addAll(fn.apply(x));
	//		return ys;
	//	}
	//
	//	public static <S, T> ArrayList<T> flatMapC(Collection<S> lst, Function<S, Collection<T>> fn) {
	//		ArrayList<T> ys = newArrayList();
	//		for (S x : lst)
	//			ys.addAll(fn.apply(x));
	//		return ys;
	//	}

	//	public static <Key, T> LinkedHashMap<Key, T> mkrevmap(Collection<T> lst, Function<T, Key> getKey) {
	//		LinkedHashMap<Key, T> ys = newLinkedHashMap();
	//		for (T x : lst)
	//			if (ys.put(getKey.apply(x), x) != null)
	//				die("mkrevmap encountered duplicate key entry: %s", getKey.apply(x));
	//		return ys;
	//	}
	//
	//	public static <S, T> LinkedHashMap<S, T> mkmap(Collection<S> lst, Function<S, T> fn) {
	//		LinkedHashMap<S, T> ys = newLinkedHashMap();
	//		for (S x : lst)
	//			ys.put(x, fn.apply(x));
	//		return ys;
	//	}
	//
	//	public static <S, T> Map<S, T> mkumap(Collection<S> lst, Function<S, T> fn) {
	//		return unmodifiableMap(mkmap(lst, fn));
	//	}
	//
	//	public static <S, T> LinkedHashMap<S, T> mkmap(Stream<S> lst, Function<S, T> fn) {
	//		LinkedHashMap<S, T> ys = newLinkedHashMap();
	//		lst.forEach(x -> ys.put(x, fn.apply(x)));
	//		return ys;
	//	}
	//
	//	public static <S, T> T lookupOrAdd(Map<S, T> map, S key, Function<S, T> valfn) {
	//		T x = map.get(key);
	//		if (x == null) {
	//			T fkey = valfn.apply(key);
	//			map.put(key, fkey);
	//			return fkey;
	//		}
	//		return x;
	//	}
	//
	//	public static <T> boolean some(Collection<T> xs, Predicate<T> pred) {
	//		for (T x : xs)
	//			if (pred.test(x))
	//				return true;
	//		return false;
	//	}
	//
	//	public static <T> boolean all(Collection<T> xs, Predicate<T> pred) {
	//		for (T x : xs)
	//			if (!pred.test(x))
	//				return false;
	//		return true;
	//	}

	// public static <T1, T2, T extends Pair<T1, T2>> UList<T> filter2(UList<T> ul,
	// BiFunction<T1, T2, Boolean> pred) {
	// ArrayList<T> ys = new ArrayList<>();
	// for (T x : ul.vals)
	// if (pred.apply(x.left, x.right))
	// ys.add(x);
	// return new UList<T>(ys);
	// }
	public static <T1, T2> UList<Pair<T1, T2>> filter2(UList<Pair<T1, T2>> ul, BiFunction<T1, T2, Boolean> pred) {
		ArrayList<Pair<T1, T2>> ys = new ArrayList<>();
		for (Pair<T1, T2> x : ul)
			if (pred.apply(x.left, x.right))
				ys.add(x);
		return mkUList(ys);
	}

	public static <T1, T2> void forEach2(UList<Pair<T1, T2>> ul, BiFunction<T1, T2, Boolean> pred) {
		for (Pair<T1, T2> x : ul)
			pred.apply(x.left, x.right);
	}

	public static <XT> boolean optEq(Optional<XT> x, XT y) {
		return x.isPresent() && x.get() == y;
	}

	public static UList<String> split(String s, String re) {
		//https://stackoverflow.com/questions/4964484/why-does-split-on-an-empty-string-return-a-non-empty-array
		UList<String> ss = mkUList(s.split(re)); // passing -1 doesn't help
		//		printf("SPLIT >%s< >%s< %s%n", s, re, ss.size());
		return ss;
	}

	public static UList<String> splitOnNewlines(String s) {
		return split(s, "\\r?\\n");
	}

	public static String dropNonASCII(String input) {
		StringBuilder sb = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if (ch <= 0xFF) {
				sb.append(ch);
			} else
				printf("Dropping %s%n", ch);
		}
		return sb.toString();
	}

	// what to do?
	public static UList<String> split1(String s, String re) {
		//		printf("Splitting >%s<%n", (int) s.charAt(0));
		if (s.isEmpty())
			return UList.empty();
		return split(s, re);
	}

	public static <K, V> V maybeGet(UMap<K, V> map, K k) {
		if (map == null)
			return null;
		return map.maybeGet(k);
	}

	public static UList<String> linesAfter(UList<String> lines, String re) {
		Pattern p = Pattern.compile(re);
		int i = lines.findFirstIndex(s -> p.matcher(s).matches());
		if (i >= 0)
			return lines.subList(i + 1);
		return UList.empty();
	}

	/* uses first instance, not last */
	public static UList<String> linesBefore(UList<String> lines, String re) {
		Pattern p = Pattern.compile(re);
		int i = lines.findFirstIndex(s -> p.matcher(s).matches());
		if (i >= 0)
			return lines.subList(0, i);
		return UList.empty();
	}

	public static <K, V> String maybeGetS(UMap<K, V> map, K k) {
		if (map == null)
			return "";
		V v = map.maybeGet(k);
		return v == null ? "" : v.toString();
	}

	public static boolean isIntegral(double x) {
		return x == (int) x;
	}

	public static <T> boolean nullOrEquals(T x, T y) {
		if (x == null)
			return (y == null);
		if (y == null)
			return false;
		return x.equals(y);
	}

	public static boolean nullOrZero(Integer x) {
		return x == null || x == 0;
	}

	public static <T> T orElse(T x, T y) {
		return x == null ? y : x;
	}

	public static int getPid(Process process) {
		try {
			Class<?> cProcessImpl = process.getClass();
			Field fPid = cProcessImpl.getDeclaredField("pid");
			if (!fPid.isAccessible()) {
				fPid.setAccessible(true);
			}
			return fPid.getInt(process);
		} catch (Exception e) {
			return -1;
		}
	}
}
