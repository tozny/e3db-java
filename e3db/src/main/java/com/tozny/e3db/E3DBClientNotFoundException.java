package com.tozny.e3db;

class E3DBClientNotFoundException extends E3DBException {
  private final String clientEmail;

  public E3DBClientNotFoundException(String clientEmail) {
    super("'" + clientEmail + "' not found.");
    this.clientEmail = clientEmail;
  }
}
