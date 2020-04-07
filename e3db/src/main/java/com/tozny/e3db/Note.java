package com.tozny.e3db;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class Note {
  @JsonProperty("note_id")
  UUID noteID;
  @JsonProperty("id_string")
  String noteName;
  @JsonProperty("client_id")
  String clientId;
  @JsonProperty("mode")
  String mode;
  @JsonProperty("recipient_signing_key")
  String recipientSigningKey;
  @JsonProperty("writer_signing_key")
  String writerSigningKey;
  @JsonProperty("writer_encryption_key")
  String writerEncryptionKey;
  @JsonProperty("encrypted_access_key")
  String eak;
  @JsonProperty("type")
  String type;
  @JsonProperty("data")
  Map<String, String> data;
  @JsonProperty("plain")
  Map<String, String> plain;
  @JsonProperty("file_meta")
  Map<String, String> fileMeta;
  @JsonProperty("signature")
  String signature;
  @JsonProperty("created_at")
  Date createdAt;
  @JsonProperty("max_views")
  int maxViews;
  @JsonProperty("views")
  int views;
  @JsonProperty("expiration")
  Date expiration;
  @JsonProperty("expires")
  boolean expires;
  @JsonProperty("eacp")
  EACP eacp;

  public Note(String noteName,
              String clientId,
              String mode,
              String recipientSigningKey,
              String writerSigningKey,
              String writerEncryptionKey,
              String eak,
              String type,
              Map<String, String> data,
              Map<String, String> plain,
              Map<String, String> fileMeta,
              String signature,
              int maxViews,
              Date expiration,
              boolean expires,
              EACP eacp) {
    this.noteName = noteName;
    this.clientId = clientId;
    this.mode = mode;
    this.recipientSigningKey = recipientSigningKey;
    this.writerSigningKey = writerSigningKey;
    this.writerEncryptionKey = writerEncryptionKey;
    this.eak = eak;
    this.type = type;
    this.data = data;
    this.plain = plain;
    this.fileMeta = fileMeta;
    this.signature = signature;
    this.maxViews = maxViews;
    this.expiration = expiration;
    this.expires = expires;
    this.eacp = eacp;
  }

  public Note() {

  }

  @JsonProperty(value = "expiration")
  public String getExpirationISO8601() {
    if (this.expiration == null) {
      return null;
    }
    return this.expiration.toString();
  }

  @JsonProperty(value = "created_at")
  public String getCreatedAtISO8601() {
    if (this.createdAt == null) {
      return null;
    }
    return this.createdAt.toString();
  }

  public UUID getNoteID() {
    return noteID;
  }

  public String getNoteName() {
    return noteName;
  }

  public String getClientId() {
    return clientId;
  }

  public String getMode() {
    return mode;
  }

  public String getRecipientSigningKey() {
    return recipientSigningKey;
  }

  public String getWriterSigningKey() {
    return writerSigningKey;
  }

  public String getWriterEncryptionKey() {
    return writerEncryptionKey;
  }

  public String getEAK() {
    return eak;
  }

  public String getType() {
    return type;
  }

  public Map<String, String> getData() {
    return data;
  }

  public Map<String, String> getPlain() {
    return plain;
  }

  public Map<String, String> getFileMeta() {
    return fileMeta;
  }

  public String getSignature() {
    return signature;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public int getMaxViews() {
    return maxViews;
  }

  public int getViews() {
    return views;
  }

  public Date getExpiration() {
    return expiration;
  }

  public boolean isExpires() {
    return expires;
  }

  public EACP getEacp() {
    return eacp;
  }
}