package com.tozny.e3db;

import java.util.UUID;

/**
 * Holds information necessary to authenticate with E3DB. See {@link Client#register(String, String, String, String, ResultHandler)} for
 * registering and capturing credentials; Use {@link Config#fromJson(String)} and {@link Config#json()} to convert credentials to a
 * standard JSON format.
 */
public interface ClientCredentials {
  /**
   * Username (API Key) for the client.
   */
  String apiKey();

  /**
   * Password (API secret) for the client.
   */
  String apiSecret();

  /**
   * ID of the client.
   */
  UUID clientId();

  /**
   * Name of the client. Can be empty but never null.
   */
  String name();

  /**
   * Public key for the client, as a Base64URL encoded string.
   */
  String publicKey();

  /**
   * Indicates if the client is enabled or not.
   */
  boolean enabled();

  /**
   * API  host with which the client was registered (always 'https://api.e3db.com').
   */
  String host();
}
