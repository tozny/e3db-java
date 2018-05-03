package com.tozny.e3db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.*;

public class LocalEncryptedRecord implements EncryptedRecord {
  private final Map<String, String> data;
  private final ClientMeta meta;
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
   * or {@link Client#encryptExisting(LocalRecord, EAKInfo)}.
   *
   * @param data Encrypted data. Cannot be {@code null}.
   * @param meta Metadata associated with the record. Note that only server-specific metadata will be ignored (i.e, {@link RecordMeta#created()},
   *             {@link RecordMeta#lastModified()}, etc.). Cannot be {@code null}.
   * @param signature Signature of the plaintext data and metadata. Cannot be {@code null}.
   */
  public LocalEncryptedRecord(Map<String, String> data, ClientMeta meta, String signature) {
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
   * Encode a record such that it can be decoded by {@link #decode(String)}.
   * @return Encoded representation of the record.
   */
  public String encode() {
    try {
      HashMap<String, Object> meta1 = new HashMap<>();
      meta1.put("type", meta().type());

      if (meta().plain() != null)
        meta1.put("plain", meta().plain());

      meta1.put("user_id", meta().userId().toString());
      meta1.put("writer_id", meta().writerId().toString());

      HashMap<String, Object> r = new HashMap<>();
      r.put("data", data());
      r.put("meta", meta1);
      r.put("rec_sig", signature());

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
  public static LocalEncryptedRecord decode(String record) throws IOException {
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
      Map<String, String> plain = meta.get("plain") != null ? getMap(meta.get("plain")) : null;
      LocalMeta clientMeta = new LocalMeta(UUID.fromString(meta.get("writer_id").asText()),
          UUID.fromString(meta.get("user_id").asText()),
          meta.get("type").asText(),
          plain);

      return new LocalEncryptedRecord(dataMap, clientMeta, signature.asText());
    } catch (NullPointerException e) {
      throw new IOException("Field missing from record. Record: " + record, e);
    } catch (IllegalArgumentException e) {
      throw new IOException("A field could not be parsed. Record: " + record, e);
    }
  }

  @Override
  public ClientMeta meta() {
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
    try {
      SortedMap<String, Object> serializable = new TreeMap<>();
      SortedMap<String, Object> metaMap = new TreeMap<>();
      metaMap.put("type", meta.type());
      metaMap.put("plain", meta.plain());
      metaMap.put("user_id", meta.userId().toString());
      metaMap.put("writer_id", meta.writerId().toString());

      serializable.put("data", data());
      serializable.put("meta", metaMap);
      serializable.put("rec_sig", signature());
     return mapper.writeValueAsString(serializable);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public LocalEncryptedRecord document() {
    return this;
  }
}
