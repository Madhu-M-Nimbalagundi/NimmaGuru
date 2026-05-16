package com.nimmaguru.app.ui.calendar

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityClassCalendarBinding
import com.nimmaguru.app.data.learning.SubjectCatalog
import com.nimmaguru.app.domain.model.CurriculumType
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.ui.common.EmptyStateComponent

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class ClassCalendarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClassCalendarBinding
    private lateinit var sessionAdapter: ClassSessionAdapter

    private val viewModel: ClassCalendarViewModel by viewModels {
        ClassCalendarViewModel.Factory((application as NimmaGuruApp).classSessionRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener { finish() }
        val app = application as NimmaGuruApp
        val user = app.authRepository.currentUser
        val role = user?.uid?.let(app.userRoleRepository::getCachedRole)
        val isGuru = role == UserRole.GURU
        binding.createSessionButton.visibility = if (isGuru) View.VISIBLE else View.GONE
        binding.createSessionButton.setOnClickListener { showSessionDialog() }

        sessionAdapter = ClassSessionAdapter(
            showGuruActions = isGuru,
            currentUserId = user?.uid.orEmpty(),
            onEdit = { session -> showSessionDialog(session) },
            onDelete = { session ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete_session)
                    .setMessage(R.string.delete_session_confirm)
                    .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteSession(session) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            },
            onEnroll = { session ->
                user?.uid?.let { uid ->
                    lifecycleScope.launch {
                        runCatching { app.classSessionRepository.enrollInSession(session.id, uid) }
                    }
                }
            },
            onViewStudents = { session ->
                showStudentsDialog(session.enrolledStudentIds)
            }
        )
        binding.sessionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sessionsRecyclerView.adapter = sessionAdapter

        viewModel.uiState.observe(this) { state ->
            binding.progressIndicator.visibility = if (state.isLoading || state.isSaving) View.VISIBLE else View.GONE
            sessionAdapter.submitList(state.sessions)

            val isEmpty = !state.isLoading && state.sessions.isEmpty()
            EmptyStateComponent.bind(binding.emptyStateTextView, isEmpty, "${getString(R.string.empty_notes_bitmoji)}\n${getString(R.string.no_upcoming_sessions)}")

            state.errorMessage?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
            state.infoMessage?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }

        viewModel.startListening()
    }

    private fun showStudentsDialog(studentIds: List<String>) {
        if (studentIds.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.view_students)
                .setMessage(R.string.no_students_enrolled)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        lifecycleScope.launch {
            val app = application as NimmaGuruApp
            val names = studentIds.map { uid ->
                async {
                    runCatching { app.userRoleRepository.loadDisplayName(uid) }.getOrDefault("Unknown Student")
                }
            }.awaitAll()
            AlertDialog.Builder(this@ClassCalendarActivity)
                .setTitle(R.string.view_students)
                .setItems(names.toTypedArray(), null)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun showSessionDialog(existing: com.nimmaguru.app.domain.model.ClassSession? = null) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 0)
        }
        fun field(hint: String): TextInputEditText {
            val layout = TextInputLayout(this).apply { this.hint = hint }
            val editText = TextInputEditText(layout.context)
            layout.addView(editText)
            container.addView(layout)
            return editText
        }
        fun subjectDropdown(): MaterialAutoCompleteTextView {
            val app = application as NimmaGuruApp
            val uid = app.authRepository.currentUser?.uid.orEmpty()
            val curriculum = runCatching { kotlinx.coroutines.runBlocking { app.userRoleRepository.loadCurriculumType(uid) } }.getOrNull()
            val grade = runCatching { kotlinx.coroutines.runBlocking { app.userRoleRepository.loadGradeLevel(uid) } }.getOrDefault("")
            val layout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox_ExposedDropdownMenu)
                .apply { hint = getString(R.string.subject) }
            val editText = MaterialAutoCompleteTextView(layout.context)
            editText.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, SubjectCatalog.subjects(this, curriculum, grade)))
            layout.addView(editText)
            container.addView(layout)
            return editText
        }
        fun dropdown(hint: String, values: List<String>, value: String = ""): MaterialAutoCompleteTextView {
            val layout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox_ExposedDropdownMenu)
                .apply { this.hint = hint }
            val editText = MaterialAutoCompleteTextView(layout.context).apply {
                setAdapter(ArrayAdapter(this@ClassCalendarActivity, android.R.layout.simple_list_item_1, values))
                setText(value, false)
            }
            layout.addView(editText)
            container.addView(layout)
            return editText
        }
        val app = application as NimmaGuruApp
        val uid = app.authRepository.currentUser?.uid.orEmpty()
        val curriculum = runCatching { kotlinx.coroutines.runBlocking { app.userRoleRepository.loadCurriculumType(uid) } }.getOrNull()
        val gradeValue = runCatching { kotlinx.coroutines.runBlocking { app.userRoleRepository.loadGradeLevel(uid) } }.getOrDefault("")
        val date = field(getString(R.string.class_date_hint)).apply { setText(existing?.date.orEmpty()) }
        val time = field(getString(R.string.class_time_hint)).apply { setText(existing?.time.orEmpty()) }
        val mentor = field(getString(R.string.name)).apply { setText(existing?.mentor.orEmpty()) }
        val grade = dropdown(getString(R.string.target_grade), (5..10).map { "Class $it" }, existing?.gradeLevel.takeIf { it?.isNotBlank() == true } ?: gradeValue.takeIf { it.isNotBlank() }?.let { "Class $it" }.orEmpty())
        val board = dropdown(
            getString(R.string.board),
            listOf(CurriculumType.KARNATAKA_STATE_BOARD.displayName, CurriculumType.CBSE.displayName),
            curriculum?.displayName.orEmpty()
        )
        val subject = subjectDropdown().apply { setText(existing?.subject.orEmpty(), false) }
        val location = field(getString(R.string.location)).apply { setText(existing?.location.orEmpty()) }
        val address = field("Detailed Address (e.g. Street/Building)").apply { setText(existing?.fullAddress.orEmpty()) }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) R.string.create_class_session else R.string.edit_session)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.createSession(
                    date.text?.toString().orEmpty(),
                    time.text?.toString().orEmpty(),
                    mentor.text?.toString().orEmpty(),
                    subject.text?.toString().orEmpty(),
                    location.text?.toString().orEmpty(),
                    grade.text?.toString().orEmpty(),
                    board.text?.toString().orEmpty(),
                    existing?.id.orEmpty(),
                    address.text?.toString().orEmpty(),
                    existing?.enrolledStudentIds ?: emptyList()
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
