package com.example.csupload.service;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FileClient {

    @Multipart
    @POST("{endpoint}")
    Call<ResponseBody> fileUpload(@Path("endpoint") String endpoint,
                                  @Part("slice") RequestBody slice,
                                  @Part("fileName") RequestBody fileName,
                                  @Part("chunkNumber") RequestBody chunkNumber,
                                  @Part("numberOfChunks") RequestBody numberOfChunks,
                                  @Part("size") RequestBody size);

    @GET("{endpoint}")
    Call<ResponseObject> getUploadedList(@Path("endpoint") String endpoint,@Query("fileName") String fileName, @Query("numberOfChunks") long numberOfChunks, @Query("size") int size);
}
