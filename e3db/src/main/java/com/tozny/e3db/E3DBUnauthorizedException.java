package com.tozny.e3db;

/**
 * Indicates that the client has not authenticated with E3DB.
 *
 * This exception usually indicates the credentials given when constructing the client
 * are not valid.
 */
public class E3DBUnauthorizedException extends E3DBException {
  public static final int code = 401;
  public final String message;

  public E3DBUnauthorizedException(String message) {
    super("HTTP " + code + ": " + message);
    this.message = message;
  }
}
