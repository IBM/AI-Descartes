// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.UList.*;
import static utils.VRAUtils.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import utils.UList.ThrowingFunction;

public class UMap<K, V> {

	private final String nm;
	//	private final Map<S, T> map = new LinkedHashMap<>();
	protected Map<K, V> map = new LinkedHashMap<>();

	protected UMap(String foo) {
		this.map = null;
		this.nm = "";
	}

	public String toString() {
		return map.toString();
	}

	protected void init(UMap<K, V> map) {
		myassert(this instanceof DelayedInitUMap || this instanceof ComputeMap);
		myassert(this.map == null, "This map DelayedUMap has already been inited!");
		this.map = map.map;
		myassert(this.map != null);
	}

	public UMap(UList<K> xs, UList<V> ys) {
		this("", xs, ys);
	}

	public UMap(String nm, UList<K> xs, UList<V> ys) {
		myassert(xs.size() == ys.size());
		this.nm = nm;
		for (int i = 0; i < xs.size(); ++i)
			map.put(assertNonNull(xs.get(i)), assertNonNull(ys.get(i)));
	}

	public UMap(Map<K, V> map) {
		this("", map);
	}

	public UMap(String nm, Map<K, V> inmap) {
		this.nm = nm;
		myassert(inmap != null);
		myassert(!inmap.containsKey(null));
		for (K x : inmap.keySet())
			map.put(x, assertNonNull(inmap.get(x)));
	}

	public UMap(UList<K> xs, Function<K, V> fn) {
		this.nm = "";
		for (K x : xs)
			map.put(x, fn.apply(x));
	}

	// problem with overloading
	//	private UMap(ThrowingFunction<? super K, ? extends V, Exception> fn, UList<K> xs) throws Exception {
	//		this.nm = "";
	//		for (K x : xs)
	//			map.put(x, fn.apply(x));
	//	}

	private UMap(UList<K> xs, ThrowingFunction<? super K, ? extends V, Exception> fn) throws Exception {
		this.nm = "";
		for (K x : xs)
			map.put(x, fn.apply(x));
	}

	// problem with overloading
	public static <K, V> UMap<K, V> newUMap(UList<K> xs, ThrowingFunction<? super K, ? extends V, Exception> fn)
			throws Exception {
		return new UMap<>(xs, fn);
	}

	public static <SS, TT> UMap<SS, TT> empty() {
		final Map<SS, TT> map = new LinkedHashMap<>();
		return new UMap<>(map);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean isNotEmpty() {
		return !map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	public UMap<K, V> sortByKeys(Comparator<K> comp) {
		final Map<K, V> map = new LinkedHashMap<>();
		for (K x : keySet().sort(comp))
			map.put(x, get(x));
		return new UMap<>(map);
	}

	/** CLUMSY, COULD SWITCH ACTUAL VALUES AROUND */
	public UMap<K, V> sortByValues(Comparator<V> comp) {
		final Map<K, V> map = new LinkedHashMap<>();
		for (V v : values().sort(comp))
			for (K k : keySet())
				if (get(k).equals(v))
					map.put(k, v);
		return new UMap<>(map);
	}

	public <Z extends Comparable<? super Z>> UMap<K, V> sortByKeyField(Function<K, Z> kfn) {
		final Map<K, V> map = new LinkedHashMap<>();
		for (K x : keySet().sortByFieldAscending(kfn))
			map.put(x, get(x));
		return new UMap<>(map);
	}

	// THIS DIDN'T WORK FOR SANJEEB - figure out later
	//	public <Z extends Comparable<? super Z>> UMap<K, V> sortByKey() {
	//		return sortByKeyField(x -> x);
	//	}

	public static <SS, TT extends Comparable<? super TT>> UMap<SS, OList<TT>> sortValues(UMap<SS, UList<TT>> inmap) {
		final Map<SS, OList<TT>> map = new LinkedHashMap<>();
		for (Map.Entry<SS, UList<TT>> entry : inmap.map.entrySet())
			map.put(entry.getKey(), sort(entry.getValue()));
		return new UMap<>(map);
	}

	public <Z> UMap<K, Z> mapValues(Function<V, Z> fn) {
		final Map<K, Z> map = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : this.map.entrySet())
			map.put(entry.getKey(), nonNull(fn.apply(entry.getValue())));
		return new UMap<>(map);
	}

	public <Z> UMap<Z, V> mapKeys(Function<K, Z> fn) {
		final Map<Z, V> map = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : this.map.entrySet())
			map.put(nonNull(fn.apply(entry.getKey())), entry.getValue());
		myassert(map.keySet().size() == this.map.keySet().size()); // 1-1
		return new UMap<>(map);
	}

	public UMap<K, V> filterByValues(Predicate<V> fn) {
		final Map<K, V> map = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : this.map.entrySet())
			if (fn.test(entry.getValue()))
				map.put(entry.getKey(), entry.getValue());
		return new UMap<>(map);
	}

	public UMap<K, V> filterByKeys(Predicate<K> fn) {
		final Map<K, V> map = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : this.map.entrySet())
			if (fn.test(entry.getKey()))
				map.put(entry.getKey(), entry.getValue());
		return new UMap<>(map);
	}

	public <Z> UMap<K, Z> mapValues(BiFunction<K, V, Z> fn) {
		final Map<K, Z> map = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : this.map.entrySet())
			map.put(entry.getKey(), fn.apply(entry.getKey(), entry.getValue()));
		return new UMap<>(map);
	}

	public V maybeGet(K x) {
		myassert(x != null);
		return map.get(x);
	}

	public V get(K x) {
		myassert(x != null);
		V y = map.get(x);
		myassert(y != null, "null retval for " + nm + "@" + x);

		return y;
	}

	public V getOrElse(K x, V def) {
		myassert(x != null);
		V y = map.get(x);
		if (y == null)
			return def;
		return y;
	}

	public UList<V> getList(K[] xs) {
		List<V> ys = new ArrayList<>();
		for (K x : xs)
			ys.add(get(x));
		return mkUList(ys);
	}

	public UList<V> getList(UList<K> xs) {
		List<V> ys = new ArrayList<>();
		for (K x : xs)
			ys.add(get(x));
		return mkUList(ys);
	}

	public UList<V> getListOrElse(UList<K> xs, V def) {
		List<V> ys = new ArrayList<>();
		for (K x : xs)
			ys.add(getOrElse(x, def));
		return mkUList(ys);
	}

	public boolean containsKey(K x) {
		myassert(x != null);
		return map.containsKey(x);
	}

	public UList<K> keySet() {
		return mkUList(map.keySet());
	}

	public UList<V> values() {
		return mkUList(map.values());
	}

	public <Z> UList<Z> bimap(BiFunction<K, V, Z> fn) {
		List<Z> rvals = new ArrayList<>();
		for (Map.Entry<K, V> entry : map.entrySet())
			rvals.add(fn.apply(entry.getKey(), entry.getValue()));
		return mkUList(rvals);
	}

	public <Z> UList<Z> bimapFilter(BiFunction<K, V, Z> fn) {
		List<Z> rvals = new ArrayList<>();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			Z vl = fn.apply(entry.getKey(), entry.getValue());
			if (vl != null)
				rvals.add(vl);
		}
		return mkUList(rvals);
	}

	@FunctionalInterface
	public interface ThrowingBiConsumer<K, V, E extends Exception> {
		void accept(K k, V v) throws E;
	}

	public void forEachEntry(BiConsumer<K, ? super V> fn) {
		for (Map.Entry<K, V> entry : map.entrySet())
			fn.accept(entry.getKey(), entry.getValue());
	}

	public <Ex extends Exception> void forEachEntryEx(ThrowingBiConsumer<K, ? super V, Ex> fn) throws Ex {
		for (Map.Entry<K, V> entry : map.entrySet())
			fn.accept(entry.getKey(), entry.getValue());
	}

	public UMap<K, V> addEntry(K key, V val) {
		Map<K, V> map2 = new LinkedHashMap<>(map);
		map2.put(key, val);
		return new UMap<>(map2);
	}

	public UMap<K, V> remove(K key) {
		Map<K, V> map2 = new LinkedHashMap<>(map);
		map2.remove(key);
		return new UMap<>(map2);
	}

	public UMap<K, V> removeAll(UList<K> keys) {
		Map<K, V> map2 = new LinkedHashMap<>(map);
		keys.forEach(key -> map2.remove(key));
		return new UMap<>(map2);
	}

	public UMap<K, V> putAll(UMap<K, V> mp) {
		Map<K, V> rmp = new LinkedHashMap<>(map);
		rmp.putAll(mp.map);
		return new UMap<>(rmp);
	}

	public UMap<K, V> extend(UList<K> keys, V def) {
		if (keys.setEqual(keySet()))
			return this;
		Map<K, V> rmp = new LinkedHashMap<>(map);
		for (K k : keySet().diff(keys))
			rmp.put(k, def);
		return new UMap<>(rmp);

	}

	public UMap<K, V> intersect(UMap<K, V> mp) {
		Map<K, V> rmp = new LinkedHashMap<>();
		for (K key : keySet()) {
			V v = mp.maybeGet(key);
			if (v == get(key))
				rmp.put(key, v);
		}

		return new UMap<>(rmp);
	}

	public boolean equals(Object that) {
		if (this == that)
			return true;// if both of them points the same address in memory

		if (!(that instanceof UMap<?, ?>))
			return false; // if "that" is not a People or a childclass

		@SuppressWarnings("unchecked")
		UMap<K, V> thatx = (UMap<K, V>) that; // than we can cast it to People safely

		return this.map.equals(thatx.map);
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	private static <K, V> UMap<K, UList<V>> convert(Map<K, List<V>> m) {
		Map<K, UList<V>> rm = newLinkedHashMap();
		for (Map.Entry<K, List<V>> entry : m.entrySet())
			rm.put(entry.getKey(), mkUList(entry.getValue()));
		return new UMap<>(rm);
	}

	public UMap<V, K> invert() {
		Map<V, K> inv = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : map.entrySet())
			myassert(inv.put(entry.getValue(), entry.getKey()) == null);

		return new UMap<V, K>(inv);
	}

	public UMap<V, UList<K>> reverseMap() {
		Map<V, List<K>> rm = newLinkedHashMap();
		for (Map.Entry<K, V> entry : map.entrySet())
			addMapList(rm, entry.getValue(), entry.getKey());

		return convert(rm);
	}

	public static <K, V> UMap<V, UList<K>> reverseMapList(UMap<K, UList<V>> m) {
		Map<V, List<K>> rm = newLinkedHashMap();
		for (Map.Entry<K, UList<V>> entry : m.map.entrySet())
			for (V v : entry.getValue())
				addMapList(rm, v, entry.getKey());
		return convert(rm);
	}

	public static <K, V> UMap<V, K> reverseMapList1(UMap<K, UList<V>> m) {
		Map<V, K> rm = newLinkedHashMap();
		for (Map.Entry<K, UList<V>> entry : m.map.entrySet())
			for (V v : entry.getValue())
				addMapOnce(rm, v, entry.getKey());
		return new UMap<>(rm);
	}

	public void dump() {
		write(System_out);
	}

	public void dump(String msg) {
		printf(msg);
		println();
		write(System_out);
	}

	public void write(PrintStream out) {
		out.println(map.size());
		for (Map.Entry<K, V> entry : map.entrySet()) {
			String ks = entry.getKey().toString();
			String vs = entry.getValue().toString();
			if (out != System_out) {
				// for parsability
				myassert(!ks.contains(" "));
				myassert(!vs.contains(" "));
			}
			out.print(ks);
			out.print(" ");
			out.println(vs);
		}
	}

}
