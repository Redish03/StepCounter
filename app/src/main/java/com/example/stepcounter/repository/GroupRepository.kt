package com.example.stepcounter.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object GroupRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    data class UserStepInfo(
        val uid: String = "",
        val name: String = "",
        val steps: Int = 0,
        val groupId: String? = null
    )

    fun updateMySteps(steps: Int) {
        val uid = auth.currentUser?.uid ?: return
        val name = auth.currentUser?.displayName ?: "익명"

        val userRef = db.collection("users").document(uid)

        val myData = mapOf(
            "uid" to uid,
            "name" to name,
            "steps" to steps
        )

        userRef.set(myData, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { exception ->
                Log.e(
                    "GroupRepo",
                    "걸음 수 업로드 실패, ${exception.message}",
                    exception
                )
            }
    }

    fun createGroup(groupName: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        // 중복되지 않는 코드를 먼저 찾습니다.
        findUniqueCode { uniqueCode ->

            // 유니크한 코드를 찾았으니 이제 방을 만듭니다.
            val newGroupRef = db.collection("groups").document()
            val groupId = newGroupRef.id

            val groupData = GroupInfo(
                groupId = groupId,
                enterCode = uniqueCode, // 검증된 유니크 코드 사용
                groupName = groupName,
                leaderUid = uid,
                members = listOf(uid)
            )

            db.runTransaction { transaction ->
                // 1. 그룹 생성 (이건 그대로)
                transaction.set(newGroupRef, groupData)

                // 2. 내 정보에 groupId 설정
                val userRef = db.collection("users").document(uid)
                val userData = mapOf("groupId" to groupId)

                // SetOptions.merge()를 사용하면 문서가 없을 땐 새로 만들고, 있을 땐 해당 필드만 덮어씁니다.
                transaction.set(userRef, userData, com.google.firebase.firestore.SetOptions.merge())

            }.addOnSuccessListener {
                onSuccess(uniqueCode)
            }.addOnFailureListener { e ->
                onFailure(e.message ?: "방 생성 실패")
            }
        }
    }

    fun joinGroup(code: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("groups").whereEqualTo("enterCode", code).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    onFailure("존재하지 않는 코드입니다.")
                    return@addOnSuccessListener
                }

                val groupDoc = querySnapshot.documents[0]
                val groupId = groupDoc.id
                val currentMembers =
                    groupDoc.toObject(GroupInfo::class.java)?.members ?: emptyList()

                if (currentMembers.contains(uid)) {
                    onFailure("이미 참여 중인 그룹입니다.")
                    return@addOnSuccessListener
                }

                db.runTransaction { transaction ->
                    val newMembers = currentMembers + uid
                    transaction.update(groupDoc.reference, "members", newMembers)
                    transaction.update(db.collection("users").document(uid), "groupId", groupId)
                }.addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it.message ?: "참여 실패") }
            }
            .addOnFailureListener { onFailure("검색 실패: ${it.message}") }
    }

    fun leaveGroup(groupId: String, onSuccess: () -> Unit){
        val uid = auth.currentUser?.uid ?: return

        val groupRef = db.collection("groups").document(groupId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(groupRef)
            val members = snapshot.toObject(GroupInfo::class.java)?.members ?: emptyList()
            val newMembers = members.filter { it != uid }

            if(newMembers.isEmpty()) {
                transaction.delete(groupRef)
            } else {
                transaction.update(groupRef, "members", newMembers)
            }

            transaction.update(db.collection("users").document(uid), "groupId", null)
        }.addOnSuccessListener { onSuccess() }
    }

    fun listenMyGroup(
        onGroupFound: (GroupInfo, List<UserStepInfo>) -> Unit,
        onNoGroup: () -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            val groupId = snapshot?.getString("groupId")

            if(groupId == null) {
                onNoGroup()
            } else {
                db.collection("groups").document(groupId).addSnapshotListener { groupSnap, _ ->
                    val groupInfo = groupSnap?.toObject(GroupInfo::class.java)
                    if(groupInfo != null) {
                        fetchMembersInfo(groupInfo.members) { members ->
                            val sortedMembers = members.sortedByDescending { it.steps }
                            onGroupFound(groupInfo, sortedMembers)
                        }
                    } else {
                        onNoGroup()
                    }
                }
            }
        }
    }

    private fun fetchMembersInfo(uids: List<String>, onComplete: (List<UserStepInfo>) -> Unit) {
        if (uids.isEmpty()) {
            onComplete(emptyList())
            return
        }
        // 'in' 쿼리는 최대 10개까지만 가능하므로 실제 앱에선 나눠서 해야하지만, 간단히 구현
        db.collection("users").whereIn("uid", uids).get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.toObjects(UserStepInfo::class.java)
                onComplete(users)
            }
    }

    private fun findUniqueCode(onFound: (String) -> Unit) {
        val code = (100000..999999).random().toString()

        // DB에 이 코드를 쓰는 방이 있는지 물어봅니다.
        db.collection("groups")
            .whereEqualTo("code", code)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // 1. 아무도 안 씀 -> 이 코드 사용!
                    onFound(code)
                } else {
                    // 2. 누가 쓰고 있음 -> 다시 뽑아! (재귀 호출)
                    Log.d("GroupRepository", "코드 충돌 발생($code). 번호 다시 생성 중...")
                    findUniqueCode(onFound)
                }
            }
            .addOnFailureListener {
                // DB 에러 시에도 일단 다시 시도하거나 에러 처리를 할 수 있습니다.
                // 여기서는 안전하게 다시 시도합니다.
                findUniqueCode(onFound)
            }
    }
}