package com.example.doatest;

import com.example.doatest.model.Location;

import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public interface JsonPlaceholderApi {
    @GET("api")
    Call<List<Location>> get_data();
}
