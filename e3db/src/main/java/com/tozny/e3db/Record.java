package com.tozny.e3db;

import java.util.List;
import java.util.Map;

/**
 * An individual, decrypted E3DB record.
 */
public interface Record extends Signable, SignedDocument<Record> {
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

  /**
   * The signature associated with the document. Can be {@code null}, and if so, indicates no
   * signature was ever associated with the document.
   * <p>
   * Otherwise, the presence of a signature indicates the document has been verified against the
   * signature.
   *
   * @return Base64URL-encoded representation of the document signature.
   */
  String signature();
}
