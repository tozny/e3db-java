package com.tozny.e3db;

public interface Result<R> {
  boolean isError();
  ErrorResult<R> asError();
  R asValue();
}
