package com.teamwalk.stepcounter

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.teamwalk.stepcounter.databinding.ActivityGroupBinding
import com.teamwalk.stepcounter.repository.GroupRepository

class GroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupBinding
    private lateinit var adapter: RankingAdapter
    private var currentGroupId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupListeners()
        startObservingGroup()
    }

    private fun setupRecyclerView() {
        adapter = RankingAdapter()
        binding.recyclerRanking.layoutManager = LinearLayoutManager(this)
        binding.recyclerRanking.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCreateGroup.setOnClickListener {
            val groupName = binding.etGroupName.text.toString()

            if(groupName.isBlank()) {
                Toast.makeText(this, "그룹 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            GroupRepository.createGroup(groupName,
                onSuccess = { code ->
                    Toast.makeText(this, "방 생성 코드:  $code", Toast.LENGTH_LONG).show()
                },
                onFailure = { msg ->
                    Toast.makeText(this, "실패: $msg", Toast.LENGTH_SHORT).show()
                    Log.d("GroupActivity", "$msg 로 인한 실패")
                }
            )
        }

        binding.btnJoinGroup.setOnClickListener {
            val code = binding.etJoinCode.text.toString()

            if (code.length == 6) {
                GroupRepository.joinGroup(
                    code,
                    onSuccess = { Toast.makeText(this, "참여 완료", Toast.LENGTH_SHORT).show() },
                    onFailure = { msg ->
                        Toast.makeText(
                            this,
                            msg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }

        binding.btnLeaveGroup.setOnClickListener {
            GroupRepository.leaveGroup(currentGroupId, onSuccess = {
                Toast.makeText(this, "그룹 나가기 성공", Toast.LENGTH_SHORT).show()
            })
            finish()
        }
    }

    private fun startObservingGroup() {
        GroupRepository.listenMyGroup(
            onGroupFound = { groupInfo, members ->
                // 그룹이 있을 때 UI 전환
                currentGroupId = groupInfo.groupId
                binding.layoutNoGroup.isVisible = false
                binding.layoutInGroup.isVisible = true

                binding.tvGroupName.text = groupInfo.groupName
                binding.tvGroupCode.text = "초대 코드: ${groupInfo.enterCode}"
                adapter.submitMember(members) // 랭킹 업데이트
            },
            onNoGroup = {
                // 그룹이 없을 때 UI 전환
                currentGroupId = ""
                binding.layoutNoGroup.isVisible = true
                binding.layoutInGroup.isVisible = false
            }
        )
    }
}