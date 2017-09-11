package com.tozny.e3db;

import java.util.Date;
import java.util.UUID;

/**
 * Immutable information about a given record.
 */
public interface RecordMeta {
  /**
   * ID of the record.
   * @return
   */
  UUID recordId();

  /**
   * ID of client that wrote the record.
   * @return
   */
  UUID writerId();

  /**
   * ID of user associated with the record.
   *
   * This field is always equal to writer ID when records are written by this
   * client.
   * @return
   */
  UUID userId();

  /**
   * Date the record was created.
   * @return
   */
  Date created();

  /**
   * Date the record was last modified.
   * @return
   */
  Date lastModified();

  /**
   * Version tag for the record.
   *
   * Versions are checked when updating and deleting records. They carry no useful
   * information otherwise (in particular, they cannot be used to order updates by time).
   * @return
   */
  String version();

  /**
   * The type given to the record.
   * @return
   */
  String type();

  /**
   * A JSON document holding any unencrypted, plaintext metadata stored with the
   * record. Always a valid document (never null or empty). If no plaintext metadata was stored, will be an empty object (<code>"{}"</code>).
   * @return
   */
  String plain();
}
