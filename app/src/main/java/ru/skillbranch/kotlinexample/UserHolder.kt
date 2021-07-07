package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
            fullName: String,
            email: String,
            password: String
    ): User = User.makeUser(fullName, email = email, password = password)
            .also {
                if (!map.containsKey(it.login))
                    map[it.login] = it
                else
                    throw IllegalArgumentException("A user with this email already exists")
            }

    fun registerUserByPhone(
            fullName: String,
            rawPhone: String
    ): User = when {
        validatePhone(rawPhone) -> User.makeUser(fullName, phone = rawPhone)
                .also {
                    if (!map.containsKey(it.login))
                        map[it.login] = it
                    else
                        throw IllegalArgumentException("A user with this phone already exists")
                }
        else -> throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
    }

    fun loginUser(login: String, password: String): String? {
    val phoneLogin = login.replace("[^+\\d]".toRegex(), "")
    return if (phoneLogin.isNotEmpty()) {
        map[phoneLogin]
    } else {
        map[login]
    }?.let {
        if (it.checkPassword(password)) it.userInfo
        else null
        }
    }


    fun requestAccessCode(login: String): Unit {
        map[login.replace("[^+\\d]".toRegex(), "")]?.let {
            it.changePassword(it.accessCode!!, it.generateAccessCode())
            it.sendAccessCodeToUser(login, it.accessCode!!)
        }
    }

    fun importUsers(list: List<String>): List<User> {
        var userList = ArrayList<User>()
        list.forEach { row ->
                        val user: User = User.importUser(row)
                        userList.add(user)
                        map[user.login] = user
                     }
        return userList
    }

    fun validatePhone(rawPhone: String): Boolean =
        rawPhone.startsWith("+") &&
        !rawPhone.contains("[А-яа-яA-za-z]".toRegex()) &&
        rawPhone.replace("[^0-9]".toRegex(), "").length == 11


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}