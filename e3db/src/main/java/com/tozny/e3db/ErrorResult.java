package com.tozny.e3db;

public class ErrorResult<R> implements Result<R> {
  private final Throwable error;

  public ErrorResult(Throwable error) {
    this.error = error;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public ErrorResult<R> asError() {
    return this;
  }

  @Override
  public R asValue() {
    return null;
  }

  public E3DBException error() {
    if(error instanceof E3DBException)
      return (E3DBException) error;
    else
      return null;
  }

  public Throwable other() {
    return this.error;
  }
}
