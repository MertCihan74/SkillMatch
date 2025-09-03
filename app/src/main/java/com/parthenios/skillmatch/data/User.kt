package com.parthenios.skillmatch.data

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
    val createdAt: Long = System.currentTimeMillis()
)
