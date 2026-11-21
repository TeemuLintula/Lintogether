package com.example.lintogether.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class QuoteResponse(
    val q: String,
    val a: String
)
interface QuoteApi {

    @GET("random")
    suspend fun getRandomQuote(): List<QuoteResponse>
}

object RetrofitInstance {

    private const val BASE_URL = "https://zenquotes.io/api/"

    val api: QuoteApi by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuoteApi::class.java)
    }
}