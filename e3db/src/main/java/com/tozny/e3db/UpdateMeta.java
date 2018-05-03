package com.tozny.e3db;

import java.util.UUID;

/**
 * Holds immutable metadata necessary to identify a record for
 * updating.
 *
 * <p>Consider using the {@link LocalUpdateMeta} implementation. To convert
 * a {@link RecordMeta}, use the {@link LocalUpdateMeta#fromRecordMeta(RecordMeta)}
 * method.
 */
public interface UpdateMeta {
  /**
   * The type of the record.
   * @return
   */
  String getType();

  /**
   * ID of the record.
   * @return
   */
  UUID getRecordId();

  /**
   * Version of the record.
   * @return
   */
  String getVersion();
}
