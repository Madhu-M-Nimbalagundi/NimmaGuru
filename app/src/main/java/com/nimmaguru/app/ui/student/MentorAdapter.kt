package com.nimmaguru.app.ui.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ItemMentorBinding
import com.nimmaguru.app.domain.model.GuruProfile

class MentorAdapter(
    private val showMessageAction: Boolean = false,
    private val onMessageClick: (GuruProfile) -> Unit = {}
) : ListAdapter<GuruProfile, MentorAdapter.MentorViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MentorViewHolder {
        val binding = ItemMentorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MentorViewHolder(binding, showMessageAction, onMessageClick)
    }

    override fun onBindViewHolder(holder: MentorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MentorViewHolder(
        private val binding: ItemMentorBinding,
        private val showMessageAction: Boolean,
        private val onMessageClick: (GuruProfile) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(profile: GuruProfile) {
            binding.nameTextView.text = profile.name
            binding.skillsTextView.text = profile.skills.joinToString(separator = ", ")
            binding.experienceTextView.text = profile.experience
            binding.availableTimeTextView.text = profile.availableTime
            binding.locationTextView.text = profile.location
            if (profile.samudayaBhavanaAvailable && profile.samudayaBhavanaAddress.isNotBlank()) {
                binding.samudayaAddressTextView.visibility = android.view.View.VISIBLE
                binding.samudayaAddressTextView.text = itemView.context.getString(R.string.samudaya_bhavana_format, profile.samudayaBhavanaAddress)
            } else {
                binding.samudayaAddressTextView.visibility = android.view.View.GONE
            }

            binding.viewStudentsButton.visibility = if (showMessageAction) android.view.View.VISIBLE else android.view.View.GONE
            binding.viewStudentsButton.text = itemView.context.getString(R.string.message_guru)
            binding.viewStudentsButton.setOnClickListener { onMessageClick(profile) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GuruProfile>() {
        override fun areItemsTheSame(oldItem: GuruProfile, newItem: GuruProfile): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: GuruProfile, newItem: GuruProfile): Boolean {
            return oldItem == newItem
        }
    }
}
