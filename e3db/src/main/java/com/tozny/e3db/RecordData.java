package com.tozny.e3db;

import java.util.Map;
import static com.tozny.e3db.Checks.*;

/**
 * Specifies the unencrypted data for a record.
 *
 * <p>This class is only used when creating a record via {@link Client#write(String, RecordData, Map, ResultHandler)}
 * or updating an existing record using {@link Client#update(RecordMeta, RecordData, Map, ResultHandler)}).
 */
public class RecordData {
  private final Map<String, String> cleartext;

  /**
   * Create a new instance using the provided {@code Map}.
   * The {@code cleartext} parameter must contain at least one non-blank entry (with a non-blank key).
   */
  public RecordData(Map<String, String> cleartext) {
    if(cleartext == null)
      throw new IllegalArgumentException("cleartext null");

    checkMap(cleartext, "cleartext");
    this.cleartext = cleartext;
  }


  /**
   * The unencrypted data that will be encrypted and written to E3DB.
   */
  public Map<String, String> getCleartext() {
    return cleartext;
  }

}
