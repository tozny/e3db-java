package com.tozny.e3db;

/**
 * A callback for receiving the result of an asynchronous operation.
 *
 * <p>The E3DB SDK uses this interface to deliver the result of all asynchronous
 * operations.
 *
 * <p>See the documentation for {@link Client} for more information about asynchronous
 * operations.
 *
 * @param <R> The type of value returned when an operation completes successfully.
 */
public interface ResultHandler<R> {
  /**
   * Receives the result of the asynchronous operation.
   *
   * <p>See the documentation for {@link Client} for more information about asynchronous
   * operations.
   *
   * @param r
   */
  void handle(Result<R> r);
}
