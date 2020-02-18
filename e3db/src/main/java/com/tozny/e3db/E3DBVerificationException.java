/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

package com.tozny.e3db;

import java.util.Map;
import java.util.UUID;

/**
 * Thrown when a record fails verification during decryption.
 */
public class E3DBVerificationException extends E3DBException {
  private final ClientMeta meta;

  public E3DBVerificationException(ClientMeta meta) {
    super("Document failed verification.");
    this.meta = meta;
  }

  public E3DBVerificationException(ClientMeta meta, Exception inner) {
    super("Document failed verification.", inner);
    this.meta = meta;
  }

  public E3DBVerificationException(ClientMeta meta, String message) {
    super(message);
    this.meta = meta;
  }

  public E3DBVerificationException(ClientMeta meta, String message, Exception inner) {
    super(message, inner);
    this.meta = meta;
  }

  // This constructor should only be used in scenarios where a full client meta is not available.
  public E3DBVerificationException(String message, String type, Map<String, String> plain) {
    super(message);
    this.meta = new LocalMeta(new UUID(0,0), new UUID(0,0), type, plain);
  }

  public ClientMeta getMeta() {
    return meta;
  }
}
