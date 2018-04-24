/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

package com.tozny.e3db;

import java.util.Map;

/**
 * Static methods to check for null, empty string, etc.
 */
public class Checks {
  /**
   * Throws if the given string is null or blank.
   *
   * @param name name.
   * @param str str.
   */
  public static void checkNotEmpty(String str, String name) {
    if(str == null || str.trim().length() == 0)
      throw new IllegalArgumentException(name + ": (string): was null or empty.");
  }

  /**
   * Throws if the given array is null or empty.
   *
   * @param arr arr.
   * @param name name.
   */
  public static void checkNotEmpty(byte[] arr, String name) {
    if(arr == null || arr.length == 0)
      throw new IllegalArgumentException(name + ": (array): was null or empty");
  }

  /**
   * Throws if the given value is null.
   *
   * @param obj obj.
   * @param name name.
   * @param <R> R.
   */
  public static <R> void checkNotNull(R obj, String name) {
    if(obj == null)
      throw new IllegalArgumentException(name + ": (object): was null.");
  }

  /**
   * Throws if the given map is empty, if it contains a key that is empty,
   * or if it contains a value that is null.
   *
   * @param map map.
   * @param name name.
   */
  public static void checkMap(Map<String, String> map, String name) {
    if(map.size() == 0)
      throw new IllegalArgumentException(name + ": (map): empty.");

    for(Map.Entry<String, String> entries : map.entrySet()) {
      checkNotEmpty(entries.getKey(), name + ": (map): field name");
      checkNotNull(entries.getValue(), name + ": (map): field value");
    }
  }

}
