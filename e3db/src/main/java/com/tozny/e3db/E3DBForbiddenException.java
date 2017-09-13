package com.tozny.e3db;

/**
 * Indicates that the client is not permitted to perform the requested operation.
 */
public class E3DBForbiddenException extends E3DBException {
  private static final int code = 403;
  /**
   * Message sent by the server.
   */
  public final String message;

  public E3DBForbiddenException(String message) {
    super("HTTP " + code + ": " + message);
    this.message = message;
  }
}
