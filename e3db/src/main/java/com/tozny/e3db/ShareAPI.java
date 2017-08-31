package com.tozny.e3db;

import java.util.UUID;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

interface ShareAPI {
  @GET("/v1/storage/clients/find")
  Call<ResponseBody> lookupClient(@Query("email") String email);

  @GET("/v1/storage/clients/{client_id}")
  Call<ResponseBody> lookupClient(@Path("client_id") UUID clientId);

  @PUT("/v1/storage/policy/{user_id}/{writer_id}/{reader_id}/{type}")
  Call<ResponseBody> putPolicy(@Path("writer_id") String userId, @Path("writer_id") String writerId, @Path("reader_id") String readerId, @Body RequestBody policy);
}
