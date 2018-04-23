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

/**
 * <h1>Secure Configuration Storage</h1>
 *
 * <p>The classes in this package support storage of E3DB credentials &amp; configuration using
 * the <a href="https://developer.android.com/training/articles/keystore.html">Android Key Store</a> (when on API 23+), or
 * using a secure filesystem based keystore.
 *
 * <p>You can use the {@link com.tozny.e3db.android.AndroidConfigStore}, in conjunction with the
 * {@link com.tozny.e3db.Config#loadConfigSecurely(com.tozny.e3db.ConfigStore, com.tozny.e3db.ConfigStore.LoadHandler)}/
 * {@link com.tozny.e3db.Config#saveConfigSecurely(com.tozny.e3db.ConfigStore, java.lang.String, com.tozny.e3db.ConfigStore.SaveHandler)}/
 * {@link com.tozny.e3db.Config#removeConfigSecurely(com.tozny.e3db.ConfigStore, com.tozny.e3db.ConfigStore.RemoveHandler)} methods
 * to manage E3DB credentials.</p>
 *
 * On API 16 - 23, we recommend storing E3DB configuration with a user-supplied password. On API 23 and above, we recommend
 * protecting E3DB configuration with the user's lock screen PIN. We do not recommend passwords on API 23+ as that protection
 * is not able to take advantage of the built-in Android Key Store.
 */
package com.tozny.e3db.android;
