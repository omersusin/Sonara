package com.sonara.app.intelligence.lastfm

import com.sonara.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object LastFmClient {
    private val okhttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE  // VULN-17: Never log headers (may contain keys)
        })
        .build()

    val api: LastFmApi = Retrofit.Builder()
        .baseUrl(LastFmApi.BASE_URL)
        .client(okhttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LastFmApi::class.java)
}
