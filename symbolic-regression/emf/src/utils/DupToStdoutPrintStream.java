// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

import static utils.VRAUtils.System_out;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Locale;

// this pruduced duplicate output to stdout.  mysterious.  using DupOutputStream instead.
public class DupToStdoutPrintStream extends PrintStream {
	final PrintStream so = System_out;
//	public DupToStdoutPrintStream(FileOutputStream fout) {
//		super(fout);
//		System_out.printf("DUPING %s%n", fout);
//	}
	public DupToStdoutPrintStream(String path) throws FileNotFoundException {
		super("/dev/null");
		System_out.printf("DUPING %s%n", path);
		PrintStream out = new PrintStream(path);
		out.printf("SUPER PRINT%n");
	}	
	public DupToStdoutPrintStream(PrintStream fout) {
		super(fout);
	}
	
	public PrintStream append(char c) { 
		super.append(c);
		so.append(c);
		return this;
	}
	
	public PrintStream 	append(CharSequence c) {
		super.append(c);
		so.append(c);
		return this;			
	}

	public PrintStream 	append(CharSequence csq, int start, int end) {
		super.append(csq,start,end);
		so.append(csq,start,end);
		return this;					
	}
	
	public boolean 	checkError() { return super.checkError(); }

	protected void 	clearError() {			super.clearError();		}

	public void 	close() { super.close(); }
	public void 	flush() { super.flush(); so.flush(); }

	public PrintStream 	format(Locale l, String format, Object... args) {
		super.format(l, format, args);
		so.format(l, format, args);
		return this;
	}
	
	public PrintStream 	format(String format, Object... args) {
		super.format(format, args);
		so.format(format, args);
		return this;
	}
	
	public void 	print(boolean b) {
		super.print(b);
		so.print(b);
	}
	
	public void 	print(char b) {
		super.print(b);
		so.print(b);
	}
	
	public void 	print(char[] b){
		super.print(b);
		so.print(b);
	}
	public void 	print(double b){
		super.print(b);
		so.print(b);
	}
	public void 	print(float b){
		super.print(b);
		so.print(b);
	}
	public void 	print(int b){
		super.print(b);
		so.print(b);
	}
	public void 	print(long b){
		super.print(b);
		so.print(b);
	}
	public void 	print(Object b){
		super.print(b);
		so.print(b);
	}
	public void 	print(String b){
		super.print(b);
		so.print(b);
	}
	
	public PrintStream 	printf(Locale l, String format, Object... args) {
		super.printf(l, format, args);
		so.printf(l, format, args);
		return this;
	}
	
	public PrintStream 	printf(String format, Object... args) {
		super.printf(format, args);
		so.printf(format, args);
		return this;
	}
	
		public void 	println() {
			super.println();
			so.println();
		}
		
		public void 	println(boolean b) {
			super.println(b);
			so.println(b);
		}
		
		public void 	println(char b) {
			super.println(b);
			so.println(b);
		}
		
		public void 	println(char[] b){
			super.println(b);
			so.println(b);
		}
		public void 	println(double b){
			super.println(b);
			so.println(b);
		}
		public void 	println(float b){
			super.println(b);
			so.println(b);
		}
		public void 	println(int b){
			super.println(b);
			so.println(b);
		}
		public void 	println(long b){
			super.println(b);
			so.println(b);
		}
		public void 	println(Object b){
			super.println(b);
			so.println(b);
		}
		public void 	println(String b){
			super.println(b);
			so.println(b);
		}
		
		
	protected void 	setError() {
		super.setError();
	}
	public void 	write(byte[] buf, int off, int len) {
		super.write(buf,off,len);
		so.write(buf,off,len);
	}
	
	public void 	write(int b) {
		super.write(b);
		so.write(b);
	}
	
}
