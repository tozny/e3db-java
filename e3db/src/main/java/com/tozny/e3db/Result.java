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
   * @return
   */
  boolean isError();

  /**
   * Gives information about any error that occured during the operation.
   *
   * <p>This method will return {@code null} if it is called when {@code isError} returns
   * {@code false}.
   * @return
   */
  ErrorResult<R> asError();

  /**
   * Gives the value returned by a successful operation.
   *
   * <P>This method will return {@code null} if it is called when {@code isError} returns
   * {@code true}.
   * @return
   */
  R asValue();
}
