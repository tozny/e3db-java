package com.tozny.e3db;

import java.util.Date;
import java.util.UUID;

public interface RecordMeta {
  UUID recordId();
  UUID writerId();
  Date created();
  Date lastModified();
  String version();
  String type();
  String plain();
}
