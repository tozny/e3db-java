package com.tozny.e3db;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

interface AuthAPI {
  @POST("/v1/auth/token")
  Call<ResponseBody> getToken(@Header("Authorization") String basic, @Body RequestBody grantReq);
}
