package com.tozny.e3db;

/**
 * Used to differentiate different version of large files that
 * were written.
 */
enum FileVersion {
  INITIAL("1"),
  FIPS("2"),
  CORRECTED_MESSAGE_TAG("3");

  public String getTag() {
    return tag;
  }

  private final String tag;

  FileVersion(String tag) {
    this.tag = tag;
  }
  public static FileVersion fromValue(String value) {
    for(FileVersion v : FileVersion.values())
      if(v.tag.equalsIgnoreCase(value))
        return v;

    throw new IllegalArgumentException("Unrecognized file version: " + value);
  }
}
