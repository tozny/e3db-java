package com.tozny.e3db;

import java.util.UUID;

/**
 * Describes records shared with a client.
 *
 * <p>This class describes a sharing relationship from the reader's perspective (that is, "who shares with me?").
 *
 * <p>Use {@link Client#getIncomingSharing(ResultHandler)} to get a list of record types shared with a client.
 */
public class IncomingSharingPolicy {
  /**
   * The client (writer) who is sharing records.
   */
  public final UUID writerId;
  /**
   * The type of records shared.
   */
  public final String type;
  /**
   * The name of the client (writer) who is sharing. May be empty but never null.
   */
  public final String writerName;

  IncomingSharingPolicy(UUID writerId, String writerName, String type) {
    this.writerId = writerId;
    this.type = type;
    this.writerName = writerName;
  }
}
