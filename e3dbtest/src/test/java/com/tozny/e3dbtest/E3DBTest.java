package com.tozny.e3dbtest;

import com.tozny.e3db.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

import static junit.framework.Assert.*;

public class E3DBTest {

  @Test
  public void testVariadic() {
    QueryParamsBuilder builder = new QueryParamsBuilder();
    builder.setTypes((String[]) null);
    assertTrue(builder.getTypes() == null);
    builder.setTypes();
    assertTrue(builder.getTypes().size() == 0);
    builder.setTypes((String) null);
    assertTrue(builder.getTypes().size() == 1);
    builder.setTypes(null, null);
    assertTrue(builder.getTypes().size() == 2);
  }
}
