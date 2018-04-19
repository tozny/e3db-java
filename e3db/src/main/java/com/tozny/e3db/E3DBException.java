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

/**
 * Represents an E3DB error.
 *
 * <p>All E3DB exceptions subclass {@code E3DBException}; if a given error is not a more
 * specific exception, then some generic (or unhandled) error occurred. In that case, this
 * instance just holds a message indicating the error that occurred.
 */
public class E3DBException extends Exception {
  public E3DBException(String message, Exception cause) {
    super(message, cause);
  }

  public E3DBException(String message) {
    super(message);
  }

  /**
   * Constructs a specific E3DBException for a given error.
   *
   * <p>If a more specific E3DB exception for the given HTTP status exists, this
   * method will find it and return an instance of that object. Otherwise, it will
   * return an instance of the {@code E3DBException} class.
   * @param code HTTP status code.
   * @param message HTTP status message.
   * @return An exception related to the code given.
   */
  public static E3DBException find(int code, String message) {
    switch(code) {
      case 401:
        return new E3DBUnauthorizedException(message);
      case 403:
        return new E3DBForbiddenException(message);
      case 409:
        return new E3DBConflictException(message);
      default:
        return new E3DBException("HTTP " + code + " " + message);
    }
  }
}
