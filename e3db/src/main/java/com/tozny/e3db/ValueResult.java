package com.tozny.e3db;

public class ValueResult<R> implements Result<R> {
  private final R value;

  public ValueResult(R value) {
    this.value = value;
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public ErrorResult<R> asError() {
    return null;
  }

  @Override
  public R asValue() {
    return value;
  }

  public R value() { return value; }
}
