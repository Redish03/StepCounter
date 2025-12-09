package com.teamwalk.stepcounter

data class User(
    val uid: String = "",
    val nickName: String = "",
    val googleId: String = "",
    val name: String = "",
    val steps: Int = 0,
    val groupId: String?
)
