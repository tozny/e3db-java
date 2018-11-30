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

import com.goterl.lazycode.lazysodium.LazySodium;
import com.goterl.lazycode.lazysodium.LazySodiumAndroid;
import com.goterl.lazycode.lazysodium.exceptions.SodiumException;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.goterl.lazycode.lazysodium.interfaces.SecretBox;
import com.goterl.lazycode.lazysodium.interfaces.SecretStream;
import com.goterl.lazycode.lazysodium.interfaces.Sign;
import com.tozny.e3db.crypto.*;

import static com.tozny.e3db.Checks.*;
import com.goterl.lazycode.lazysodium.SodiumAndroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

class AndroidCrypto implements Crypto {
  private static final int BLOCK_SIZE = 65_536; // 2 ^ 16
  private static final int SECRET_STREAM_TAG_MESSAGE = 0x0;

  private final LazySodium lazySodium;
  private static final Charset UTF8 = Charset.forName("UTF-8");
  public AndroidCrypto() {
    lazySodium = new LazySodiumAndroid(Init.sodium);
  }

  private static class Init {
    // Static inner class as a singleton to make sure
    public static final SodiumAndroid sodium;
    // Sodium library is initialized once and only once.
    static {
      try {
         sodium = new SodiumAndroid();
      }
      catch(Throwable ex) {
        ex.printStackTrace();
        throw new RuntimeException(ex);
      }
    }
  }

  @Override
  public CipherSuite suite() {
    return CipherSuite.Sodium;
  }

  @Override
  public CipherWithNonce encryptSecretBox(byte[] message, byte[] key) throws E3DBEncryptionException {
    checkNotNull(message, "message");
    checkNotEmpty(key, "key");
    byte[] nonce = lazySodium.randomBytesBuf(SecretBox.NONCEBYTES);
    byte[] cipher = new byte[SecretBox.MACBYTES + message.length];
    if(! lazySodium.cryptoSecretBoxEasy(cipher, message, message.length, nonce, key))
      throw new E3DBEncryptionException("Could not encrypt message.");

    return new CipherWithNonce(cipher, nonce);
  }

  @Override
  public byte[] decryptSecretBox(CipherWithNonce message, byte[] key) throws E3DBDecryptionException {
    checkNotNull(message, "message");
    checkNotEmpty(key, "key");
    byte[] messageBytes = new byte[message.getCipher().length - SecretBox.MACBYTES];
    if(! lazySodium.cryptoSecretBoxOpenEasy(messageBytes, message.getCipher(), message.getCipher().length, message.getNonce(), key))
      throw new E3DBDecryptionException("Could not decrypt message.");
    return messageBytes;
  }

  @Override
  public CipherWithNonce encryptBox(byte[] message, byte[] publicKey, byte[] privateKey) throws E3DBEncryptionException {
    checkNotNull(message, "message");
    checkNotEmpty(publicKey, "publicKey");
    checkNotEmpty(privateKey, "privateKey");

    byte[] nonce = lazySodium.randomBytesBuf(Box.NONCEBYTES);
    byte[] cipher = new byte[Box.MACBYTES + message.length];
    if(! lazySodium.cryptoBoxEasy(cipher, message, message.length, nonce, publicKey, privateKey))
      throw new E3DBEncryptionException("Unable to encrypt message.");

    return new CipherWithNonce(cipher, nonce);
  }

  @Override
  public byte[] decryptBox(CipherWithNonce message, byte[] publicKey, byte[] privateKey) throws E3DBDecryptionException {
    checkNotNull(message, "message");
    checkNotNull(publicKey, "publicKey");
    checkNotNull(privateKey, "privateKey");
    byte[] messageBytes = new byte[message.getCipher().length - Box.MACBYTES];
    if(! lazySodium.cryptoBoxOpenEasy(messageBytes, message.getCipher(), message.getCipher().length, message.getNonce(), publicKey, privateKey))
      throw new E3DBDecryptionException("Could not decrypt message.");

    return messageBytes;
  }

  @Override
  public byte[] newPrivateKey() throws E3DBCryptoException {
    try {
      return lazySodium.cryptoBoxKeypair().getSecretKey();
    } catch (SodiumException e) {
      throw new E3DBCryptoException("Failure to get secret key", e);
    }
  }

  @Override
  public byte[] getPublicKey(byte[] privateKey) throws E3DBCryptoException {
    checkNotEmpty(privateKey, "privateKey");
    try {
      return lazySodium.cryptoScalarMultBase(privateKey).getPublicKey();
    } catch (SodiumException e) {
      throw new E3DBCryptoException("Failure to get public key", e);
    }
  }

  @Override
  public byte[] newSecretKey() {
    byte [] key = new byte[SecretBox.KEYBYTES];
    lazySodium.cryptoSecretBoxKeygen(key);
    return key;
  }

  @Override
  public byte[] signature(byte[] message, byte[] signingKey) throws E3DBCryptoException {
    byte[] signatureBytes = new byte[Sign.BYTES];

    if(! lazySodium.cryptoSignDetached(signatureBytes, new long[]{0}, message, message.length, signingKey))
      throw new E3DBCryptoException("Unable to sign document.");

    return signatureBytes;
  }

  @Override
  public boolean verify(Signature signature, byte[] message, byte[] publicSigningKey) {
    checkNotNull(signature, "signature");
    checkNotNull(message, "message");
    checkNotNull(publicSigningKey, "publicSigningKey");

    return lazySodium.cryptoSignVerifyDetached(signature.bytes, message, message.length, publicSigningKey);
  }

  @Override
  public byte[] newPrivateSigningKey() throws E3DBCryptoException {
    try {
      return lazySodium.cryptoSignKeypair().getSecretKey();
    } catch (SodiumException e) {
      throw new E3DBCryptoException(e);
    }
  }

  @Override
  public byte[] getPublicSigningKey(byte[] privateKey) throws E3DBCryptoException {
    try {
      return lazySodium.cryptoSignSecretKeyPair(privateKey).getPublicKey();
    } catch (SodiumException e) {
      throw new E3DBCryptoException(e);
    }
  }

  @Override
  public File encryptFile(File file, byte[] secretKey) throws IOException, E3DBEncryptionException {
    byte[] dataKey = new byte[SecretStream.KEYBYTES];
    lazySodium.cryptoSecretStreamKeygen(dataKey);
    byte[] header = new byte[SecretStream.HEADERBYTES];
    SecretStream.State state = new SecretStream.State();
    if(! lazySodium.cryptoSecretStreamInitPush(state, header, dataKey))
      throw new E3DBEncryptionException("Error initializing encryption operation.");

    CipherWithNonce edk = encryptSecretBox(dataKey, secretKey);
    String version = "3";
    String e3dbHeader = new StringBuilder()
        .append(version)
        .append(".")
        .append(edk.toMessage())
        .append(".")
        .toString();

    File encryptedFile = File.createTempFile("e2e-", ".bin", new File(file.getParent()));
    FileOutputStream out = new FileOutputStream(encryptedFile);
    FileInputStream source = new FileInputStream(file);
    try {
      out.write(e3dbHeader.getBytes(UTF8));
      out.write(header);

      // Simulate a 2-element queue, which makes it
      // easy to detect EOF.
      byte[] head = new byte[BLOCK_SIZE];
      byte[] next = new byte[head.length];
      byte[] cipher = new byte[head.length + SecretStream.ABYTES];

      for(int headAmt = source.read(head), nextAmt = source.read(next);
          headAmt != -1;
          headAmt = nextAmt, head = next, nextAmt = source.read(next)) {

        byte messageTag = nextAmt != -1 ? SECRET_STREAM_TAG_MESSAGE : SecretStream.TAG_FINAL;
        if(! lazySodium.cryptoSecretStreamPush(state, cipher, head, headAmt, messageTag))
          throw new E3DBEncryptionException("Error encrypting file.");

        out.write(cipher, 0, headAmt + SecretStream.ABYTES);
      }
    } finally {
      out.close();
      source.close();
    }

    return encryptedFile;
  }

  @Override
  public void decryptFile(File encrypted, byte[] secretKey, File dest) throws IOException, E3DBDecryptionException {
    FileOutputStream out = new FileOutputStream(dest, false);
    FileInputStream in = new FileInputStream(encrypted);
    try {

      // Read version
      FileVersion v = FileVersion.fromValue(new String(new byte[]{(byte) in.read()}, UTF8));
      if(v != FileVersion.CORRECTED_MESSAGE_TAG && v != FileVersion.INITIAL)
        throw new E3DBDecryptionException("Unknown file version: " + v);
      in.read(); // "."

      // Read EDK/EDKn
      byte[] dataKey;
      {
        int separators = 0;
        StringBuffer b = new StringBuffer();
        while (separators < 2) {
          String c = new String(new byte[]{(byte) in.read()}, UTF8);
          if (c.equalsIgnoreCase("."))
            separators++;

          if (separators < 2)
            b.append(c);
        }

        dataKey = decryptSecretBox(CipherWithNonce.decode(b.toString()), secretKey);
      }

      // Read Header
      byte[] header = new byte[SecretStream.HEADERBYTES];
      in.read(header);

      // Decrypt
      {
        SecretStream.State state = new SecretStream.State();
        if(! lazySodium.cryptoSecretStreamInitPull(state, header, dataKey))
          throw new E3DBDecryptionException("Error initializing decrypt operation.");

        byte[] cipherBlock = new byte[BLOCK_SIZE + SecretStream.ABYTES];
        byte[] tag = new byte[1];
        boolean sawFinal = false;

        for (int cipherAmt = in.read(cipherBlock); cipherAmt != -1; cipherAmt = in.read(cipherBlock)) {
          if(sawFinal)
            throw new E3DBDecryptionException("Unexpected trailing data.");

          byte[] messageBlock = new byte[cipherAmt - SecretStream.ABYTES];
          if(! lazySodium.cryptoSecretStreamPull(state, messageBlock, tag, cipherBlock, cipherAmt))
            throw new E3DBDecryptionException("Decryption error.");

          switch(v) {
            case INITIAL:
              // For backwards compatibility support, as the lazysodium library had a bug
              // and all messages were tagged final.
              if(tag[0] != SecretStream.TAG_FINAL)
                throw new E3DBDecryptionException("Invalid decryption.");
              break;
            case CORRECTED_MESSAGE_TAG:
              if(tag[0] != SECRET_STREAM_TAG_MESSAGE && tag[0] != SecretStream.TAG_FINAL)
                throw new E3DBDecryptionException("Invalid decryption.");
              break;
          }

          sawFinal = tag[0] == SecretStream.TAG_FINAL;
          out.write(messageBlock);
        }

        if(! sawFinal)
          throw new E3DBDecryptionException("Invalid file.");
      }
    }
    finally {
      in.close();
      out.close();
    }
  }

  @Override
  public int getBlockSize() {
    return BLOCK_SIZE;
  }
}
