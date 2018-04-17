package com.tozny.e3db.crypto;

import android.app.Application;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;
import com.tozny.e3db.Crypto;
import com.tozny.e3db.Signature;
import org.junit.Test;
import org.libsodium.jni.crypto.Random;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.libsodium.jni.Sodium.crypto_sign_bytes;
import static org.libsodium.jni.Sodium.crypto_sign_publickeybytes;

public class AndroidConfigHelperTest {
    private final Crypto crypto = new AndroidCrypto();
    private final Random random = new Random();

    private static final String TAG = "AndroidConfigHelperTest";

    @Test
    public void testFileSystemKeystore() {
        Context context = InstrumentationRegistry.getTargetContext();

        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("foo", Context.MODE_PRIVATE));
            outputStreamWriter.write("foo");
            outputStreamWriter.close();
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
        }


    }

    @Test
    public void testInvalidSignature() {
//    byte[] privateSigningKey = crypto.newPrivateSigningKey();
//    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);
//
//    byte[] document = random.randomBytes(100_000);
//    byte[] signature = random.randomBytes(crypto_sign_bytes());
//
//    assertFalse("Verification should fail with wrong signature.", crypto.verify(new Signature(signature), document, publicSigningKey));
//    assertFalse("Verification should fail with zero signature.", crypto.verify(new Signature(new byte[crypto_sign_bytes()]), document, publicSigningKey));
//    assertFalse("Verification should fail with empty signature.", crypto.verify(new Signature(new byte[0]), document, publicSigningKey));
    }

    @Test
    public void testWrongPublicKey() {
//    byte[] privateSigningKey = crypto.newPrivateSigningKey();
//    byte[] wrongSigningKey = random.randomBytes(crypto_sign_publickeybytes());
//    byte[] document = random.randomBytes(100_000);
//
//    byte[] signature = crypto.signature(document, privateSigningKey);
//    assertFalse("Verification should fail with wrong public key.", crypto.verify(new Signature(signature), document, wrongSigningKey));
//    assertFalse("Verification should fail with zero public key.", crypto.verify(new Signature(signature), document, new byte[crypto_sign_publickeybytes()]));
//    assertFalse("Verification should fail with empty public key.", crypto.verify(new Signature(signature), document, new byte[0]));
    }

    @Test
    public void testWrongDocument() {
//    byte[] privateSigningKey = crypto.newPrivateSigningKey();
//    byte[] publicSigningKey = crypto.getPublicSigningKey(privateSigningKey);
//    byte[] document = random.randomBytes(100_000);
//    byte[] wrongDocument = random.randomBytes(100_000);
//    byte[] signature = crypto.signature(document, privateSigningKey);
//
//    assertFalse("Verification should fail with wrong document.", crypto.verify(new Signature(signature), wrongDocument, publicSigningKey));
//    assertFalse("Verification should fail with zero document.", crypto.verify(new Signature(signature), new byte[100_000], publicSigningKey));
//    assertFalse("Verification should fail with empty document.", crypto.verify(new Signature(signature), new byte[0], publicSigningKey));
    }
}
