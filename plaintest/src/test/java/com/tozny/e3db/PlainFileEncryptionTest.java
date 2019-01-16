package com.tozny.e3db;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.security.SecureRandom;

import static org.junit.Assert.assertTrue;

public class PlainFileEncryptionTest {

  @Test
  public void testEncryptMultiMegabyteFile() throws Exception {
    String textFileName = "/com/tozny/e3db/austen2.txt";
    SecureRandom secureRandom = new SecureRandom();
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    String resource = getClass().getResource(textFileName).getPath();
    File originalFile = new File(resource);
    PlainCrypto plainCrypto = new PlainCrypto();
    File encryptedFile = plainCrypto.encryptFile(originalFile, bytes);

    File dest = File.createTempFile("austen", ".txt");
    plainCrypto.decryptFile(encryptedFile, bytes, dest);
    File decryptedFile = new File(dest.getAbsolutePath());

    assertTrue("The files differ!", FileUtils.contentEquals(originalFile, decryptedFile));
  }

  @Test
  public void testEncryptSmallFile() throws Exception {
    String textFileName = "/com/tozny/e3db/smallFile";
    SecureRandom secureRandom = new SecureRandom();
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    String resource = getClass().getResource(textFileName).getPath();
    File originalFile = new File(resource);
    PlainCrypto plainCrypto = new PlainCrypto();
    File encryptedFile = plainCrypto.encryptFile(originalFile, bytes);

    File dest = File.createTempFile("small", ".txt");
    plainCrypto.decryptFile(encryptedFile, bytes, dest);
    File decryptedFile = new File(dest.getAbsolutePath());
    assertTrue("The files differ!", FileUtils.contentEquals(originalFile, decryptedFile));
  }
}
