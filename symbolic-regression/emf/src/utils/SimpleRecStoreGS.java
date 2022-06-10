// © Copyright IBM Corporation 2022. All Rights Reserved.
// LICENSE: MIT License https://opensource.org/licenses/MIT
// SPDX-License-Identifier: MIT

package utils;

public class SimpleRecStoreGS {
	// These routines were used to convert java objects with get/set interfaces
	// to the SimpleRecStore format, for the Enbridge project.
	// I hope to never have to use it again, but I'm keeping it here just in case.
	// I converted some routines from object methods to static in order to extract
	// them from SimpleRecStore, so I don't know if they actually work.

//	public static <T> SimpleRecStore makeStore(String tag, T[] xs) {
//		SimpleRecStore store = new SimpleRecStore();
//		for (T x : xs)
//			addRec(store,tag, x);
//		return store;
//	}
//	
//	public static SimpleRecStore makeInputStore(Object x) {
//		SimpleRecStore store = new SimpleRecStore();
//		for (Method m : x.getClass().getDeclaredMethods()) {
//			final String accessor = m.getName();
//			if (accessor.startsWith("get") && m.getParameterTypes().length == 0 && m.getReturnType().isArray()) {
//				String fldNm0 = accessor.substring(3);
//				String fldNm = fldNm0.substring(0, 1).toLowerCase() + fldNm0.substring(1);
//
//				try {
//					store.addResults(makeStore(fldNm, (Object[]) m.invoke(x)));
//				} catch (Exception e) {
//					e.printStackTrace();
//					die("should never happen - don't invoke on type with non-public get methods");
//				}
//			}
//		}
//		return store;
//	}
//	
//	/** this takes an object with getXYZ/isXYZ style getters and extracts the fields */
//	public static <T> void addRec(SimpleRecStore store, String tag, T x) {
//		/*
//		 * from http://stackoverflow.com/questions/5196534/order-of-fields-returned-by-class-getfields:
//		 
//		Javadoc for Class.getFields() say: "The elements in the array returned are not sorted and are not in any particular order."
//		...
//		  
//		On my JVM, at least,
//
//		Class.getFields() returns fields in declaration order.
//
//		Class.getMethods(), on the other hand, doesn't always. It returns them in (I believe) the order the classloader sees the strings.
//		 */
//		IORec rec = store.newRec(tag);
//		//for (Method m : x.getClass().getDeclaredMethods()) {
//		for (Field fld : x.getClass().getDeclaredFields()) {
//			String fldNm = fld.getName();
//			Method m = null;
//			String fldNm1 = fldNm.substring(0, 1).toUpperCase() + fldNm.substring(1);
//			try {
//				m = x.getClass().getDeclaredMethod("get" + fldNm1);
//			} catch (NoSuchMethodException e1) {
//			} catch (SecurityException e1) {
//			}
//			if (m == null)
//				try {
//					m = x.getClass().getDeclaredMethod("is" + fldNm1);
//				} catch (NoSuchMethodException e1) {
//				} catch (SecurityException e1) {
//				}
//			if (m != null) {
//				Class<?> rt = m.getReturnType();
//				Object rval = null;
//				try {
//					rval = m.invoke(x);
//				} catch (Exception e) {
//					e.printStackTrace();
//					die("should never happen - don't invoke on type with non-public get methods");
//				}
//
//				if (rt.equals(String.class))
//					rec.add(fldNm, (String) rval);
//				else if (rt.equals(Double.TYPE))
//					rec.add(fldNm, (Double) rval);
//				else if (rt.equals(Integer.TYPE))
//					rec.add(fldNm, (Integer) rval);
//				else if (rt.equals(Boolean.TYPE))
//					rec.add(fldNm, (Boolean) rval);
//				else {
//					die("%s", rt);
//				}
//			}
//		}
//		rec.done();
//	}
//
//	public <T> ArrayList<T> getRecsAsObjects(SimpleRecStore store, String tag, Class<T> clz) throws IllegalAccessException,
//					InstantiationException, IllegalArgumentException, InvocationTargetException {
//		ArrayList<T> xs = newArrayList();
//		Map<Class<?>, Class<?>> clmap = newLinkedHashMap();
//		clmap.put(String.class, String.class);
//		clmap.put(double.class, Double.class);
//		clmap.put(int.class, Integer.class);
//		clmap.put(boolean.class, Boolean.class);
//		for (IORec r : store.getRecs(tag)) {
//			// You really shouldn't get an exception here - would only happen if there is no appropriate constructor
//			T x = clz.newInstance();
//			xs.add(x);
//			for (Method m : clz.getDeclaredMethods()) {
//				final String setter = m.getName();
//				//printf("GETRECOBJ: %s %s%n", clz.getName(), setter);
//				if (setter.startsWith("set") && m.getParameterTypes().length == 1) {
//					String fldNm0 = setter.substring(3);
//					String fldNm = fldNm0.substring(0, 1).toLowerCase() + fldNm0.substring(1);
//					// If you get an error here, that means that 
//					// - the input object clz has a getXYZ method for which there is no corresponding entry in the record, or
//					// - there is an entry, but is the wrong type.
//					//printf("GETTING %s %s %s%n", tag, fldNm, m.getParameterTypes()[0].getName());
//					//System_out.flush();
//					m.invoke(x, r.getObject(fldNm, clmap.get(m.getParameterTypes()[0])));
//				}
//			}
//		}
//		return xs;
//	}
//
//	@SuppressWarnings("unchecked")
//	public <T> void getObjRecs(SimpleRecStore store, String tag, List<T> rvals, Class<T> cls) {
//		rvals.clear();
//
//		for (IORec rec : store.getRecs(tag))
//			for (Method m : cls.getDeclaredMethods()) {
//				final String setter = m.getName();
//				if (setter.startsWith("set") && m.getParameterTypes().length == 1) {
//					String fldNm0 = setter.substring(3);
//					String fldNm = fldNm0.substring(0, 1).toLowerCase() + fldNm0.substring(1);
//
//					//Class<?> rt = m.getReturnType();
//					Class<?> rt = m.getParameterTypes()[0];
//					try {
//						Object x = cls.newInstance();
//						if (rt.equals(String.class))
//							m.invoke(x, rec.nextString(fldNm));
//						else if (rt.equals(Double.TYPE))
//							m.invoke(x, rec.nextDouble(fldNm));
//						else if (rt.equals(Integer.TYPE))
//							m.invoke(x, rec.nextInt(fldNm));
//						else {
//							die("%s %s %s", tag, setter, rt);
//						}
//						rvals.add((T) x);
//					} catch (Exception e) {
//						e.printStackTrace();
//						die("should never happen - don't invoke on type with non-public get methods");
//					}
//				}
//			}
//	}

}
