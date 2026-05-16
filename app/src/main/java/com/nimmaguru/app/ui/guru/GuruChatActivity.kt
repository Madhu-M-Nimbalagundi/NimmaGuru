package com.nimmaguru.app.ui.guru

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityGuruChatBinding
import kotlinx.coroutines.launch

class GuruChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuruChatBinding
    private lateinit var adapter: GuruChatAdapter
    private var messageListener: ListenerRegistration? = null

    private val otherGuruId: String by lazy { intent.getStringExtra(EXTRA_OTHER_GURU_ID).orEmpty() }
    private val otherGuruName: String by lazy { intent.getStringExtra(EXTRA_OTHER_GURU_NAME).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuruChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as NimmaGuruApp
        val currentUser = app.authRepository.currentUser
        if (currentUser == null || otherGuruId.isBlank()) {
            Toast.makeText(this, getString(R.string.sign_in_required), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val currentGuruId = currentUser.uid
        val currentGuruName = currentUser.email?.substringBefore("@").orEmpty().ifBlank { getString(R.string.guru) }

        binding.topAppBar.title = otherGuruName.ifBlank { getString(R.string.guru_chat) }
        binding.topAppBar.setNavigationOnClickListener { finish() }

        adapter = GuruChatAdapter(currentGuruId)
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.messagesRecyclerView.adapter = adapter

        messageListener = app.guruChatRepository.listenMessages(currentGuruId, otherGuruId) { result ->
            result
                .onSuccess { messages ->
                    binding.progressIndicator.visibility = View.GONE
                    adapter.submitList(messages) {
                        if (messages.isNotEmpty()) {
                            binding.messagesRecyclerView.scrollToPosition(messages.lastIndex)
                        }
                    }
                    binding.emptyStateTextView.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { error ->
                    binding.progressIndicator.visibility = View.GONE
                    Toast.makeText(this, error.localizedMessage.orEmpty(), Toast.LENGTH_LONG).show()
                }
        }

        binding.sendButton.setOnClickListener {
            val text = binding.messageEditText.text?.toString().orEmpty()
            if (text.isBlank()) return@setOnClickListener
            binding.sendButton.isEnabled = false
            lifecycleScope.launch {
                runCatching {
                    app.guruChatRepository.sendMessage(
                        currentGuruId = currentGuruId,
                        currentGuruName = currentGuruName,
                        otherGuruId = otherGuruId,
                        otherGuruName = otherGuruName.ifBlank { getString(R.string.guru) },
                        text = text
                    )
                }.onSuccess {
                    binding.messageEditText.setText("")
                    binding.sendButton.isEnabled = true
                }.onFailure { error ->
                    binding.sendButton.isEnabled = true
                    Toast.makeText(this@GuruChatActivity, error.localizedMessage.orEmpty(), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        messageListener?.remove()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_OTHER_GURU_ID = "extra_other_guru_id"
        private const val EXTRA_OTHER_GURU_NAME = "extra_other_guru_name"

        fun intent(context: Context, otherGuruId: String, otherGuruName: String): Intent {
            return Intent(context, GuruChatActivity::class.java)
                .putExtra(EXTRA_OTHER_GURU_ID, otherGuruId)
                .putExtra(EXTRA_OTHER_GURU_NAME, otherGuruName)
        }
    }
}
