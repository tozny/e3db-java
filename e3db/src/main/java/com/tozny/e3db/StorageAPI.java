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

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

interface StorageAPI {
  @GET("/v1/storage/records/{record_ids}")
  Call<ResponseBody> getRecord(@Path("record_ids") String recordIds);

  @POST("/v1/storage/search")
  Call<ResponseBody> query(@Body RequestBody queryRequest);

  @POST("/v1/storage/records")
  Call<ResponseBody> writeRecord(@Body RequestBody record);

  @GET("/v1/storage/access_keys/{writer_id}/{user_id}/{reader_id}/{record_type}")
  Call<ResponseBody> getAccessKey(@Path("writer_id") String writerId, @Path("user_id") String userId, @Path("reader_id") String readerId, @Path("record_type") String recordType);

  @PUT("/v1/storage/access_keys/{writer_id}/{user_id}/{reader_id}/{record_type}")
  Call<ResponseBody> putAccessKey(@Path("writer_id") String writerId, @Path("user_id") String userId, @Path("reader_id") String readerId, @Path("record_type") String recordType, @Body RequestBody body);

  @PUT("/v1/storage/records/safe/{record_id}/{version_id}")
  Call<ResponseBody> updateRecord(@Path("record_id") String recordId, @Path("version_id") String version, @Body RequestBody requestBody);

  @DELETE("/v1/storage/records/safe/{record_id}/{version_id}")
  Call<ResponseBody> deleteRecord(@Path("record_id") String recordId, @Path("version_id") String version);

  @DELETE("/v1/storage/access_keys/{writer_id}/{user_id}/{reader_id}/{record_type}")
  Call<ResponseBody> deleteAccessKey(@Path("writer_id") String writerId, @Path("user_id") String userId, @Path("reader_id") String readerId, @Path("record_type") String recordType);

  @POST("/v1/storage/files")
  Call<ResponseBody> writeFile(@Body RequestBody record);

  @PATCH("/v1/storage/files/{pending_file_id}")
  Call<ResponseBody> commitFile(@Path("pending_file_id") String pendingFileId);

  @GET("/v1/storage/files/{record_id}")
  Call<ResponseBody> readFile(@Path("record_id") String recordId);
}
