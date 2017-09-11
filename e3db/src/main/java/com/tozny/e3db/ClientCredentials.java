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
   * @return
   */
  String apiKey();

  /**
   * Password (API secret) for the client.
   * @return
   */
  String apiSecret();

  /**
   * ID of the client.
   * @return
   */
  UUID clientId();

  /**
   * Name of the client. Can be empty but never null.
   * @return
   */
  String name();

  /**
   * Public key for the client, as a Base64URL encoded string.
   * @return
   */
  String publicKey();

  /**
   * Indicates if the client is enabled or not.
   * @return
   */
  boolean enabled();

  /**
   * API  host with which the client was registered (always 'https://api.e3db.com').
   * @return
   */
  String host();
}
