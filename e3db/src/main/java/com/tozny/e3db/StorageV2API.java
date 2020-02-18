package com.tozny.e3db;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StorageV2API {
  public static final String V2_BASE_PATH = "v2/storage";

  @POST(V2_BASE_PATH + "/notes")
  Call<Note> writeNote(@Body Note note);

  @PUT(V2_BASE_PATH + "/notes")
  Call<Note> replaceNote(@Body Note note);

  @PATCH(V2_BASE_PATH + "/notes/challenge")
  Call<Void> challengeNote(@Query("id_string")String idString, @Body ChallengeRequest challengeRequest);

  @GET(V2_BASE_PATH + "/notes")
  Call<Note> getNote(@Query("note_id") UUID noteId, @Query("id_string") String noteName);

  @DELETE(V2_BASE_PATH + "/notes/{note_id}")
  Call<Void> deleteNote(@Path("note_id") UUID noteId);
}
