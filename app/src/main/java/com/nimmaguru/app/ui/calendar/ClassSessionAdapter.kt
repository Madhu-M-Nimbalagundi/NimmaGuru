package com.nimmaguru.app.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ItemClassSessionBinding
import com.nimmaguru.app.domain.model.ClassSession
import com.nimmaguru.app.util.NimmaDateFormatter

class ClassSessionAdapter(
    private val showGuruActions: Boolean = false,
    private val currentUserId: String = "",
    private val onEdit: (ClassSession) -> Unit = {},
    private val onDelete: (ClassSession) -> Unit = {},
    private val onEnroll: (ClassSession) -> Unit = {},
    private val onViewStudents: (ClassSession) -> Unit = {}
) :
    ListAdapter<ClassSession, ClassSessionAdapter.ClassSessionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassSessionViewHolder {
        val binding = ItemClassSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClassSessionViewHolder(binding, showGuruActions, currentUserId, onEdit, onDelete, onEnroll, onViewStudents)
    }

    override fun onBindViewHolder(holder: ClassSessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ClassSessionViewHolder(
        private val binding: ItemClassSessionBinding,
        private val showGuruActions: Boolean,
        private val currentUserId: String,
        private val onEdit: (ClassSession) -> Unit,
        private val onDelete: (ClassSession) -> Unit,
        private val onEnroll: (ClassSession) -> Unit,
        private val onViewStudents: (ClassSession) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(session: ClassSession) {
            val context = itemView.context
            binding.dateTextView.text = NimmaDateFormatter.localize(session.startsAt?.toDate()?.let(NimmaDateFormatter::format) ?: session.date)
            binding.timeTextView.text = NimmaDateFormatter.localize(session.time)
            val localizedSubject = when(session.subject.lowercase()) {
                "science" -> context.getString(R.string.science_subject)
                "mathematics", "math" -> context.getString(R.string.mathematics_subject)
                "english" -> context.getString(R.string.english_subject)
                "kannada" -> context.getString(R.string.kannada_subject)
                "social science" -> context.getString(R.string.social_science_subject)
                else -> session.subject
            }
            binding.subjectTextView.text = context.getString(R.string.subject_format, localizedSubject)
            binding.gradeTextView.text = context.getString(R.string.target_grade_label, NimmaDateFormatter.localize(session.gradeLevel.ifBlank { "-" }))
            binding.boardTextView.text = context.getString(R.string.board_label, session.boardType.ifBlank { "-" })
            binding.mentorTextView.text = session.mentor
            binding.locationTextView.text = context.getString(R.string.location_label, session.location)
            
            if (session.fullAddress.isNotBlank()) {
                binding.addressTextView.visibility = android.view.View.VISIBLE
                binding.addressTextView.text = session.fullAddress
            } else {
                binding.addressTextView.visibility = android.view.View.GONE
            }

            if (showGuruActions) {
                binding.actionsLayout.visibility = android.view.View.VISIBLE
                binding.enrollButton.visibility = android.view.View.VISIBLE
                binding.enrollButton.text = context.getString(R.string.students_enrolled_count, session.enrolledStudentIds.size)
                binding.enrollButton.isEnabled = true
                binding.enrollButton.setOnClickListener { onViewStudents(session) }
            } else {
                binding.actionsLayout.visibility = android.view.View.GONE
                if (currentUserId.isNotBlank()) {
                    val isEnrolled = session.enrolledStudentIds.contains(currentUserId)
                    binding.enrollButton.visibility = android.view.View.VISIBLE
                    binding.enrollButton.text = if (isEnrolled) context.getString(R.string.enrolled_check) else context.getString(R.string.enroll_now)
                    binding.enrollButton.isEnabled = !isEnrolled
                    binding.enrollButton.setOnClickListener { onEnroll(session) }
                } else {
                    binding.enrollButton.visibility = android.view.View.GONE
                }
            }

            binding.editButton.setOnClickListener { onEdit(session) }
            binding.deleteButton.setOnClickListener { onDelete(session) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ClassSession>() {
        override fun areItemsTheSame(oldItem: ClassSession, newItem: ClassSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ClassSession, newItem: ClassSession): Boolean {
            return oldItem == newItem
        }
    }
}
