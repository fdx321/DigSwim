package com.digswim.app.di

import com.digswim.app.data.SwimRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.digswim.app.data.remote.GarminService
import com.digswim.app.data.remote.PersistentCookieJar
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

import okhttp3.Interceptor

import com.digswim.app.data.remote.GarminSwimRepository
import com.digswim.app.data.remote.GarminSessionManager

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSwimRepository(
        garminSwimRepository: GarminSwimRepository
    ): SwimRepository

    companion object {
        @Provides
        @Singleton
        fun provideCookieJar(): PersistentCookieJar {
            return PersistentCookieJar()
        }

        @Provides
        @Singleton
        fun provideSessionManager(): GarminSessionManager {
            return GarminSessionManager()
        }

        @Provides
        @Singleton
        fun provideOkHttpClient(cookieJar: PersistentCookieJar): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS // Log headers to see cookies/redirects
            }
            
            // Add required headers to mimic a browser and pass Garmin's checks
            val headerInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val builder = originalRequest.newBuilder()
                
                // Common User-Agent (Chrome 143 as per user curl)
                val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
                builder.header("User-Agent", userAgent)
                
                // Add common browser headers for ALL requests to look legitimate
                builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                builder.header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7")
                builder.header("Connection", "keep-alive")

                if (originalRequest.url.host.contains("connect.garmin.cn")) {
                    // Specific headers for Connect API based on working curl
                    // builder.header("accept", "*/*") // Let's use the common one which is more browser-like, or override if API fails
                    // API might prefer JSON? The curl used "*/*". Browser usually sends full string.
                    // Let's stick to the curl specific headers for Connect to be safe, but they overwrite the common ones if keys match.
                    
                    builder.header("accept", "*/*") // Overwrites common Accept if key matches case-insensitively? OkHttp headers are case-insensitive keys.
                    builder.header("accept-language", "en,und;q=0.9,zh-CN;q=0.8,zh;q=0.7,ja;q=0.6")
                    builder.header("priority", "u=1, i")
                    builder.header("referer", "https://connect.garmin.cn/modern/activity/553049558")
                    builder.header("sec-ch-ua", "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"")
                    builder.header("sec-ch-ua-mobile", "?0")
                    builder.header("sec-ch-ua-platform", "\"macOS\"")
                    builder.header("sec-fetch-dest", "empty")
                    builder.header("sec-fetch-mode", "cors")
                    builder.header("sec-fetch-site", "same-origin")
                    
                    // NOTE: 'connect-csrf-token' is missing here. 
                    // If 403 persists, we must implement logic to extract this token from the dashboard HTML.
                    // But 'NK: NT' and 'Origin' are removed to match the curl.
                } else {
                    // For SSO login endpoints
                    builder.header("NK", "NT")
                    // Add some sec-fetch headers for SSO too, to look like a browser navigation
                    builder.header("sec-fetch-dest", "document")
                    builder.header("sec-fetch-mode", "navigate")
                    builder.header("sec-fetch-site", "none")
                    builder.header("sec-fetch-user", "?1")
                    builder.header("Upgrade-Insecure-Requests", "1")
                }

                chain.proceed(builder.build())
            }
            
            return OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor(headerInterceptor)
                .addInterceptor(logging)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideGarminService(okHttpClient: OkHttpClient): GarminService {
            return Retrofit.Builder()
                .baseUrl("https://sso.garmin.cn/") // Base URL doesn't matter much as we use full URLs in interface
                .addConverterFactory(GsonConverterFactory.create()) // Add Gson Converter
                .client(okHttpClient)
                .build()
                .create(GarminService::class.java)
        }
    }
}
