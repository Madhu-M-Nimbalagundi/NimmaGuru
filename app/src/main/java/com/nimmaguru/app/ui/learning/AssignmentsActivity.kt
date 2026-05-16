package com.nimmaguru.app.ui.learning

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityAssignmentsBinding
import com.nimmaguru.app.domain.model.Assignment
import com.nimmaguru.app.ui.common.EmptyStateComponent

class AssignmentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAssignmentsBinding
    private val assignmentAdapter = AssignmentAdapter(::showSubmissionDialog)

    private val viewModel: AssignmentsViewModel by viewModels {
        val app = application as NimmaGuruApp
        AssignmentsViewModel.Factory(app.learningRepository, app.taskRepository, app.userRoleRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = (application as NimmaGuruApp).authRepository.currentUser?.uid.orEmpty()

        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.assignmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.assignmentsRecyclerView.adapter = assignmentAdapter

        viewModel.uiState.observe(this) { state ->
            binding.progressIndicator.visibility = if (state.isLoading || state.isSubmitting) View.VISIBLE else View.GONE
            assignmentAdapter.submitList(state.assignments)
            EmptyStateComponent.bind(
                binding.emptyStateTextView,
                !state.isLoading && state.assignments.isEmpty(),
                getString(R.string.no_assignments_found)
            )

            state.infoMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
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

    private fun showSubmissionDialog(assignment: Assignment) {
        val uid = (application as NimmaGuruApp).authRepository.currentUser?.uid.orEmpty()
        val input = EditText(this).apply {
            minLines = 4
            setText(assignment.submissionText)
            hint = getString(R.string.submission_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(assignment.title)
            .setView(input)
            .setPositiveButton(R.string.submit_assignment) { _, _ ->
                viewModel.submit(uid, assignment.id, input.text?.toString().orEmpty())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
