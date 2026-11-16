package com.elearning.smartflash.data.remote.api;

import android.content.Context;

import com.elearning.smartflash.BuildConfig;
import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Central Retrofit2 client configured with {@link AuthInterceptor},
 * {@link TokenAuthenticator}, and OkHttp logging.
 */
public class RetrofitClient {

    private static volatile RetrofitClient instance;

    private final Retrofit retrofit;
    private final AuthApiService authApiService;

    private RetrofitClient(Context context) {
        TokenManager tokenManager = TokenManager.getInstance(context);
        Gson gson = new GsonBuilder().create();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(
                BuildConfig.DEBUG
                        ? HttpLoggingInterceptor.Level.BODY
                        : HttpLoggingInterceptor.Level.NONE
        );

        // Dedicated bare client for /auth/refresh to avoid recursive authenticator loops.
        OkHttpClient refreshHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit refreshRetrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(refreshHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        AuthApiService refreshApiService = refreshRetrofit.create(AuthApiService.class);

        OkHttpClient mainHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(tokenManager))
                .authenticator(new TokenAuthenticator(tokenManager, refreshApiService))
                .addInterceptor(loggingInterceptor)
                .build();

        this.retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(mainHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        this.authApiService = this.retrofit.create(AuthApiService.class);
    }

    public static RetrofitClient getInstance(Context context) {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    instance = new RetrofitClient(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public AuthApiService getAuthApi() {
        return authApiService;
    }

    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }
}
