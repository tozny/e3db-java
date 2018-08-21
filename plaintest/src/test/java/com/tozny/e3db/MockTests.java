package com.tozny.e3db;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

public class MockTests {
  @Test
  public void testClient() {
    E3DBClient c = mock(E3DBClient.class);
    assertNotNull(c);
  }
}
