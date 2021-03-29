package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
        private val firstName: String,
        private val lasName: String?,
        email: String? = null,
        rawPhone: String? = null,
        meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lasName)
                .joinToString(" ")
                .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lasName)
                .map { it.first().toUpperCase() }
                .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!

    private var salt: String? = null

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    //for mail
    constructor(
        firstName: String,
        lasName: String?,
        email: String,
        password: String
    ): this(firstName, lasName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary email constructor")
        passwordHash = encrypt(password)
    }

    //for phone
    constructor(
            firstName: String,
            lasName: String?,
            rawPhone: String
    ): this(firstName, lasName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        println("Phone passwordHash is $passwordHash")
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

    //for CSV
    constructor(
            firstName: String,
            lastName: String?,
            email: String?,
            salt: String,
            password: String,
            rawPhone: String?
    ): this(firstName, lastName, email = email, rawPhone = rawPhone, meta = mapOf("src" to "csv")) {
        println("Secondary CSV constructor")
        this.salt = salt
        this.passwordHash = password
    }

    init {
        println("First init block, primary constructor was called")

        check(firstName.isNotBlank()) { "FirstName must not be blank" }
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) { "Phone or Email must not be blank or null" }

        phone = rawPhone
        login = email ?: phone!!

        userInfo ="""
           firstName: $firstName
           lastName: $lasName
           login: $login
           fullName: $fullName
           initials: $initials
           email: $email
           phone: $phone
           meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash.also {
        println("Checking passwordHash is $passwordHash")
    }

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) {
            passwordHash = encrypt(newPass)
            if (!accessCode.isNullOrEmpty()) accessCode = newPass
            println("Password $oldPass has been changed on new password $newPass")
        } else throw IllegalArgumentException("The entered password does not match the current password")
    }

    fun sendAccessCodeToUser(phone: String?, code: String) {
        println(".... sending access code: $code on $phone")
    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGabcdefgZJEEzjee0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun encrypt(password: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        }
        println("Salt while encrypt: $salt")
        return salt.plus(password).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
                fullName: String,
                email: String? = null,
                password: String? = null,
                phone: String? = null
        ): User {
            val(firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrEmpty() && !password.isNullOrBlank() -> User(
                        firstName,
                        lastName,
                        email,
                        password
                )
                else -> throw IllegalArgumentException("Email or Phone must not be null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> =
                this.split(" ")
                        .filter { it.isNotBlank() }
                        .run {
                            when (size) {
                                1 -> first() to null
                                2 -> first() to last()
                                else -> throw IllegalArgumentException("Fullname must contain only first name ans last name," +
                                        "current split result: ${this@fullNameToPair}  ")
                            }
                        }

        fun importUser(rawUser: String): User {
            val userInfo = rawUser.split(";", ":")
            val(firstName, lastName) = userInfo[0].fullNameToPair()
            val email = if (userInfo[1].isNotBlank()) userInfo[1] else null
            val phone = if (userInfo[4].isNotBlank()) userInfo[4] else null
            return User(firstName,
                        lastName,
                        email,
                        userInfo[2],
                        userInfo[3],
                        phone
            )
        }
    }
}
