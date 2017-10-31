package com.tozny.e3db;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable information about a given record.
 */
public interface RecordMeta {
  /**
   * ID of the record.
   *
   * @return recordId.
   */
  UUID recordId();

  /**
   * ID of client that wrote the record.
   *
   * @return writerId.
   */
  UUID writerId();

  /**
   * ID of user associated with the record.
   *
   * This field is always equal to writer ID when records are written by this
   * client.
   *
   * @return userId.
   */
  UUID userId();

  /**
   * Date the record was created.
   *
   * @return created.
   */
  Date created();

  /**
   * Date the record was last modified.
   *
   * @return lastModified.
   */
  Date lastModified();

  /**
   * Version tag for the record.
   *
   * Versions are checked when updating and deleting records. They carry no useful
   * information otherwise (in particular, they cannot be used to order updates by time).
   *
   * @return version.
   */
  String version();

  /**
   * The type given to the record.
   *
   * @return type.
   */
  String type();

  /**
   * Unencrypted, metadata values stored with the record. May be {@code null}.
   *
   * @return plain.
   */
  Map<String, String> plain();
}
