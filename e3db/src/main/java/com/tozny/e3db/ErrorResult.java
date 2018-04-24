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

import static com.tozny.e3db.Checks.*;

/**
 * Holds information about any error that occurred during an asynchronous operation.
 *
 * <p>This class holds can hold one of two different types of errors: E3DB-specific errors, or
 * something else. In the first case, the {@link #error()} method will return a non-{@code null} value
 * (representing a specific E3DB error); in the second, {@link #error()} will return {@code null}, and
 * you should use {@link #other()} to get the specific exception that occurred.
 *
 * <p><b>Note</b>: You should never need to use {@code instanceof} to test that a {@link Result} instance has this class &mdash; the
 * {@link Result#isError()} and {@link Result#asError()} methods work together to do the same.
 *
 * @param <R> The type of value returned when an operation completes successfully. Only present because
 *           the interface requires it.
 */
public class ErrorResult<R> implements Result<R> {
  private final Throwable error;

  public ErrorResult(Throwable error) {
    checkNotNull(error, "error");
    this.error = error;
  }

  /**
   * Always {@code true}.
   */
  @Override
  public boolean isError() {
    return true;
  }

  /**
   * Always this instance.
   */
  @Override
  public ErrorResult<R> asError() {
    return this;
  }

  /**
   * Always {@code null}.
   */
  @Override
  public R asValue() {
    return null;
  }

  /**
   * Returns any E3DB-specific error that occurred.
   *
   * <p>This method will return {@code null} if a general exception
   * occurred during the operation; otherwise, it will return a specific E3DB error.
   *
   * @return The  error, if any.
   */
  public E3DBException error() {
    if(error instanceof E3DBException)
      return (E3DBException) error;
    else
      return null;
  }

  /**
   * Returns any error that ocurrred.
   *
   * <p>This method always returns a value. However, this method should not be used if
   * {@link #error()} returns a non-{@code null} value.
   *
   * @return The error, if any.
   */
  public Throwable other() {
    return this.error;
  }
}
