package com.teamwalk.stepcounter.repository

import androidx.annotation.Keep

@Keep
data class GroupInfo(
    val groupId: String = "",
    val enterCode: String = "",
    val groupName: String = "",
    val leaderUid: String = "",
    val members: List<String> = emptyList()
)