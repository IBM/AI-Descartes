// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.System_out;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class DupOutputStream extends FileOutputStream {
//	// Unix style
//	PrintStream nps = new PrintStream(new FileOutputStream("/dev/null"));
//	System.setErr(nps);
//	System.setOut(nps);
//
//	//Windows style
//	PrintStream nps = new PrintStream(new FileOutputStream("NUL:"));
//	System.setErr(nps);
//	System.setOut(nps);

	final PrintStream so = System_out;
//	public DupToStdoutPrintStream(FileOutputStream fout) {
//		super(fout);
//		System_out.printf("DUPING %s%n", fout);
//	}
	public DupOutputStream(String path) throws FileNotFoundException {
		//super("/dev/null");
		super(path);
		System_out.printf("DUPING %s%n", path);
		PrintStream out = new PrintStream(path);
		out.printf("SUPER PRINT%n");
	}	

	public void write(int b) throws IOException {
		super.write(b);
		so.write(b);
	}
	
	public void write(byte[] b) throws IOException {
		super.write(b);
		so.write(b);
	}
	
	public void write(byte[] b,
			         int off,
			         int len) throws IOException {
		super.write(b, off, len);
		so.write(b, off, len);
	}	
}
