package com.alhashim.oneit

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class MyCookieJar(context: Context) : CookieJar {
    private val cookiePrefs: SharedPreferences = context.getSharedPreferences("CookiePersistence", Context.MODE_PRIVATE)
    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    init {
        // Load any previously stored cookies on initialization
        loadFromPrefs()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies
        // Persist cookies to SharedPreferences
        val editor = cookiePrefs.edit()
        cookies.forEach { cookie ->
            if (cookie.name.equals("JSESSIONID", ignoreCase = true)) {
                editor.putString("JSESSIONID", cookie.value)
            }
        }
        editor.apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }

    private fun loadFromPrefs() {
        val sessionId = cookiePrefs.getString("JSESSIONID", null)
        if (sessionId != null) {
            // Recreate the cookie - adjust the domain and path according to your Spring server
            val cookie = Cookie.Builder()
                .name("JSESSIONID")
                .value(sessionId)
                .domain("your.domain.com") // Replace with your server domain
                .path("/")
                .build()
            cookieStore["your.domain.com"] = listOf(cookie)
        }
    }

    fun clearCookies() {
        cookieStore.clear()
        cookiePrefs.edit().clear().apply()
    }

    fun getSessionId(): String? {
        return cookiePrefs.getString("JSESSIONID", null)
    }
}

class OneITApplication : Application() {
    lateinit var cookieJar: MyCookieJar
        private set

    override fun onCreate() {
        super.onCreate()
        cookieJar = MyCookieJar(this)
    }
}
