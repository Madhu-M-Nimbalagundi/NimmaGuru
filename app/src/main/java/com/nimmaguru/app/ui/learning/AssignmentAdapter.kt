package com.nimmaguru.app.ui.learning

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ItemAssignmentBinding
import com.nimmaguru.app.domain.model.Assignment

class AssignmentAdapter(
    private val onSubmit: (Assignment) -> Unit
) : ListAdapter<Assignment, AssignmentAdapter.AssignmentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignmentViewHolder {
        return AssignmentViewHolder(
            ItemAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: AssignmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AssignmentViewHolder(
        private val binding: ItemAssignmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(assignment: Assignment) {
            val context = binding.root.context
            binding.titleTextView.text = assignment.title
            binding.metaTextView.text = context.getString(
                R.string.assignment_meta_format,
                assignment.subject,
                assignment.dueDate,
                assignment.status.replaceFirstChar(Char::titlecase)
            )
            binding.instructionsTextView.text = assignment.instructions
            binding.feedbackTextView.visibility =
                if (assignment.feedback.isBlank() && assignment.score == null) View.GONE else View.VISIBLE
            binding.feedbackTextView.text = listOfNotNull(
                assignment.score?.let { context.getString(R.string.assignment_score_format, it) },
                assignment.feedback.ifBlank { null }
            ).joinToString(" • ")
            binding.actionButton.text = if (assignment.isSubmitted) {
                context.getString(R.string.update_submission)
            } else {
                context.getString(R.string.submit_assignment)
            }
            binding.actionButton.setOnClickListener { onSubmit(assignment) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Assignment>() {
        override fun areItemsTheSame(oldItem: Assignment, newItem: Assignment): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Assignment, newItem: Assignment): Boolean = oldItem == newItem
    }
}
