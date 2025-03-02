package com.example.m1.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object for Retrofit client setup
 */
object RetrofitClient {
    private const val BASE_URL = "https://tocuul9kqj.execute-api.us-west-1.amazonaws.com/"

    /**
     * Lazy initialized API service
     */
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}