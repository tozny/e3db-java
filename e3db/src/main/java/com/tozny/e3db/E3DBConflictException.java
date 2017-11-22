package com.tozny.e3db;

/**
 * Thrown when an update operation fails because
 * the item already exists.
 */
public class E3DBConflictException extends E3DBException {
  private static final int code = 409;
  /**
   * Message sent by the server.
   */
  public final String message;

  public E3DBConflictException(String message) {
    super("HTTP " + code + ": " + message);
    this.message = message;
  }
}
