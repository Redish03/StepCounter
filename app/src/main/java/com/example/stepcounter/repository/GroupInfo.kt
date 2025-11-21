package com.example.stepcounter.repository

data class GroupInfo(
    val groupId: String = "",
    val enterCode: String = "",
    val leaderUid: String = "",
    val members: List<String> = emptyList()
)