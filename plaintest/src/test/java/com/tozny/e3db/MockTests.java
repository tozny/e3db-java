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

  public void testRegistrationClient() {
    E3DBRegistrationClient r = mock(E3DBRegistrationClient.class);
    assertNotNull(r);

    E3DBClient c = mock(E3DBClient.class);
    when(c.registrationClient()).thenReturn(r);

    assertNotNull(c.registrationClient());
  }
}
