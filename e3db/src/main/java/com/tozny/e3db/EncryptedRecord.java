package com.tozny.e3db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.*;

import static com.tozny.e3db.Checks.checkNotNull;

/**
 * A record holding encrypted data, plus a signature. Should be used to persist records to storage (and to
 * restore them from storage, as well).
 *
 * <p>Usually produced by calling  {@link Client#encryptRecord(String, RecordData, Map, EAKInfo)}
 * or {@link Client#encryptExisting(Record, EAKInfo)}.
 */
public class EncryptedRecord implements Record {
  private final Map<String, String> data;
  private final RecordMeta meta;
  private final String signature;

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  /**
   * Create an instance with encrypted data, metadata, and the given signature.
   *
   * <p>This constructor
   * should generally not be called directly; instead consider using {@link Client#encryptRecord(String, RecordData, Map, EAKInfo)}
   * or {@link Client#encryptExisting(Record, EAKInfo)}.
   *
   * @param data Encrypted data. Cannot be {@code null}.
   * @param meta Metadata associated with the record. Note that only server-specific metadata will be ignored (i.e, {@link RecordMeta#created()},
   *             {@link RecordMeta#lastModified()}, etc.). Cannot be {@code null}.
   * @param signature Signature of the plaintext data and metadata. Cannot be {@code null}.
   */
  public EncryptedRecord(Map<String, String> data, RecordMeta meta, String signature) {
    this.data = data;
    this.meta = meta;
    this.signature = signature;
  }

  private static Map<String, String> getMap(JsonNode data) {
    Map<String, String> dataMap = new HashMap<>();

    Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
    while(fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      dataMap.put(field.getKey(), field.getValue().asText());
    }
    return dataMap;
  }

  /**
   * Encodes this instance to a string, suitable for storage. See {@link #encode(EncryptedRecord)} for details.
   * @return {@link #encode(EncryptedRecord)}.
   */
  public String encode() {
    return encode(this);
  }

  /**
   * Encode a record such that it can be decoded by {@link #decode(String)}. Note that this does <b>not</b> encrypt the
   * record (use {@link Client#encryptExisting(Record, EAKInfo)} or {@link Client#encryptRecord(String, RecordData, Map, EAKInfo)}
   * first).
   *
   * @param record Record to serialize. Cannot be {@code null}. {@code data()} cannot
   *               return {@code null} (though it can return an empty map). Values returned by {@code meta()} are treated as follows:
   *
   *               <ul>
   *               <li>{@code recordId()}, {@code lastModified()}, {@code version()}, and {@code created()} - Not serialized.</li>
   *               <li>{@code type()}, {@code userId()}, and {@code writerId()} - Always serialized. Cannot be {@code null}.</li>
   *               <li>{@code plain()} and {@code signature} - Serialized only if non-{@code null}.</li>
   *               </ul>
   *
   * @return Encoded representation of the record.
   */
  public static String encode(EncryptedRecord record) {
    checkNotNull(record, "record");

    try {
      HashMap<String, String> meta = new HashMap<>();
      meta.put("type", record.meta().type());

      if (record.meta().plain() != null)
        meta.put("plain", mapper.writeValueAsString(record.meta().plain()));

      meta.put("user_id", record.meta().userId().toString());
      meta.put("writer_id", record.meta().writerId().toString());

      HashMap<String, Object> r = new HashMap<>();
      r.put("data", record.data());
      r.put("meta", meta);
      r.put("rec_sig", record.signature());

      return mapper.writeValueAsString(r);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Decode a string produced by {@link #encode()} into a {@code LocalRecord} instance. Note that this does
   * <b>not</b> decrypt the record.
   *
   * @param record
   * @return The decoded record.
   * @throws IOException If the record cannot be decoded (missing values, ill-formed fields, or
   * an invalid encoding altogether).
   */
  public static EncryptedRecord decode(String record) throws IOException {
    JsonNode root = mapper.readTree(record);
    JsonNode data = root.get("data");
    JsonNode meta = root.get("meta");
    JsonNode signature = root.get("rec_sig");

    if(data == null)
      throw new IOException("data field missing: " + record);

    if(meta == null)
      throw new IOException("meta field missing: " + record);

    try {
      Map<String, String> dataMap = getMap(data);
      Map<String, String> plain = meta.get("plain") != null ? getMap(mapper.readTree(meta.get("plain").asText())) : null;
      LocalMeta localMeta = new LocalMeta(UUID.fromString(meta.get("writer_id").asText()),
          UUID.fromString(meta.get("user_id").asText()),
          meta.get("type").asText(),
          plain);

      return new EncryptedRecord(dataMap, localMeta, signature.asText());
    } catch (NullPointerException e) {
      throw new IOException("Field missing from record. Record: " + record, e);
    } catch (IllegalArgumentException e) {
      throw new IOException("A field could not be parsed. Record: " + record, e);
    }
  }

  @Override
  public RecordMeta meta() {
    return meta;
  }

  @Override
  public Map<String, String> data() {
    return data;
  }

  @Override
  public String signature() {
    return signature;
  }

  @Override
  public String toSerialized() {
    return LocalRecord.toSerialized(this);
  }

  @Override
  public Record document() {
    return this;
  }
}
