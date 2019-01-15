package com.tozny.e3db;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
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
    FileInputStream originalStream = new FileInputStream(originalFile);
    FileInputStream decryptedStream = new FileInputStream(decryptedFile);
    int blockSize = 65_536;
    //Block one matches
    byte[] originalBytes = new byte[blockSize];
    byte[] decryptedBytes = new byte[blockSize];
    int originalReadSize = originalStream.read(originalBytes);
    int decryptedReadSize = decryptedStream.read(decryptedBytes);
    assertThat(originalReadSize).isNotEqualTo(-1).isEqualTo(decryptedReadSize);
    assertThat(originalBytes).isEqualTo(decryptedBytes);
    //Block Two Matches
    int origSecondReadSize = originalStream.read(originalBytes);
    int decryptedSecondReadSize = decryptedStream.read(decryptedBytes);
    assertThat(origSecondReadSize).isNotEqualTo(-1).isEqualTo(decryptedSecondReadSize);
    assertThat(originalBytes).isEqualTo(decryptedBytes);


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
    FileInputStream originalStream = new FileInputStream(originalFile);
    FileInputStream decryptedStream = new FileInputStream(decryptedFile);
    int blockSize = 1000; //65_536;
    byte[] originalBytes = new byte[blockSize];
    byte[] decryptedBytes = new byte[blockSize];
    int originalRead = originalStream.read(originalBytes);
    int decryptedRead = decryptedStream.read(decryptedBytes);
    assertEquals(originalRead, decryptedRead);
    assertTrue(originalRead != -1);
    assertThat(originalBytes).usingDefaultElementComparator().isEqualTo(decryptedBytes);
    assertTrue("The files differ!", FileUtils.contentEquals(originalFile, decryptedFile));
  }
}
