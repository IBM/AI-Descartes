// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.UList.*;
import static utils.VRAUtils.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;

public class YamlOpts {
	//	private final Map<String, String> stringMap;
	//	private final Map<String, UList<String>> stringListMap;
	//	private final Map<String, UMap<String, String>> top_ss_maps;
	private final Map<String, Object> topMap;
	private final GrowList<String> keysRegistered = new GrowList<>();

	private final GrowList<DelayedInitUList<?>> delayedLists = new GrowList<>();
	private final GrowList<DelayedInitUMap<?, ?>> delayedMaps = new GrowList<>();

	public final String home = System.getProperty("user.home");

	/** format: CPSol */
	public final String outputDir; // = dirName("outputDir");

	protected String fileName(String s) {
		return fileName_aux(s, stringVal(s, ""));
	}

	protected String fileName_aux(String propnm, String s) {
		dump("AUX ", propnm, s, outputDir, home);
		String fname = s.replace("OUTPUTDIR", outputDir).replace("~", home);
		if (fname.startsWith("/") || fname.isEmpty())
			if (VRAFileUtils.fileExists(fname))
				return fname;
			else
				die("file %s not found!", fname);

		String nm = VRAFileUtils.fileExists(fname.replace("~", home), outputDir);
		printf("fileName %s %s %s%n", propnm, fname, nm);
		//return nm == null ? "" : nm;
		if (nm == null)
			die("file %s not found!", fname);
		return nm;
	}

	protected UList<String> fileNameRegexp(String propnm, String sval) {
		String fname = sval.replace("OUTPUTDIR", outputDir).replace("~", home);
		//		printf("FREG %s %s %s%n", fname, sval, fname.contains("*"));
		//		die();
		if (fname.contains("*")) {
			int slash = fname.lastIndexOf('/');
			if (slash >= 0)
				return VRAFileUtils.allFiles(fname.substring(0, slash), fname.substring(slash + 1));
			die("no regexp for dir yet.");
			return null;
		}
		return mkUList1(fileName_aux(propnm, sval));
	}

	protected String dirName(String s) {
		String nm = stringVal(s, "").replace("~", home); //replace("OUTPUTDIR", outputDir)
		return nm.endsWith("/") ? nm : nm + "/";
	}

	protected UList<String> fileNames(String s) {
		return stringList(s).flatMap(x -> fileNameRegexp(s, x));
	}

	public final void initDelayedVals() {
		delayedLists.elems().forEach(x -> x.init());
		delayedMaps.elems().forEach(x -> x.init());
	}

	private void registerName(String nm) {
		if (keysRegistered.checkAdd(nm))
			die("Yaml key %s has already been registered:  fix the java file that handles yaml.", nm);
	}

	protected void printOpts() {
		{
			String pathname = outputDir + "/opts-all.yaml";
			PrintStream out = VRAFileUtils.newPrintStream(pathname);
			for (String nm : topMap.keySet())
				out.printf("%s: %s%n", nm, topMap.get(nm));
			out.close();
		}
		{
			String pathname = outputDir + "/opts-non-default.yaml";
			PrintStream out = VRAFileUtils.newPrintStream(pathname);
			for (String nm : not_used_default.elems())
				out.printf("%s: %s%n", nm, topMap.get(nm));
			out.close();
		}
	}

	protected void checkRegistered() {
		UList<String> keysRegistered = this.keysRegistered.elems();

		for (String nm : topMap.keySet())
			if (!nm.equals("outputDir") && !keysRegistered.contains(nm) && !nm.startsWith("IGNORE_"))
				die("yaml key %s is not defined.%n", nm);

		if (not_used_default.currentSize() > 0) {
			printf("The following yaml items have non-default values:%n");
			for (String nm : not_used_default.elems())
				printf("  %s: %s%n", nm, topMap.get(nm));
		}

		// outputDir not created yet
		//	printOpts();
	}

	private static int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception x) {
			die("This was supposed to be an integer: %s", s);
			return 0;
		}
	}

	private static UList<UList<String>> getOpts(String[] args) {
		UList<UList<String>> opts = UList.empty();

		int i;
		for (i = 0; i < args.length; i++)
			// this is so that we can comment out yaml includes in eclipse run-configs
			if (args[i].equals("#--yaml")) {
				printf("IGNORING NEXT TWO ARGS: %s %s%n", args[i], args[i + 1]);
				i++;
			} else if (args[i].trim().endsWith(".yaml")) {
				opts = opts.add(mkUList1("", args[i]));
			} else if (args[i].trim().startsWith("--")) {
				String arg = args[i].substring(2);
				if (arg.equals("yaml")) {
					i++;
					opts = opts.add(mkUList1("", args[i]));
				} else if (arg.equals("yamlString")) {
					i++;
					opts = opts.add(mkUList1(args[i]));
				} else
					die("bad args: %s", args[i]);
			} else
				break;

		if (i + 1 == args.length) {
			if (args[i].endsWith("/")) {
				String dir = args[i];
				opts = mkUList1(mkUList1("infile: " + dir + "/input.dat")).concat(opts);
				opts = mkUList1(mkUList1("outputDir: " + dir + "/tmp")).concat(opts);
				opts = opts.add(mkUList1("", dir + "/opts.yaml"));
				printf("YAML OPTS from dir: %s %s%n", dir, opts);
				return opts;
			}
			opts = mkUList1(mkUList1("infile: " + args[i])).concat(opts);
			if (args[i].contains("/")) {
				int pos = args[i].lastIndexOf("/");
				opts = mkUList1(mkUList1("outputDir: " + args[i].substring(0, pos))).concat(opts);
			} else
				opts = mkUList1(mkUList1("outputDir: " + ".")).concat(opts);
		} else if (i + 2 == args.length) {
			opts = mkUList1(mkUList1("infile: " + args[i])).concat(opts);
			opts = mkUList1(mkUList1("outputDir: " + args[i + 1])).concat(opts);
		} else
			die("need one or two non-option args at the end of the command line (infile, output dir) or just (infile)");

		printf("YAML OPTS: %s%n", opts);

		//		printf("OPTS %s%n", opts);
		return opts;
	}

	public YamlOpts(String[] args) throws FileNotFoundException, YamlException {
		Map<String, Object> topmap = new LinkedHashMap<>();
		String outDir = null;

		for (UList<String> optx : getOpts(args)) {
			//			printf("Processing yamlx %s%n", optx);
			if (optx.first().equals("")) {
				readYamlFile(topmap, optx.get(1), outDir);
			} else if (optx.first().startsWith("outputDir:")) {
				myassert(outDir == null);
				outDir = optx.first().replaceAll("outputDir:", "").trim();
			} else {
				String str = optx.first();
				//				printf("Processing yaml string %s%n", str);

				YamlReader reader = new YamlReader(new StringReader(str));
				Object object = reader.read();
				System_out.println("YAML STRING " + str);
				System_out.println("YAML  " + object);
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) object;
				if (map == null) {
					printf("NO YAML OPTS IN STRING %s%n", str);
				} else {
					for (String s : map.keySet()) {
						if (topmap.containsKey(s))
							printf("WARNING: yaml key %s is being overridden in string %s.%n", s, str);

						topmap.put(s, map.get(s));
					}
					printf("YAML opts from string %s: %s%n", str, map);
				}
			}
		}
		//	if (fnames.size() > 1)
		//	printf("combined YAML opts: %s%n", topmap);

		this.outputDir = outDir;
		this.topMap = topmap;
	}

	private void readYamlFile(Map<String, Object> topmap, String fname, String outDir)
			throws FileNotFoundException, YamlException {
		String fex = VRAFileUtils.fileExists(fname.replace("~", home), outDir);
		if (fex == null)
			die("Can't find YAML option file %s", fname);
		YamlReader reader = new YamlReader(new FileReader(fex));

		UList<String> includes = null;
		//		printf("reading yaml %s%n", fname);
		Object object = reader.read();
		//			System_out.println(object);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) object;
		if (map == null) {
			printf("NO YAML OPTS IN FILE %s%n", fname);
		} else {
			for (String s : map.keySet())
				if (topmap.containsKey(s))
					printf("WARNING: yaml key %s is being overridden in file %s.%n", s, fname);

			//								String endMarker = "__END__";
			String ignoreMarker = "IGNORE_";
			for (String s : map.keySet())
				//					if (s.equals(endMarker)) {
				//						printf("IGNORING ALL yaml entries AFTER %s%n", endMarker);
				//						break;
				//					} else 
				if (s.startsWith(ignoreMarker))
					printf("IGNORING yaml entry %s%n", s);
				else if (s.equals("include"))
					if (includes == null) {
						if (map.get(s).equals(""))
							printf("(skipping empty include)%n");
						else if (!(map.get(s) instanceof List))
							die("'include' must be a list! %s", map.get(s));
						else {
							//						for (Object s1 : (List<?>) map.get(s))
							includes = mkUList((List<String>) map.get(s));
						}
					} else
						die("two includes in one yaml file!");
				else
					topmap.put(s, map.get(s));
			printf("YAML opts from %s: ", fname);
			for (String key : topmap.keySet()) {
				String vs = topmap.get(key).toString();
				if (vs.length() > 20)
					printf("%s=... ", key);
				else
					printf("%s=%s ", key, vs);
			}
			printf("%n");

			if (includes != null)
				for (String s : includes) {
					String ifname = fileName_aux("<include>", s);
					printf("reading yaml include %s%n", ifname);
					readYamlFile(topmap, ifname, outDir);
				}
		}
	}

	//	protected boolean boolVal(String nm, boolean defval) {
	//		registerName(nm);
	//		String s = stringMap.get(nm);
	//		if (s == null)
	//			return defval;
	//		else {
	//			//			printf("yaml %s <- %s%n", nm, s);
	//			boolean t = s.equalsIgnoreCase("true");
	//			boolean f = s.equalsIgnoreCase("false");
	//			myassert(t || f, s, "%nyaml property %s is supposed to have a boolean value but has this: %s", nm, s);
	//			return t;
	//		}
	//	}

	//	protected int intVal(String nm, int defval) {
	//		registerName(nm);
	//		String s = stringMap.get(nm);
	//		if (s == null)
	//			return defval;
	//		else
	//			try {
	//				//				printf("yaml %s <- %s%n", nm, s);
	//				return Integer.parseInt(s.trim());
	//			} catch (NumberFormatException x) {
	//				die("%nyaml property %s is supposed to have an integer value but has this: >%s<", nm, s);
	//				return defval;
	//			}
	//	}
	//
	//	protected double doubleVal(String nm, double defval) {
	//		registerName(nm);
	//		String s = stringMap.get(nm);
	//		if (s == null)
	//			return defval;
	//		else
	//			try {
	//				//				printf("yaml %s <- %s%n", nm, s);
	//				return Double.parseDouble(s.trim());
	//			} catch (NumberFormatException x) {
	//				die("%nyaml property %s is supposed to have an integer value but has this: >%s<", nm, s);
	//				return defval;
	//			}
	//	}

	public final GrowList<String> not_used_default = new GrowList<>(true);

	// could probably do this with just Function<>
	public abstract class YamlObj<T> {
		public abstract T conv(String propLoc, Object x);

		//		public abstract T defval();

		public final T conv(String propLoc, Object x, T defval) {
			if (x == null) {
				if (defval == null) {
					//					die("%nyaml property %s is supposed to have a boolean value but is null", propLoc);
					die("%nyaml property %s is supposed to have a value but is null", propLoc);
					return null;
				} else
					return defval;
			} else {
				T rval = conv(propLoc, x);
				if (!rval.equals(defval))
					not_used_default.add(propLoc);
				return rval;
			}
		}

		public final T convOpt(String propLoc, Object x) {
			if (x == null) {
				return null;
			} else {
				T rval = conv(propLoc, x);
				myassert(rval != null);
				not_used_default.add(propLoc);
				return rval;
			}
		}
	};

	public class YamlBool extends YamlObj<Boolean> {
		public Boolean conv(String propLoc, Object x) {
			if (x instanceof String) {
				String s = (String) x;
				boolean t = s.equalsIgnoreCase("true");
				boolean f = s.equalsIgnoreCase("false");
				myassert(t || f, s, "%nyaml property %s is supposed to have a boolean value but has this: %s", propLoc,
						s);
				return t;
			} else {
				die("%nyaml property %s is supposed to have an bool value but has this: >%s<", propLoc, x);
			}
			return true;
		}
	};

	public class YamlInt extends YamlObj<Integer> {
		public Integer conv(String propLoc, Object x) {
			if (x instanceof String)
				try {
					return Integer.parseInt((String) x);
				} catch (NumberFormatException ex) {
					die("%nyaml property %s is supposed to have an integer value but has this: >%s<", propLoc, x);
				}
			else {
				die("%nyaml property %s is supposed to have an integer value but has this: >%s<", propLoc, x);
			}
			return 0;
		}
	};

	public class YamlDouble extends YamlObj<Double> {

		public Double conv(String propLoc, Object x) {
			if (x instanceof String)
				try {
					return Double.parseDouble(((String) x).trim());
				} catch (NumberFormatException ex) {
					die("%nyaml property %s is supposed to have an double value but has this: >%s<", propLoc, x);
					return 0.0;
				}
			else {
				die("%nyaml property %s is supposed to have an integer value but has this: >%s<", propLoc, x);
			}
			return 0.0;
		}
	};

	public class YamlString extends YamlObj<String> {
		public String conv(String propLoc, Object x) {
			if (x instanceof String)
				return (String) x;
			else {
				die("%nyaml property %s is supposed to have an integer value but has this: >%s<", propLoc, x);
				return "";
			}
		}
	};

	private class YamlFileName extends YamlObj<String> {
		//		final String outDir;
		//
		//		YamlFilename(String outDir) {
		//			this.outDir = outDir;
		//		}

		public String conv(String propLoc, Object x) {
			if (x instanceof String) {
				String home = System.getProperty("user.home");
				String fname = (String) x;
				if (fname == null || fname.isEmpty())
					return "";
				return VRAFileUtils.fileExists(fname.replace("~", home), outputDir);
			} else {
				die("%nyaml property %s is supposed to have an integer value but has this: >%s<", propLoc, x);
				return "";
			}
		}
	};

	public class YamlLookup<T> extends YamlObj<T> {
		public final Function<String, T> lookup;

		public YamlLookup(Function<String, T> lookup) {
			this.lookup = lookup;
		}

		public YamlLookup(UMap<String, T> lookup) {
			this.lookup = x -> lookup.get(x);
		}

		public T conv(String propLoc, Object x) {
			if (x instanceof String)
				return lookup.apply((String) x);
			else {
				die("%nyaml property %s is supposed to have an lookup-able value but has this: >%s<", propLoc, x);
				return null;
			}
		}
	};

	public class YamlIntRange extends YamlObj<IntRange> {
		public IntRange conv(String propLoc, Object x) {
			if (x instanceof String) {
				UList<Integer> is = split((String) x, ",").map(s1 -> parseInt(s1));
				myassert(is.size() == 2, "There were supposed to be exactly two ints for yaml %s: %s", propLoc, x);
				return new IntRange(is.get(0), is.get(1));
			} else {
				die("%nyaml property %s is supposed to have an IntRagen value but has this: >%s<", propLoc, x);
				return null;
			}
		}
	};

	public class YamlList<T> extends YamlObj<UList<T>> {
		public final YamlObj<T> acc;

		public YamlList(YamlObj<T> acc) {
			this.acc = acc;
		}

		public UList<T> conv(String propLoc, Object s) {
			if (s == null)
				return UList.empty();
			else if (s instanceof List) {
				//				printf("yaml %s <- %s%n", nm, s);
				List<T> xs = new ArrayList<>();
				for (Object s1 : (List<?>) s)
					xs.add(acc.conv(propLoc, s1));
				return UList.mkUList(xs);
			} else if (s.equals("")) {
				return UList.empty();
			} else {
				//printf("s==>%s<%n", s);
				die("yaml property %s is supposed to be a list, but was this: %s (%s)", propLoc, s, s.getClass());
				return UList.empty();
			}
		}

	};

	protected <T> DelayedInitUList<T> delayedListOf(String nm, YamlObj<T> yobj) {
		registerName(nm);

		DelayedInitUList<T> dlist = new DelayedInitUList<T>(() -> new YamlList<>(yobj).conv(nm, topMap.get(nm)), nm);

		delayedLists.add(dlist);
		return dlist;
	}

	//	public  class DI_YamlList<T> extends YamlObj<UList<T>> {
	//		public final YamlObj<T> acc;
	//		public final String propLoc;
	//		public final Object x;
	//
	//		public DI_YamlList(YamlObj<T> acc, String propLoc, Object s) {
	//			this.acc = acc;
	//			this.propLoc = propLoc;
	//			this.x = s;
	//		}
	//
	//		public DelayedInitUList<T> conv(String propLoc, Object s) {
	//			return new DelayedInitUList<>(() -> {
	//				if (s == null)
	//					return DelayedInitUList.empty();
	//				else if (s instanceof List) {
	//					//				printf("yaml %s <- %s%n", nm, s);
	//					List<T> xs = new ArrayList<>();
	//					for (Object s1 : (List<?>) s)
	//						xs.add(acc.conv(propLoc, s1));
	//					return DelayedInitUList.mkUList(xs);
	//				} else {
	//					die("yaml property %s is supposed to be a list, but was this: ", propLoc, s);
	//					return DelayedInitUList.empty();
	//				}
	//			});
	//		}
	//	};

	public class YamlMap<K, V> extends YamlObj<UMap<K, V>> {
		public final YamlObj<K> getKey;
		public final YamlObj<V> getValue;

		public YamlMap(YamlObj<K> getKey, YamlObj<V> getValue) {
			this.getKey = getKey;
			this.getValue = getValue;
		}

		public UMap<K, V> conv(String propLoc, Object s) {
			if (s == null)
				return UMap.empty();
			else if (s instanceof Map) {
				//				printf("yaml %s <- %s%n", nm, s);
				Map<K, V> mp = new LinkedHashMap<>();
				for (Map.Entry<?, ?> entry : ((Map<?, ?>) s).entrySet())
					mp.put(getKey.conv(propLoc, entry.getKey()), getValue.conv(propLoc, entry.getValue()));
				return new UMap<>(mp);
			} else {
				die("yaml property %s is supposed to be a map, but was this: ", propLoc, s);
				return UMap.empty();
			}
		}
	};

	protected YamlBool yamlBool = new YamlBool();
	protected YamlInt yamlInt = new YamlInt();
	protected YamlDouble yamlDouble = new YamlDouble();
	protected YamlObj<IntRange> yamlIntRange = new YamlIntRange();
	protected YamlObj<String> yamlString = new YamlString();

	protected YamlObj<String> yamlFileName = new YamlFileName();

	protected YamlObj<UList<Integer>> yamlIntList = new YamlList<>(yamlInt);

	protected boolean boolVal(String nm, boolean defval) {
		registerName(nm);
		return yamlBool.conv(nm, topMap.get(nm), defval);
	}

	protected int intVal(String nm, int defval) {
		registerName(nm);
		return yamlInt.conv(nm, topMap.get(nm), defval);
	}

	protected Integer nonnegIntegerVal(String nm) {
		registerName(nm);
		return yamlInt.convOpt(nm, topMap.get(nm));
	}

	protected double doubleVal(String nm, double defval) {
		registerName(nm);
		return yamlDouble.conv(nm, topMap.get(nm), defval);
	}

	protected double doubleVal(String nm, double defval, double lbnd, double ubnd) {
		double x = doubleVal(nm, defval);
		if (!(lbnd <= x))
			die("yaml opt %s too small: %s < %s", nm, x, lbnd);
		else if (!(x <= ubnd))
			die("yaml opt %s too large: %s < %s", nm, ubnd, x);
		return x;
	}

	//	protected <T> Optional<T> optionalOf(String nm, YamlObj<T> yobj) {
	//		registerName(nm);
	//		return new YamlList<>(yobj).conv(nm, topMap.getOrDefault(nm, def));
	//		return listOf(nm, yobj, null);
	//	}

	protected <T> UList<T> listOf(String nm, YamlObj<T> yobj) {
		return listOf(nm, yobj, null);
	}

	protected <T> UList<T> listOf(String nm, YamlObj<T> yobj, List<String> def) {
		registerName(nm);
		return new YamlList<>(yobj).conv(nm, topMap.getOrDefault(nm, def));
	}

	protected <T> UList<T> sa_listOf(String nm, YamlObj<T> yobj) {
		registerName(nm);
		return new YamlList<>(yobj).conv(nm, topMap.get(nm));
	}

	protected UList<Integer> intList(String nm) {
		return listOf(nm, yamlInt);
	}

	protected UList<Double> doubleList(String nm, Double... defvals) {
		if (topMap.containsKey(nm))
			return listOf(nm, yamlDouble);
		return mkUList(defvals);
	}

	protected UList<String> stringListEnum(String nm, String... opts0) {
		UList<String> strs = stringList(nm);
		UList<String> opts = mkUList(opts0);
		for (String s : strs)
			if (!opts.contains(s))
				die("yaml option %s has %s: it must contain only one of %s%n", nm, s, opts);
		return strs;
	}

	protected UList<String> stringList(String nm, String... def) {
		return listOf(nm, yamlString, Arrays.asList(def));
	}

	protected <K, V> UMap<K, V> mapOf(String nm, YamlObj<K> getKey, YamlObj<V> getValue) {
		registerName(nm);
		return new YamlMap<>(getKey, getValue).conv(nm, topMap.get(nm));
	}

	protected <K, V> DelayedInitUMap<K, V> delayedMapOf(String nm, YamlObj<K> getKey, YamlObj<V> getValue) {
		registerName(nm);
		DelayedInitUMap<K, V> dmap = new DelayedInitUMap<K, V>(
				() -> new YamlMap<>(getKey, getValue).conv(nm, topMap.get(nm)));
		delayedMaps.add(dmap);
		return dmap;
	}

	protected UMap<String, Integer> stringIntMap(String nm) {
		return mapOf(nm, yamlString, yamlInt);
	}

	//	protected UMap<String, Pair<Integer, Integer>> stringIntPairMap(String nm) {
	//		return mapOf(nm, yamlString, yamlInt);
	//	}
	//
	//	protected <T> UMap<T, Pair<Integer, Integer>> stringIntPairMapCM(String nm, Function<String, T> lookup) {
	//		registerName(nm);
	//		return convertMap(top_ss_maps.get(nm), str -> parseIntPair(str)).mapKeys(str -> lookup.apply(str));
	//	}

	protected <T> UMap<T, IntRange> stringIntRangeMapCM(String nm, Function<String, T> lookup) {
		return mapOf(nm, new YamlLookup<>(lookup), yamlIntRange);
	}

	protected UMap<String, String> stringStringMap(String nm) {
		return mapOf(nm, yamlString, yamlString);
	}

	protected String stringVal(String nm, String defval) {
		myassert(defval != null);
		registerName(nm);
		Object x = topMap.get(nm);
		if (x == null)
			return defval;
		else if (x instanceof String)
			return ((String) x).trim();
		else
			die("yaml prop %s is supposed to be a string, was %s", nm, x);
		die();
		return null;
	}

	protected String enumVal(String nm, String... opts) {
		String str = stringVal(nm, "");
		if (str.equals(""))
			die("Must specify value for enum '%s'", nm);

		for (String opt : opts)
			if (str.equals(opt))
				return str;
		die("Invalid value for enum '%s': %s", nm, str);
		return null;
	}

	public class NestedList {
	};

	public class NestedListList extends NestedList {
		public final UList<NestedList> nodes;

		public String toString() {
			return "[" + nodes.map(n -> n.toString()).join(", ") + "]";
		}

		public NestedListList(UList<NestedList> nodes) {
			this.nodes = nodes;
		}
	};

	public class NestedListLeaf extends NestedList {
		public final String leaf;

		public String toString() {
			return leaf;
		}

		public NestedListLeaf(String leaf) {
			this.leaf = leaf;
		}
	};

	public class YamlTree extends YamlObj<NestedList> {

		//		public final YamlObj<String> acc;
		//
		//		public YamlTree(YamlObj<String> acc) {
		//			this.acc = acc;
		//		}

		public NestedList conv(String propLoc, Object s) {
			if (s == null) {
				return die_null("no empty trees");
			} else if (s instanceof String) {
				return new NestedListLeaf((String) s);
			} else if (s instanceof List) {
				//				printf("yaml %s <- %s%n", nm, s);
				List<NestedList> xs = new ArrayList<>();
				List<?> sl = (List<?>) s;
				if (sl.isEmpty())
					die("no empty trees");

				for (Object s1 : sl)
					xs.add(conv(propLoc, s1));
				return new NestedListList(mkUList(xs));
			} else if (s.equals("")) {
				return die_null("no empty trees");
			} else {
				//printf("s==>%s<%n", s);
				return die_null("yaml property %s is supposed to be a tree, but was this: %s (%s)", propLoc, s,
						s.getClass());
			}
		}

	};

	protected <T> NestedList treeOf(String nm) {
		registerName(nm);
		Object tr = topMap.get(nm);
		if (tr == null)
			return null;
		return new YamlTree().conv(nm, tr);
	}
}
