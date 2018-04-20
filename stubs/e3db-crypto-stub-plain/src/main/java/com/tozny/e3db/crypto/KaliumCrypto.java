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

import com.tozny.e3db.CipherWithNonce;
import com.tozny.e3db.Crypto;
import com.tozny.e3db.Signature;

public class KaliumCrypto implements Crypto {
  @Override
  public CipherWithNonce encryptSecretBox(byte[] message, byte[] key) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] decryptSecretBox(CipherWithNonce message, byte[] key) {
    throw new IllegalStateException();
  }

  @Override
  public CipherWithNonce encryptBox(byte[] message, byte[] publicKey, byte[] privateKey) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] getPublicKey(byte[] privateKey) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] newPrivateKey() {
    throw new IllegalStateException();
  }

  @Override
  public byte[] newSecretKey() {
    throw new IllegalStateException();
  }

  @Override
  public byte[] signature(byte[] message, byte[] signingKey) {
    throw new IllegalStateException();
  }

  @Override
  public boolean verify(Signature signature, byte[] message, byte[] publicSigningKey) {
    throw new IllegalStateException();
  }

  @Override
  public byte[] newPrivateSigningKey() {
    throw new IllegalStateException();
  }

  @Override
  public byte[] getPublicSigningKey(byte[] privateKey) {
    throw new IllegalStateException();
  }
}

