package com.tozny.e3db;

import java.util.Map;

/**
 * A record holding encrypted data, plus a signature. Consider using the {@link LocalEncryptedRecord}
 * implementation.
 */
public interface EncryptedRecord extends Signable, SignedDocument<EncryptedRecord> {
  /**
   * Metadata controlled by the client.
   * @return metadata.
   */
  ClientMeta meta();

  /**
   * A map from field names to encrypted data.
   * @return Encrypted data.
   */
  Map<String, String> data();

  @Override
  String signature();

  @Override
  String toSerialized();

  @Override
  EncryptedRecord document();
}
