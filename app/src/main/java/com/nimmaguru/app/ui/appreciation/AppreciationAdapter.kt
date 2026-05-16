package com.nimmaguru.app.ui.appreciation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ItemAppreciationBinding
import com.nimmaguru.app.domain.model.AppreciationMessage
import com.nimmaguru.app.util.NimmaDateFormatter

class AppreciationAdapter :
    ListAdapter<AppreciationMessage, AppreciationAdapter.AppreciationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppreciationViewHolder {
        val binding = ItemAppreciationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppreciationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppreciationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AppreciationViewHolder(
        private val binding: ItemAppreciationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: AppreciationMessage) {
            binding.messageTextView.text = message.message
            binding.studentTextView.text = "From: ${message.studentName.ifBlank { message.studentEmail.substringBefore("@") }}"
            binding.recipientTextView.text = "To: Guru ${message.guruName}"
            binding.recipientTextView.visibility = if (message.guruName.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE

            binding.createdAtTextView.text = message.createdAt
                ?.toDate()
                ?.let { NimmaDateFormatter.format(it) }
                .orEmpty()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppreciationMessage>() {
        override fun areItemsTheSame(
            oldItem: AppreciationMessage,
            newItem: AppreciationMessage
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AppreciationMessage,
            newItem: AppreciationMessage
        ): Boolean {
            return oldItem == newItem
        }
    }
}
