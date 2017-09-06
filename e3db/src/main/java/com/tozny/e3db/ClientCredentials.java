package com.tozny.e3db;

import java.util.UUID;

public interface ClientCredentials {
  String apiKey();
  String apiSecret();
  UUID clientId();
  String name();
  String publicKey();
  boolean enabled();
}
