package com.parthenios.skillmatch.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.parthenios.skillmatch.data.User

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        prefs.edit().putString("current_user", userJson).apply()
    }

    fun getUser(): User? {
        val userJson = prefs.getString("current_user", null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun clearUser() {
        prefs.edit().remove("current_user").apply()
    }

    fun isUserLoggedIn(): Boolean {
        return getUser() != null
    }
}
