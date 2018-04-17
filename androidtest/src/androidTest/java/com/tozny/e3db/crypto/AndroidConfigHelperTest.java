package com.tozny.e3db.crypto;

import android.app.Application;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;
import com.tozny.e3db.Crypto;
import com.tozny.e3db.Signature;
import org.junit.Test;
import org.libsodium.jni.crypto.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.SecureRandom;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.libsodium.jni.Sodium.crypto_sign_bytes;
import static org.libsodium.jni.Sodium.crypto_sign_publickeybytes;

public class AndroidConfigHelperTest {
    private static final String TAG = "AndroidConfigHelperTest";

    private static final String name  = "foo";
    private static final String loc   = "bar";
    private static final String alias = "baz";

    private static boolean fileExists(Context context, String privateFile) {
        File filesDir = context.getFilesDir();
        return new File(filesDir, privateFile).exists();
    }

    @Test
    public void testFileSystemKeystore() {
        Context context = InstrumentationRegistry.getTargetContext();

        try {
            if (fileExists(context, name) || fileExists(context, loc)) {
                File file = new File(context.getFilesDir(), name);
                file.delete();
                file = new File(context.getFilesDir(), loc);
                file.delete();
            }

            assertFalse(fileExists(context, name));
            assertFalse(fileExists(context, loc));

            Class<?> clazz = Class.forName("com.tozny.e3db.crypto.FSKSWrapper");
            Method method = clazz.getDeclaredMethod("getFSKS", Context.class, String.class, String.class);
            method.setAccessible(true);
            Object o = method.invoke(null, context, name, loc);

            assertTrue(fileExists(context, name));
            assertTrue(fileExists(context, loc));

            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            SecureRandom random = new SecureRandom();
            keyGen.init(random);
            SecretKey secretKey = keyGen.generateKey();

            ((KeyStore)o).setEntry(alias, new KeyStore.SecretKeyEntry(secretKey), new KeyStore.PasswordProtection(null));

            method = clazz.getDeclaredMethod("saveFSKS", Context.class, String.class, String.class);
            method.setAccessible(true);
            o = method.invoke(null, context, name, loc);

            method = clazz.getDeclaredMethod("getFSKS", Context.class, String.class, String.class);
            method.setAccessible(true);
            o = method.invoke(null, context, name, loc);

            assertTrue(((KeyStore)o).containsAlias(alias));

            assertTrue(fileExists(context, name));
            assertTrue(fileExists(context, loc));

        } catch (Exception e) {
            e.printStackTrace();
            assertFalse(e.getLocalizedMessage(), true);
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
