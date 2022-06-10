// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static java.util.Collections.*;
import static utils.VRAUtils.*;

import java.io.BufferedInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class UList<T //extends Comparable<? super T>
> implements Iterable<T> //, Comparable<UList<T>> 
{
	/**
	 * I'd like to make vals final, but I want to be able to set it after initialization for LazyUList.
	 * The more important issue is that no values are changed, which I don't enforce using UnchangableList,
	 * this would just have to be verified by examination.
	 */
	//private final List<T> vals; if this is used, there should be no errors in this file except in setVals(), only in LazyUList.
	private List<T> vals;
	private LinkedHashSet<T> set___;

	protected void setVals(List<T> vals) {
		myassert(this.vals == null);
		myassert(vals != null);
		this.vals = vals;
	}

	// ONLY FOR TEMPORARY VALUES
	//	protected void setVals(Set<T> vals) {
	//		myassert(this.vals == null);
	//		myassert(vals != null);
	//		this.vals = vals.iterator();
	//	}

	private LinkedHashSet<T> set() {
		if (set___ == null)
			set___ = valsSet();
		return set___;
	}

	protected List<T> vals() {
		return vals;
	};

	/** specifically for LazyUList
	 * As a special exception, I want vals to be null in this case, so that any use before initialization will cause an error. 
	 */
	protected UList(String fooie) {
		this.vals = null;
		this.set___ = null;
	}

	public boolean inited() {
		return vals != null;
	}

	protected void init(UList<T> vals) {
		myassert(this instanceof DelayedInitUList || this instanceof SingleAssignUList);
		myassert(this.vals == null, "This list has already been initialized!");
		myassert(vals != null);
		this.vals = vals.vals;
		myassert(this.vals != null);
	}

	/**
	 * protected, so OrderedUList can access it; doesn't make sense for public use
	 */
	protected UList(UList<T> vs) {
		this.vals = vs.vals;
		// unnecessary
		//		vs.forEach(x -> myassert(x != null, "UList init has null pointer."));
		this.set___ = null;
	}

	/**
	 * protected, so GrowList can access it; doesn't make sense for public use, use empty()
	 */
	protected UList() {
		this.vals = new ArrayList<T>();
		this.set___ = null;
	}

	protected UList(List<T> vs) {
		this.vals = vs;
		vs.forEach(x -> myassert(x != null, "UList init has null pointer."));
		this.set___ = null;
	}

	private UList(Stream<T> vs) {
		this(VRAUtils.toArrayList(vs));
		vals.forEach(x -> myassert(x != null, "UList init has null pointer."));
	}

	private UList(LinkedHashSet<T> vs) {
		this.vals = toList(vs);
		vs.forEach(x -> myassert(x != null, "UList init has null pointer."));
		this.set___ = vs;
	}

	private UList(UList<T> parent, int fromIndex, int toIndex) {
		// List.sublist doesn't actually copy 
		this.vals = parent.vals.subList(fromIndex, toIndex);
		this.set___ = null;
	}

	public UList(int sz, Function<Integer, T> fn) {
		vals = new ArrayList<T>(sz);
		for (int i = 0; i < sz; ++i)
			vals.add(fn.apply(i));
		this.set___ = null;
	}

	// nuts, must be dup
	public static UList<Integer> seqTo(int sz) {
		return new UList<>(sz, n -> n);
	}

	public static <T> UList<T> nCopies(int sz, T x) {
		return new UList<>(sz, i -> x);
	}

	public List<T> asCollection() {
		return unmodifiableList(vals);
	}

	// Can't make this generic
	//	@SuppressWarnings("unchecked")
	//	public T[] asArray() {
	//		T[] xs = new T[vals.size()];
	//		return (T[]) vals.toArray();
	//	}

	public String toString() {
		return vals.toString();
	}

	public String toString1() {
		return vals.size() == 1 ? vals.get(0).toString() : vals.toString();
	}

	public boolean equalsOne(T x) {
		return size() == 1 && vals.get(0) == x;
	}

	public boolean equals(Object that) {
		if (this == that)
			return true;// if both of them points the same address in memory

		if (!(that instanceof UList<?>))
			return false; // if "that" is not a People or a childclass

		@SuppressWarnings("unchecked")
		UList<T> thatx = (UList<T>) that; // than we can cast it to People safely

		return this.vals.equals(thatx.vals);
	}

	@Override
	public int hashCode() {
		return vals.hashCode();
	}

	// private static <S, T> ArrayList<T> map(Collection<S> lst, Function<S, T> fn)
	// {
	// ArrayList<T> ys = new ArrayList<>();
	// for (S x : lst)
	// ys.add(fn.apply(x));
	// return ys;
	// }

	public Iterator<T> iterator() {
		return new MyIterator();
	}

	class MyIterator implements Iterator<T> {

		private int index = 0;

		public boolean hasNext() {
			return index < vals.size();
		}

		public T next() {
			return vals.get(index++);
		}

		public void remove() {
			throw new UnsupportedOperationException("not supported - unmodifiable");
		}
	}

	public int size() {
		return vals.size();
	}

	/** NOTE: THIS IS AMBIGUOUS FOR INTEGER LISTS!!! */
	public UList<T> remove(int ind) {
		List<T> xs = new ArrayList<>(vals);
		xs.remove(ind);
		return mkUList(xs);

	}

	public UList<T> set(int ind, T x) {
		List<T> xs = new ArrayList<>(vals);
		xs.set(ind, x);
		return mkUList(xs);
	}

	/** NOTE: THIS IS AMBIGUOUS FOR INTEGER LISTS!!! */
	public UList<T> remove(T x) {
		List<T> xs = new ArrayList<>(vals);
		xs.remove(x);
		return mkUList(xs);
	}

	public final T get(int ind) {
		return vals.get(ind);
	}

	public UList<T> getList(UList<Integer> inds) {
		return inds.map(ind -> vals.get(ind));
	}

	public UList<T> getList(Integer... inds) {
		return mkUList(inds).map(ind -> vals.get(ind));
	}

	public T last() {
		myassert(!vals.isEmpty());
		return vals.get(vals.size() - 1);
	}

	public T first() {
		//		myassert(!vals.isEmpty());
		return vals.get(0);
	}

	public UList<T> rest() {
		//		myassert(!vals.isEmpty());
		return subList(1);
	}

	public T any() {
		//		myassert(!vals.isEmpty());
		return vals.get(0);
	}

	public boolean isEmpty() {
		return vals.isEmpty();
	}

	public boolean isNotEmpty() {
		return !vals.isEmpty();
	}

	private LinkedHashSet<T> valsSet() {
		LinkedHashSet<T> l = new LinkedHashSet<>();
		for (T x : vals)
			l.add(x);
		return l;
	}

	public Set<T> toSet() {
		return valsSet();
	}

	@SuppressWarnings("unchecked")
	public static <TT> UList<TT> empty() {
		return new UList<TT>(Collections.EMPTY_LIST);
	}

	public UList<Integer> indexRange() {
		return rangeOpenUL(0, size());
	}

	private static UList<Integer> rangeClosedUL(int start, int last) {
		return new UList<>(IntStream.rangeClosed(start, last).boxed());
	}

	private static UList<Integer> rangeOpenUL(int start, int last) {
		return new UList<>(IntStream.range(start, last).boxed());
	}

	public static OList<Integer> rangeClosed(int start, int last) {
		return new OList<Integer>(rangeClosedUL(start, last));
	}

	public static OList<Integer> rangeClosed(int last) {
		return new OList<Integer>(rangeClosedUL(0, last));
	}

	public static <T> UList<T> rangeClosedMap(int start, int last, Function<Integer, T> fn) {
		return new OList<Integer>(rangeClosedUL(start, last)).map(fn);
	}

	public static OList<Integer> rangeOpen(int last) {
		return rangeOpen(0, last);
	}

	public static OList<Integer> rangeOpen(int start, int last) {
		return new OList<Integer>(rangeOpenUL(start, last));
	}

	public static UList<Double> mkDList(int start, int last, Function<Integer, Double> fn) {
		return rangeOpenUL(start, last).map(i -> fn.apply(i));
		//		return rangeOpen(start, last).map(i -> fn.apply(i));
	}

	public static <T> UList<T> mkUList(T[] vals) {
		return new UList<>(toList(vals));
	}

	//	public static UList<Integer> mkUList(int[] vals) {
	//		return new UList<>(toList(vals));
	//	}

	private static <T> List<T> toList(T[] xs) {
		ArrayList<T> l = new ArrayList<T>();
		for (T x : xs)
			l.add(x);
		return l;
	}

	// nuts
	//	private static List<Integer> toList(int[] xs) {
	//		ArrayList<Integer> l = new ArrayList<>();
	//		for (Integer x : xs)
	//			l.add(x);
	//		return l;
	//	}
	public static List<Double> toList(double[] xs) {
		ArrayList<Double> l = new ArrayList<>();
		for (Double x : xs)
			l.add(x);
		return l;
	}

	private static <T> List<T> toList(Collection<T> xs) {
		ArrayList<T> l = new ArrayList<T>();
		for (T x : xs)
			l.add(x);
		return l;
	}

	public static UList<Double> mkUList(double[] vals) {
		ArrayList<Double> l = new ArrayList<Double>();
		for (double x : vals)
			l.add(x);
		return new UList<>(l);
	}

	public static UList<Integer> mkUList(int[] vals) {
		ArrayList<Integer> l = new ArrayList<Integer>();
		for (int x : vals)
			l.add(x);
		return new UList<>(l);
	}

	public double[] todoublearray() {
		double[] xs = new double[size()];
		for (int i = 0; i < size(); ++i)
			xs[i] = (double) vals.get(i); // NUTS!
		return xs;
	}

	public static <T> UList<T> mkUList(T[] vals, int fromIndex, int toIndex) {
		return new UList<>(toList(vals).subList(fromIndex, toIndex));
	}

	// I think so, anyway...
	@SafeVarargs
	public static <T> UList<T> mkUList1(T... vals) {
		return new UList<>(toList(vals));
	}

	@SafeVarargs
	public static <T extends Comparable<? super T>> OList<T> mkOList1(T... vals) {
		return new OList<>(vals); // toList(vals));
	}

	public static <T> UList<T> mkUList(Collection<T> vals) {
		return new UList<>(toList(vals));
	}

	public static <T extends Comparable<? super T>> OList<T> mkOUList(Collection<T> vals) {
		return new OList<T>(vals);
	}

	public static <T> UList<T> mkUList1(T x) {
		ArrayList<T> xs = new ArrayList<>();
		xs.add(x);
		return new UList<>(xs);
	}

	public UList<T> add(T x) {
		ArrayList<T> union = new ArrayList<T>(vals);
		union.add(x);
		return new UList<>(union);
	}

	public UList<T> maybeAdd(T x) {
		if (x == null)
			return this;
		return add(x);
	}

	public UList<T> maybeAdd(boolean b, T x) {
		if (b)
			return add(x);
		return this;
	}

	public UList<T> cons(T x) {
		ArrayList<T> union = new ArrayList<T>();
		union.add(x);
		union.addAll(vals);
		return new UList<>(union);
	}

	public static <T> UList<T> cons(T x, UList<T> s2) {
		ArrayList<T> union = new ArrayList<T>();
		union.add(x);
		union.addAll(s2.vals);
		return new UList<>(union);
	}

	public static <S, T> UList<Object> consx(S x, UList<T> s2) {
		ArrayList<Object> union = new ArrayList<Object>();
		union.add(x);
		union.addAll(s2.vals);
		return new UList<>(union);
	}

	public UList<T> concat(UList<T> x) {
		if (vals.isEmpty())
			return x;
		if (x.isEmpty())
			return this;
		ArrayList<T> union = new ArrayList<T>(vals);
		union.addAll(x.vals);
		return new UList<>(union);
	}

	public UList<T> maybeConcat(boolean cond, UList<T> x) {
		return cond ? concat(x) : this;
	}

	//	public UList<?> concat1(UList<? super T> x) {
	//		if (vals.isEmpty())
	//			return x;
	//		ArrayList<?> union = new ArrayList<?>();
	//		union.addAll(vals);
	//		union.addAll(x.vals);
	//		return new UList<>(union);
	//	}

	public static <T, TL extends UList<T>> UList<T> concatULists(UList<TL> xs) {
		ArrayList<T> union = new ArrayList<T>();
		for (UList<T> x : xs)
			union.addAll(x.vals);
		return new UList<>(union);
	}

	@SafeVarargs
	public static <T, TL extends UList<T>> UList<T> concatULists1(TL... xs) {
		ArrayList<T> union = new ArrayList<T>();
		for (UList<T> x : xs)
			union.addAll(x.vals);
		return new UList<>(union);
	}

	public UList<T> union(UList<T> s1) {
		if (vals.isEmpty())
			return s1;
		if (s1.isEmpty())
			return this;
		LinkedHashSet<T> union1 = new LinkedHashSet<T>(vals);
		myassert(union1.size() == vals.size()); // else wasn't a set to begin with; use concat

		union1.addAll(s1.vals);
		return new UList<>(union1);
	}

	public UList<T> diff(UList<T> s2) {
		return diff(s2.vals);
	}

	public UList<T> diff(Collection<T> s2) {
		if (s2.isEmpty())
			return this;
		//		LinkedHashSet<T> diff1 = new LinkedHashSet<T>(vals);
		ArrayList<T> diff1 = new ArrayList<T>(vals);
		diff1.removeAll(s2);
		return new UList<>(diff1);
	}

	public UList<T> rdiff(UList<T> s2) {
		return s2.diff(this);
	}

	//	public static <T, CT extends Iterable<T>> UList<T> unionLists(UList<CT> xs) {
	//		Set<T> union = new LinkedHashSet<T>();
	//		for (Iterable<T> x : xs)
	//			// union.addAll(x);
	//			for (T y : x)
	//				union.add(y);
	//		return new UList<>(union);
	//	}
	public static <T, CT extends UList<T>> UList<T> unionLists(UList<CT> xs) {
		LinkedHashSet<T> union = new LinkedHashSet<T>();
		for (Iterable<T> x : xs)
			// union.addAll(x);
			for (T y : x)
				union.add(y);
		return new UList<>(union);
	}

	//	public UList<T> asSet() {
	//		Set<T> l = newLinkedHashSet();
	//		for (T x : vals)
	//			l.add(x);
	//		return new UList<>(l);
	//	}

	public UList<T> distinct() {
		ArrayList<T> union = new ArrayList<T>();
		LinkedHashSet<T> all = new LinkedHashSet<T>();
		for (T x : vals)
			if (all.add(x))
				// new
				union.add(x);
		return new UList<>(union);
	}

	public <S> UList<T> distinct(Function<T, S> fn) {
		ArrayList<T> union = new ArrayList<T>();
		LinkedHashSet<S> all = new LinkedHashSet<S>();
		for (T x : vals)
			if (all.add(fn.apply(x)))
				// new
				union.add(x);
		return new UList<>(union);
	}

	public UMap<T, Integer> numOccurrences() {
		return new UMap<>(raw_numOccurrences());
	}

	private Map<T, Integer> raw_numOccurrences() {
		Map<T, Integer> map = new LinkedHashMap<>();
		for (T x : vals)
			map.put(x, map.getOrDefault(x, 0) + 1);
		return map;
	}

	public UList<T> duped() {
		ArrayList<T> union = new ArrayList<T>();
		Map<T, Integer> map = raw_numOccurrences();
		for (T x : vals)
			if (map.get(x) > 1)
				union.add(x);
		return new UList<>(union).distinct();
	}

	public boolean areDistinct() {
		return this.duped().isEmpty();
	}

	public UList<T> checkDistinct() {
		return checkDistinct("");
	}

	public UList<T> checkDistinct(String msg) {
		UList<T> duped = this.duped();
		if (!duped.isEmpty()) {
			for (T x : duped)
				printf(" %s%n", x);
			if (msg.equals(""))
				die("list has duped elems.");
			else
				die(msg);
		}
		return this;
	}

	public <Y> UList<Pair<T, Y>> zip(UList<Y> ys) {
		myassert(size() == ys.size());
		return mapI((i, x) -> new Pair<>(x, ys.get(i)));
	}

	public <Y, Z> UList<Z> zipmap(UList<Y> ys, BiFunction<T, Y, Z> fn) {
		myassert(size() == ys.size());
		return zip(ys).map(pair -> fn.apply(pair.left, pair.right));
	}

	public <Y> UMap<T, Y> zipMap(UList<Y> zs) {
		myassert(size() == zs.size());
		LinkedHashMap<T, Y> ys = new LinkedHashMap<>();
		for (int i : rangeOpen(size())) {
			ys.put(get(i), zs.get(i));
		}
		return new UMap<>(ys);
	}

	public static <T> boolean anyPairsIntersect(UList<UList<T>> xs) {
		for (int i = 0; i < xs.size(); ++i)
			for (int j = i + 1; j < xs.size(); ++j) {
				UList<T> x = xs.vals.get(i);
				UList<T> y = xs.vals.get(j);
				if (x.intersects(y)) {
					printf("anyPairsIntersect: two members intersect: %s:%s %s:%s %s%n", i, x, j, y, x.intersection(y));
					return true;
				}
			}
		return false;
	}

	//	public static <T> UList<T> intersectAll(UList<UList<T>> xls) {
	//		myassert(!xls.isEmpty());
	//		return xls.reduce(xls.get(0), (x, y) -> x.intersection(y));
	//	}
	//
	//	public static <T> boolean emptyIntersection(UList<UList<T>> xls) {
	//		if (xls.size() <= 1)
	//			return true; // ??
	//		return intersectAll(xls).isEmpty();
	//	}

	public UList<T> checkNonEmpty() {
		if (isEmpty()) {
			die("list is empty.");
		}
		return this;
	}

	public boolean some(Predicate<T> pred) {
		for (T x : vals)
			if (pred.test(x))
				return true;
		return false;
	}

	public boolean someUnique(Predicate<T> pred) {
		boolean seen = false;
		for (T x : vals)
			if (pred.test(x))
				if (seen)
					return false;
				else
					seen = true;
		return seen;
	}

	public boolean atLeast(int n, Predicate<T> pred) {
		int nseen = 0;
		for (T x : vals)
			if (pred.test(x)) {
				nseen++;
				if (nseen >= n)
					return true;
			}
		return false;
	}

	public boolean all(Predicate<T> pred) {
		for (T x : vals)
			if (!pred.test(x))
				return false;
		return true;
	}

	public boolean alli(BiFunction<Integer, ? super T, Boolean> pred) {
		int i = 0;
		for (T x : vals)
			if (!pred.apply(i++, x))
				return false;
		return true;
	}

	public static boolean all(UList<Boolean> xs) {
		for (boolean x : xs.vals)
			if (!x)
				return false;
		return true;
	}

	public UList<T> maybeFilter(boolean b, Predicate<T> pred) {
		if (b)
			return filter(pred);
		return this;
	}

	public UList<T> filter(Predicate<T> pred) {
		ArrayList<T> ys = new ArrayList<>();
		for (T x : vals)
			if (pred.test(x))
				ys.add(x);
		return new UList<T>(ys);
	}

	public UList<T> allAfter(Predicate<T> pred) {
		int n = findFirstIndex(pred);
		if (n >= 0)
			return subList(n + 1);
		return UList.empty();
	}

	public UList<T> allBefore(Predicate<T> pred) {
		int n = findFirstIndex(pred);
		if (n > 0)
			return subList(0, n);
		return UList.empty();
	}

	public <ST extends T> UList<ST> filterSubType(Class<ST> cl) {
		ArrayList<ST> ys = new ArrayList<>();
		for (T x : vals)
			if (cl.isInstance(x))
				ys.add(cl.cast(x));
		return new UList<ST>(ys);
	}

	// can't do that
	//	public UList<T> filterNonEmpty() {
	//		return filter(x -> !x.isEmpty());
	//	}

	public int count(Predicate<T> pred) {
		int n = 0;
		for (T x : vals)
			if (pred.test(x))
				n++;
		return n;
	}

	public int count(T x0) {
		int n = 0;
		for (T x : vals)
			if (x.equals(x0))
				n++;
		return n;
	}

	public UMap<T, Integer> popCount() {
		//		LinkedHashMap<T, Integer> mpx = new LinkedHashMap<>();

		UMap<T, Integer> mp = groupingBy(x -> x).mapValues(x -> x.size());
		//		myassert(mp.keySet().set().equals(mpx.keySet()));
		//		...

		return mp;
	}

	/** first arg is the 0-based iterator, second arg is the elem */
	public UList<T> filterI(BiFunction<Integer, ? super T, Boolean> pred) {
		ArrayList<T> ys = new ArrayList<>();
		int i = 0;
		for (T x : vals) {
			if (pred.apply(i, x))
				ys.add(x);
			i++;
		}
		return new UList<T>(ys);
	}

	public <Z> Z reduce(Z ident, BiFunction<Z, ? super T, Z> fn) {
		Z z = ident;
		for (T x : vals)
			z = fn.apply(z, x);
		return z;
	}

	public int findFirstIndex(Predicate<T> pred) {
		for (int i = 0; i < vals.size(); ++i)
			if (pred.test(vals.get(i)))
				return i;
		return -1;
	}

	public int findFirstIndexI(BiFunction<Integer, ? super T, Boolean> pred) {
		for (int i = 0; i < vals.size(); ++i)
			if (pred.apply(i, vals.get(i)))
				return i;
		return -1;
	}

	public Optional<T> findFirst(Predicate<T> pred) {
		for (T x : vals)
			if (pred.test(x))
				return Optional.of(x);
		return Optional.empty();
	}

	public Optional<T> findLast(Predicate<T> pred) {
		// I can't believe there is no reverse iterator for lists...
		for (int i = vals.size() - 1; i >= 0; --i)
			if (pred.test(vals.get(i)))
				return Optional.of(vals.get(i));
		return Optional.empty();
	}

	public int findLastIndex(Predicate<T> pred) {
		// I can't believe there is no reverse iterator for lists...
		for (int i = vals.size() - 1; i >= 0; --i)
			if (pred.test(vals.get(i)))
				return i;
		return -1;
	}

	public static int min(UList<Integer> xs) {
		return Collections.min(xs.vals);
	}

	public static int minOrElse(UList<Integer> xs, int elsex) {
		if (xs.isEmpty())
			return elsex;
		return Collections.min(xs.vals);
	}

	public static int max(UList<Integer> xs) {
		return Collections.max(xs.vals);
	}

	public static double mind(UList<Double> xs) {
		return Collections.min(xs.vals);
	}

	public static double mindOrElse(UList<Double> xs, double elsex) {
		if (xs.isEmpty())
			return elsex;
		return Collections.min(xs.vals);
	}

	public static double maxd(UList<Double> xs) {
		return Collections.max(xs.vals);
	}

	public static double maxdOrElse(UList<Double> xs, double elsex) {
		if (xs.isEmpty())
			return elsex;
		return Collections.max(xs.vals);
	}

	public T max(Function<T, Integer> fn) {
		myassert(!vals.isEmpty());
		T mx = vals.get(0);
		int mxval = fn.apply(vals.get(0));
		for (T x : vals) {
			int vl = fn.apply(x);
			if (mxval < vl) {
				mxval = vl;
				mx = x;
			}
		}
		return mx;
	}

	public T min(Function<T, Integer> fn) {
		myassert(!vals.isEmpty());
		T mx = vals.get(0);
		int mxval = fn.apply(vals.get(0));
		for (T x : vals) {
			int vl = fn.apply(x);
			if (mxval > vl) {
				mxval = vl;
				mx = x;
			}
		}
		return mx;
	}

	public T minOrElse(Function<T, Integer> fn, T deft) {
		if (vals.isEmpty())
			return deft;
		T mx = vals.get(0);
		int mxval = fn.apply(vals.get(0));
		for (T x : vals) {
			int vl = fn.apply(x);
			if (mxval > vl) {
				mxval = vl;
				mx = x;
			}
		}
		return mx;
	}

	public T mind(Function<T, Double> fn) {
		myassert(!vals.isEmpty());
		T mx = vals.get(0);
		double mxval = fn.apply(vals.get(0));
		for (T x : vals) {
			double vl = fn.apply(x);
			if (mxval > vl) {
				mxval = vl;
				mx = x;
			}
		}
		return mx;
	}

	public static <XT> UList<XT> filterOptional(UList<Optional<XT>> xs) {
		return xs.filter(x -> x.isPresent()).map(x -> x.get());
	}

	public void forEach(Consumer<? super T> action) {
		vals.forEach(action);
	}

	public void printEach(String fmt) {
		vals.forEach(x -> printf(fmt, x));
	}

	public void assertEach(Predicate<? super T> action) {
		vals.forEach(x -> myassert(action.test(x), "assertEach failed on item " + x));
	}

	public void assertSize1() {
		myassert(size() == 1, this);
	}

	//	public void assertEach(Predicate<? super T> action, Object... args) {
	//		vals.forEach(x -> myassert(action.test(x), "assertEach failed on item " + x, args));
	//	}

	// http://www.baeldung.com/java-lambda-exceptions
	@FunctionalInterface
	public interface ThrowingConsumer<T, E extends Exception> {
		void accept(T t) throws E;
	}

	//	public static <T, Ex extends Exception> Consumer<T> throwingConsumerWrapper(
	//			ThrowingConsumer<T, Ex> throwingConsumer) {
	//
	//		return i -> {
	//			try {
	//				throwingConsumer.accept(i);
	//			} catch (Ex ex) {
	//				throw new RuntimeException(ex);
	//			}
	//		};
	//	}

	//	static <T, E extends Exception> Consumer<T> handlingConsumerWrapper(ThrowingConsumer<T, E> throwingConsumer,
	//			Class<E> exceptionClass) {
	//
	//		return i -> {
	//			try {
	//				throwingConsumer.accept(i);
	//			} catch (Exception ex) {
	//				try {
	//					E exCast = exceptionClass.cast(ex);
	//					System.err.println("Exception occured : " + exCast.getMessage());
	//					throw new RuntimeException(ex);
	//				} catch (ClassCastException ccEx) {
	//					throw new RuntimeException(ex);
	//				}
	//			}
	//		};
	//	}

	public <Ex extends Exception> void forEachEx(ThrowingConsumer<? super T, Ex> action) throws Ex {
		//		if (vals.isEmpty())
		//			return;
		//		vals.forEach(throwingConsumerWrapper(action));
		for (T x : vals)
			action.accept(x);
	}

	@FunctionalInterface
	public interface ThrowingPredicate<T, E extends Exception> {
		boolean accept(T t) throws E;
	}

	@FunctionalInterface
	public interface ThrowingFunction<S, T, E extends Exception> {
		T apply(S t) throws E;
	}

	@FunctionalInterface
	public interface ThrowingBiFunction<S, V, T, E extends Exception> {
		T apply(S t, V v) throws E;
	}

	//	static <T> Predicate<T> throwingPredicateWrapper(ThrowingPredicate<T, Exception> throwingPredicate) {
	//
	//		return i -> {
	//			try {
	//				return throwingPredicate.accept(i);
	//			} catch (Exception ex) {
	//				throw new RuntimeException(ex);
	//			}
	//		};
	//	}

	//	static <T, E extends Exception> Predicate<T> handlingPredicateWrapper(ThrowingPredicate<T, E> throwingPredicate,
	//			Class<E> exceptionClass) {
	//
	//		return i -> {
	//			try {
	//				return throwingPredicate.accept(i);
	//			} catch (Exception ex) {
	//				try {
	//					E exCast = exceptionClass.cast(ex);
	//					System.err.println("Exception occured : " + exCast.getMessage());
	//					throw new RuntimeException(ex);
	//				} catch (ClassCastException ccEx) {
	//					throw new RuntimeException(ex);
	//				}
	//			}
	//		};
	//	}

	public UList<T> filterEx(ThrowingPredicate<? super T, Exception> pred) throws Exception {
		ArrayList<T> ys = new ArrayList<>();
		for (T x : vals)
			if (pred.accept(x))
				ys.add(x);
		return new UList<T>(ys);
	}

	public void forEachI(BiConsumer<Integer, ? super T> action) {
		int i = 0;
		for (T x : vals)
			action.accept(i++, x);
	}

	//	static <S, T> Function<S, T> throwingFunctionWrapper(ThrowingFunction<S, T, Exception> throwingFunction) {
	//
	//		return i -> {
	//			try {
	//				return throwingFunction.apply(i);
	//			} catch (Exception ex) {
	//				throw new RuntimeException(ex);
	//			}
	//		};
	//	}

	//	static <S, T, E extends Exception> Function<S, T> handlingFunctionWrapper(
	//			ThrowingFunction<S, T, E> throwingFunction, Class<E> exceptionClass) {
	//
	//		return i -> {
	//			try {
	//				return throwingFunction.apply(i);
	//			} catch (Exception ex) {
	//				try {
	//					E exCast = exceptionClass.cast(ex);
	//					System.err.println("Exception occured : " + exCast.getMessage());
	//					throw new RuntimeException(ex);
	//				} catch (ClassCastException ccEx) {
	//					throw new RuntimeException(ex);
	//				}
	//			}
	//		};
	//	}

	public <R> UList<R> map(UMap<? super T, ? extends R> mapper) {
		return map(x -> mapper.get(x));
	}

	public <R> UList<R> map(Function<? super T, ? extends R> mapper) {
		ArrayList<R> ys = new ArrayList<>();
		for (T x : vals)
			ys.add(mapper.apply(x));
		return new UList<R>(ys);
	}

	public static <R> UList<R> maybeList(boolean cond, UList<R> lst) {
		return cond ? lst : UList.empty();
	}

	public static <R> UList<R> maybeList1(boolean cond, R x) {
		return cond ? mkUList1(x) : UList.empty();
	}

	public static <R> UList<R> maybeList1(boolean cond, Supplier<R> fn) {
		return cond ? mkUList1(fn.get()) : UList.empty();
	}

	public static <R> UList<R> maybeList(boolean cond, Supplier<UList<R>> fn) {
		return cond ? fn.get() : UList.empty();
	}

	public <R> UList<T> filterEquiv(Function<? super T, ? extends R> mapper) {
		ArrayList<T> ys = new ArrayList<>();
		Set<R> eqvs = new LinkedHashSet<>();
		for (T x : vals)
			if (eqvs.add(mapper.apply(x)))
				ys.add(x);
		return new UList<T>(ys);
	}

	public <R> UList<R> mapDropNulls(Function<? super T, ? extends R> mapper) {
		ArrayList<R> ys = new ArrayList<>();
		for (T x : vals) {
			R y = mapper.apply(x);
			if (y != null)
				ys.add(y);
		}
		return new UList<R>(ys);
	}

	public <R, Ex extends Exception> UList<R> mapEx(ThrowingFunction<? super T, ? extends R, Ex> mapper) throws Ex {
		ArrayList<R> ys = new ArrayList<>();
		for (T x : vals)
			ys.add(mapper.apply(x));
		return new UList<R>(ys);
	}

	public <R, Ex extends Exception> UList<R> mapDropNullsEx(ThrowingFunction<? super T, ? extends R, Ex> mapper)
			throws Ex {
		ArrayList<R> ys = new ArrayList<>();
		for (T x : vals) {
			R y = mapper.apply(x);
			if (y != null)
				ys.add(y);
		}
		return new UList<R>(ys);
	}

	/** first arg is the 0-based iterator, second arg is the elem */
	public <R> UList<R> mapI(BiFunction<Integer, ? super T, ? extends R> mapper) {
		ArrayList<R> ys = new ArrayList<>();
		int i = 0;
		for (T x : vals)
			ys.add(mapper.apply(i++, x));
		return new UList<R>(ys);
	}

	public UList<Pair<T, Integer>> pairIright() {
		return mapI((i, x) -> new Pair<T, Integer>(x, i));
	}

	public <R, Ex extends Exception> UList<R> mapIEx(ThrowingBiFunction<Integer, ? super T, ? extends R, Ex> mapper)
			throws Ex {
		ArrayList<R> ys = new ArrayList<>();
		int i = 0;
		for (T x : vals)
			ys.add(mapper.apply(i++, x));
		return new UList<R>(ys);
	}

	/** == map(mapper).distinct().size()<=1 */
	public <R> boolean allSame(Function<? super T, ? extends R> mapper) {
		if (isEmpty())
			return true;
		R y = mapper.apply(get(0));
		for (T x : vals)
			if (!y.equals(mapper.apply(x)))
				return false;
		return true;
	}

	// public <R> UList<R> mapList(Function<? super T, List<? extends R>> mapper) {
	// ArrayList<R> ys = new ArrayList<>();
	// for (T x : vals)
	// ys.addAll(mapper.apply(x));
	// return new UList<R>(ys);
	// }

	public <VT> UList<VT> flatMap(Function<T, UList<? extends VT>> fn) {
		ArrayList<VT> ys = new ArrayList<>();
		for (T x : vals)
			ys.addAll(fn.apply(x).vals);
		return new UList<>(ys);
	}

	public <VT, VT1 extends VT> UList<VT> flatMapDropNull(Function<T, UList<VT1>> fn) {
		ArrayList<VT> ys = new ArrayList<>();
		for (T x : vals) {
			UList<VT1> y = fn.apply(x);
			if (y != null)
				ys.addAll(y.vals);
		}
		return new UList<>(ys);
	}

	public static <T> UList<T> flatten1(UList<T>... xs) {
		return flatten(mkUList1(xs));
	}

	public static <T, TL extends UList<T>> UList<T> flatten(UList<TL> xs) {
		ArrayList<T> ys = new ArrayList<>();
		for (UList<T> x : xs.vals)
			ys.addAll(x.vals);
		return new UList<>(ys);
	}

	//	public UList<T> flatten() {
	//		if (vals.isEmpty())
	//			return empty();
	//		if (vals.get(0) instanceof UList<?>) {
	//			@SuppressWarnings("unchecked")
	//			List<UList<T>> xs = List.class.cast(vals);
	//			ArrayList<T> ys = new ArrayList<>();
	//			for (UList<T> x : xs)
	//				ys.addAll(x.vals);
	//			return new UList<>(ys);
	//		} else {
	//			die("This was supposed to be a UList<UList<T>>: %s%n", this);
	//			return null;
	//		}
	//	}

	public <VT> UList<VT> flatMapI(BiFunction<Integer, T, UList<? extends VT>> fn) {
		ArrayList<VT> ys = new ArrayList<>();
		int i = 0;
		for (T x : vals)
			ys.addAll(fn.apply(i++, x).vals);
		return new UList<>(ys);
	}

	//	public UList<T> sortByIntFieldAscending(Function<T, Integer> field) {
	//		return sort(new Comparator<T>() {
	//			@Override
	//			public int compare(T e1, T e2) {
	//				return field.apply(e1) - field.apply(e2);
	//			}
	//		});
	//	}

	public <S extends Comparable<? super S>> UList<T> sortByFieldAscending(Function<T, S> field) {
		return sort(new Comparator<T>() {
			@Override
			public int compare(T e1, T e2) {
				return field.apply(e1).compareTo(field.apply(e2));
			}
		});
	}

	public <S extends Comparable<? super S>, W extends Comparable<? super W>> UList<T> sortByFieldAscending(
			Function<T, S> field, Function<T, W> field2) {
		return sort(new Comparator<T>() {
			@Override
			public int compare(T e1, T e2) {
				int cmp = field.apply(e1).compareTo(field.apply(e2));
				if (cmp != 0)
					return cmp;
				return field2.apply(e1).compareTo(field2.apply(e2));
			}
		});
	}

	public <S extends Comparable<? super S>> UList<T> sortByFieldDescending(Function<T, S> field) {
		return sort(new Comparator<T>() {
			@Override
			public int compare(T e1, T e2) {
				return -field.apply(e1).compareTo(field.apply(e2));
			}
		});
	}

	// This is OK because we never modify the list.
	@SuppressWarnings("unchecked")
	public static <S, T extends S> UList<S> conv(UList<T> x) {
		return (UList<S>) x;
	}

	public static <S extends Comparable<? super S>> OList<S> sort(UList<S> xs) {
		return new OList<>(xs);
	}

	public static <S extends Comparable<? super S>> UList<S> sort(UList<S> xs, Comparator<S> comp) {
		return xs.sort(comp);
	}

	public static <S extends Comparable<S>> UList<S> sort1(UList<S> xs, Comparator<S> comp) {
		return xs.sort(comp);
	}

	public static <S extends Comparable<? super S>, T> UMap<S, T> sortByKeys(UMap<S, T> xs) {
		final Map<S, T> map = new LinkedHashMap<>();
		for (S x : sort(xs.keySet()))
			map.put(x, xs.get(x));
		return new UMap<>(map);
	}

	//	public static <S extends Comparable<? super S>> OList<S> checkSorted(UList<S> xs) {
	//		// fix some day so doesn't actually sort
	//		return new OList<>(xs);
	//	}

	public static <T extends Comparable<? super T>> boolean checkSorted(UList<T> xs) {
		T last = null;
		for (T x : xs)
			if (last == null)
				last = x;
			else if (last.compareTo(x) != -1)
				return false;
		return true;
	}

	public static <S extends Comparable<? super S>> OList<S> sortDistinct(UList<S> xs) {
		return new OList<>(xs.distinct());
	}

	public void printAll() {
		vals.forEach(x -> printf("%s%n", x));
	}

	public UList<T> sort(Comparator<T> comp) {
		ArrayList<T> idlst = new ArrayList<T>(vals);
		Collections.sort(idlst, comp);
		//		printf("SORTED: %s%n", idlst);
		UList<T> res = new UList<>(idlst);
		//		myassert(res.strictlyIncreasing(comp));
		return res;
	}

	// can't do this, because T isn't assumed to be comparable
	//	public OList<T> sorto(Comparator<T> comp) {
	//		return new OList<T>(this, comp);
	//	}

	//	public UList<T> sort(BiFunction<T, T, Integer> comp) {
	//		return sort(new Comparator<T>() {
	//			@Override
	//			public int compare(T e1, T e2) {
	//				return comp.apply(e1, e2);
	//			}
	//		});
	//	}

	public boolean strictlyIncreasing(Comparator<T> comp) {
		//		die("untested");
		for (int i = 0; i < size() - 1; ++i)
			if (comp.compare(get(i), get(i + 1)) == 1) {
				printf("BAD %s %s%n", get(i), get(i + 1));
				return false;
			}
		return true;
	}

	public UList<T> subList(int fromIndex, int toIndex) {
		return new UList<>(this, fromIndex, toIndex);
	}

	public UList<T> maybeSubList(int fromIndex, int toIndex) {
		if (fromIndex < 0 || toIndex < 0)
			return this;
		if (toIndex < 0)
			return subList(fromIndex);
		return subList(fromIndex, toIndex);
	}

	public boolean subset(UList<T> xs) {
		//return VRAUtils.subset(vals, xs.vals);
		return xs.vals.containsAll(vals);
	}

	public UList<T> subList(int fromIndex) {
		return new UList<>(this, fromIndex, vals.size());
	}

	/** returns some number of lists of size n (or n-1 for the leftovers) */
	public UList<UList<T>> partitionSz(int sz) {
		myassert(sz > 0);
		int vsz = vals.size();
		int n = vsz / sz;
		return rangeOpen(0, n).map(i -> subList(i * sz, i + 1 == n ? vsz : (i + 1) * sz));
	}

	/** returns n lists of roughly equal size */
	public UList<UList<T>> partition(int n) {
		myassert(n > 0);
		int vsz = vals.size();
		int sz = Math.max(1, vsz / n);
		return rangeOpen(0, n).map(i -> subList(i * sz, i + 1 == n ? vsz : (i + 1) * sz));
	}

	public boolean startsWith(UList<T> xs) {
		if (xs.size() > vals.size())
			return false;
		return vals.subList(0, xs.size()).equals(xs.vals);
	}

	public boolean checkSortedIncreasing(Comparator<T> comp) {
		T last = null;
		for (T x : vals) {
			if (last != null && comp.compare(last, x) != -1) {
				// printf("FAILED: %s%n %s%n", last, x);
				return false;
			}
			last = x;
		}
		return true;
	}

	public boolean checkSortedIncreasingByField(Function<T, Integer> field) {
		return checkSortedIncreasing(new Comparator<T>() {
			@Override
			public int compare(T e1, T e2) {
				// printf("TRY: %s%n %s%n", e1, e2);
				// return field.apply(e1) - field.apply(e2);
				// apparently it must return 1 or -1, not just any int.
				// the examples I saw on the web don't seem to do that.
				int i1 = field.apply(e1);
				int i2 = field.apply(e2);
				if (i1 < i2)
					return -1;
				if (i1 > i2)
					return 1;
				return 0;
			}
		});
	}

	// NUTS
	public boolean checkSortedNonDecreasing(Comparator<T> comp) {
		T last = null;
		for (T x : vals) {
			if (last != null && comp.compare(last, x) == 1) {
				printf("FAILED: %s%n %s%n", last, x);
				return false;
			}
			last = x;
		}
		return true;
	}

	public boolean checkSortedNonDecreasingByField(Function<T, Integer> field) {
		return checkSortedNonDecreasing(new Comparator<T>() {
			@Override
			public int compare(T e1, T e2) {
				// printf("TRY: %s%n %s%n", e1, e2);
				// return field.apply(e1) - field.apply(e2);
				// apparently it must return 1 or -1, not just any int.
				// the examples I saw on the web don't seem to do that.
				int i1 = field.apply(e1);
				int i2 = field.apply(e2);
				if (i1 < i2)
					return -1;
				if (i1 > i2)
					return 1;
				return 0;
			}
		});
	}

	public boolean contains(T x) {
		return set().contains(x);
	}

	public int indexOf(T x) {
		return vals.indexOf(x);
	}

	public UList<T> intersection(UList<T> s) {
		LinkedHashSet<T> inter = new LinkedHashSet<T>(vals);
		inter.retainAll(s.vals);
		return new UList<T>(inter);
	}

	public UList<T> orNull() {
		if (isEmpty())
			return null;
		return this;
	}

	public boolean intersects(UList<T> s2) {
		for (T x : vals)
			if (s2.contains(x)) {
				return true;
			}
		return false;
	}

	public static <T> boolean maybeContains(UList<T> xs, T x) {
		return xs == null || xs.contains(x);
	}

	public static <T> UList<T> listIntersection(UList<UList<T>> xs) {
		if (xs.isEmpty())
			return UList.empty();
		LinkedHashSet<T> inter = new LinkedHashSet<T>(xs.get(0).vals);
		for (int i = 1; i < xs.size(); ++i)
			inter.retainAll(xs.get(i).vals);
		return new UList<T>(inter);
	}

	public boolean setEqual(UList<T> s) {
		return set().equals(s.set());
	}

	public UMap<Boolean, UList<T>> partitioningBy(Predicate<? super T> predicate) {
		ArrayList<T> ts = new ArrayList<>();
		ArrayList<T> fs = new ArrayList<>();
		for (T x : vals)
			if (predicate.test(x))
				ts.add(x);
			else
				fs.add(x);
		Map<Boolean, UList<T>> map = new LinkedHashMap<>();
		map.put(true, new UList<>(ts));
		map.put(false, new UList<>(fs));
		return new UMap<>(map);
	}

	//	public <Key> Map<Key, UList<T>> groupingBy(Function<T, Key> fn) {
	//		Map<Key, List<T>> map = new LinkedHashMap<>();
	//		for (T x : vals) {
	//			Key key = fn.apply(x);
	//			List<T> xs = map.get(key);
	//			if (xs == null) {
	//				xs = new ArrayList<>();
	//				map.put(key, xs);
	//			}
	//			xs.add(x);
	//		}
	//		Map<Key, UList<T>> map2 = new LinkedHashMap<>();
	//		for (Map.Entry<Key, List<T>> entry : map.entrySet())
	//			map2.put(entry.getKey(), new UList<>(entry.getValue()));
	//		return unmodifiableMap(map2);
	//	}

	protected <Key, TL> UMap<Key, TL> ugroupingBy(Function<T, Key> fn, Function<List<T>, TL> constructor) {
		Map<Key, List<T>> map = new LinkedHashMap<>();
		for (T x : vals) {
			Key key = fn.apply(x);
			List<T> xs = map.get(key);
			if (xs == null) {
				xs = new ArrayList<>();
				map.put(key, xs);
			}
			xs.add(x);
		}
		Map<Key, TL> map2 = new LinkedHashMap<>();
		for (Map.Entry<Key, List<T>> entry : map.entrySet())
			map2.put(entry.getKey(), constructor.apply(entry.getValue()));
		return new UMap<>(map2);
	}

	public <Key> UMap<Key, UList<T>> groupingBy(Function<T, Key> fn) {
		return ugroupingBy(fn, xs -> new UList<T>(xs));
	}

	//	public <Key> UMap<Key, Integer> multiset(Function<T, UList<Key>> fn) {
	//		Map<Key, Integer> map = new LinkedHashMap<>();
	//		for (T x : vals)
	//			for (Key key : fn.apply(x))
	//				map.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
	//		return new UMap<>(map);
	//	}
	public UMap<T, Integer> multiset() {
		Map<T, Integer> map = new LinkedHashMap<>();
		for (T x : vals)
			map.compute(x, (k, v) -> (v == null) ? 1 : v + 1);
		return new UMap<>(map);
	}

	/** must be exactly one object per key */
	public <Key> UMap<Key, T> groupingByUnique(Function<T, Key> fn) {
		Map<Key, T> map = new LinkedHashMap<>();
		for (T x : vals) {
			Key key = fn.apply(x);
			myassert(key != null);
			T old = map.put(key, x);
			if (old != null)
				die("There was only supposed to be one object with key %s, but there are two: %s and %s%n", key, old,
						x);
		}
		return new UMap<>(map);
	}

	/** fn returns a list, not just an individual key */
	public <Key> UMap<Key, UList<T>> groupingByList(Function<T, UList<Key>> fn) {
		return groupingByList(fn, x -> new UList<>(x));
	}

	public <Key, TL> UMap<Key, TL> groupingByList(Function<T, UList<Key>> fn, Function<List<T>, TL> constructor) {
		Map<Key, List<T>> map = new LinkedHashMap<>();
		for (T x : vals) {
			for (Key key : fn.apply(x)) {
				List<T> xs = map.get(key);
				if (xs == null) {
					xs = new ArrayList<>();
					map.put(key, xs);
				}
				xs.add(x);
			}
		}
		Map<Key, TL> map2 = new LinkedHashMap<>();
		for (Map.Entry<Key, List<T>> entry : map.entrySet())
			map2.put(entry.getKey(), constructor.apply(entry.getValue()));
		return new UMap<>(map2);
	}

	// inefficient, doesn't matter
	public static <Key, T extends Comparable<? super T>> UMap<Key, OList<T>> orderedGroupingBy(UList<T> xs,
			Function<T, Key> fn) {
		UMap<Key, UList<T>> map = xs.groupingBy(fn);
		return new UMap<>(map.keySet(), x -> new OList<T>(map.get(x)));
	}

	public <T2> UMap<T, T2> mkmap(Function<T, T2> fn) {
		return mkumap(fn);
	}

	public <T2> UMap<T, T2> mkmapDropNulls(Function<T, T2> fn) {
		LinkedHashMap<T, T2> ys = new LinkedHashMap<>();
		for (T x : vals) {
			T2 y = fn.apply(x);
			if (y != null)
				ys.put(x, y);
		}
		return new UMap<>(ys);
	}

	public <R, Ex extends Exception> UMap<T, R> mkmapEx(ThrowingFunction<? super T, ? extends R, Ex> fn) throws Ex {
		LinkedHashMap<T, R> ys = new LinkedHashMap<>();
		for (T x : vals)
			ys.put(x, fn.apply(x));
		return new UMap<>(ys);
	}

	/** first arg is the 0-based iterator, second arg is the elem */
	public <T2> UMap<T, T2> mkmapI(BiFunction<Integer, ? super T, ? extends T2> fn) {
		LinkedHashMap<T, T2> ys = new LinkedHashMap<>();
		int i = 0;
		for (T x : vals)
			ys.put(x, fn.apply(i++, x));
		return new UMap<>(ys);
	}

	public <T2, Ex extends Exception> UMap<T, T2> mkmapIEx(ThrowingBiFunction<Integer, ? super T, ? extends T2, Ex> fn)
			throws Ex {
		LinkedHashMap<T, T2> ys = new LinkedHashMap<>();
		int i = 0;
		for (T x : vals)
			ys.put(x, fn.apply(i++, x));
		return new UMap<>(ys);
	}

	public <T2> UMap<T, T2> mkumap(Function<T, T2> fn) {
		LinkedHashMap<T, T2> ys = new LinkedHashMap<>();
		for (T x : vals)
			ys.put(x, fn.apply(x));
		return new UMap<>(ys);
	}

	public <K, V> UMap<K, V> mkmap2(Function<T, Pair<K, V>> fn) {
		LinkedHashMap<K, V> ys = new LinkedHashMap<>();
		for (T x : vals) {
			Pair<K, V> kv = fn.apply(x);
			ys.put(kv.left, kv.right);
		}
		return new UMap<>(ys);
	}

	public static <K, V> UMap<K, V> mkmapPair(UList<Pair<K, V>> lst) {
		LinkedHashMap<K, V> ys = new LinkedHashMap<>();
		for (Pair<K, V> kv : lst) {
			ys.put(kv.left, kv.right);
		}
		return new UMap<>(ys);
	}

	public <T2> UMap<T2, T> mkrevmap(Function<T, T2> fn) {
		LinkedHashMap<T2, T> ys = new LinkedHashMap<>();
		for (T x : vals) {
			T2 xval = nonNull(fn.apply(x), x);
			T oldval = ys.put(xval, x);
			myassert(oldval == null, x, xval, oldval);
		}
		return new UMap<>(ys);
	}

	public <T2> UMap<T2, T> mkrevmapList(Function<T, UList<T2>> fn) {
		LinkedHashMap<T2, T> ys = new LinkedHashMap<>();
		for (T x : vals)
			for (T2 xval : fn.apply(x)) {
				T oldval = ys.put(nonNull(xval), x);
				myassert(oldval == null, x, xval, oldval);
			}
		return new UMap<>(ys);
	}

	public <T2> UMap<T2, UList<T>> mkrevmapList2(Function<T, T2> fn) {
		LinkedHashMap<T2, UList<T>> ys = new LinkedHashMap<>();
		for (T x : vals) {
			T2 xval = nonNull(fn.apply(x), x);
			if (ys.containsKey(xval))
				ys.put(xval, ys.get(xval).add(x));
			else
				ys.put(xval, mkUList1(x));
		}
		return new UMap<>(ys);
	}

	public static int sum(UList<Integer> xs) {
		int exp = 0;
		for (int x : xs)
			exp += x;
		return exp;
	}

	public static double sumd(UList<Double> xs) {
		double exp = 0;
		for (double x : xs)
			exp += x;
		return exp;
	}

	public static int sumi(UList<Integer> xs) {
		return sum(xs);
	}

	public int sumi(Function<T, Integer> fn) {
		int exp = 0;
		for (T x : vals)
			exp += fn.apply(x);
		return exp;
	}

	public int sumi() {
		int exp = 0;
		for (T x : vals)
			if (x instanceof Integer)
				exp += (Integer) x;
			else
				die("This was supposed to be an int list");
		return exp;
	}

	public static double prodd(UList<Double> xs) {
		double exp = 1;
		for (double x : xs)
			exp *= x;
		return exp;
	}

	public static double prodi(UList<Integer> xs) {
		double exp = 1;
		for (double x : xs)
			exp *= x;
		return exp;
	}

	public Map<Boolean, T> mkboolmap(T trues, T falses) {
		LinkedHashMap<Boolean, T> ys = new LinkedHashMap<>();
		ys.put(true, trues);
		ys.put(false, falses);
		return unmodifiableMap(ys);
	}

	// for an Ordered UList, the value is ordered, of course, but unfortunately not
	// for the
	// default comparator.
	public UList<T> reverse() {
		ArrayList<T> union = new ArrayList<T>(vals);
		Collections.reverse(union);
		return mkUList(union);
	}

	public String join(String sep) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (T x : vals) {
			if (first)
				first = false;
			else
				sb.append(sep);
			sb.append(x);
		}
		return sb.toString();
	}

	public UList<UList<T>> perms() {
		if (size() <= 1)
			if (isEmpty())
				return mkUList1(UList.empty());
			else
				return mkUList1(this);
		UList<UList<T>> rval = flatMap(x -> remove(x).perms().map(l -> cons(x, l)));
		//		rval.assertEach(x -> x.size() == size());
		//		rval.assertEach(x -> setEqual(x));

		return rval;
	}

	public UList<T> randOrder() {
		return randElems(vals.size());
	}

	/** if seed==0, the list is returned as-is */
	public UList<T> randOrder(int seed) {
		if (seed == 0)
			return this;
		return randElems(vals.size(), seed);
	}

	private static Random randElemRand = new Random(0);

	public T randElem() {
		//https://stackoverflow.com/questions/4702036/take-n-random-elements-from-a-liste
		return vals.get(randElemRand.nextInt(vals.size()));
	}

	public UList<T> shuffle(Random rand) {
		List<T> list = new ArrayList<>(vals);
		Collections.shuffle(list, rand);
		return new UList<>(list);
	}

	public UList<T> randElems(int n) {
		//https://stackoverflow.com/questions/4702036/take-n-random-elements-from-a-liste
		List<T> list = new ArrayList<>(vals);
		Collections.shuffle(list);
		return new UList<>(list.subList(0, n));
	}

	public UList<T> randElems(int n, int seed) {
		Random rand = new Random(seed);
		//https://stackoverflow.com/questions/4702036/take-n-random-elements-from-a-liste
		List<T> list = new ArrayList<>(vals);
		Collections.shuffle(list, rand);
		return new UList<>(list.subList(0, n));
	}

	public UList<T> randElems(int n, Random rand) {
		//https://stackoverflow.com/questions/4702036/take-n-random-elements-from-a-liste
		List<T> list = new ArrayList<>(vals);
		Collections.shuffle(list, rand);
		return new UList<>(list.subList(0, n));
	}

	public static <T> UList<T> readList(Function<String, T> map, BufferedInputStream inp) {
		ArrayList<T> xs = new ArrayList<>();
		Scanner sc = new Scanner(inp);
		int n = sc.nextInt();
		xs.ensureCapacity(n);
		for (int i = 0; i < n; ++i)
			xs.add(map.apply(sc.next()));
		myassert(!sc.hasNext());
		sc.close();
		return mkUList(xs);
	}

	public void write(PrintStream out, boolean writeSize) {
		if (writeSize)
			out.println(vals.size());
		for (T x : vals)
			out.println(x.toString());
	}

	public void write(String pathname) {
		write(pathname, true);
	}

	public void write(String pathname, boolean writeSize) {
		PrintStream out = VRAFileUtils.newPrintStream(pathname);
		write(out, writeSize);
		out.close();
	}

	public static <K> UMap<K, Double> readDoubleMap(UMap<String, K> keymap, Scanner sc) {
		int n = sc.nextInt();
		Map<K, Double> map = new LinkedHashMap<>();
		for (int i = 0; i < n; ++i) {
			String ks = sc.next();
			String vs = sc.next();
			map.put(keymap.get(ks), Double.parseDouble(vs));
		}
		return new UMap<>(map);
	}

	public void dump(String fmt, Object... xs) {
		System_out.println(String.format(fmt, xs));
		for (int i = 0; i < vals.size(); ++i)
			printf("%2s: %s%n", i, vals.get(i));
	}

	public static <T> void dump_diffs(String msg, UList<T> xs, UList<T> ys) {
		println(msg);
		if (xs.size() != ys.size())
			printf(" DIFF SIZE: %s %s%n", xs.size(), ys.size());
		//		int n = Math.min(xs.size(), ys.size());
		//		for (int i = 0; i < n; ++i)
		//			if (xs.get(i) != ys.get(i))
		//				printf("%2s: %s %s%n", i, xs.get(i), ys.get(i));
		printf("xs.diff(ys): %s%n", xs.diff(ys));
		printf("ys.diff(xs): %s%n", ys.diff(xs));
	}

	public static final UList<Boolean> trueFalse = mkUList1(true, false);

	//	// causes eclipse import issues if in LinearExpr, don't understand why, don't care
	//	public static <T extends GSvar> LinearExpr newLinearExpr(UList<T> xs) {
	//		return new LinearExpr(xs.map(x -> x.coeffVar1));
	//	}

	//* output lists have one of choices, ntimes, so get all combos of choices
	public UList<UList<T>> allchoices(int ntimes) {
		GrowList<UList<T>> out = new GrowList<>();
		allchoicesAux(out, UList.empty(), ntimes);
		return out.elems();
	}

	private void allchoicesAux(GrowList<UList<T>> out, UList<T> prefix, int ntimes) {
		if (ntimes == 1)
			for (T x : vals)
				out.add(prefix.add(x));
		else
			for (T x : vals)
				allchoicesAux(out, prefix.add(x), ntimes - 1);
	}

	public static UList<OList<Integer>> allSubsets_int(int n, int subsetSz) {
		return allSubsets_int(rangeOpen(0, n), subsetSz);
	}

	private static UList<OList<Integer>> allSubsets_int(OList<Integer> rng, int subsetSz) {
		myassert(subsetSz > 0);
		int sz = rng.size();
		if (subsetSz > sz)
			return UList.empty();
		if (subsetSz == sz)
			return mkUList1(rng);
		if (subsetSz == 1)
			return rng.map(i -> mkOList1(i));

		//		printf("AS %s %s%n", subsetSz, size());
		UList<OList<Integer>> subs = allSubsets_int(rng, subsetSz - 1);

		return rng.flatMap(i -> subs.mapDropNulls(is -> is.some(j -> j <= i) ? null : is.insert(i)));
	}

	public UList<UList<T>> allSubsets(int subsetSz) {
		myassert(subsetSz <= size());
		return allSubsets_int(size(), subsetSz).map(lst -> lst.map(i -> get(i)));
	}

	//	public UList<OList<T>> allSubsets(int subsetSz) {
	//		return allSubsets_aux(subsetSz).map(x -> sort(x)).distinct();
	//	}
	// used by OList::allSubsets
	protected UList<UList<T>> allSubsets_aux(int subsetSz) {
		myassert(subsetSz > 0);
		if (subsetSz > size())
			return UList.empty();
		if (subsetSz == size())
			return mkUList1(this);
		if (subsetSz == 1)
			return map(x -> mkUList1(x));
		return flatMapI((i, x) -> subList(i + 1).allSubsets_aux(subsetSz)
				.concat(subList(i + 1).allSubsets_aux(subsetSz - 1).map(xs -> cons(x, xs))));
	}

	public static <T> UList<UList<T>> allSubseqs(UList<T> xs) {
		int n = xs.size();
		return cons(UList.empty(), rangeOpen(0, n).flatMap(i -> rangeClosed(i + 1, n).map(j -> xs.subList(i, j))));
	}

	/** all combinations of one element from each input set */
	public static <T> UList<UList<T>> allCombs(UList<UList<T>> sets) {
		if (sets.isEmpty())
			return UList.empty();
		if (sets.size() == 1)
			return sets.get(0).map(x -> mkUList1(x));
		UList<UList<T>> subcombs = allCombs(sets.subList(1));
		//		printf("NSUB %s%n", subcombs.size());
		return sets.get(0).flatMap(x -> subcombs.map(sc -> cons(x, sc)));
	}

	public static <S, T> UList<Pair<S, T>> allPairs(UList<S> xs, UList<T> ys) {
		return xs.flatMap(x -> ys.map(y -> new Pair<>(x, y)));
	}

	public static <T> UList<UList<T>> transpose(UList<UList<T>> xs) {
		if (xs.isEmpty())
			return xs;
		//		UList<Integer> rngi = rangeOpen(xs.size());
		//return rangeOpen(xs.first().size()).map(j -> rngi.map(i -> xs.get(j).get(i)));
		return rangeOpen(xs.first().size()).map(j -> xs.map(row -> row.get(j)));
	}

	//public static <T extends Comparable<T>> int chainCompare(UList<T> x0ul, UList<T> x1ul) {
	//	@Override
	//	public int compareTo(UList<T> x) {
	//		UList<T> x0ul = this;
	//		UList<T> x1ul = x;
	//
	//		List<T> x0 = x0ul.vals();
	//		List<T> x1 = x1ul.vals();
	//
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
}
