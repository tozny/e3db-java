package com.tozny.e3db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static com.tozny.e3db.Checks.checkNotNull;

/**
 * Represents metadata about a record.
 */
public class LocalMeta implements ClientMeta {
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  private final UUID writerId;
  private final UUID userId;
  private final String type;
  private final Map<String, String> plain;

  /**
   * Create instance.
   *
   * @param writerId ID of the writer.
   * @param userId ID of the user whom this record is about (typically equal to {@code writerId}).
   * @param type Type of the record.
   * @param plain Plaintext metadata about the record. Can be {@code null}.
   */
  public LocalMeta(UUID writerId, UUID userId, String type, Map<String, String> plain) {
    checkNotNull(writerId, "writerId");
    checkNotNull(userId, "userId");
    checkNotNull(type, "type");

    this.writerId = writerId;
    this.userId = userId;
    this.type = type;
    this.plain = plain;
  }

  @Override
  public UUID writerId() {
    return writerId;
  }

  @Override
  public UUID userId() {
    return userId;
  }

  @Override
  public String type() {
    return type;
  }
  @Override
  public Map<String, String> plain() {
    return plain;
  }

  @Override
  public String toSerialized() {
    try {
      SortedMap<String, Object> clientMeta = new TreeMap<>();
      clientMeta.put("writer_id", writerId().toString());
      clientMeta.put("user_id", userId().toString());
      clientMeta.put("type", type());
      clientMeta.put("plain", plain() == null ?
                                  new TreeMap<String, String>() :
                                  new TreeMap<>(plain()));

      return mapper.writeValueAsString(clientMeta);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
