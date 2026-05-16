package com.nimmaguru.app.ui.appreciation

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityWallOfFameBinding
import com.nimmaguru.app.domain.model.UserRole

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class WallOfFameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWallOfFameBinding
    private val appreciationAdapter = AppreciationAdapter()

    private val viewModel: WallOfFameViewModel by viewModels {
        WallOfFameViewModel.Factory((application as NimmaGuruApp).appreciationRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallOfFameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        val app = application as NimmaGuruApp
        val user = app.authRepository.currentUser
        val isStudent = user?.uid?.let(app.userRoleRepository::getCachedRole) == UserRole.STUDENT

        binding.composerGroup.visibility = if (isStudent) View.VISIBLE else View.GONE

        binding.appreciationRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appreciationRecyclerView.adapter = appreciationAdapter

        binding.postButton.setOnClickListener {
            if (user == null) {
                Snackbar.make(binding.root, getString(R.string.sign_in_to_post_thanks), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val studentName = app.userRoleRepository.loadDisplayName(user.uid)
                viewModel.postMessage(
                    studentUid = user.uid,
                    studentEmail = user.email.orEmpty(),
                    studentName = studentName,
                    guruName = binding.guruNameEditText.text?.toString().orEmpty(),
                    message = binding.messageEditText.text?.toString().orEmpty()
                )
            }
        }

        binding.suggestMessageButton.setOnClickListener {
            binding.messageEditText.setText(AppreciationMessageGenerator.generate(this))
        }

        viewModel.uiState.observe(this) { state ->
            binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.postButton.isEnabled = !state.isPosting
            binding.suggestMessageButton.isEnabled = !state.isPosting
            appreciationAdapter.submitList(state.messages)

            val isEmpty = !state.isLoading && state.messages.isEmpty()
            binding.emptyStateTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE

            if (state.isPosted) {
                binding.messageEditText.setText("")
                binding.guruNameEditText.setText("")
                Snackbar.make(binding.root, getString(R.string.thank_you_posted), Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }

            state.errorMessage?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }

        viewModel.startListening()
    }
}
