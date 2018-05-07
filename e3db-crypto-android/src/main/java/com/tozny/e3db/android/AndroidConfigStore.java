/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

package com.tozny.e3db.android;

import android.content.Context;
import com.tozny.e3db.ConfigStore;

import javax.crypto.Cipher;

/**
 * Implements secure configuration storage. Uses the Android Key Store or a filesystem-based
 * keystore to protect the configuration, depending on the key protection selected and the SDK level of the device.
 */
public class AndroidConfigStore implements ConfigStore {
  private final Context context;
  private final String identifier;
  private final KeyAuthentication protection;
  private final KeyAuthenticator keyAuthenticator;

  private static final String pattern = "^[a-zA-Z0-9-_]+$";
  private static final String COM_TOZNY_E3DB_CRYPTO = "com.tozny.e3db.crypto-";
  private static final String DEFAULT_IDENTIFER = "credentials";

  /**
   * Save, remove, or load a config (with a default identifier) that is not protected by a password
   * or other mechanism.
   * @param context Application context.
   */
  public AndroidConfigStore(Context context) {
    this(context, DEFAULT_IDENTIFER, KeyAuthentication.withNone(), KeyAuthenticator.noAuthentication());
  }

  /**
   * Save, remove, or load a config (with a default identifier) that is protected by an authentication method.
   * @param context Application context. Cannot be {@code null}.
   * @param protection Protection used for securing the configuration. Cannot be {@code null}. If this uses the '{@link KeyAuthentication.KeyAuthenticationType#NONE}' protection type, consider using thie
   *                   {@link #AndroidConfigStore(Context)} constructor instead.
   * @param keyAuthenticator Used to authenticate with the user when saving, loading or removing the key. Cannot be {@code null}.
   */
  public AndroidConfigStore(Context context, KeyAuthentication protection, KeyAuthenticator keyAuthenticator) {
    this(context, DEFAULT_IDENTIFER, protection, keyAuthenticator);
  }

  /**
   * Save, remove, or load a config (with the given identifier) that is not protected by a password
   * or other mechanism.
   *
   * @param context Application context. Cannot be {@code null}.
   * @param identifier Identifier for the config. Cannot be {@code null} and must be valid as defined by the {@link #validIdentifier(String)} method.
   */
  public AndroidConfigStore(Context context, String identifier) {
    this(context, identifier, KeyAuthentication.withNone(), KeyAuthenticator.noAuthentication());
  }

  /**
   * Save, remove, or load a config (with the given identifer) that is protected by an authentication method.
   *
   * @param context Application context. Cannot be {@code null}.
   * @param identifier Identifier for the config. Cannot be {@code null} and must be valid as defined by the {@link #validIdentifier(String)} method.
   * @param protection Protection used for securing the configuration. Cannot be {@code null}. If this uses the '{@link KeyAuthentication.KeyAuthenticationType#NONE}' protection type, consider using this
   *                   {@link #AndroidConfigStore(Context, String)} constructor instead.
   * @param keyAuthenticator Used to authenticate with the user when saving, loading or removing the key. Cannot be {@code null}.
   */
  public AndroidConfigStore(Context context, String identifier, KeyAuthentication protection, KeyAuthenticator keyAuthenticator) {
    if(context == null)
      throw new IllegalArgumentException("context cannot be null.");
    if(protection == null)
      throw new IllegalArgumentException("protection cannot be null.");
    if(keyAuthenticator == null)
      throw new IllegalArgumentException("keyAuthenticator cannot be null.");

    checkIdentifier(identifier);

    this.context    = context;
    this.identifier = identifier;
    this.protection = protection;
    this.keyAuthenticator = keyAuthenticator;
  }

  private static void checkIdentifier(String identifier) {
    if(identifier == null || identifier.trim().isEmpty())
      throw new IllegalArgumentException("identifier cannot be null or blank.");
    if (!identifier.matches(pattern)) {
      throw new IllegalArgumentException("Identifier string can only contain alphanumeric characters, underscores, and hyphens.");
    }
    if (identifier.length() > 100) /* In case device file system limits filenames 127 characters (and we're adding roughly 25 characters). */
      throw new IllegalArgumentException("Identifier string cannot be more than 100 characters in length.");
  }

  private static String full(String identifier, KeyAuthentication protection) {
    final KeyAuthentication.KeyAuthenticationType keyAuthenticationType = protection.authenticationType();
    switch (keyAuthenticationType) {
      case NONE:
        return COM_TOZNY_E3DB_CRYPTO + identifier + "-NO";
      case FINGERPRINT:
        return COM_TOZNY_E3DB_CRYPTO + identifier + "-FP";
      case LOCK_SCREEN:
        return COM_TOZNY_E3DB_CRYPTO + identifier + "-LS";
      case PASSWORD:
        return COM_TOZNY_E3DB_CRYPTO + identifier + "-PW";
      default:
        throw new RuntimeException("Unrecognized key protection type: " +  keyAuthenticationType.name());
    }
  }

  /**
   * Determines if the given identifier is valid. Must consist of only
   * alphanumeric characters, underscore, or hyphens; cannot be greater than 100
   * characters long, and cannot be null or blank.
   * @param identifier Identifier to check.
   * @return {@code true} if the identifier is valid; {@code false} otherwise.
   */
  public static boolean validIdentifier(String identifier) {
    try {
      checkIdentifier(identifier);
      return true;
    }
    catch(IllegalArgumentException e) { }

    return false;
  }

  @Override
  public void save(final String config, final SaveHandler SaveHandler) {
    if (config == null)
      throw new IllegalArgumentException("config cannot be null.");

    if (SaveHandler == null)
      throw new IllegalArgumentException("SaveHandler cannot be null.");

    try {
      final String fullIdentifier = full(identifier, protection);

      KeyStoreManager.getCipher(context, fullIdentifier, protection, keyAuthenticator, new CipherManager.SaveCipherGetter(protection.authenticationType()), new KeyStoreManager.AuthenticatedCipherHandler() {
        @Override
        public void onAuthenticated(Cipher cipher) {
          SecureStringManager.saveStringToSecureStorage(context, fullIdentifier, config, cipher);

          SaveHandler.saveConfigDidSucceed();
        }

        @Override
        public void onCancel() {
          SaveHandler.saveConfigDidCancel();
        }

        @Override
        public void onError(Throwable e) {
          SaveHandler.saveConfigDidFail(new RuntimeException(e));
        }
      });

    } catch (Throwable e) {
      SaveHandler.saveConfigDidFail(e);
    }
  }

  @Override
  public void load(final LoadHandler LoadHandler) {
    if (LoadHandler == null)
      throw new IllegalArgumentException("LoadHandler cannot be null.");

    try {
      final String fullIdentifier = full(identifier, protection);

      if (!SecureStringManager.secureStringExists(context, fullIdentifier)) {
        LoadHandler.loadConfigNotFound();

      } else {
        KeyStoreManager.getCipher(context, fullIdentifier, protection, keyAuthenticator, new CipherManager.LoadCipherGetter(protection.authenticationType()), new KeyStoreManager.AuthenticatedCipherHandler() {
          @Override
          public void onAuthenticated(Cipher cipher) {
            String configString = SecureStringManager.loadStringFromSecureStorage(context, fullIdentifier, cipher);

            LoadHandler.loadConfigDidSucceed(configString);
          }

          @Override
          public void onCancel() {
            LoadHandler.loadConfigDidCancel();
          }

          @Override
          public void onError(Throwable e) {
            LoadHandler.loadConfigDidFail(new RuntimeException(e));
          }
        });

      }
    } catch (Throwable e) {
      LoadHandler.loadConfigDidFail(e);
    }
  }

  @Override
  public void remove(RemoveHandler RemoveHandler) {
    if (RemoveHandler == null)
      throw new IllegalArgumentException("RemoveHandler");

    try {
      String fullIdentifier = full(identifier, protection);
      try { try {
          KeyStoreManager.removeSecretKey(context, fullIdentifier);
        } finally {
          SecureStringManager.deleteStringFromSecureStorage(context, fullIdentifier);
        }
      }
      finally {
        CipherManager.deleteInitializationVector(context, fullIdentifier);
      }

      RemoveHandler.removeConfigDidSucceed();
    } catch (Throwable e) {
      RemoveHandler.removeConfigDidFail(e);
    }
  }
}
