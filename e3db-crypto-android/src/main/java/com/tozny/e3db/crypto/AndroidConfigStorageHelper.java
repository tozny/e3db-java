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

package com.tozny.e3db.crypto;

import android.content.Context;
import com.tozny.e3db.ConfigStorageHelper;
import javax.crypto.Cipher;

public class AndroidConfigStorageHelper implements ConfigStorageHelper {
    private final Context context;
    private final String identifier;
    private final KeyProtection protection;
    private final KeyAuthenticator keyAuthenticator;

    private static final String pattern = "^[a-zA-Z0-9-_]+$";
    private final static String COM_TOZNY_E3DB_CRYPTO = "com.tozny.e3db.crypto-";

    /**
     * Create a helper for storing, deleting, or retrieving a config that is not protected by a password
     * or other mechanism.
     * @param context
     * @param identifier
     */
    public AndroidConfigStorageHelper(Context context, String identifier) {
        this(context, identifier, KeyProtection.withNone(), KeyAuthenticator.noAuthentication());
    }

    public AndroidConfigStorageHelper(Context context, String identifier, KeyProtection protection, KeyAuthenticator keyAuthenticator) {
        if(context == null)
            throw new IllegalArgumentException("context cannot be null.");
        if(identifier == null || identifier.trim().isEmpty())
            throw new IllegalArgumentException("identifier cannot be null or blank.");
        if (!identifier.matches(pattern)) {
            throw new IllegalArgumentException("Identifier string can only contain alphanumeric characters, underscores, and hyphens.");
        }
        if (identifier.length() > 100) /* In case device file system limits filenames 127 characters (and we're adding roughly 25 characters). */
            throw new IllegalArgumentException("Identifier string cannot be more than 100 characters in length.");
        if(protection == null)
            throw new IllegalArgumentException("protection cannot be null.");
        if(keyAuthenticator == null)
            throw new IllegalArgumentException("keyAuthenticator cannot be null.");

        this.context    = context;
        this.identifier = identifier;
        this.protection = protection;
        this.keyAuthenticator = keyAuthenticator;
    }

    private static String full(String identifier, KeyProtection protection) {
        final KeyProtection.KeyProtectionType keyProtectionType = protection.protectionType();
        switch (keyProtectionType) {
            case NONE:
                return COM_TOZNY_E3DB_CRYPTO + identifier + "-NO";
            case FINGERPRINT:
                return COM_TOZNY_E3DB_CRYPTO + identifier + "-FP";
            case LOCK_SCREEN:
                return COM_TOZNY_E3DB_CRYPTO + identifier + "-LS";
            case PASSWORD:
                return COM_TOZNY_E3DB_CRYPTO + identifier + "-PW";
            default:
                throw new RuntimeException("Unrecognized key protection type: " +  keyProtectionType.name());
        }
    }

    @Override
    public void saveConfigSecurely(final String config, final SaveConfigHandler saveConfigHandler) {
        if (config == null)
            throw new IllegalArgumentException("config cannot be null.");

        if (saveConfigHandler == null)
            throw new IllegalArgumentException("saveConfigHandler cannot be null.");

        try {
            final String fullIdentifier = full(identifier, protection);

            KeyStoreManager.getCipher(context, fullIdentifier, protection, keyAuthenticator, CipherManager.saveCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                @Override
                public void onAuthenticated(Cipher cipher) throws Throwable {
                    SecureStringManager.saveStringToSecureStorage(context, fullIdentifier, config, cipher);

                    saveConfigHandler.saveConfigDidSucceed();
                }

                @Override
                public void onCancel() {
                    saveConfigHandler.saveConfigDidCancel();
                }

                @Override
                public void onError(Throwable e) {
                    saveConfigHandler.saveConfigDidFail(new RuntimeException(e));
                }
            });

        } catch (Throwable e) {
            saveConfigHandler.saveConfigDidFail(e);
        }
    }

    @Override
    public void loadConfigSecurely(final LoadConfigHandler loadConfigHandler) {
        if (loadConfigHandler == null)
            throw new IllegalArgumentException("loadConfigHandler cannot be null.");

        try {
            final String fullIdentifier = full(identifier, protection);

            if (!SecureStringManager.secureStringExists(context, fullIdentifier)) {
                loadConfigHandler.loadConfigNotFound();

            } else {
                KeyStoreManager.getCipher(context, fullIdentifier, protection, keyAuthenticator, CipherManager.loadCipherGetter(), new KeyStoreManager.AuthenticatedCipherHandler() {
                    @Override
                    public void onAuthenticated(Cipher cipher) throws Throwable {
                        String configString = SecureStringManager.loadStringFromSecureStorage(context, fullIdentifier, cipher);

                        loadConfigHandler.loadConfigDidSucceed(configString);
                    }

                    @Override
                    public void onCancel() {
                        loadConfigHandler.loadConfigDidCancel();
                    }

                    @Override
                    public void onError(Throwable e) {
                        loadConfigHandler.loadConfigDidFail(new RuntimeException(e));
                    }
                });

            }
        } catch (Throwable e) {
            loadConfigHandler.loadConfigDidFail(e);
        }
    }

    @Override
    public void removeConfigSecurely(RemoveConfigHandler removeConfigHandler) {
        if (removeConfigHandler == null)
            throw new IllegalArgumentException("removeConfigHandler");

        try {
            String fullIdentifier = full(identifier, protection);

            Throwable throwable = null;
            
            try { KeyStoreManager.removeSecretKey(context, fullIdentifier); }
            catch (Throwable e) { throwable = e; }

            try { SecureStringManager.deleteStringFromSecureStorage(context, fullIdentifier); }
            catch (Throwable e) { throwable = e; }

            try { CipherManager.deleteInitializationVector(context, fullIdentifier); }
            catch (Throwable e) { throwable = e; }

            if (throwable != null) throw throwable;

            removeConfigHandler.removeConfigDidSucceed();
        } catch (Throwable e) {
            removeConfigHandler.removeConfigDidFail(new RuntimeException(e));
        }
    }
}
