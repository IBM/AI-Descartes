// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

// Need '?' so we can use GSvar with its compareTo for subclasses
// https://stackoverflow.com/questions/2827585/what-is-super-t-syntax
public class OList<T extends Comparable<? super T>> extends UList<T> implements Comparable<OList<T>>
//, UListInt<T, OList<T>> 
{

	// for subtypes, avoids sorting, otherwise doesn't make sense
	protected OList(OList<T> xs) {
		super(xs);
	}

	//	public OList<T> getThis() {
	//		return this;
	//	}
	//
	//	public OList<T> construct(UList<T> vals) {
	//		return new OList<>(vals);
	//	}

	/** hack specifically for GSList
	 * As a special exception, I want vals to be null in this case, so that any use before initialization will cause an error. 
	 */
	protected OList(String fooie) {
		super(fooie);
	}

	protected OList(UList<T> xs, String fooie) {
		super(xs);
		// already sorted
		myassert(this.equals(new OList<>(xs)));
	}

	//	protected OList(IntStream xs, String fooie) {
	//		super(xs.toArray());
	//		// already sorted
	//		myassert(this.equals(new OList<Integer>(xs.toArray())));
	//	}

	public OList(List<T> xs, String fooie) {
		super(xs);
		// already sorted
		myassert(this.equals(new OList<>(xs)));
	}

	public OList(UList<T> xs) {
		super(xs.sort((x, y) -> x.compareTo(y)));
	}

	public OList(UList<T> xs, Comparator<T> comp) {
		super(xs.sort(comp));
	}

	// nuts
	public OList(T[] xs) {
		super(mkUList(xs).sort((x, y) -> x.compareTo(y)));
	}

	public OList(Collection<T> xs) {
		super(mkUList(xs).sort((x, y) -> x.compareTo(y)));
	}

	private OList(OList<T> xs, OList<T> ys) {
		super(merge(xs.vals(), ys.vals()));
		//		myassert(this.equals(mkUList(this.vals()).sort((x, y) -> x.compareTo(y))));
	}

	public OList<T> insert(T x) {
		return new OList<>(this, mkOList1(x));
	}

	@Override
	public int compareTo(OList<T> x) {
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

	public boolean strictlyIncreasing() {
		for (int i = 0; i < size() - 1; ++i)
			if (get(i).compareTo(get(i + 1)) == 1)
				return false;
		return true;
	}

	// in UList.java
	// right?
	//	@SafeVarargs
	//	public static <T extends Comparable<? super T>> OList<T> mkOList(T... xs) {
	//		return new OList<>(xs);
	//	}

	@SuppressWarnings("unchecked")
	public static <TT extends Comparable<? super TT>> OList<TT> emptyOUL() {
		return new OList<TT>(Collections.EMPTY_LIST);
	}

	public boolean subsetOL(OList<T> yl) {
		return size() == intersectionSize(yl);
	}

	public OList<T> filter(Predicate<T> pred) {
		return new OList<>(super.filter(pred), "already sorted");
	}

	public int intersectionSize(OList<T> yl) {
		List<T> ys = yl.vals();
		// final int n=Math.max(vals.size(), ys.size());
		final int m = size(), n = ys.size();
		if (n == 0)
			return 0;
		int i = 0, j = 0;
		int intsz = 0;
		while (i < m && j < n) {
			int comp = get(i).compareTo(ys.get(j));
			if (comp == 0) {
				intsz++;
				i++;
				// get dups
				//				j++;
			} else if (comp == -1) {
				i++;
			} else {
				j++;
			}
		}

		// slow, tested on some input, may still be buggy.
		if (listTests)
			myassert(intsz == this.slowIntersection(yl).size());

		return intsz;
	}

	final public static boolean listTests = false;

	public static int intersectionSizeInts(OList<Integer> xl, OList<Integer> yl) {
		List<Integer> xs = xl.vals();
		List<Integer> ys = yl.vals();

		final int m = xs.size(), n = ys.size();
		if (n == 0)
			return 0;
		int i = 0, j = 0;
		int intsz = 0;
		while (i < m && j < n) {
			int comp = Integer.compare(xs.get(i), ys.get(j));
			if (comp == 0) {
				intsz++;
				i++;
				j++;
			} else if (comp == -1) {
				i++;
			} else {
				j++;
			}
		}

		// slow, tested on some input, may still be buggy.
		// myassert(intsz == this.intersection(yl).size());

		return intsz;
	}

	//https://gist.github.com/louisbros/8514819
	//louisbros/MergeSort.java
	//	private static <T extends Comparable<? super T>> void merge(int low, int mid, int high, List<T> values,
	//			List<T> aux) {
	//
	//		int left = low;
	//		int right = mid + 1;
	//
	//		for (int i = low; i <= high; i++) {
	//			aux.set(i, values.get(i));
	//		}
	//
	//		while (left <= mid && right <= high) {
	//			values.set(low++, aux.get(left).compareTo(aux.get(right)) < 0 ? aux.get(left++) : aux.get(right++));
	//		}
	//
	//		while (left <= mid) {
	//			values.set(low++, aux.get(left++));
	//		}
	//	}

	public OList<T> merge(OList<T> ys) {
		return new OList<>(this, ys);
	}

	private static <T extends Comparable<? super T>> ArrayList<T> merge(List<T> xs, List<T> ys) {
		int xsz = xs.size();
		int ysz = ys.size();
		ArrayList<T> values = new ArrayList<>(xsz + ysz);
		int left = 0;
		int right = 0;

		while (left < xsz && right < ysz) {
			values.add(xs.get(left).compareTo(ys.get(right)) < 0 ? xs.get(left++) : ys.get(right++));
		}
		while (left < xsz)
			values.add(xs.get(left++));
		while (right < ysz)
			values.add(ys.get(right++));

		return values;
	}

	public OList<T> subListOL(int fromIndex) {
		return new OList<T>(subList(fromIndex, vals().size()), "already sorted");
	}

	// inefficient
	public OList<T> concat(OList<T> x) {
		return new OList<T>(this, x);
	}

	// inefficient
	public OList<T> diffOL(OList<T> x) {
		return new OList<T>(this.diff(x));
	}

	public OList<T> diffUL(UList<T> x) {
		return new OList<T>(this.diff(x));
	}

	public boolean disjoint(OList<T> xs) {
		return intersectionSize(xs) == 0;
	}

	public OList<T> slowIntersection(OList<T> s) {
		ArrayList<T> inter = new ArrayList<T>(s.vals());
		inter.retainAll(vals()); // can do faster someday, like intersectionSize
		return new OList<T>(inter, "already sorted");
	}

	public OList<T> intersection(OList<T> yl) {
		ArrayList<T> inter = new ArrayList<T>();

		List<T> ys = yl.vals();
		// final int n=Math.max(vals.size(), ys.size());
		final int m = size(), n = ys.size();
		if (n > 0) {
			int i = 0, j = 0;
			while (i < m && j < n) {
				int comp = get(i).compareTo(ys.get(j));
				if (comp == 0) {
					inter.add(get(i));
					i++;
					// get dups
					//					j++;
				} else if (comp == -1) {
					i++;
				} else {
					j++;
				}
			}
		}
		OList<T> rval = new OList<T>(inter, "already sorted");

		// reasonably tested 
		if (listTests)
			myassert(rval.equals(this.slowIntersection(yl)), this, yl, rval, this.slowIntersection(yl));

		return rval;
	}

	public OList<T> intersection(UList<T> s) {
		return new OList<T>(super.intersection(s));
	}

	// make faster someday
	public OList<T> diff(OList<T> s2) {
		return new OList<T>(super.diff(s2));
	}

	// make faster someday
	public OList<T> add(T x) {
		return new OList<>(super.add(x));
	}

	// make faster someday	
	public OList<T> distinct() {
		return sort(super.distinct());
	}

	public OList<T> checkDistinct() {
		// someday make faster
		super.checkDistinct();
		return this;
	}

	public <Key> UMap<Key, OList<T>> ogroupingBy(Function<T, Key> fn) {
		Map<Key, List<T>> map = new LinkedHashMap<>();
		for (T x : vals()) {
			Key key = fn.apply(x);
			List<T> xs = map.get(key);
			if (xs == null) {
				xs = new ArrayList<>();
				map.put(key, xs);
			}
			xs.add(x);
		}
		Map<Key, OList<T>> map2 = new LinkedHashMap<>();
		for (Map.Entry<Key, List<T>> entry : map.entrySet())
			map2.put(entry.getKey(), new OList<>(entry.getValue()));
		return new UMap<>(map2);
	}

	public UList<OList<T>> allSubsetsO(int subsetSz) {
		return allSubsets_aux(subsetSz).map(x -> sort(x)).distinct();
	}

	// nuts
	public UList<OList<T>> allSubsets() {
		return cons(OList.emptyOUL(), concatULists(rangeClosed(1, size()).map(sz -> allSubsetsO(sz))));
	}

	public static <TT extends Comparable<? super TT>, TL extends UList<TT>> UList<OList<TT>> allCombsOL(
			UList<TL> sets) {
		if (sets.isEmpty())
			return OList.emptyOUL();
		if (sets.size() == 1)
			return sets.get(0).map(x -> mkOList1(x));
		//		printf("ALLC %s%n", sets.subList(1));
		UList<OList<TT>> subcombs = OList.allCombsOL(sets.subList(1));
		return sets.get(0).flatMap(x -> subcombs.map(sc -> sc.add(x))).distinct();
	}

	/** filters out lists that have duplicate elements */
	//	public static <TEQ, TT extends Comparable<? super TT>, TL extends UList<TT>> UList<OList<TT>> allCombsOLareDistinct(
	//			UList<TL> sets, Function<T, TEQ> equivFn) {
	//		if (sets.isEmpty())
	//			return OList.emptyOUL();
	//		if (sets.size() == 1)
	//			return sets.get(0).map(x -> mkOList1(x));
	//		//		printf("ALLC %s%n", sets.subList(1));
	//		UList<OList<TT>> subcombs = OList.allCombsOLareDistinct(sets.subList(1), equivFn);
	//		return sets.get(0).flatMap(x -> subcombs.filter(scombs -> !scombs.contains(x)).map(sc -> sc.add(x))).distinct();
	//	}

}
