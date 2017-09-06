package com.tozny.e3db;

public interface ResultHandler<R> {
  void handle(Result<R> r);
}
