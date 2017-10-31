package com.tozny.e3db;

import java.util.List;
import java.util.Map;

/**
 * An individual, decrypted E3DB record.
 */
public interface Record extends Signable {
  /**
   * Information about the record.
   *
   * Includes items such as writer ({@link RecordMeta#writerId()}),
   * creation date ({@link RecordMeta#created()}, type ({@link RecordMeta#type}), etc.
   *
   * @return meta.
   */
  RecordMeta meta();

  /**
   * Decrypted field names and values.
   *
   * @return data.
   */
  Map<String, String> data();
}
