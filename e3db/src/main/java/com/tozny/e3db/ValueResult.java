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

import java.util.UUID;

/**
 * Holds the result of a successful asynchronous operation.
 *
 * <p>This class carries the result of a given asynchronous operation.
 *
 * <p><b>Note</b>: You should never need to use {@code instanceof} to test that a {@link Result} instance has this class &mdash; the
 * {@link Result#isError()} and {@link Result#asValue()} methods work together to do the same.
 *
 * @param <R> The type of value returned by the operation.
 */
public class ValueResult<R> implements Result<R> {
  private final R value;

  public ValueResult(R value) {
    this.value = value;
  }

  /**
   * Always {@code false}.
   */
  @Override
  public boolean isError() {
    return false;
  }

  /**
   * Always {@code null}.
   */
  @Override
  public ErrorResult<R> asError() {
    return null;
  }

  /**
   * The value returned by the operation.
   *
   * <p>Note that in some cases that may
   * still be a {@code null} value. For example, {@link Client#share(String, UUID, ResultHandler)}
   * specifies a {@link Void} result type, of which {@code null} is the only valid value. In that
   * case, the fact that {@code isError} is {@code false} is the only information necessary to know
   * the operation completed successfully.
   */
  @Override
  public R asValue() {
    return value;
  }
}
