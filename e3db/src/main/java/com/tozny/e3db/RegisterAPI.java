package com.tozny.e3db;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface RegisterAPI {
  @POST("/v1/account/e3db/clients/register")
  Call<ResponseBody> register(@Body RequestBody registration);
}
