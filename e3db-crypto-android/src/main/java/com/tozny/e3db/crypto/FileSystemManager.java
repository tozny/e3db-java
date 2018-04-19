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

package com.tozny.e3db.crypto;

import android.content.Context;


import java.io.File;

class FileSystemManager {

    private final static String SECURE_STRING_STORAGE_DIRECTORY = "com.tozny.e3db.crypto";
    private final static String IV_DIRECTORY                    = "ivs";

    private static String filesDirectory(Context context) throws Throwable {
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        File sssDirectory     = new File(filesDirectory + File.separator + SECURE_STRING_STORAGE_DIRECTORY);
        File ivDirectory      = new File(filesDirectory + File.separator + SECURE_STRING_STORAGE_DIRECTORY + File.separator + IV_DIRECTORY);

        boolean success = true;
        if (!sssDirectory.exists()) {
            success = sssDirectory.mkdir();
        }

        if (!success) {
            throw new Exception("Error creating secure string storage directory.");
        }

        if (!ivDirectory.exists()) {
            success = ivDirectory.mkdir();
        }

        if (!success) {
            throw new Exception("Error creating secure string storage directory.");
        }

        return filesDirectory;
    }

    static String getInitializationVectorFilePath(Context context, String fileName) throws Throwable {
        return filesDirectory(context) +
                File.separator + SECURE_STRING_STORAGE_DIRECTORY +
                File.separator + IV_DIRECTORY + File.separator + fileName;
    }

    static String getEncryptedDataFilePath(Context context, String fileName) throws Throwable {
        return filesDirectory(context) + File.separator + SECURE_STRING_STORAGE_DIRECTORY + File.separator + fileName;
    }

}
