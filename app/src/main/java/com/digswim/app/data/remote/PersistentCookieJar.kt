package com.digswim.app.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.CopyOnWriteArrayList

class PersistentCookieJar : CookieJar {
    // Use a thread-safe list to store all cookies
    private val cookies = CopyOnWriteArrayList<Cookie>()

    override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
        newCookies.forEach { newCookie ->
            // Remove any existing cookie with the same name, domain, and path
            // to ensure we update it with the new value.
            val iterator = cookies.iterator()
            while (iterator.hasNext()) {
                val current = iterator.next()
                if (current.name == newCookie.name &&
                    current.domain == newCookie.domain &&
                    current.path == newCookie.path
                ) {
                    cookies.remove(current)
                }
            }
            cookies.add(newCookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val validCookies = mutableListOf<Cookie>()
        val iterator = cookies.iterator()
        val now = System.currentTimeMillis()

        while (iterator.hasNext()) {
            val cookie = iterator.next()

            // Remove expired cookies
            if (cookie.expiresAt < now) {
                cookies.remove(cookie)
                continue
            }

            // Use OkHttp's standard matching logic (handles domain sharing)
            if (cookie.matches(url)) {
                validCookies.add(cookie)
            }
        }
        return validCookies
    }
    
    fun clear() {
        cookies.clear()
    }
}
