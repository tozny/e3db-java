package com.tozny.e3db;

import java.util.List;
import java.util.Map;

/**
 * An individual, decrypted E3DB record.
 */
public interface Record {
  /**
   * Information about the record.
   *
   * Includes items such as writer ({@link RecordMeta#writerId()}),
   * creation date ({@link RecordMeta#created()}, type ({@link RecordMeta#type}), etc.
   */
  RecordMeta meta();

  /**
   * Decrypted field names and values.
   */
  Map<String, String> data();
}
