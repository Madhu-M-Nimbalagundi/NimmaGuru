package com.nimmaguru.app.ui.task

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityTaskManagerBinding
import com.nimmaguru.app.domain.model.Task
import com.nimmaguru.app.domain.model.UserRole
import kotlinx.coroutines.launch

class TaskManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskManagerBinding
    private var taskListener: ListenerRegistration? = null
    private val taskAdapter = TaskAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as NimmaGuruApp
        val user = app.authRepository.currentUser
        val role = user?.uid?.let(app.userRoleRepository::getCachedRole)
        binding.backButton.setOnClickListener { finish() }
        binding.titleTextView.text = if (role == UserRole.GURU) getString(R.string.task_manager) else getString(R.string.assigned_tasks)
        binding.createTaskButton.visibility = if (role == UserRole.GURU) View.VISIBLE else View.GONE
        binding.createTaskButton.setOnClickListener { showCreateTaskDialog(user?.uid.orEmpty()) }

        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tasksRecyclerView.adapter = taskAdapter

        if (user != null) {
            lifecycleScope.launch {
                if (role == UserRole.GURU) {
                    taskListener = app.taskRepository.listenGuruTasks(user.uid) { result ->
                        result.onSuccess { tasks -> 
                            taskAdapter.submitList(tasks)
                            binding.emptyTasksTextView.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                } else {
                    val grade = app.userRoleRepository.loadGradeLevel(user.uid)
                    taskListener = app.taskRepository.listenAssignedTasks(grade) { result ->
                        result.onSuccess { tasks -> 
                            taskAdapter.submitList(tasks)
                            binding.emptyTasksTextView.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        taskListener?.remove()
        taskListener = null
        super.onDestroy()
    }

    private fun showCreateTaskDialog(guruUid: String) {
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
        val gradeInput = field(getString(R.string.student_grade))
        val title = field(getString(R.string.task_title))
        val description = field(getString(R.string.task_description))
        val dueDate = field(getString(R.string.due_date))

        AlertDialog.Builder(this)
            .setTitle(R.string.create_task)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                lifecycleScope.launch {
                    runCatching {
                        (application as NimmaGuruApp).taskRepository.createTask(
                            Task(
                                ownerGuruId = guruUid,
                                targetGrade = gradeInput.text?.toString().orEmpty(),
                                title = title.text?.toString().orEmpty(),
                                description = description.text?.toString().orEmpty(),
                                dueDate = dueDate.text?.toString().orEmpty(),
                            )
                        )
                    }.onSuccess {
                        Snackbar.make(binding.root, getString(R.string.task_created), Snackbar.LENGTH_SHORT).show()
                    }.onFailure {
                        Snackbar.make(binding.root, it.localizedMessage.orEmpty(), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
