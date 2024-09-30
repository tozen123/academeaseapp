package com.doublehammerstudio.academeaseapp.Interfaces;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("/upload-test")
    Call<ResponseBody> uploadImageTest(@Part MultipartBody.Part image);

    @GET("/hello")
    Call<ResponseBody> testHello();
    @Multipart
    @POST("/upload")  // Replace with your API endpoint
    Call<ResponseBody> uploadImageAndAnswers(
            @Part MultipartBody.Part image,
            @Part("answers") RequestBody answers
    );
}
