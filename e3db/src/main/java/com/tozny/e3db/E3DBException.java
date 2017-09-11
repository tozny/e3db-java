package com.tozny.e3db;

/**
 * Represents an E3DB error.
 *
 * <p>All E3dB exceptions subclass {@code E3DBException}; if a given error is not a more
 * specific exception, then some generic (or unhandled) error occurred. In that case, this
 * instance holds the HTTP status code and message returned by E3DB.
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
   * @return
   */
  public static E3DBException find(int code, String message) {
    switch(code) {
      case 401:
        return new E3DBUnauthorizedException(message);
      case 403:
        return new E3DBForbiddenException(message);
      default:
        return new E3DBException("HTTP " + code + " " + message);
    }
  }
}
