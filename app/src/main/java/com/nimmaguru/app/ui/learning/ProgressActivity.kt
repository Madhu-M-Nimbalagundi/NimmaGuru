package com.nimmaguru.app.ui.learning

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityProgressBinding
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.util.NimmaDateFormatter

class ProgressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgressBinding

    private val viewModel: ProgressViewModel by viewModels {
        val app = application as NimmaGuruApp
        ProgressViewModel.Factory(app.learningRepository, app.badgeManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as NimmaGuruApp
        val user = app.authRepository.currentUser
        val uid = user?.uid.orEmpty()
        val role = uid.let(app.userRoleRepository::getCachedRole)
        
        binding.topAppBar.setNavigationOnClickListener { finish() }

        viewModel.uiState.observe(this) { state ->
            val summary = state.summary
            binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.scoreTextView.text = getString(R.string.progress_score_percent, summary.averageScore)
            binding.scoreProgressIndicator.progress = summary.averageScore.coerceIn(0, 100)
            
            if (role == UserRole.GURU) {
                binding.progressSummaryTextView.text = getString(R.string.classes_taken_count, summary.classesTaken)
            } else {
                binding.progressSummaryTextView.text = getString(
                    R.string.progress_live_summary,
                    NimmaDateFormatter.localize(summary.classesAttended),
                    NimmaDateFormatter.localize(summary.assignmentsSubmitted),
                    NimmaDateFormatter.localize(summary.streakDays)
                )
            }

            binding.activityTextView.text = summary.recentActivity.ifEmpty {
                listOf(getString(R.string.no_recent_activity))
            }.joinToString("\n")

            state.errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }

        if (uid.isBlank()) {
            Snackbar.make(binding.root, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG).show()
        } else {
            viewModel.startListening(uid)
        }
    }
}
