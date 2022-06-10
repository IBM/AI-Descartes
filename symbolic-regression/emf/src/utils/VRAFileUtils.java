// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.UList.*;
import static utils.VRAUtils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class VRAFileUtils {

	// nuts - hack that may not work
	// on second thought, used web code:
	// https://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes/1757107
	public static String[] cvs_split(String input) {
		// String slots[] = line.split(",");
		// ArrayList<String> merge_slots = newArrayList();
		// boolean merge = false;
		// for (String s : slots) {
		// if (
		// }
		List<String> result = new ArrayList<String>();
		int start = 0;
		boolean inQuotes = false;
		for (int current = 0; current < input.length(); current++) {
			if (input.charAt(current) == '\"')
				inQuotes = !inQuotes; // toggle state
			boolean atLastChar = (current == input.length() - 1);
			if (atLastChar)
				result.add(strip_quotes(input.substring(start)));
			else if (input.charAt(current) == ',' && !inQuotes) {
				result.add(strip_quotes(input.substring(start, current)));
				start = current + 1;
			}
		}
		String[] res = new String[result.size()];
		result.toArray(res);
		return res;

	}

	public static String strip_quotes(String s) {
		if (s.length() == 0)
			return s;
		if (s.charAt(0) == '"') {
			myassert(s.charAt(s.length() - 1) == '"');
			return s.substring(1, s.length() - 1);
		} else {
			myassert(s.charAt(s.length() - 1) != '"');
			return s;
		}
	}

	public static UList<String> readFile(String nm) {
		try {
			// File file = new File(nm);

			BufferedReader br = new BufferedReader(new FileReader(nm));

			ArrayList<String> buf = new ArrayList<String>();
			String st;
			while ((st = br.readLine()) != null)
				buf.add(st);
			br.close();

			return mkUList(buf);

		} catch (FileNotFoundException e) {
			System.out.printf("COULDN'T OPEN FILE %s", nm);
			// TODO Auto-generated catch block
			e.printStackTrace();
			die();
		} catch (IOException e) {
			System.out.printf("ERROR READING FILE %s", nm);
			// TODO Auto-generated catch block
			e.printStackTrace();
			die("");
		}
		return null; // not reachable
	}

	public static String readCVSheader(String nm) {
		UList<String> buf = readFile(nm + ".csv");
		return buf.get(0);
	}

	public static UList<String> readCVS(String nm) {
		UList<String> buf = readFile(nm + ".csv");

		return buf.remove(0).map((s) -> s);
	}

	public static String maybeAdd(String nm, String suffix) {
		if (nm.endsWith(suffix))
			return nm;
		return nm + suffix;
	}

	public static UList<SSLine> readCVS2(String nm) {
		UList<String> buf = readFile(maybeAdd(nm, ".csv")).remove(0);
		return buf.mapI((i, s) -> new SSLine(nm, s, i + 2));
	}

	public static UList<SSLine> readCVS2(String nm, int tabno) {
		return readCVS2(nm + "-" + tabno);
	}

	public static UList<String[]> readCVS(String nm, int tabno, String newheader) {
		return readCVS2(nm, tabno, newheader).map(x -> x.slots);
	}

	public static UList<SSLine> readCVS2(String nm, int tabno, String newheader) {
		String nm1 = nm + "-" + tabno + ".csv";
		UList<String> buf = readFile(nm1);
		String[] header = buf.get(0).split(",");

		List<String> header2 = Arrays.asList(header);
		String bareName = nm1.replaceAll(".*/", "");

		if (header2.size() != newheader.split(",").length) {
			java.util.Collections.sort(header2);
			List<String> newhdr = Arrays.asList(newheader.split(","));
			java.util.Collections.sort(newhdr);
			for (int i = 0; i < Math.max(header2.size(), newhdr.size()); i++)
				printf(" %s %s%n", header2.get(i), newhdr.get(i));
			die("BAD NEWHEADER: %n%s%n%s%n", buf.get(0), newheader);
		}

		int[] hmap = new int[header2.size()];
		{
			int i = 0;
			myassert(hmap.length == newheader.split(",").length);
			for (String s : newheader.split(",")) {
				int ind = header2.indexOf(s);
				if (ind < 0) {
					printf("No entry in header for %s%n", s);
					// if (newheader.split(",").length != header.length) {
					die("PROBLEMS WITH NEW HEADER: %n%s%n%s%n", buf.get(0), newheader);
					// }
				}
				hmap[i++] = ind;
			}
		}
		{
			List<String> newhdr = Arrays.asList(newheader.split(","));
			for (String s : header) {
				int ind = newhdr.indexOf(s);
				if (ind < 0) {
					printf("No entry in newheader for %s%n", s);
					// if (newheader.split(",").length != header.length) {
					die("PROBLEMS WITH NEW HEADER: %n%s%n%s%n", buf.get(0), newheader);
					// }
				}
			}
		}

		if (newheader.split(",").length != header.length) {
			die("PROBLEMS WITH NEW HEADER: %n%s%n%s%n", buf.get(0), newheader);
		}

		// for comparing comments between different years
		final boolean printComments = false;
		int commentInd = header2.indexOf("Comment");

		return buf.remove(0).mapI((base0ind, s) -> {
			String[] xs = cvs_split(s);
			if (xs.length != header2.size())
				die("WRONG NUMBER OF ENTRIES:%n%s%n%s%n", buf.get(0), s);
			String[] ys = new String[xs.length];
			for (int i = 0; i < xs.length; ++i)
				ys[i] = xs[hmap[i]];
			if (printComments)
				printf("HDR %s %s%n", tabno, xs[commentInd]);

			// header is line 1, but was dropped
			return new SSLine(bareName, s, ys, base0ind + 2);
		});
	}

	public static boolean fileExists(String nm) {
		Path path = Paths.get(nm);

		return Files.exists(path);
	}

	public static String fileExists(String s, String... dirs) {
		if (s.startsWith("/") || s.startsWith("."))
			return fileExists(s) ? s : null;

		if (fileExists(s))
			return s;

		String dir = null;
		for (String d : dirs) {
			if (VRAFileUtils.fileExists(d + "/" + s))
				if (dir == null)
					dir = d;
				else
					die("File %s exists in both of these dirs: %s %s%n", dir, s);
		}
		if (dir == null)
			return null;
		String fname = dir + "/" + s;
		if (fileExists(fname))
			return fname;
		die("file %s not found!", fname);
		return null;
	}

	public static UList<String> allFiles(String dir) {
		return mkUList(new File(dir).listFiles()).map(x -> x.getName());
	}

	public static UList<String> allFiles(String dir, String regex) {
		final Pattern p = Pattern.compile(regex.replace("*", ".*"));
		//		printf("DIR %s%n", dir);
		File[] listFiles = new File(dir).listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				boolean mtch = p.matcher(f.getName()).matches();
				//				printf("MATCH %s %s %s %s%n", dir, regex, f.getName(), mtch);
				return mtch;
			}
		});
		if (listFiles == null)
			die("NO SUCH DIR: %s", dir);

		return mkUList(listFiles).map(x -> dir + "/" + x.getName());
	}

	public static void writeString(String pathname, String val) {
		PrintStream out = VRAFileUtils.newPrintStream(pathname);
		out.println(val);
		out.close();
	}

	public static PrintStream newPrintStream(String pathname) {
		try {
			return new PrintStream(pathname);
		} catch (FileNotFoundException e) {
			die("%n%nFileNotFoundException: %s%n%n%n", pathname);
			return null;
		}
	}

}
