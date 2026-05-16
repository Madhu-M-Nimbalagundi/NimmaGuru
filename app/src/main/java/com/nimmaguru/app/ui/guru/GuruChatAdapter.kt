package com.nimmaguru.app.ui.guru

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.app.databinding.ItemGuruChatMessageBinding
import com.nimmaguru.app.domain.model.ChatMessage

class GuruChatAdapter(
    private val currentGuruId: String
) : ListAdapter<ChatMessage, GuruChatAdapter.MessageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemGuruChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding, currentGuruId)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        private val binding: ItemGuruChatMessageBinding,
        private val currentGuruId: String
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            val isMine = message.senderId == currentGuruId
            binding.senderTextView.text = if (isMine) "You" else message.senderName
            binding.messageTextView.text = message.text
            binding.messageContainer.gravity = if (isMine) {
                android.view.Gravity.END
            } else {
                android.view.Gravity.START
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
