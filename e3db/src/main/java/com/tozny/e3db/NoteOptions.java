package com.tozny.e3db;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class NoteOptions {
  //Premium options
  UUID clientID;
  int maxViews;
  String noteName;
  Instant expiration;
  boolean expires;

  //Non-premium option
  String noteType;
  Map<String, String> plain;
  Map<String, String> fileMeta;

  public NoteOptions(UUID clientID, int maxViews, String noteName, Instant expiration, boolean expires, String noteType, Map<String, String> plain, Map<String, String> fileMeta) {
    this.clientID = clientID;
    this.maxViews = maxViews;
    this.noteName = noteName;
    this.expiration = expiration;
    this.expires = expires;
    this.noteType = noteType;
    this.plain = plain;
    this.fileMeta = fileMeta;
  }

  public NoteOptions() {}

  public UUID getClientID() {
    return clientID;
  }

  public void setClientID(UUID clientID) {
    this.clientID = clientID;
  }

  public int getMaxViews() {
    return maxViews;
  }

  public void setMaxViews(int maxViews) {
    this.maxViews = maxViews;
  }

  public String getNoteName() {
    return noteName;
  }

  public void setNoteName(String noteName) {
    this.noteName = noteName;
  }

  public Instant getExpiration() {
    return expiration;
  }

  public void setExpiration(Instant expiration) {
    this.expiration = expiration;
  }

  public boolean isExpires() {
    return expires;
  }

  public void setExpires(boolean expires) {
    this.expires = expires;
  }

  public String getNoteType() {
    return noteType;
  }

  public void setNoteType(String noteType) {
    this.noteType = noteType;
  }

  public Map<String, String> getPlain() {
    return plain;
  }

  public void setPlain(Map<String, String> plain) {
    this.plain = plain;
  }

  public Map<String, String> getFileMeta() {
    return fileMeta;
  }

  public void setFileMeta(Map<String, String> fileMeta) {
    this.fileMeta = fileMeta;
  }
}

