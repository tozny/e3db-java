package com.tozny.e3db;

/**
 * Indicates that the client is not permitted to perform the requested operation.
 */
public class E3DBForbiddenException extends E3DBException {
  public static final int code = 403;
  public final String message;

  public E3DBForbiddenException(String message) {
    super("HTTP " + code + ": " + message);
    this.message = message;
  }
}
