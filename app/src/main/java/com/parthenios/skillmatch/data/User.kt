package com.parthenios.skillmatch.data

import java.io.Serializable
import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val birthDate: Date? = null,
    val age: Int = 0,
    val city: String = "",
    val knownSkills: List<String> = emptyList(),
    val wantedSkills: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val birthday: String = "", // String format for easier handling
    val publicKey: String = "" // Cihazın Keystore public key'i (Base64-encoded X.509)
) : Serializable
