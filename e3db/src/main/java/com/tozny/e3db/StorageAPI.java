package com.tozny.e3db;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

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
}
