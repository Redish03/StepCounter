package com.teamwalk.stepcounter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.teamwalk.stepcounter.repository.GroupRepository

class RankingAdapter: RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {
    private var members = listOf<GroupRepository.UserStepInfo>()

    fun submitMember(newMembers: List<GroupRepository.UserStepInfo>) {
        members = newMembers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        // 기존: simple_list_item_2 -> 변경: item_ranking
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ranking, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val member = members[position]

        // ID 변경에 따른 매핑
        val tvRank = holder.itemView.findViewById<TextView>(R.id.tv_rank)
        val tvName = holder.itemView.findViewById<TextView>(R.id.tv_name)
        val tvSteps = holder.itemView.findViewById<TextView>(R.id.tv_steps)

        tvRank.text = "${position + 1}"
        tvName.text = member.name
        tvSteps.text = "${member.steps}"
    }

    override fun getItemCount(): Int {
        return members.size
    }

    class RankingViewHolder(view: android.view.View): RecyclerView.ViewHolder(view)
}