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

package com.tozny.e3db;

import com.tozny.e3db.crypto.*;
import org.abstractj.kalium.NaCl;
import static org.abstractj.kalium.NaCl.Sodium.*;

import org.junit.*;
import static org.junit.Assert.*;

public class KaliumCryptoTest {
  private final Crypto crypto = new KaliumCrypto();

  @Test
  public void testValidSignature() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);

    byte[] document = new byte[100_000];
    NaCl.sodium().randombytes(document, document.length);
    byte[] signature = crypto.signature(document, privateSigningKey);
    assertTrue("Unable to verify signature on document.", crypto.verify(new Signature(signature), document, publicSigningKey));
  }

  @Test
  public void testInvalidSignature() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);

    byte[] document = new byte[100_000];
    byte[] signature = new byte[CRYPTO_SIGN_ED25519_BYTES];

    NaCl.sodium().randombytes(document, document.length);
    NaCl.sodium().randombytes(signature, signature.length);

    assertFalse("Verification should fail with wrong signature.", crypto.verify(new Signature(signature), document, publicSigningKey));
    assertFalse("Verification should fail with zero signature.", crypto.verify(new Signature(new byte[CRYPTO_SIGN_ED25519_BYTES]), document, publicSigningKey));
    assertFalse("Verification should fail with empty signature.", crypto.verify(new Signature(new byte[0]), document, publicSigningKey));
  }

  @Test
  public void testWrongPublicKey() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] wrongSigningKey = new byte[CRYPTO_SIGN_ED25519_PUBLICKEYBYTES];
    byte[] document = new byte[100_000];

    NaCl.sodium().randombytes(document, document.length);
    NaCl.sodium().randombytes(wrongSigningKey, wrongSigningKey.length);

    byte[] signature = crypto.signature(document, privateSigningKey);
    assertFalse("Verification should fail with wrong public key.", crypto.verify(new Signature(signature), document, wrongSigningKey));
    assertFalse("Verification should fail with zero public key.", crypto.verify(new Signature(signature), document, new byte[CRYPTO_SIGN_ED25519_PUBLICKEYBYTES]));
    assertFalse("Verification should fail with empty public key.", crypto.verify(new Signature(signature), document, new byte[0]));
  }

  @Test
  public void testWrongDocument() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);

    byte[] document = new byte[100_000];
    NaCl.sodium().randombytes(document, document.length);

    byte[] wrongDocument = new byte[100_000];
    NaCl.sodium().randombytes(wrongDocument, wrongDocument.length);

    byte[] signature = crypto.signature(document, privateSigningKey);

    assertFalse("Verification should fail with wrong document.", crypto.verify(new Signature(signature), wrongDocument, publicSigningKey));
    assertFalse("Verification should fail with zero document.", crypto.verify(new Signature(signature), new byte[100_000], publicSigningKey));
    assertFalse("Verification should fail with empty document.", crypto.verify(new Signature(signature), new byte[0], publicSigningKey));
  }
}
