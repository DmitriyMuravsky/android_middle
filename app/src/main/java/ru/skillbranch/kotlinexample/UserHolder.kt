package ru.skillbranch.kotlinexample

import android.support.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
            fullName: String,
            email: String,
            password: String
    ): User = User.makeUser(fullName, email = email, password = password)
            .also { user -> map[user.login] = user }

    fun loginUser(login: String, password: String): String? =
            map[login.trim()]?.let {
                if (it.checkPassword(password)) it.userInfo
                else null
            }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}