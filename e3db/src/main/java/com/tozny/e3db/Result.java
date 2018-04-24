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
 * Carries the result of an asynchronous E3DB operation.
 *
 * <p>This interface represents whether an asynchronous operation completed successfully or
 * resulted in an error.
 *
 * <p>If an error occured, the {@link #isError()} method will return {@code true}. In that
 * case, use {@link #asError()} to inspect the error that occurred.
 *
 * <p>Otherwise, the {@link #asValue()} method will return a value representing the result
 * of the operation (which varies depending on the operation performed).
 *
 * <p>See the documentation for {@link Client} for more information about asynchronous
 * operations.
 *
 * @param <R> The type of the value returned when an operation completes successfully.
 */
public interface Result<R> {
  /**
   * Indicates if the operation resulted in an error or not.
   *
   * @return isError.
   */
  boolean isError();

  /**
   * Gives information about any error that occured during the operation.
   *
   * <p>This method will return {@code null} if it is called when {@code isError} returns
   * {@code false}.
   *
   * @return The error, if any.
   */
  ErrorResult<R> asError();

  /**
   * Gives the value returned by a successful operation.
   *
   * <P>This method will return {@code null} if it is called when {@code isError} returns
   * {@code true}.
   *
   * @return The value, if any.
   */
  R asValue();
}
