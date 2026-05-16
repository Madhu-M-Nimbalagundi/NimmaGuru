package com.nimmaguru.app.ui.student

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityStudentSearchBinding
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.ui.guru.GuruChatActivity

class StudentSearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentSearchBinding
    private lateinit var mentorAdapter: MentorAdapter

    private val viewModel: StudentSearchViewModel by viewModels {
        StudentSearchViewModel.Factory((application as NimmaGuruApp).guruProfileRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        val app = application as NimmaGuruApp
        val user = app.authRepository.currentUser
        val communityMode = intent.getBooleanExtra(EXTRA_COMMUNITY_MODE, false)
        val currentUid = user?.uid.orEmpty()
        if (user == null || (!communityMode && app.userRoleRepository.getCachedRole(currentUid) != UserRole.STUDENT)) {
            Snackbar.make(binding.root, getString(R.string.only_student_search_error), Snackbar.LENGTH_LONG).show()
            finish()
            return
        }

        mentorAdapter = MentorAdapter(showMessageAction = communityMode) { profile ->
            startActivity(
                GuruChatActivity.intent(
                    context = this,
                    otherGuruId = profile.uid,
                    otherGuruName = profile.name.ifBlank { getString(R.string.guru) }
                )
            )
        }

        binding.mentorsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.mentorsRecyclerView.adapter = mentorAdapter

        if (communityMode) {
            binding.topAppBar.title = getString(R.string.nav_community)
            binding.searchTitleTextView.visibility = View.GONE
            binding.searchSubtitleTextView.text = getString(R.string.community_search_subtitle)
        }

        binding.searchButton.setOnClickListener {
            applyFilters()
        }

        binding.clearFiltersButton.setOnClickListener {
            binding.skillFilterChipGroup.clearCheck()
            binding.locationEditText.setText("")
            applyFilters()
        }

        viewModel.uiState.observe(this) { state ->
            binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            mentorAdapter.submitList(state.mentors)

            val isEmpty = !state.isLoading && state.mentors.isEmpty()
            binding.emptyStateTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE

            state.errorMessage?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }

        applyFilters()
    }

    private fun applyFilters() {
        viewModel.search(
            skillFilters = selectedSkills(),
            location = binding.locationEditText.text?.toString().orEmpty()
        )
    }

    private fun selectedSkills(): List<String> {
        return binding.skillFilterChipGroup.checkedChipIds.mapNotNull { chipId ->
            binding.skillFilterChipGroup.findViewById<Chip>(chipId)?.skillValue()
        }
    }

    private fun Chip.skillValue(): String {
        return tag?.toString().orEmpty().ifBlank { text.toString() }
    }

    companion object {
        const val EXTRA_COMMUNITY_MODE = "extra_community_mode"
    }
}
