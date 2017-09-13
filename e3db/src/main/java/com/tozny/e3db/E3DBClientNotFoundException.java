package com.tozny.e3db;

/**
 * Indicates that the given client could not be found.
 *
 * This exception can occur when sharing records, where the
 * recipient is not found.
 */
public class E3DBClientNotFoundException extends E3DBException {
  /**
   * ID of the client that was not found.
   */
  public final String id;

  public E3DBClientNotFoundException(String id) {
    super("'" + id + "' not found.");
    this.id = id;
  }
}
