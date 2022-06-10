// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package emf;

import static utils.UList.*;
import static utils.VRAUtils.*;

import utils.GrowList;
import utils.UList;

public abstract class Table {
	public final Input inpx;
	public final UList<DataLine> inputx;
	public final int nLines;
	public final int nLines1;
	public final int numInVars;
	public final boolean allow1Const;

	public Table(Input inpx) {
		this.inpx = inpx;
		this.inputx = inpx.inputSampleSize == 0 ? inpx.unsampled_input : inpx.unsampled_input.subList(0, 10);
		this.numInVars = inpx.numInVars;
		this.nLines = inputx.size();
		this.nLines1 = nLines - 1;

		this.allow1Const = inpx.max_num_consts == 1;
		//		this.allow1Const = false;

		inv();
		//		inputx.dump("unsamp");
	}

	private void inv() {
		if (inpx.max_num_consts > 1)
			die("more than 1 const not yet supported");
	}

	// retain only those entries with dims matching these
	//	public Table(Table that, byte[] dims) {
	//		this.inpx = that.inpx;
	//		this.inputx = that.inputx;
	//		this.numInVars = that.numInVars;
	//		this.nLines = inputx.size();
	//		this.nLines1 = nLines - 1;
	//
	//		this.allow1Const = inpx.max_num_consts == 1;
	//		//		this.allow1Const = false;
	//		inv();
	//	}

	public abstract double[] copyElems();

	//	private static byte sqr(byte x) {
	//		return (byte) (x * x);
	//	}

	// for a given comb (i.e. possible monomial), store the values, one per input line, in out.
	// need maxexp to decode combind, since the variable powers are in packed format.
	public void enx(double[] out, int combind, UList<DataLine> xs, int maxexp, long[] dimout) {
		int div = 2 * maxexp + 1;
		{
			int base = combind * nLines;
			for (DataLine line : xs) {
				double xv = 1;
				long ec = combind;
				for (int vi = 0; vi < numInVars; vi++) {
					int e = (int) (ec % div) - maxexp;
					ec /= div;
					double x = line.invals.get(vi);
					switch (e) {
					case -2:
						xv /= (x * x);
						break;
					case -1:
						xv /= x;
						break;
					case 0:
						break;
					case 1:
						xv *= x;
						break;
					case 2:
						xv *= (x * x);
						break;
					default:
						die();
					}
				}
				out[base + line.index] = xv;
			}
		}
		//		{
		//			long ec = combind;
		//			UList<Integer> mvals0 = inpx.mask_dims(dimout[combind]);
		//			for (int vi = 0; vi < numInVars; vi++) {
		//				int e = (int) (ec % div) - maxexp;
		//				ec /= div;
		//				long dim0 = dimout[combind];
		//				dimout[combind] += e * inpx.invar_dim_masks[vi];
		//				printf("DIMX %s %s %s   %s %s%n", dim0, dimout[combind], e * inpx.invar_dim_masks[vi], e,
		//						inpx.invar_dim_masks[vi]);
		//				myassert(dimout[combind] >= 0);
		//			}
		//			UList<Integer> mvals1 = inpx.mask_dims(dimout[combind]);
		//			for (int di = 0; di < inpx.ndimunits; ++di) {
		//				int x = mvals0.get(di);
		//				printf("XX %s ", x);
		//				for (int vi = 0; vi < numInVars; vi++)
		//					printf("%s ", inpx.dimensionVars0.get(inpx.invarnms.get(vi)).get(di));
		//				printf(" == %s%n", mvals1.get(di));
		//			}
		//		}
	}

	public static double ipow(double x, int exp) {
		double out = 1;
		for (; exp > 0; exp--)
			out *= x;
		for (; exp < 0; exp++)
			out /= x;
		return out;
	}

	public double eval_monoList(UList<Integer> lst, int pti) {
		return prodd(lst.mapI((j, exp) -> ipow(inputx.get(pti).invals.get(j), exp)));
	}

	public UList<Integer> monoList(int monoi) {
		int maxexp = inpx.max_var_exp;
		int div = 2 * maxexp + 1;
		double xv = 1;
		long ec = monoi;
		GrowList<Integer> xs = new GrowList<>();
		for (int vi = 0; vi < numInVars; vi++) {
			int e = (int) (ec % div) - maxexp;
			ec /= div;
			xs.add(e);
		}
		return xs.elems();
	}

	//	public static void quickSort2(double arr[]) {
	//		int sz = arr.length >> 1;
	//		myassert(sz * 2 == arr.length);
	//		quickSort2(arr, 0, sz - 1);
	//	}
	//
	//	//https://www.baeldung.com/java-quicksort
	//	// end is the LAST INDEX in the array, NOT its length!
	//	private static void quickSort2(double arr[], int begin, int end) {
	//		if (begin < end) {
	//			int partitionIndex = partition(arr, begin, end);
	//
	//			quickSort2(arr, begin, partitionIndex - 1);
	//			quickSort2(arr, partitionIndex + 1, end);
	//		}
	//	}
	//
	//	private static int partition(double arr[], int begin, int end) {
	//		double pivot = arr[2 * end];
	//		int i = (begin - 1);
	//
	//		//		printf("par %s %s%n", begin, end);
	//		myassert(arr[0] != 0);
	//
	//		for (int j = begin; j < end; j++) {
	//			if (arr[2 * j] <= pivot) {
	//				i++;
	//
	//				double swapTemp = arr[2 * i];
	//				myassert(swapTemp != 0);
	//				myassert(arr[2 * j] != 0);
	//
	//				arr[2 * i] = arr[2 * j];
	//				arr[2 * j] = swapTemp;
	//
	//				swapTemp = arr[2 * i + 1];
	//				arr[2 * i + 1] = arr[2 * j + 1];
	//				arr[2 * j + 1] = swapTemp;
	//			}
	//		}
	//		myassert(arr[0] != 0);
	//		myassert(arr[2 * end] != 0);
	//
	//		double swapTemp = arr[2 * (i + 1)];
	//		arr[2 * (i + 1)] = arr[2 * end];
	//		arr[2 * end] = swapTemp;
	//
	//		myassert(arr[0] != 0, i, swapTemp);
	//
	//		swapTemp = arr[2 * (i + 1) + 1];
	//		arr[2 * (i + 1) + 1] = arr[2 * end + 1];
	//		arr[2 * end + 1] = swapTemp;
	//
	//		myassert(arr[0] != 0);
	//
	//		return i + 1;
	//	}

	public static void quickSort2(double arr[], int[] other) {
		quickSort2(arr, other, 0, arr.length - 1);
	}

	//https://www.baeldung.com/java-quicksort
	// end is the LAST INDEX in the array, NOT its length!
	private static void quickSort2(double arr[], int[] other, int begin, int end) {
		if (begin < end) {
			int partitionIndex = partition(arr, other, begin, end);

			quickSort2(arr, other, begin, partitionIndex - 1);
			quickSort2(arr, other, partitionIndex + 1, end);
		}
	}

	private static int partition(double arr[], int[] other, int begin, int end) {
		double pivot = arr[end];
		int i = (begin - 1);

		//		printf("par %s %s%n", begin, end);
		for (int j = begin; j < end; j++) {
			if (arr[j] <= pivot) {
				i++;
				{
					double swapTemp = arr[i];
					arr[i] = arr[j];
					arr[j] = swapTemp;
				}
				{
					int swapTemp = other[i];
					other[i] = other[j];
					other[j] = swapTemp;
				}
			}
		}
		int j = end;
		{
			double swapTemp = arr[i + 1];
			arr[i + 1] = arr[j];
			arr[j] = swapTemp;
		}
		{
			int swapTemp = other[i + 1];
			other[i + 1] = other[j];
			other[j] = swapTemp;
		}

		return i + 1;
	}

	public static void quickSort2(double arr[], double[] other) {
		myassert(arr.length <= other.length);
		quickSort2(arr, other, 0, arr.length - 1);
	}

	//https://www.baeldung.com/java-quicksort
	// end is the LAST INDEX in the array, NOT its length!
	private static void quickSort2(double arr[], double[] other, int begin, int end) {
		if (begin < end) {
			int partitionIndex = partition(arr, other, begin, end);

			quickSort2(arr, other, begin, partitionIndex - 1);
			quickSort2(arr, other, partitionIndex + 1, end);
		}
	}

	private static int partition(double arr[], double[] other, int begin, int end) {
		double pivot = arr[end];
		int i = (begin - 1);

		//		printf("par %s %s%n", begin, end);
		for (int j = begin; j < end; j++) {
			if (arr[j] <= pivot) {
				i++;
				{
					double swapTemp = arr[i];
					arr[i] = arr[j];
					arr[j] = swapTemp;
				}
				{
					double swapTemp = other[i];
					other[i] = other[j];
					other[j] = swapTemp;
				}
			}
		}
		int j = end;
		{
			double swapTemp = arr[i + 1];
			arr[i + 1] = arr[j];
			arr[j] = swapTemp;
		}
		{
			double swapTemp = other[i + 1];
			other[i + 1] = other[j];
			other[j] = swapTemp;
		}

		return i + 1;
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
}
