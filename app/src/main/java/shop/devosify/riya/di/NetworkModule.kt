package shop.devosify.riya.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import shop.devosify.riya.BuildConfig
import shop.devosify.riya.security.ApiKeyProvider
import shop.devosify.riya.service.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(apiKeyProvider: ApiKeyProvider): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder()
            .add("api.openai.com", "sha256/...")  // Add OpenAI's certificate hash
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${apiKeyProvider.getApiKey()}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWhisperService(retrofit: Retrofit): WhisperApiService {
        return retrofit.create(WhisperApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAssistantsService(retrofit: Retrofit): AssistantsApiService {
        return retrofit.create(AssistantsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGptService(retrofit: Retrofit): GptApiService {
        return retrofit.create(GptApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTtsService(retrofit: Retrofit): TtsApiService {
        return retrofit.create(TtsApiService::class.java)
    }
} 