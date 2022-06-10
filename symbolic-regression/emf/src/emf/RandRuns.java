// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.VRAFileUtils.*;
import static utils.VRAUtils.*;

import java.io.File;

import utils.UList;

public class RandRuns {
	public static void main(String[] args) throws Exception {
		//		UList.mkUList1(0, 1, 2, 3).perms().dump("perms");
		//		die();

		String fname = args[0];
		String basename = fname.replaceAll(".*/", "");
		String dirname = args[1];
		myassert(new File(dirname).mkdirs(), "can't create dir");
		int limit = args.length > 2 ? toInt(args[2]) : 1000;

		UList<String> lines = readFile(fname);
		String startTag = "OBJ: minimize";
		String endTag = " 0;";
		UList<String> prog = linesBefore(lines, startTag);
		UList<String> obj = linesBefore(linesAfter(lines, startTag), endTag);

		UList<String> objToPerm = limit < 1000 ? obj.subList(0, limit) : obj;
		UList<String> objNotToPerm = limit < 1000 ? obj.subList(limit, obj.size()) : UList.empty();
		printf("Perm limit: %s%n", objToPerm.size());

		int i = 0;
		for (UList<String> objperm0 : objToPerm.perms()) {
			UList<String> objperm = objperm0.concat(objNotToPerm);
			String pstr = prog.concat(cons(startTag, objperm.concat(mkUList1(endTag)))).join("\n");
			String outname = dirname + "/" + basename.replace(".bar", "_" + i + ".bar");
			writeString(outname, pstr);
			i++;
			//			printf("%s%n", pstr);
			//			if (i == 2)
			//				die();
		}
	}

}
