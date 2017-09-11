package com.tozny.e3db;

import java.util.UUID;

/**
 * Holds the result of a successful asynchronous operation.
 *
 * <p>This class carries the result of a given asynchronous operation.
 *
 * <p><b>Note</b>: You should never need to use {@code instanceof} to test that a {@link Result} type has this class -- the
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
   * @return
   */
  @Override
  public boolean isError() {
    return false;
  }

  /**
   * Always {@code null}.
   * @return
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
   * @return
   */
  @Override
  public R asValue() {
    return value;
  }
}
