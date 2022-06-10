// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static java.util.Collections.*;
import static utils.UList.*;
import static utils.VRAUtils.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This primitive class allows us to easily save and retrieve simply-structured
 * data. The data saved isn't worth defining classes for, since it is never
 * operated on in that form; it is simply temporarily stored in these records in
 * order to later be used to update the 'real' data structures. It has the
 * flavor of a simplified DB or XML. There is some rudimentary type checking,
 * and organizing by tags. Records are returned in the same order they are
 * entered. The point is primarily to save and restore the data, not to
 * manipulate it in memory, although there are simple methods for retrieving
 * records grouped by tag.
 * 
 * I initially wrote this class to receive data by being called from Java, so I
 * stored the data as it was passed in, as Double, Integer, or String. On
 * retrospect, this seemed awkward; also, when I had to read data from an OPL
 * text .dat file, this turned out to be unsatisfactory, since I couldn't rely
 * on the double that I parse from the text file to produce an identical string
 * on output. This isn't all that important, of course, but I prefer to be able
 * to rely on a property like that. So, I ended up reimplementing it so that
 * everything is stored as strings. I sometimes still receive binary data (a
 * cplex CPSoln, for example), in which case I maintain a mapping from the
 * string representation to the double (no such mapping is needed for integers,
 * of course; I'm not bothering with octals or whatever). In principle, one
 * might have a combination where in one place we have 1.1 as text input, and in
 * another place we add a double that prints as 1.1 (but is not actually
 * identical to that), in which case we'd have an unremediable conflict. I'll
 * deal with that if it every actually occurs. We implement OPL string
 * arrays/sets with records of single strings; this simplifies things so far,
 * hopefully it won't cause problems.
 */
public class SimpleRecStore {

	private LinkedHashMap<String, Recs> allrs = newLinkedHashMap();
	private Map<String, Double> dobjs = newLinkedHashMap();

	/*
	 * tuple yield_history_tuple{ string hybrid; string site; };
	 * {yield_history_tuple} FeasYieldHistoryIndex=...;
	 * 
	 * We don't actually represent the tuple definition itself, just the set
	 * variable. Note that in this example the OPL doesn't actually say that the
	 * hybrid field must contain values that occur in the hybrid data variable; we
	 * add that information ourselves.
	 */
	// indexedBy.get(A) == [B,"",C] ==> field 0 of A occurs in B, field 2 of A
	// occurs in C
	// I only handle cases where B and C are a setof(string) data field.
	private Map<Recs, Map<Integer, Recs>> fieldsOf = newLinkedHashMap();

	// this states a constraint:
	// the items of field subtuple must be a subset of those in field supertuple
	// the point is that if we remove items from the store, that may entail
	// removing items from subtuple that correspond to items removed from
	// supertuple.
	static class TupleSubset {
		public final Recs subtuple;
		public final Recs supertuple;
		public final List<Integer> inds;

		private TupleSubset(SimpleRecStore store, String subnm, String supernm, List<Integer> inds) {
			this.subtuple = getNonNull(store.allrs, subnm);
			this.supertuple = getNonNull(store.allrs, supernm);
			this.inds = copy2unmodifiableList(inds);
		}
	};

	private Map<Recs, TupleSubset> tupleSubsets = newLinkedHashMap();

	// handles:
	// setof(string) hybrid=...;
	// setof(string) early_rm_hybrid=...;
	// Here, although it is not expressed directly in OPL, we know that
	// early_rm_hybrid
	// should be a subset of hybrid.
	// I only handle setof(string), since that's all I've seen so far.
	// I first thought that I could represent this using fieldsOf,
	// since I represent string sets using single-field recs;
	// however,
	// private Map<String,String> subsetOf=newLinkedHashMap();

	// corresponds to OPL: float arr[domainArr]=...;
	private Map<Recs, Recs> functionVar = newLinkedHashMap();

	// corresponds to OPL: float arr[domainArr]=...;
	public void addFunctionVar(String arr, String domainArr) {
		myassert(!fieldsOf.containsKey(arr));
		addMapOnce(functionVar, getNonNull(allrs, arr), getNonNull(allrs, domainArr));
	}

	private Map<Recs, List<IORec>> missingFieldRef(Set<IORec> recsToDrop) {
		Map<Recs, List<IORec>> danglingRefs = newLinkedHashMap();

		for (Recs ss : fieldsOf.keySet()) {
			Map<Integer, Recs> fieldRefs = fieldsOf.get(ss);
			for (int i : fieldRefs.keySet()) {
				Recs domainArr = fieldRefs.get(i);
				List<String> dss = domainArr.getStrings();
				for (IORec ssr : ss.recs)
					if (!dss.contains(ssr.objs.get(i)) || recsToDrop.contains(ssr))
						addMapList(danglingRefs, ss, ssr);
			}
		}

		for (TupleSubset ts : tupleSubsets.values()) {
			ArrayList<List<String>> rstrs = ts.supertuple.getRecsStrings(ts.inds);

			for (IORec ssr : ts.subtuple.recs)
				if (!rstrs.contains(ssr.objs) || recsToDrop.contains(ssr))
					addMapList(danglingRefs, ts.subtuple, ssr);
		}

		return danglingRefs;
	}

	public SimpleRecStore dropMissingRefs() {
		Set<IORec> recsToDrop = newLinkedHashSet();
		return dropMissingRefs(recsToDrop);
	}

	public SimpleRecStore dropMissingRefs(Set<IORec> recsToDrop) {
		myassert(dobjs.isEmpty()); // don't want to deal with this

		Map<Recs, List<IORec>> danglingRefs = missingFieldRef(recsToDrop);
		if (danglingRefs.isEmpty())
			return null;

		SimpleRecStore store = new SimpleRecStore();
		for (Recs rs : allrs.values()) {
			List<List<String>> recValues = newArrayList();

			if (functionVar.containsKey(rs)) {
				Recs domainVar = functionVar.get(rs);
				Set<IORec> badrecs = newLinkedHashSet();
				if (danglingRefs.get(domainVar) != null)
					badrecs = toSet(danglingRefs.get(domainVar));

				for (int i = 0; i < rs.recs.size(); ++i) {
					IORec ior = rs.recs.get(i);
					IORec dom_ior = domainVar.recs.get(i);
					if (!badrecs.contains(dom_ior))
						recValues.add(ior.objs);
				}
			} else {
				Set<IORec> badrecs = newLinkedHashSet();
				if (danglingRefs.get(rs) != null)
					badrecs = toSet(danglingRefs.get(rs));

				for (IORec ior : rs.recs)
					if (!badrecs.contains(ior))
						recValues.add(ior.objs);
			}

			// same operations as in LoadOPL
			if (rs.isStringSet) {
				die();
				//				store.addStringVar(rs.tag, concatAll(recValues), rs.isSet, rs.quotes.get(0));
			} else
				store.addRecVar(rs.tag, recValues, rs.isSet, rs.quotes);
		}

		for (Recs rcs : fieldsOf.keySet())
			store.addFields(rcs.tag, toMap(fieldsOf.get(rcs)));
		for (Recs rcs : functionVar.keySet())
			store.addFunctionVar(rcs.tag, functionVar.get(rcs).tag);

		for (TupleSubset ts : tupleSubsets.values())
			store.addTupleSubset(ts.subtuple.tag, ts.supertuple.tag, ts.inds);

		myassert(subset(recsToDrop, this.allrecs()));

		die(); // had to remove because concatAll is gone 
		//		myassert(subset(recsToDrop, concatAll(danglingRefs.values())));
		//		myassert(!intersects(concatAll(danglingRefs.values()), store.allrecs()));

		// assert store.dropMissingRefs() == null;
		SimpleRecStore store2 = store.dropMissingRefs();// no point in passing recsToDrop

		return store2 == null ? store : store2;
	}

	public void addFields(String arrnm, Object... args) {
		addFields(arrnm, toMap(args));
	}

	public void addTupleSubset(String sub, String superset, Integer... superinds) {
		addTupleSubset(sub, superset, toList(superinds));
	}

	private static <T> List<T> toList(T[] xs) {
		ArrayList<T> l = new ArrayList<T>();
		for (T x : xs)
			l.add(x);
		return l;
	}

	public void addTupleSubset(String sub, String superset, List<Integer> superinds) {
		addMapOnce(tupleSubsets, allrs.get(sub), new TupleSubset(this, sub, superset, superinds));
	}

	private Map<Integer, String> toMap(Object[] args) {
		Map<Integer, String> m = newLinkedHashMap();
		for (int j = 0; j < args.length; j += 2)
			addMapOnce(m, (Integer) args[j], (String) args[j + 1]);
		return m;
	}

	private Map<Integer, String> toMap(Map<Integer, Recs> m0) {
		Map<Integer, String> m = newLinkedHashMap();
		for (int j : m0.keySet())
			addMapOnce(m, j, m0.get(j).tag);
		return m;
	}

	public void addFields(String arrnm, Map<Integer, String> args) {
		Recs arr = assertNonNull(allrs.get(arrnm));
		myassert(!functionVar.containsKey(arrnm));
		Map<Integer, Recs> fieldTypes = newLinkedHashMap();
		for (int argi : args.keySet()) {
			String domainArrNm = args.get(argi);
			Recs domainArr = assertNonNull(allrs.get(domainArrNm));
			addMapOnce(fieldTypes, argi, domainArr);
		}
		addMapOnce(fieldsOf, arr, fieldTypes);
	}

	// corresponds to OPL: setof(string) arr=...;
	// where we know that in fact the only string values allowed are in domainArr
	public void addSubset(String arr, String domainArr) {
		myassert(allrs.containsKey(arr));
		myassert(allrs.containsKey(domainArr));
		myassert(allrs.get(arr).isStringSet);
		myassert(allrs.get(domainArr).isStringSet);
		addFields(arr, 0, domainArr);
	}

	private static class Recs {
		public final String tag;
		public final boolean isSet; // to represent OPL sets
		public final boolean isStringSet;
		private final Map<String, Class<?>> tags;
		private final ArrayList<IORec> recs;

		// this is for producing an OPL .dat file; I don't bother store it.
		// if you want to create an OPL file, do it in the same run that you read the
		// OPL.
		private final List<Boolean> quotes;

		public int numFields() {
			return recs.get(0).objs.size();
		}

		// Map<List<String>, Object> omap;
		// <T> T lookupObj(List<String> strs) {
		//
		// T to = omap.get(strs);
		// return omap.get(strs);
		// }
		Recs(String tag, LinkedHashMap<String, Class<?>> tags, boolean isSet) {
			this(tag, tags, isSet, null);
		}

		Recs(String tag, LinkedHashMap<String, Class<?>> tags, boolean isSet, List<Boolean> quotes) {
			this.tag = tag;
			// printf("NEW RECS %s\n", tag);
			this.tags = tags == null ? tags : unmodifiableMap(tags);
			this.isSet = isSet;
			this.isStringSet = false;
			this.recs = newArrayList();
			this.quotes = quotes == null ? quotes : copy2unmodifiableList(quotes);
		}

		// I forget why I made a special flag for isStringSet...
		Recs(String tag, boolean isSet, List<String> strs, SimpleRecStore store, boolean quote) {
			this.tag = tag;
			// printf("NEW RECS %s (SS)\n", tag);
			this.isSet = isSet;
			this.isStringSet = true;
			this.tags = null;
			ArrayList<IORec> recs = newArrayList();
			for (String s : strs)
				recs.add(new IORec(tag, newlist(s), store));
			// this.recs=unmodifiableList(recs);
			this.recs = recs;
			this.quotes = unmodifiableList(newlist(quote));
			if (isSet)
				myassert(strs.size() == toSet(strs).size());
		}

		public List<String> getStrings() {
			myassert(isStringSet);
			return getStrings(0);
		}

		public List<String> getStrings(int fieldno) {
			List<String> ss = newArrayList();
			for (IORec ior : recs)
				ss.add(ior.objs.get(fieldno));
			return ss;
		}

		public ArrayList<String> getStringsQuoted() {
			ArrayList<String> ss = newArrayList();
			for (IORec ior : recs)
				ss.add(enquote(ior.objs.get(0)));
			return ss;
		}

		public ArrayList<List<String>> getRecsStrings(List<Integer> inds) {
			ArrayList<List<String>> ss = newArrayList();
			for (IORec r0 : recs)
				ss.add(r0.getStrings(inds));
			return ss;
		}
	};

	// OPL string arrays represented using length-1 recs
	public List<String> getStrings(String nm) {
		return allrs.get(nm).getStrings();
	}

	public boolean hasTable(String nm) {
		return allrs.containsKey(nm);
	}

	public Set<String> getStringSet(String nm) {
		return toSet(getStrings(nm));
	}

	public List<Double> getDoubles(String nm) {
		List<Double> ds = newArrayList();
		for (String s : getStrings(nm))
			ds.add(toDouble(s));
		return ds;
	}

	// public Double getDouble(String s) { return dobjs.get(s); }
	// private static String addDobj(Map<String,Double> dobjs, double x) {
	//// String s = Double.toString(x);
	//// Double d = dobjs.get(s);
	//// if (d!=null && d != x) {
	//// s = String.format("%a", x);
	//// d = dobjs.get(s);
	//// if (d!=null && d != x)
	//// die("two conflicting floats! %f %s %f\n", x, s, d);
	//// }
	// String s = String.format("%a", x);
	// Double d = dobjs.get(s);
	// if (d!=null && d != x)
	// die("two conflicting floats! %f %s %f\n", x, s, d);
	// dobjs.put(s, x);
	// return s;
	// }

	public List<IORec> allrecs() {
		List<IORec> rs = newArrayList();
		for (Recs r : allrs.values())
			rs.addAll(r.recs);
		return rs;
	}

	LinkedHashMap<String, Map<String, Class<?>>> alltags() {
		LinkedHashMap<String, Map<String, Class<?>>> tags = newLinkedHashMap();
		for (Recs r : allrs.values())
			tags.put(r.tag, r.tags);
		return tags;
	}

	// private Map<List<String>, Object> objmap;
	// @SuppressWarnings("unchecked")
	// public <T> T lookupObj(List<String> fieldIds) {
	// return (T)(objmap.get(fieldIds));
	// }

	public ArrayList<IORec> getRecs(String tag) {
		Recs r = allrs.get(tag);
		return (r == null ? new ArrayList<IORec>() : r.recs);
	}

	public ArrayList<List<String>> getRecsStrings(String tag) {
		Recs r = allrs.get(tag);
		ArrayList<List<String>> ss = newArrayList();
		if (r != null)
			for (IORec r0 : r.recs)
				ss.add(r0.getStrings());
		return ss;
	}

	public IORec getRec(String tag) {
		ArrayList<IORec> rs = getRecs(tag);
		myassert(rs.size() == 1);
		return rs.get(0);
	}

	public void addRecVar(String nm, List<List<String>> items, boolean isSet, List<Boolean> quotes) {
		Recs r = new Recs(nm, null, isSet, quotes);
		for (List<String> xs : items)
			r.recs.add(new IORec(nm, xs, this));
		allrs.put(nm, r);
	}

	public void addStringVar(String nm, List<String> items) {
		addStringVar(nm, items, false, false);
	}

	public void addStringVar(String nm, List<String> items, boolean isSet, boolean quote) {
		Recs r = new Recs(nm, isSet, items, this, quote);
		allrs.put(nm, r);
	}

	// copied from old VRAUtils
	public static IORecBuilder newRec(String tag) {
		IORecBuilder rval = new IORecBuilder(tag);
		return rval;
	}

	public void addVar(String nm, double val) {
		newRec(nm).add("val", val).done(this);
	}

	public double getDouble(String nm) {
		return getRec(nm).getDouble("val");
	}

	// this is NOT used for reading OPL - it doesn't pass in quote info
	public IORec done(IORecBuilder rb) {
		String tag = rb.tag;
		Recs rs = allrs.get(tag);
		if (rs != null)
			myassert(rb.subtags.equals(rs.tags));
		else
			allrs.put(tag, rs = new Recs(tag, rb.subtags, rb.isSet));
		IORec r = new IORec(rb, this);

		assert r.objs.size() == rs.tags.size();
		rs.recs.add(r);

		return r;
	}

	// private LinkedHashMap<String, LinkedHashMap<String, Class<?>>> tags =
	// newLinkedHashMap();
	// private ArrayList<IORec> recs = newArrayList();

	/*
	 * public Set<String> tags() { return unmodifiableSet(tags.keySet()); }
	 * 
	 * public int numRecs() { return recs.size(); }
	 */

	/*
	 * public SimpleRecStore removeRecs(String... tags) { SimpleRecStore res = new
	 * SimpleRecStore(); res.tags = newLinkedHashMap(); res.tags.putAll(this.tags);
	 * // ?? ok to leave tags in??? ArrayList<IORec> recs = res.recs; List<String>
	 * taglist = newArrayList(); for (String s : tags) taglist.add(s); for (IORec
	 * rec : this.recs) if (!taglist.contains(rec.tag)) recs.add(rec); return res; }
	 */

	/*
	 * public interface RecStoreObj { SimpleRecStore toStore(); }
	 */

	/*
	 * public <S extends ObjId, T extends RecStoreObj> void addMap(String subtag,
	 * Map<S, T> xs) { newRec(subtag).addIds(subtag + "-keys",
	 * toList(xs.keySet())).done(); for (S x : xs.keySet())
	 * addResults(xs.get(x).toStore()); }
	 */

	/*
	 * public void addResults(SimpleRecStore res) { recs.addAll(res.recs); for
	 * (Map.Entry<String, LinkedHashMap<String, Class<?>>> entry :
	 * res.tags.entrySet()) { LinkedHashMap<String, Class<?>> newsubtags =
	 * entry.getValue(); LinkedHashMap<String, Class<?>> oldsubtags =
	 * tags.put(entry.getKey(), newsubtags); if (oldsubtags != null) // require for
	 * this simple implementation assert oldsubtags.equals(newsubtags); } };
	 */

	/*
	 * public void addResults(ArrayList<IORec> recs0) { recs.addAll(recs0); if
	 * (!recs0.isEmpty()) { SimpleRecStore res0 = recs0.get(0).result; for
	 * (Map.Entry<String, LinkedHashMap<String, Class<?>>> entry :
	 * res0.tags.entrySet()) { LinkedHashMap<String, Class<?>> newsubtags =
	 * entry.getValue(); LinkedHashMap<String, Class<?>> oldsubtags =
	 * tags.put(entry.getKey(), newsubtags); if (oldsubtags != null) // require for
	 * this simple implementation assert oldsubtags.equals(newsubtags); } } };
	 */

	/*
	 * private String getTagFromTable(String tag) { for (String tg : tags.keySet())
	 * if (tg.equals(tag)) return tg; return tag; // presumably won't match
	 * anything, ok }
	 */

	/*
	 * public ArrayList<IORec> getNotRecs(String tag) { return getRecs(tag, false);
	 * }
	 */

	// public ArrayList<IORec> getTypedRecs(String tag, Class<?> clz) {
	// ArrayList<IORec> rs = newArrayList();
	// for (IORec r : getRecs(tag, true))
	// if (r.
	// return
	// }

	/*
	 * public boolean hasTable(String tag) { return tags.containsKey(tag); }
	 */

	/*
	 * private ArrayList<IORec> getRecs(String tag, boolean match) { tag =
	 * getTagFromTable(tag);
	 * 
	 * ArrayList<IORec> rval = newArrayList(); for (IORec r : recs) if
	 * (r.tag.equals(tag) == match) rval.add(r); // nuts for (IORec r : recs)
	 * r.reset();
	 * 
	 * return rval; }
	 */

	/*
	 * private static Object getField(IORec r, String subtag, LinkedHashMap<String,
	 * Class<?>> tag_subtags) { Iterator<String> subtagIter =
	 * tag_subtags.keySet().iterator(); int i = 0; while (subtagIter.hasNext()) if
	 * (subtagIter.next().equals(subtag)) // can't remember if subtag can be
	 * normalized return r.objs.get(i); else i++; throw new
	 * NoSuchElementException(subtag); }
	 */

	/*
	 * public ArrayList<Object> getFieldOfRecs(String tag, String subtag) { //tag =
	 * getTagFromTable(tag); LinkedHashMap<String, Class<?>> tag_subtags =
	 * tags.get(tag);
	 * 
	 * ArrayList<Object> rval = newArrayList(); for (IORec r : recs) if
	 * (r.tag.equals(tag)) rval.add(getField(r, subtag, tag_subtags));
	 * 
	 * // nuts for (IORec r : recs) r.reset();
	 * 
	 * return rval; }
	 * 
	 * public static ArrayList<Object> getFieldOfRecs(ArrayList<IORec> recs, String
	 * subtag) { ArrayList<Object> rval = newArrayList(); if (recs.size() == 0)
	 * return rval; SimpleRecStore store = recs.get(0).result;
	 * 
	 * String tag = recs.get(0).tag; LinkedHashMap<String, Class<?>> tag_subtags =
	 * store.tags.get(tag);
	 * 
	 * for (IORec r : recs) if (r.tag.equals(tag)) rval.add(getField(r, subtag,
	 * tag_subtags));
	 * 
	 * // nuts for (IORec r : recs) r.reset();
	 * 
	 * return rval; }
	 */

	public void print(String pathname) throws IOException {
		// OutputStream out = new FileOutputStream(pathname);
		// print(out);
		PrintStream out = new PrintStream(pathname);
		// Writer objwr = new Writer(this, new TextWriter(out));
		// objwr.print();
		print(out);
		out.close();
	}

	public void printNoThrow(String pathname) {
		try {
			print(pathname);
		} catch (IOException e) {
			printf("COULDN'T OPEN %s%n", pathname);
		}
	}

	public void print(OutputStream out0) {
		// BufferedOutputStream out = new BufferedOutputStream(out0);
		// new Buffered new PrintStream(out0);
		// out = new DataOutputStream(fos);
		PrintStream out = new PrintStream(new BufferedOutputStream(out0));

		String nl = System.getProperty("line.separator");
		out.append("" + allrs.size()).append(nl);
		for (Map.Entry<String, Recs> entry : allrs.entrySet()) {
			out.append(entry.getKey()).append(" ");
			Recs rs = entry.getValue();
			if (rs.tags != null) {
				out.append(rs.tags.size() + " ");
				for (Map.Entry<String, Class<?>> subentry : rs.tags.entrySet()) {
					out.append(subentry.getKey()).append(" ");
					Class<?> clz = subentry.getValue();
					String clstr = clz == Double.class ? "double"
							: clz == Integer.class ? "int" : clz == String.class ? "string" : "???";
					out.append(clstr).append(" ");
				}
			}
			out.append(nl);
		}
		out.append("" + allrs.size()).append(nl);

		List<IORec> recs = allrecs();
		out.append("" + recs.size()).append(nl);
		for (IORec rec : recs) {
			out.append(rec.tag).append(" ");
			for (Object x : rec.objs) {
				if (x == null)
					// printf("NO VAL FOR %s: %s%n", rec.tag, rec.objs);
					out.append("<NULL> ");
				else {
					String str = x.toString();
					// assert str.length() > 0;

					// I'm not going to worry about whether this is parsable anymore...
					if (str.isEmpty())
						out.append("\"\" ");
					else {
						// for (Character c : str)white space match
						// for (int j = 0; j < str.length(); ++j) {
						// if (Character.isWhitespace(str.charAt(j)))
						// printf(">%s<%n", str);
						// assert !Character.isWhitespace(str.charAt(j));
						// }
						// assert !(str.contains(" "));
						out.append(str).append(" ");
					}
				}
			}
			// out.newLine();
			out.append(nl);
		}
		out.append("" + recs.size()).append(nl);
		out.flush(); // remember to flush the BUFFERED stream
		printf("done write%n");
	}

	// this reads from an ASCII file; I haven't used it in a while, it may not work
	// public void read(String pathname) throws FileNotFoundException {
	// BufferedInputStream inp = new BufferedInputStream(new
	// FileInputStream(pathname));
	// // it is CRUCIAL that the stream be buffered - it will by 10x slower
	// unbuffered
	// Scanner sc = new Scanner(inp);
	//
	// LinkedHashMap<String, LinkedHashMap<String, Class<?>>> tags =
	// newLinkedHashMap();
	//// ArrayList<IORec> recs = newArrayList();
	//
	// LinkedHashMap<String, String> normalizedTag = newLinkedHashMap();
	//
	// assert sc.hasNextInt();
	// int ntags = sc.nextInt();
	// for (int i = 0; i < ntags; ++i) {
	// String tag = sc.next();
	// int nsubtags = sc.nextInt();
	//
	// LinkedHashMap<String, Class<?>> subtags = newLinkedHashMap();
	// for (int j = 0; j < nsubtags; ++j) {
	// String subtag = sc.next();
	// String cltp = sc.next();
	// Class<?> clz = null;
	// if (cltp.equals("double"))
	// clz = Double.class;
	// else if (cltp.equals("int"))
	// clz = Integer.class;
	// else if (cltp.equals("string"))
	// clz = String.class;
	// else
	// die("bad class");
	// subtags.put(subtag, clz);
	// }
	// tags.put(tag, subtags);
	// normalizedTag.put(tag, tag);
	// }
	// int ntags2 = sc.nextInt();
	// assert ntags == ntags2;
	//
	// int nvals = sc.nextInt();
	// //printf("NVALS: %d%n", nvals);
	// for (int i = 0; i < nvals; ++i) {
	// String tag = sc.next();
	// LinkedHashMap<String, Class<?>> subtags = tags.get(tag);
	// if (subtags == null)
	// printf("NO SUBTAGS! %s%n", tag); // will soon fail
	// tag = normalizedTag.get(tag);
	// int nsubtags = subtags.size();
	// Iterator<Class<?>> iter = subtags.values().iterator();
	//// IORec rval = new IORec(this, tag);
	// IORecBuilder rval = new IORecBuilder(tag);
	// for (int j = 0; j < nsubtags; ++j) {
	// Class<?> clz = iter.next();
	// if (clz == Double.class)
	// // it turns out that just parsing the doubles in the input takes the vast
	// majority of time
	// rval.objs.add(sc.nextDouble());
	// else if (clz == Integer.class)
	// rval.objs.add(sc.nextInt());
	// else if (clz == String.class)
	// rval.objs.add(sc.next());
	// else
	// die("bad class");
	// }
	// this.done(rval);
	//// recs.add(rval);
	// }
	// int nvals2 = sc.nextInt();
	// assert nvals == nvals2;
	// assert !sc.hasNext();
	// sc.close();
	// printf("done read%n");
	// }

	public void printBin(String pathname) {
		try {
			OutputStream fos = new BufferedOutputStream(new FileOutputStream(pathname));
			DataOutputStream out = new DataOutputStream(fos);
			print(out);
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			printf("COULDN'T CREATE FILE TO WRITE RESULTS TO! %s%n", pathname);
		} catch (IOException e) {
			printf("COULDN'T WRITE RESULTS! %s%n", pathname);
		}
	}

	public void print(DataOutputStream out) throws IOException {
		// ObjectWriter objwr = new ObjectWriter(alltags(),allrecs(),
		// convertBatchLabels, this);
		// objwr.print(out,convertBatchLabels);
		Writer objwr = new Writer(this, new DataWriter(out));
		objwr.print();
		out.close();
	}

	public static SimpleRecStore readBinStore(String pathname) throws IOException {
		InputStream fos = new BufferedInputStream(new FileInputStream(pathname));
		DataInputStream in = new DataInputStream(fos);
		return new Reader(in).read();
	}

	/*
	 * public static Map<String, String> getMapS2S(List<IORec> recs, String
	 * key_subtag, String value_subtag) { Map<String, String> m =
	 * newLinkedHashMap(); for (IORec r : recs) addMapOnce(m,
	 * r.getString(key_subtag), r.getString(value_subtag)); return m; }
	 * 
	 * public static Map<String, List<String>> getMapS2SL(List<IORec> recs, String
	 * key_subtag, String value_subtag) { Map<String, List<String>> m =
	 * newLinkedHashMap(); for (IORec r : recs) addMapList(m,
	 * r.getString(key_subtag), r.getString(value_subtag)); return m; }
	 * 
	 * public Map<String, String> getMap(String tag) { Map<String, String> m =
	 * newLinkedHashMap(); for (IORec r : getRecs(tag)) { String s = (String)
	 * r.objs.get(0); String t = (String) r.objs.get(1); addMapOnce(m, s, t); }
	 * return m; } public Map<String, Double> getDMap(String tag) { Map<String,
	 * Double> m = newLinkedHashMap(); for (IORec r : getRecs(tag)) { String s =
	 * (String) r.objs.get(0); Double t = (Double) r.objs.get(1); addMapOnce(m, s,
	 * t); } return m; }
	 * 
	 * public <S, T> Map<S, T> getMap(String tag, Map<String, S> smap, Map<String,
	 * T> tmap) { Map<S, T> m = newLinkedHashMap(); for (IORec r : getRecs(tag)) {
	 * String s = (String) r.objs.get(0); String t = (String) r.objs.get(1);
	 * addMapOnce(m, assertNonNull(smap.get(s)), assertNonNull(tmap.get(t))); }
	 * return m; }
	 */

	/*
	 * public Collection<List<IORec>> getGroupedBy(String tag, String... subtags) {
	 * return getHMGroupedBy(tag, subtags).values(); }
	 * 
	 * public Map<String, List<IORec>> getMappedByString(String tag, String subtag)
	 * { Map<String, List<IORec>> groups = newLinkedHashMap(); for
	 * (Map.Entry<List<Object>, List<IORec>> entry : getHMGroupedBy(tag,
	 * subtag).entrySet()) groups.put((String) entry.getKey().get(0),
	 * entry.getValue()); return groups; }
	 */

	/*
	 * private List<Integer> getFieldInds(String tag, String... subtags) {
	 * List<Integer> fieldInds = newArrayList(); LinkedHashMap<String, Class<?>>
	 * tag_subtags = tags.get(tag); if (tag_subtags == null) return fieldInds;
	 * 
	 * //LinkedHashMap<String,Integer> subtagPos = newLinkedHashMap(); for (String
	 * subtag : subtags) { int i = 0; // keySet is a set, so in principle there is
	 * no order, but in fact LinkedHashSet does guarantee the order for (String
	 * subtag2 : tag_subtags.keySet()) { if (subtag.equals(subtag2))
	 * fieldInds.add(i); i++; } } assert fieldInds.size() == subtags.length; return
	 * fieldInds; }
	 */

	/*
	 * private int getFieldInd(String tag, String subtag) { LinkedHashMap<String,
	 * Class<?>> tag_subtags = tags.get(tag); if (tag_subtags == null) return -1; {
	 * int i = 0; // keySet is a set, so in principle there is no order, but in fact
	 * LinkedHashSet does guarantee the order for (String subtag2 :
	 * tag_subtags.keySet()) { if (subtag.equals(subtag2)) return i; i++; } }
	 * die("internal error - bad field: %s %s", tag, subtag); return 0; }
	 */

	/*
	 * public Map<List<Object>, List<IORec>> getHMGroupedBy(String tag, String...
	 * subtags) { LinkedHashMap<List<Object>, List<IORec>> groups =
	 * newLinkedHashMap(); List<Integer> fieldInds = getFieldInds(tag, subtags); if
	 * (fieldInds.isEmpty()) return groups; for (IORec rec : getRecs(tag))
	 * addMapList(groups, rec.getFields(fieldInds), rec); return groups; }
	 * 
	 * public Map<String, List<IORec>> getHMGroupedByString(String tag, String
	 * subtag) { LinkedHashMap<String, List<IORec>> groups = newLinkedHashMap(); int
	 * fieldInd = getFieldInd(tag, subtag); for (IORec rec : getRecs(tag))
	 * addMapList(groups, (String) rec.objs.get(fieldInd), rec); return groups; }
	 */

	/*
	 * public static Map<String, List<IORec>> getHMGroupedByString(List<IORec> recs,
	 * String subtag) { LinkedHashMap<String, List<IORec>> groups =
	 * newLinkedHashMap(); if (recs != null) for (IORec rec : recs)
	 * addMapList(groups, rec.getString(subtag), rec); return groups; }
	 * 
	 *//** given "batches","batchId" ==> "Bxyz" -> BIORec *//*
																														 * public Map<String, IORec> getHMGroupedByString1(String
																														 * tag, String subtag) { Map<String, IORec> groups =
																														 * newLinkedHashMap(); int fieldInd = getFieldInd(tag,
																														 * subtag); for (IORec rec : getRecs(tag))
																														 * addMapOnce(groups, (String) rec.objs.get(fieldInd), rec);
																														 * return groups; }
																														 */

	/*
	 * public List<Pair<IORec, List<IORec>>> getMainWithSubs(String maintag, String
	 * mainsubtag, String grouptag, String groupsubtag, InputErrorCode errorCode)
	 * throws InputError {
	 * 
	 * // e.g.: all the batch IORecs //List<Integer> main_fieldInds =
	 * getFieldInds(maintag, mainsubtag); //int main_fieldInd = getFieldInd(maintag,
	 * mainsubtag);
	 * 
	 * Map<String, IORec> mainObjs = getMappedByString1(maintag, mainsubtag);
	 * 
	 * // e.g.: all the activity recs, groups by batchId Map<String, List<IORec>>
	 * groups = getHMGroupedByString(grouptag, groupsubtag);
	 * 
	 * List<Pair<IORec, List<IORec>>> mainWithGroups = newArrayList();
	 * 
	 * for (String mainId : mainObjs.keySet()) { List<IORec> g = groups.get(mainId);
	 * if (g == null) inputError(errorCode, "main rec with no subs:  %s", mainId);
	 * 
	 * mainWithGroups.add(newPair(mainObjs.get(mainId), g)); }
	 * 
	 * if (!setEqual(mainObjs.keySet(), groups.keySet())) inputError(errorCode,
	 * "subs with no main: %s", diff(groups.keySet(), mainObjs.keySet()));
	 * 
	 * return mainWithGroups; }
	 */

	// public IORec newRec(String tag) {
	// IORec rval = new IORec(this, tag);
	// recs.add(rval);
	// return rval;
	// }

	/*
	 * public void defineSubtags(String tag, LinkedHashMap<String, Class<?>>
	 * subtagTypes) { assert tag != null; LinkedHashMap<String, Class<?>> prev =
	 * tags.put(tag, subtagTypes); assert prev == null; }
	 */

	public static class IORecBuilder {
		public final String tag;
		public final boolean isSet; // to represent OPL sets
		private ArrayList<Object> objs = newNullableArrayList();
		// ArrayList<String> subtags = newArrayList();
		// ArrayList<Class<?>> types = newArrayList();
		private LinkedHashMap<String, Class<?>> subtags;

		public IORecBuilder(String tag) {
			this(tag, false);
		}

		public IORecBuilder(String tag, boolean isSet) {
			this.tag = tag;
			this.objs = newArrayList();
			this.subtags = newLinkedHashMap();
			this.isSet = isSet;
		}

		private IORecBuilder checkTag(String subtag, Class<?> clz, Object x) {
			addMapOnce(subtags, subtag, clz);
			objs.add(x);
			return this;
		}

		// cheat - use int for boolean
		public IORecBuilder add(String subtag, boolean x) {
			return checkTag(subtag, Boolean.class, x);
		}

		// if we don't add this method, Java promotes the x arg to double, which
		// confuses me
		public IORecBuilder add(String subtag, long x) {
			assert (int) x == x;
			return add(subtag, (int) x);
		}

		public IORecBuilder add(String subtag, int x) {
			return checkTag(subtag, Integer.class, x);
		}

		public IORecBuilder add(String subtag, double x) {
			return checkTag(subtag, Double.class, x);
		}

		public IORecBuilder add(String subtag, String x) {
			return checkTag(subtag, String.class, x);
		}

		// this is the relic of an old syntax that I converted to a new style.
		// I should eventually get rid of it.
		public void done(SimpleRecStore store) {
			store.done(this);
		}
	}

	private String stringify(Object x) {
		myassert(x != null);
		if (x instanceof String)
			return (String) x;
		if (x instanceof Boolean)
			return x.equals(true) ? "T" : "F";
		if (x instanceof Integer)
			return x.toString();
		myassert(x instanceof Double);
		String s = String.format("%a", x);
		// addDoubles(rb.dobjs);
		Double d = dobjs.get(s);
		if (d != null && !d.equals(x))
			die("two conflicting floats! %f %s %f\n", x, s, d);
		dobjs.put(s, (Double) x);
		;
		return s;
	}

	public static class IORec {
		public final String tag;
		private final Map<String, Class<?>> subtagTypes;
		private final Map<String, String> objsByTag;
		private final UList<String> tags;
		private final List<String> objs;
		private final SimpleRecStore store;

		private void inv() {
			// for (String s : objs) myassert(s!=null);
		}

		// used by DATreader. no tags or types.
		private IORec(String tag, List<String> strs, SimpleRecStore store) {
			this.tag = tag;
			this.subtagTypes = null;
			this.objs = strs;
			this.tags = null;
			objsByTag = null;
			this.store = store;
			inv();
		}

		private IORec(IORecBuilder rb, SimpleRecStore store) {
			this(rb.tag, make_objsByTag(store, rb.objs), store, false);
		}

		private static List<String> make_objsByTag(SimpleRecStore store, List<Object> objs) {
			List<String> objsByTag = newArrayList();
			for (Object o : objs)
				objsByTag.add(store.stringify(o));
			return objsByTag;
		}

		// dummy boolean because of erasure issue with List<String>
		private IORec(String tag, List<String> objStrs, SimpleRecStore store, boolean fooie) {
			this.tag = tag;
			this.subtagTypes = store.allrs.get(tag).tags;

			if (subtagTypes == null) {
				this.objsByTag = null;
				this.tags = null;
			} else {
				Map<String, String> objsByTag = newLinkedHashMap();
				int n = 0;
				for (String subtag : subtagTypes.keySet())
					objsByTag.put(subtag, objStrs.get(n++));
				this.objsByTag = unmodifiableMap(objsByTag);
				this.tags = mkUList(objsByTag.keySet());
			}

			this.objs = objStrs;

			this.store = store;
			inv();
		}

		@Override
		public String toString() {
			return "IORec " + tag + " " + mkUList(objsByTag.values()) + subtagTypes;
		}

		// public List<Object> getFields(List<Integer> fieldInds) {
		// List<Object> rval = newArrayList();
		// for (int i : fieldInds)
		// rval.add(objs.get(i));
		// return rval;
		// }

		/*
		 * void checkTag(String subtag, Class<?> clz) { if (subtagTypes != null) {
		 * String st = subtagIter.next(); assert subtag.equals(st); } else
		 * definingSubtags.put(subtag, clz); }
		 * 
		 * // cheat - use int for boolean public IORec add(String subtag, boolean x) {
		 * return add(subtag, x ? 1 : 0); }
		 * 
		 * // if we don't add this method, Java promotes the x arg to double, which
		 * confuses me public IORec add(String subtag, long x) { assert (int) x == x;
		 * return add(subtag, (int) x); }
		 * 
		 * public IORec add(String subtag, int x) { checkTag(subtag, Integer.class);
		 * objs.add(x); return this; }
		 * 
		 * public IORec add(String subtag, double x) { checkTag(subtag, Double.class);
		 * objs.add(x); return this; }
		 * 
		 * public IORec add(String subtag, String x) { checkTag(subtag, String.class);
		 * objs.add(x); return this; }
		 * 
		 * public <T extends ObjId> IORec addIds(String subtag, List<T> xs) {
		 * checkTag(subtag, String.class); StringBuilder sb = new StringBuilder();
		 * boolean first = true; for (T x : xs) { if (first) first = false; else
		 * sb.append("/"); sb.append(x.id()); } objs.add(sb.toString()); return this; }
		 * 
		 * public <T extends ObjId> List<T> getIdList(String tag, Map<String, T> xmap) {
		 * List<T> xs = newArrayList(); String xids = getString(tag); for (String id :
		 * xids.split("/")) xs.add(assertNonNull(xmap.get(id))); return xs; }
		 */

		/*
		 * public void done() { if (definingSubtags != null) { result.defineSubtags(tag,
		 * definingSubtags); subtagTypes = definingSubtags; // subtags = new
		 * ArrayList<String>(); // for (String tp : subtagTypes.keySet()) //
		 * subtags.add(tp); definingSubtags = null; } reset(); }
		 */

		// USE THIS TO TEST IF A TAG EXISTS
		public Class<?> getTagType(String subtag) {
			return subtagTypes.get(subtag);
		}

		public boolean tagHashTypeInt(String subtag) {
			return subtagTypes.get(subtag) == Integer.class;
		}

		public boolean tagHashTypeString(String subtag) {
			return subtagTypes.get(subtag) == String.class;
		}

		public String getObject(String subtag, Class<?> clz) {
			final Class<?> subtp = subtagTypes.get(subtag);
			if (subtp != clz) {
				// The interface code still insists that times be double, even if they are
				// integral.
				// this hack permits them to be stored as int and fetched as double.
				// if (OptiDebugOpts.integralTimeHack &&
				// (subtag.equals("startTime") || subtag.equals("endTime")) &&
				// subtp == Integer.class && // stored as int
				// clz == Double.class) // needed as double
				// return (Double) (double) (Integer) (objsByTag.get(subtag));

				die("ERROR IN getObject TYPE: %s %s %s%n", subtag, clz.getName(),
						subtp == null ? null : subtp.getName());
				assert false;
			}
			return objsByTag.get(subtag);
		}

		private String checktag(String subtag, Class<?> clz) {
			final Class<?> subtp = subtagTypes.get(subtag);
			if (subtp != clz) {
				die("ERROR IN getObject TYPE: %s %s %s%n", subtag, clz.getName(),
						subtp == null ? null : subtp.getName());
				assert false;
			}
			return objsByTag.get(subtag);
		}

		private String checktag(int ind, Class<?> clz) {
			if (subtagTypes == null)
				return objs.get(ind);
			die(""); // for now
			return null;
		}

		public String getString(int ind) {
			return checktag(ind, String.class);
		}

		public String getString(String subtag) {
			return checktag(subtag, String.class);
		}

		public List<String> getStrings() {
			return copyList(objs);
		}

		public ArrayList<String> getStringsQuoted(Recs rs) {
			ArrayList<String> ss2 = newArrayList();
			assert objs.size() == rs.quotes.size();
			for (int i = 0; i < objs.size(); ++i) {
				String s = objs.get(i);
				boolean quoted = rs.quotes.get(i);
				ss2.add(quoted ? enquote(s) : s);
			}
			return ss2;
		}

		public double getDouble(String subtag) {
			return store.dobjs.get(checktag(subtag, Double.class));
		}

		public double getDouble(int ind) {
			return store.dobjs.get(checktag(ind, Double.class));
		}

		// public double getNonnegDouble(String subtag) {
		// double d = (Double) getObject(subtag, Double.class);
		// if (d < 0)
		// malformedInputError(MalformedInputErrorCode.NEGATIVE_VALUE, "negative value
		// for field %s", subtag);
		// return d;
		// }

		public int getInt(String subtag) {
			return toInt(checktag(subtag, Integer.class));
		}

		public int getInt(int ind) {
			return toInt(checktag(ind, Integer.class));
		}

		public ArrayList<String> getStrings(List<Integer> inds) {
			ArrayList<String> ss = newArrayList();
			for (int i : inds)
				ss.add(objs.get(i));
			return ss;
		}

		// public int getNonegInt(String subtag) {
		// int d = (Integer) getObject(subtag, Integer.class);
		// if (d < 0)
		// malformedInputError(MalformedInputErrorCode.NEGATIVE_VALUE, "negative value
		// for field %s", subtag);
		// return d;
		// }

		// public boolean getBool(String subtag) {
		// return (Boolean) getObject(subtag, Boolean.class);
		// }
	}

	// rcsy x y vlsz 10
	// x y 20
	// x z 30
	// rcsz y y 40
	//
	// for recnm=='rcsy', fieldind == 1, valsnm == 'vlsz',
	// we look up the recs for rcsy (here, the first three)
	// then for each distinct string from the column fieldind (here, the 1st, so 'y'
	// and 'z')
	// we sum the corresponding entries in the 'vlsz' array
	// so we have: y => 10+20
	// and: z => 30
	public Map<String, Double> dmap(String recnm, int fieldind, String valsnm) {
		// List<Double> surface_capacity = dr.getDoubles("surface_capacity");
		// List<Double> maxslackcapacity = dr.getDoubles("maxslackcapacity");
		// Map<String,Double> sc = newLinkedHashMap();
		// Map<String,Double> msc = newLinkedHashMap();
		// List<List<String>> recs = dr.getRecs("FeasLocIrrRowPatSite");
		// for (int i = 0; i<recs.size(); ++i) {
		// String sitenm = recs.get(i).get(4);
		// addMapDouble(sc, sitenm, surface_capacity.get(i));
		// addMapDouble(msc, sitenm, maxslackcapacity.get(i));
		// }
		List<Double> surface_capacity = getDoubles(valsnm);
		Map<String, Double> sc = newLinkedHashMap();
		List<IORec> recs = getRecs(recnm);
		for (int i = 0; i < recs.size(); ++i) {
			String sitenm = recs.get(i).getString(fieldind);
			myassert(sitenm != null);
			addMapDouble(sc, sitenm, surface_capacity.get(i));
		}
		// printf("dmap %s\n", toList(sc.keySet()));
		return sc;
	}

	/*
	 * private Comparator<IORec> getComparator(Object x, int fieldno) { if (x
	 * instanceof String) return new CompareRecString(fieldno); if (x instanceof
	 * Integer) return new CompareRecInteger(fieldno); if (x instanceof Double)
	 * return new CompareRecDouble(fieldno); printf("NO TYPE FOR %s: %s%n", fieldno,
	 * x.toString()); assert false; return null; }
	 */

	/*
	 * public void naturalRecSort(List<IORec> recs, String tag, String subtag) { if
	 * (recs.isEmpty()) return;
	 * 
	 * List<Integer> fieldInds = getFieldInds(tag, subtag); int fieldno =
	 * fieldInds.get(0); Object x = recs.get(0).objs.get(fieldno);
	 * Collections.sort(recs, getComparator(x, fieldno)); }
	 */

	/*
	 * static class CompareRecString implements Comparator<IORec> { public final int
	 * fieldno;
	 * 
	 * public CompareRecString(int fieldno) { this.fieldno = fieldno;
	 * printf("New String comparator%n"); }
	 * 
	 * @Override public int compare(IORec x, IORec y) { String xf = (String)
	 * x.objs.get(fieldno); String yf = (String) y.objs.get(fieldno); return
	 * xf.compareTo(yf); } };
	 * 
	 * static class CompareRecInteger implements Comparator<IORec> { public final
	 * int fieldno;
	 * 
	 * public CompareRecInteger(int fieldno) { this.fieldno = fieldno;
	 * printf("New Integer comparator%n"); }
	 * 
	 * @Override public int compare(IORec x, IORec y) { int xf = (Integer)
	 * x.objs.get(fieldno); int yf = (Integer) y.objs.get(fieldno); //return
	 * Integer.compare(xf, yf); return (xf < yf) ? -1 : ((xf == yf) ? 0 : 1); } };
	 * 
	 * static class CompareRecDouble implements Comparator<IORec> { public final int
	 * fieldno;
	 * 
	 * public CompareRecDouble(int fieldno) { this.fieldno = fieldno;
	 * printf("New Double comparator%n"); }
	 * 
	 * @Override public int compare(IORec x, IORec y) { double xf = (Double)
	 * x.objs.get(fieldno); double yf = (Double) y.objs.get(fieldno); //return
	 * Double.compare(xf, yf); return (xf < yf) ? -1 : ((xf == yf) ? 0 : 1); } };
	 */

	/*
	 * public <T extends ObjId> void storeIdSet(String tag, Set<T> xs) { for (T x :
	 * xs) newRec(tag).add("id", x.id()).done(); //newRec(tag).add("ids",
	 * ids(xs)).done(); }
	 * 
	 * public <T extends ObjId> Set<T> getIdSet(String tag, Map<String, T> xmap) {
	 * Set<T> xs = newLinkedHashSet(); for (IORec r : getRecs(tag))
	 * xs.add(assertNonNull(xmap.get(r.getString("id")))); return xs; }
	 * 
	 * public void printAsTxt(String tag, PrintStream out) { printHeader(tag, out);
	 * LinkedHashMap<String, Class<?>> tag_subtags = tags.get(tag);
	 * 
	 * final int ntags = tag_subtags.keySet().size(); int[] wdth = new int[ntags]; {
	 * int i=0; for (String subtag : tag_subtags.keySet()) wdth[i++] =
	 * subtag.length(); } for (IORec r : getRecs(tag)) { int i = 0; for (String
	 * subtag : tag_subtags.keySet()) { //if (r.objs.get(i) == null)
	 * printf("OOPS %s %s%n", subtag, i); //printf("%s %s %s%n",subtag, r.objs, i);
	 * Object x = r.objs.get(i); String s = x == null ? "" : x instanceof Double ?
	 * Integer.toString((int) (double) ((Double) x)) : x .toString(); wdth[i] =
	 * Math.max(wdth[i], s.length()); i++; } }
	 * 
	 * for (IORec r : getRecs(tag)) { int i = 0; out.print(" "); // for "#" in
	 * header for (Object x : r.objs) { int wd = wdth[i++]; String s = x == null ?
	 * "" : x instanceof Double ? Integer.toString((int) (double) ((Double) x)) : x
	 * .toString(); out.print(s); for (int j = 0; j < wd - s.length(); ++j)
	 * out.print(' '); if (i < ntags) out.print(' '); } out.println(); }
	 * out.flush(); }
	 */

	private void printAsCSV(Recs rcs, PrintStream out) {
		if (rcs.tags != null) {
			out.print("#");
			String comma = "";
			for (String tag : rcs.tags.keySet()) {
				out.print(comma);
				out.print(tag);
				comma = ",";
			}
			out.println();
		}

		for (IORec r : rcs.recs) {
			String comma = "";
			for (String x : r.objs) {
				out.print(comma);
				out.print(x);
				comma = ",";
			}
			out.println();
		}
		out.flush();
	}

	public void printAsCSV(String tag, PrintStream out) {
		printAsCSV(allrs.get(tag), out);
	}

	public void printAsCSV(String dir) throws FileNotFoundException {
		new File(dir).mkdirs();
		for (Recs r : allrs.values()) {
			PrintStream out = new PrintStream(dir + "/" + r.tag + ".csv");
			printAsCSV(r, out);
			out.close();
		}
	}

	private static abstract class FWriter {
		public abstract void writeInt(int n) throws IOException;

		public abstract void writeUTF(String n) throws IOException;

		public abstract void writeDouble(double n) throws IOException;

		public abstract void writeBoolean(boolean n) throws IOException;
	};

	private static class DataWriter extends FWriter {
		private final DataOutputStream out;

		private DataWriter(DataOutputStream out) {
			this.out = out;
		}

		@Override
		public void writeInt(int n) throws IOException {
			out.writeInt(n);
		}

		@Override
		public void writeUTF(String n) throws IOException {
			out.writeUTF(n);
		}

		@Override
		public void writeDouble(double n) throws IOException {
			out.writeDouble(n);
		}

		@Override
		public void writeBoolean(boolean n) throws IOException {
			out.writeBoolean(n);
		}
	};

	private static class TextWriter extends FWriter {
		private final PrintStream out;

		private TextWriter(PrintStream out) {
			this.out = out;
		}

		@Override
		public void writeInt(int n) throws IOException {
			out.printf("%d\n", n);
		}

		@Override
		public void writeUTF(String n) throws IOException {
			out.printf("%s\n", n);
		}

		@Override
		public void writeDouble(double n) throws IOException {
			out.printf("%f\n", n);
		}

		@Override
		public void writeBoolean(boolean n) throws IOException {
			out.printf("%s\n", n);
		}
	};

	private static class Writer {
		private final FWriter out;
		private final Map<String, Integer> allStringsMap;

		private final Map<String, Recs> allrs;
		private final Map<String, Double> dobjs;

		Writer(SimpleRecStore store, FWriter out) {
			this.out = out;
			this.allrs = store.allrs;
			this.dobjs = store.dobjs;

			LinkedHashMap<String, Integer> allStringsMap = newLinkedHashMap();

			// collect strings
			int n = 0;
			allStringsMap.put(null, n++); // NULL STRING - predefined, index 0
			for (Recs r : allrs.values())
				n = addString(r.tag, allStringsMap, n);
			for (Recs r : allrs.values())
				if (r.tags != null)
					n = addStrings(r.tags.keySet(), allStringsMap, n);
			for (Recs r : allrs.values())
				if (r.recs != null)
					for (IORec ior : r.recs)
						n = addStrings(ior.objs, allStringsMap, n);

			this.allStringsMap = unmodifiableMap(allStringsMap);
		}

		void print() throws IOException {

			// write string table
			{
				out.writeUTF("THE STRING TABLE:");
				out.writeInt(allStringsMap.size());
				int pos = 0;
				for (String str : allStringsMap.keySet()) {
					if (pos == 0) {
						assert str == null;
						assert allStringsMap.get(str) == 0;
					} else {
						out.writeUTF(str);
						assert allStringsMap.get(str) == pos; // because linked
					}
					pos++;
				}
				out.writeInt(allStringsMap.size()); // just a check
			}
			// printf("NSTRS: %d %n", allStringsMap.size());

			{
				out.writeUTF("THE DOUBLE TABLE:");
				out.writeInt(dobjs.size());
				for (String x : dobjs.keySet()) {
					out.writeUTF(x);
					out.writeDouble(dobjs.get(x));
				}
				out.writeInt(dobjs.size()); // check
			}

			out.writeUTF("THE RECORDS:");
			out.writeInt(allrs.size());
			// now write the actual records; doesn't have to be in the same order.
			for (Recs r : allrs.values())
				writeRecs(r);
			out.writeInt(allrs.size()); // check
		}

		private void writeRecs(Recs r) throws IOException {
			out.writeUTF("RECORD:");
			writeString(r.tag);
			out.writeBoolean(r.isSet);
			out.writeBoolean(r.isStringSet);
			// printf("W ISSTRING? %s %s\n",r.tag, r.isStringSet);
			out.writeBoolean(r.tags != null);
			if (r.tags != null) {
				out.writeInt(r.tags.size());
				;
				for (String tag : r.tags.keySet()) {
					writeString(assertNonNull(tag));
					Class<?> clz = r.tags.get(tag);
					out.writeInt(clz == String.class ? 0 : clz == Integer.class ? 1 : clz == Boolean.class ? 2 : 3);
				}
			}
			{
				// printf("writing %s %s\n", r.tag, r.recs.size());
				out.writeInt(r.recs.size());
				out.writeInt(r.recs.isEmpty() ? 0 : r.recs.get(0).objs.size());
				for (IORec ior : r.recs) {
					writeStrings(ior.getStrings());
				}
			}
		}

		private void writeStrings(Collection<String> ss) throws IOException {
			for (String s : ss)
				writeString(s);
		}

		private void writeString(String s) throws IOException {
			int pos = allStringsMap.get(s);
			assert pos >= 0;
			// if (intStrInds)
			out.writeInt(pos);
			// else
			// out.writeShort(pos);
		}

		private int addStrings(Collection<String> ss, LinkedHashMap<String, Integer> allStringsMap, int n) {
			for (String tag : ss)
				n = addString(tag, allStringsMap, n);
			return n;
		}

		private int addString(String tag, LinkedHashMap<String, Integer> allStringsMap, int n) {
			if (!allStringsMap.containsKey(tag))
				allStringsMap.put(tag, n++);
			return n;
		}
	}

	private static class Reader {
		private final DataInputStream in;
		private final Map<Integer, String> allStringsMap;

		private final SimpleRecStore store = new SimpleRecStore();

		Reader(DataInputStream in) throws IOException {
			this.in = in;

			{
				in.readUTF(); // header
				LinkedHashMap<Integer, String> allStringsMap = newLinkedHashMap();
				int sz = in.readInt();
				int pos = 0;
				allStringsMap.put(pos++, null);
				for (int i = 1; i < sz; ++i) {
					String str = in.readUTF();
					myassert(str != null);
					addMapOnce(allStringsMap, pos++, str);
				}
				int sz2 = in.readInt();
				myassert(sz == sz2); // check
				this.allStringsMap = unmodifiableMap(allStringsMap);
			}

			{
				in.readUTF(); // header
				Map<String, Double> dobjs = store.dobjs;
				int sz = in.readInt();
				for (int i = 0; i < sz; ++i) {
					String str = in.readUTF();
					double d = in.readDouble();
					addMapOnce(dobjs, str, d);
				}
				int sz2 = in.readInt();
				myassert(sz == sz2); // check
			}
		}

		SimpleRecStore read() throws IOException {
			in.readUTF(); // header
			int sz = in.readInt();
			// now write the actual records; doesn't have to be in the same order.
			for (int i = 0; i < sz; ++i) {
				readRecs();
			}
			int nrecs2 = in.readInt();
			myassert(sz == nrecs2);

			in.close();
			return store;
		}

		void readRecs() throws IOException {
			String header = in.readUTF(); // header
			myassert(header.equals("RECORD:"));
			String tag = readString();
			boolean isSet = in.readBoolean();
			boolean isStringSet = in.readBoolean();
			// printf("ISSTRING? %s %s\n",tag, isStringSet);
			LinkedHashMap<String, Class<?>> tags = null;
			if (in.readBoolean()) {
				tags = newLinkedHashMap();
				int ntags = in.readInt();
				// printf("READING %s tags %s\n", ntags, tag);
				for (int j = 0; j < ntags; ++j) {
					String rtag = readString();
					int clzind = in.readInt();
					// out.writeInt(clz==String.class?0:clz==Integer.class?1:clz==Boolean.class?2:3);
					Class<?> clz = clzind == 0 ? String.class
							: clzind == 1 ? Integer.class : clzind == 2 ? Boolean.class : Double.class;
					addMapOnce(tags, rtag, clz);
				}
			}

			{
				if (isStringSet) {
					// I have to read in the strings up front so that they can be passed to the
					// constructor,
					// since this is how string sets are created when parsing the OPL dat file.
					// clumsy.
					int nrecs = in.readInt();
					int nobjs = in.readInt();
					assert nobjs == 1;
					ArrayList<String> ss = newArrayList();
					for (int j = 0; j < nrecs; ++j) {
						List<String> objStrs = readStrings(nobjs);
						ss.add(objStrs.get(0));
					}
					Recs rs = new Recs(tag, isSet, ss, store, false);
					addMapOnce(store.allrs, tag, rs);
				} else {
					Recs rs = new Recs(tag, tags, isSet);
					addMapOnce(store.allrs, tag, rs);

					int nrecs = in.readInt();
					int nobjs = in.readInt();
					for (int j = 0; j < nrecs; ++j) {
						List<String> objStrs = readStrings(nobjs);
						// printf("reading %s %s\n", r.tag, r.recs.size());

						IORec r = new IORec(tag, objStrs, store, false);
						rs.recs.add(r);
					}
				}
			}
		}

		private List<String> readStrings(int n) throws IOException {
			List<String> ss = newArrayList();
			for (int i = 0; i < n; ++i)
				ss.add(readString());
			return ss;
		}

		private String readString() throws IOException {
			int pos = in.readInt();
			assert pos >= 0;
			// if (intStrInds)
			return allStringsMap.get(pos);
			// else
			// out.writeShort(pos);
		}
	};

	// public static SimpleRecStore readDAT(String pathname) throws IOException {
	// return LoadOPLdat.parseOPLdat(pathname);
	// }

	// private static String joinstr(List<String> ss, boolean keepQuotes) {
	// List<String> ss2 = newArrayList();
	// for (String s : ss)
	// ss2.add(keepQuotes ? s : "\"" + s + "\"");
	// return join(ss2, " ");
	// }
	private static ArrayList<String> mapstr(List<String> ss) {
		ArrayList<String> ss2 = newArrayList();
		for (String s : ss)
			ss2.add(enquote(s));
		return ss2;
	}

	public void printOPL(String pathname) throws IOException {
		PrintStream out = new PrintStream(pathname);

		for (String key : allrs.keySet()) {
			Recs rs = allrs.get(key);
			String brace = rs.isSet ? "{" : "[";
			String close = rs.isSet ? "}" : "]";
			out.printf("%s = %s ", key, brace);
			if (rs.isStringSet) {
				if (!rs.recs.isEmpty()) {
					// out.printf("%s", join(rs.getStrings(), " "));
					// out.printf("%s", joinstr(rs.getStrings(), false));
					out.printf("%s", join(rs.getStringsQuoted(), " "));
				}
			} else {
				if (!rs.recs.isEmpty()) {
					for (IORec r : rs.recs)
						out.printf("  <%s>\n", join(r.getStringsQuoted(rs), " "));
				}
			}
			out.printf("%s;\n", close);
		}
		out.close();
	}

	public static <T, TSet extends Iterable<T>> ArrayList<T> concatAll(UList<TSet> s) {
		ArrayList<T> union = new ArrayList<T>();
		for (TSet ts : s)
			for (T t : ts)
				union.add(t);
		return union;
	}

	public static <T> boolean subset(Collection<T> s1, Set<T> s2) {
		return s2.containsAll(s1);
	}

	public static <T> boolean subset(Collection<T> s1, Collection<T> s2) {
		return new LinkedHashSet<T>(s2).containsAll(s1);
	}
};
