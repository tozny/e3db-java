package com.tozny.e3db;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SearchAPI {
  @POST("v2/search")
  Call<ResponseBody> search(@Body SearchRequest request);
}
