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
import com.goterl.lazycode.lazysodium.LazySodiumJava;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Sign;
import com.tozny.e3db.crypto.*;

import org.junit.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PlainCryptoTest {
  private final Crypto crypto = new PlainCrypto();
  private final LazySodium lazySodium = new LazySodiumJava(new SodiumJava());

  @Test
  public void testValidSignature() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);

    byte[] document = lazySodium.randomBytesBuf(100_000);
    byte[] signature = crypto.signature(document, privateSigningKey);
    assertTrue("Unable to verify signature on document.", crypto.verify(new Signature(signature), document, publicSigningKey));
  }

  @Test
  public void testInvalidSignature() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);

    byte[] document = lazySodium.randomBytesBuf(100_000);
    byte[] signature = lazySodium.randomBytesBuf(Sign.ED25519_BYTES);

    assertFalse("Verification should fail with wrong signature.", crypto.verify(new Signature(signature), document, publicSigningKey));
    assertFalse("Verification should fail with zero signature.", crypto.verify(new Signature(new byte[Sign.ED25519_BYTES]), document, publicSigningKey));
    assertFalse("Verification should fail with empty signature.", crypto.verify(new Signature(new byte[0]), document, publicSigningKey));
  }

  @Test
  public void testWrongPublicKey() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] wrongSigningKey = lazySodium.randomBytesBuf(Sign.ED25519_PUBLICKEYBYTES);
    byte[] document = lazySodium.randomBytesBuf(100_000);
    byte[] signature = crypto.signature(document, privateSigningKey);

    assertFalse("Verification should fail with wrong public key.", crypto.verify(new Signature(signature), document, wrongSigningKey));
    assertFalse("Verification should fail with zero public key.", crypto.verify(new Signature(signature), document, new byte[Sign.ED25519_PUBLICKEYBYTES]));
    assertFalse("Verification should fail with empty public key.", crypto.verify(new Signature(signature), document, new byte[0]));
  }

  @Test
  public void testWrongDocument() {
    byte[] privateSigningKey = crypto.newPrivateSigningKey();
    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);
    byte[] document = lazySodium.randomBytesBuf(100_000);
    byte[] wrongDocument = lazySodium.randomBytesBuf(100_000);
    byte[] signature = crypto.signature(document, privateSigningKey);

    assertFalse("Verification should fail with wrong document.", crypto.verify(new Signature(signature), wrongDocument, publicSigningKey));
    assertFalse("Verification should fail with zero document.", crypto.verify(new Signature(signature), new byte[100_000], publicSigningKey));
    assertFalse("Verification should fail with empty document.", crypto.verify(new Signature(signature), new byte[0], publicSigningKey));
  }

  @Test
  public void testBlnsSerializationJavaMatchesNode() throws IOException {
    BlnsTest blns = new BlnsTest();
    List<Map<String, String>> javaTests = blns.serializeBlns();
    List<Map<String, String>> nodeTests = blns.loadBlnsResults("/com/tozny/e3db/blns-node.json");
    List<String> failedTests = BlnsTest.compareBlnsResults(javaTests, nodeTests);

    // known indexes for tests that fail based on known issues
    // 92, 94, 503, 504 have unicode case-sensitivity related errors
    List<String> knownFailures = Arrays.asList("92", "94", "503", "504");
    assertArrayEquals(knownFailures.toArray(), failedTests.toArray());
  }

  @Test
  public void testBlnsSerializationJavaMatchesSwift() throws IOException {
    BlnsTest blns = new BlnsTest();
    List<Map<String, String>> javaTests = blns.serializeBlns();
    List<Map<String, String>> swiftTests = blns.loadBlnsResults("/com/tozny/e3db/blns-swift.json");
    List<String> failedTests = BlnsTest.compareBlnsResults(javaTests, swiftTests);

    // known indexes for tests that fail based on known issues
    // 96 and 169 have hidden-whitespace related errors
    // 92, 94, 503, 504 have unicode case-sensitivity related errors
    List<String> knownFailures = Arrays.asList("92", "94", "96", "169", "503", "504");
    assertArrayEquals(knownFailures.toArray(), failedTests.toArray());
  }
}
