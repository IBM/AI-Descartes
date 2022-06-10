// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

public class FormCode {
	public final static int b_P = 0; // P
	public final static int b_2P = 1; // (P+P)
	public final static int b_3P = 2; // (P+P+P)
	public final static int b_pow2P = 3; // pow((P+P))
	public final static int b_Pdiv2P = 4; // (P/(P+P))
	public final static int b_4P = 5; // (P+P+P+P)
	public final static int b_pow3P = 6; // pow((P+P+P))
	public final static int b_Ppluspow2P = 7; // (P+pow((P+P)))
	public final static int b_Pmulpow2P = 8; // (P*pow((P+P)))
	public final static int b_pow2PdivP = 9; // (pow((P+P))/P)
	public final static int b_Pdiv3P = 10; // (P/(P+P+P))
	public final static int b_Pdivpow2P = 11; // (P/pow((P+P)))
	public final static int b_pow4P = 14; // pow((P+P+P+P))
	public final static int b_2Pdiv2P = 15; // ((P+P)/(P+P))
	public final static int b_2Ppluspow2P = 17; // (P+P+pow((P+P)))
	public final static int b_pow2Pmul2P = 18; // (pow((P+P))*(P+P))
	public final static int b_Pdiv4P = 19; // (P/(P+P+P+P))
	public final static int b_3Pdiv2P = 21; // ((P+P+P)/(P+P))
	public final static int b_pow2Pdiv2P = 22; // (pow((P+P))/(P+P))
	public final static int b_2Pdiv3P = 23; // ((P+P)/(P+P+P))
	public final static int b_2Pdivpow2P = 24; // ((P+P)/pow((P+P)))
	public final static int b_3Ppluspow2P = 26; // (P+P+P+pow((P+P)))
	public final static int b_pow2Ppluspow2P = 29; // (pow((P+P))+pow((P+P)))
	public final static int b_pow2Pmul3P = 30; // (pow((P+P))*(P+P+P))
	public final static int b_3Pdiv3P = 32; // ((P+P+P)/(P+P+P))
	public final static int b_3Pdivpow2P = 33; // ((P+P+P)/pow((P+P)))
	public final static int b_4Pdiv2P = 34; // ((P+P+P+P)/(P+P))
	public final static int b_pow2Pdiv3P = 35; // (pow((P+P))/(P+P+P))
	public final static int b_2Pdiv4P = 36; // ((P+P)/(P+P+P+P))
	public final static int b_4Ppluspow2P = 38; // (P+P+P+P+pow((P+P)))
	public final static int b_pow2Pmul4P = 42; // (pow((P+P))*(P+P+P+P))
	public final static int b_3Pdiv4P = 46; // ((P+P+P)/(P+P+P+P))
	public final static int b_4Pdiv3P = 47; // ((P+P+P+P)/(P+P+P))
	public final static int b_4Pdivpow2P = 48; // ((P+P+P+P)/pow((P+P)))
	public final static int b_pow2Pdiv4P = 49; // (pow((P+P))/(P+P+P+P))
	public final static int b_4Pdiv4P = 56; // ((P+P+P+P)/(P+P+P+P))

	public final static String[] formStrs = new String[100];
	static {
		formStrs[0] = "P";
		formStrs[1] = "(P+P)";
		formStrs[2] = "(P+P+P)";
		formStrs[3] = "pow((P+P))";
		formStrs[4] = "(P/(P+P))";
		formStrs[5] = "(P+P+P+P)";
		formStrs[6] = "pow((P+P+P))";
		formStrs[7] = "(P+pow((P+P)))";
		formStrs[8] = "(P*pow((P+P)))";
		formStrs[9] = "(pow((P+P))/P)";
		formStrs[10] = "(P/(P+P+P))";
		formStrs[11] = "(P/pow((P+P)))";
		formStrs[14] = "pow((P+P+P+P))";
		formStrs[15] = "((P+P)/(P+P))";
		formStrs[17] = "(P+P+pow((P+P)))";
		formStrs[18] = "(pow((P+P))*(P+P))";
		formStrs[19] = "(P/(P+P+P+P))";
		formStrs[21] = "((P+P+P)/(P+P))";
		formStrs[22] = "(pow((P+P))/(P+P))";
		formStrs[23] = "((P+P)/(P+P+P))";
		formStrs[24] = "((P+P)/pow((P+P)))";
		formStrs[26] = "(P+P+P+pow((P+P)))";
		formStrs[29] = "(pow((P+P))+pow((P+P)))";
		formStrs[30] = "(pow((P+P))*(P+P+P))";
		formStrs[32] = "((P+P+P)/(P+P+P))";
		formStrs[33] = "((P+P+P)/pow((P+P)))";
		formStrs[34] = "((P+P+P+P)/(P+P))";
		formStrs[35] = "(pow((P+P))/(P+P+P))";
		formStrs[36] = "((P+P)/(P+P+P+P))";
		formStrs[38] = "(P+P+P+P+pow((P+P)))";
		formStrs[42] = "(pow((P+P))*(P+P+P+P))";
		formStrs[46] = "((P+P+P)/(P+P+P+P))";
		formStrs[47] = "((P+P+P+P)/(P+P+P))";
		formStrs[48] = "((P+P+P+P)/pow((P+P)))";
		formStrs[49] = "(pow((P+P))/(P+P+P+P))";
		formStrs[56] = "((P+P+P+P)/(P+P+P+P))";
	}
}
