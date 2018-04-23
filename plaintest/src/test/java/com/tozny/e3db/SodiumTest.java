package com.tozny.e3db;

import org.junit.*;
import static org.junit.Assert.*;

import org.abstractj.kalium.NaCl;
import org.abstractj.kalium.NaCl.Sodium;

public class SodiumTest {

  @Test
  public void testSodiumVersion() {
    Sodium x = jnr.ffi.LibraryLoader.create(Sodium.class).load("libsodium");
    assertEquals("1.0.14", x.sodium_version_string());
  }
}
