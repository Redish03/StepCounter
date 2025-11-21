package com.example.stepcounter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.RecyclerView
import com.example.stepcounter.repository.GroupRepository

class RankingAdapter: RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {
    private var members = listOf<GroupRepository.UserStepInfo>()

    fun submitMember(newMembers: List<GroupRepository.UserStepInfo>) {
        members = newMembers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RankingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RankingViewHolder,
        position: Int
    ) {
        val member = members[position]
        val text1 = holder.itemView.findViewById<TextView>(android.R.id.text1)
        val text2 = holder.itemView.findViewById<TextView>(android.R.id.text2)

        text1.text = "${position + 1}등, ${member.name}"
        text2.text = "${member.steps}걸음"
    }

    override fun getItemCount(): Int {
        return members.size
    }

    class RankingViewHolder(view: android.view.View): RecyclerView.ViewHolder(view)
}