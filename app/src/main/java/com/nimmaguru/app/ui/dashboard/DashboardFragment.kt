package com.nimmaguru.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.FragmentDashboardBinding
import com.nimmaguru.app.domain.model.Task
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.ui.calendar.ClassCalendarActivity
import com.nimmaguru.app.ui.guru.GuruProfileActivity
import com.nimmaguru.app.ui.learning.ProgressActivity
import com.nimmaguru.app.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var taskListener: ListenerRegistration? = null
    private var currentBadgeLabel: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = requireActivity().application as NimmaGuruApp
        val user = app.authRepository.currentUser
        val role = user?.uid?.let(app.userRoleRepository::getCachedRole)
        val fallbackName = user?.email?.substringBefore("@") ?: getString(R.string.friend)

        bindProfile(fallbackName, role, getString(R.string.badge_none))
        binding.primaryButton.text = if (role == UserRole.GURU) getString(R.string.manage_sessions) else getString(R.string.enrolled_classes)
        binding.secondaryButton.text = if (role == UserRole.GURU) getString(R.string.edit_guru_profile) else getString(R.string.my_progress)
        binding.primaryButton.setOnClickListener { startActivity(Intent(requireContext(), ClassCalendarActivity::class.java)) }
        binding.secondaryButton.setOnClickListener {
            startActivity(Intent(requireContext(), if (role == UserRole.GURU) GuruProfileActivity::class.java else ProgressActivity::class.java))
        }
        binding.settingsButton.setOnClickListener { startActivity(Intent(requireContext(), SettingsActivity::class.java)) }
        binding.assignedTasksTitleTextView.text = if (role == UserRole.GURU) {
            getString(R.string.guru_task_manager_summary)
        } else {
            getString(R.string.assigned_tasks)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            user?.uid?.let { uid ->
                val storedName = runCatching { app.userRoleRepository.loadDisplayName(uid) }.getOrDefault("")
                val preferred = storedName.ifBlank { fallbackName }
                val badge = runCatching { app.guruProfileRepository.getProfile(uid)?.badges?.lastOrNull() }
                    .getOrNull()
                    ?: if (role == UserRole.GURU) getString(R.string.badge_none) else getString(R.string.student)
                bindProfile(preferred, role, badge)
                if (storedName.isBlank()) {
                    promptPreferredName(app, uid, user.email.orEmpty())
                }
            }
        }

        bindTasks(app, user?.uid.orEmpty(), role)
    }

    override fun onDestroyView() {
        taskListener?.remove()
        taskListener = null
        _binding = null
        super.onDestroyView()
    }

    private fun bindProfile(name: String, role: UserRole?, badge: String) {
        currentBadgeLabel = badge
        binding.nameTextView.text = name
        binding.avatarTextView.text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "N"
        binding.roleTextView.text = when (role) {
            UserRole.GURU -> getString(R.string.guru)
            UserRole.STUDENT -> getString(R.string.student)
            null -> getString(R.string.not_selected)
        }
        binding.badgeTextView.text = getString(R.string.badge_level_format, badge)
    }

    private fun bindTasks(app: NimmaGuruApp, uid: String, role: UserRole?) {
        if (uid.isBlank()) {
            binding.assignedTasksTextView.text = getString(R.string.sign_in_required)
            return
        }
        taskListener?.remove()
        taskListener = if (role == UserRole.GURU) {
            app.taskRepository.listenGuruTasks(uid) { result ->
                result.onSuccess { tasks -> binding.assignedTasksTextView.text = tasks.formatDashboardTasks() }
                    .onFailure { binding.assignedTasksTextView.text = it.localizedMessage.orEmpty() }
            }
        } else {
            app.taskRepository.listenAssignedTasks(uid) { result ->
                result.onSuccess { tasks -> binding.assignedTasksTextView.text = tasks.formatDashboardTasks() }
                    .onFailure { binding.assignedTasksTextView.text = it.localizedMessage.orEmpty() }
            }
        }
    }

    private fun promptPreferredName(app: NimmaGuruApp, uid: String, email: String) {
        if (!isAdded) return
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.what_name_should_we_call_you)
        }
        val input = TextInputEditText(inputLayout.context)
        inputLayout.addView(input)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.personalization_title)
            .setView(inputLayout)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text?.toString().orEmpty().trim()
                if (name.isNotBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        runCatching { app.userRoleRepository.saveDisplayName(uid, email, name) }
                        bindProfile(name, app.userRoleRepository.getCachedRole(uid), currentBadgeLabel)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun List<Task>.formatDashboardTasks(): String {
        if (isEmpty()) return getString(R.string.no_tasks_found)
        return take(3).joinToString("\n\n") { task ->
            "${task.title}\n${task.description}\n${getString(R.string.due_date)}: ${task.dueDate}"
        }
    }
}
