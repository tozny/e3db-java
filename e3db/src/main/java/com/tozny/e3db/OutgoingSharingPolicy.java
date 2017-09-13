package com.tozny.e3db;

import java.util.UUID;

/**
 * Describes records that a client shares.
 *
 * <p>This class describes a sharing relationship from the writers's perspective (that is, "who am I sharing with?").
 *
 * <p>Use {@link Client#getOutgoingSharing(ResultHandler)} to get a list of all record types shared by a client.
 */
public class OutgoingSharingPolicy {
  /**
   * The client (reader) that the records are shared with.
   */
  public final UUID readerId;
  /**
   * The type of records that are shared.
   */
  public final String type;
  /**
   * The name of the client (reader) that is shared with. May be empty but never null.
   */
  public final String readerName;

  OutgoingSharingPolicy(UUID readerId, String readerName, String type) {
    this.readerId = readerId;
    this.type = type;
    this.readerName = readerName;
  }
}
