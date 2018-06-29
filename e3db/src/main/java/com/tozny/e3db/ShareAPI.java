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

import java.util.UUID;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

interface ShareAPI {
  @GET("/v1/storage/clients/find")
  Call<ResponseBody> lookupClient(@Query("email") String email);

  @GET("/v1/storage/clients/{client_id}")
  Call<ResponseBody> lookupClient(@Path("client_id") UUID clientId);

  @PUT("/v1/storage/policy/{user_id}/{writer_id}/{reader_id}/{type}")
  Call<ResponseBody> putPolicy(@Path("user_id") String userId, @Path("writer_id") String writerId, @Path("reader_id") String readerId, @Path("type") String type, @Body RequestBody policy);

  @DELETE("/v1/storage/policy/{user_id}/{writer_id}/{reader_id}")
  Call<ResponseBody> deletePolicy(@Path("user_id") String userId, @Path("writer_id") String writerId, @Path("reader_id") String readerId);

  @GET("/v1/storage/policy/incoming")
  Call<ResponseBody> getIncoming();

  @GET("/v1/storage/policy/outgoing")
  Call<ResponseBody> getOutgoing();

  @GET("/v1/storage/policy/proxies")
  Call<ResponseBody> getProxies();

  @GET("/v1/storage/policy/granted")
  Call<ResponseBody> getGranted();
}
