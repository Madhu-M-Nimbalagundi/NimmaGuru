package com.nimmaguru.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityDashboardBinding
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.util.NimmaDateFormatter
import com.nimmaguru.app.ui.auth.LoginActivity
import com.nimmaguru.app.ui.calendar.ClassCalendarActivity
import com.nimmaguru.app.ui.guru.GuruProfileActivity
import com.nimmaguru.app.ui.learning.ProgressActivity
import com.nimmaguru.app.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private var profileListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as NimmaGuruApp
        val currentUser = app.authRepository.currentUser
        val role = currentUser?.uid?.let(app.userRoleRepository::getCachedRole)

        binding.backButton.setOnClickListener { finish() }

        setupRow(
            binding.primaryActionRow.root,
            if (role == UserRole.GURU) getString(R.string.manage_sessions) else getString(R.string.enrolled_classes),
            if (role == UserRole.GURU) "Schedule and edit your classes" else "View your joined classes",
            "📗",
        ) {
            startActivity(Intent(this, ClassCalendarActivity::class.java))
        }

        setupRow(
            binding.secondaryActionRow.root,
            if (role == UserRole.GURU) getString(R.string.edit_guru_profile) else getString(R.string.my_progress),
            if (role == UserRole.GURU) getString(R.string.edit_profile_subtitle) else "Track your learning journey",
            if (role == UserRole.GURU) "👤" else "📊"
        ) {
            startActivity(Intent(this, if (role == UserRole.GURU) GuruProfileActivity::class.java else ProgressActivity::class.java))
        }

        setupRow(
            binding.settingsRow.root,
            getString(R.string.settings),
            "Notifications, language and theme",
            "⚙️"
        ) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.logoutRow.root.setOnClickListener {
            app.authRepository.logout()
            app.userRoleRepository.clearCache()
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }

        currentUser?.uid?.let { uid ->
            profileListener = app.userRoleRepository.listenProfile(uid) { result ->
                result.onSuccess { data ->
                    val preferred = data["displayName"] as? String ?: ""
                    val avatar = data["avatar"] as? String ?: ""
                    
                    if (preferred.isNotBlank()) {
                        binding.nameTextView.text = preferred
                        if (avatar.isNotBlank()) {
                            binding.avatarTextView.text = avatar
                        } else {
                            binding.avatarTextView.text = preferred.first().uppercaseChar().toString()
                        }
                    } else {
                        val email = currentUser.email.orEmpty()
                        binding.nameTextView.text = email.substringBefore("@").replaceFirstChar(Char::titlecase)
                        binding.avatarTextView.text = binding.nameTextView.text.toString().trim().firstOrNull()?.uppercaseChar()?.toString() ?: "N"
                    }
                    
                    binding.roleTextView.text = when (role) {
                        UserRole.GURU -> getString(R.string.teacher)
                        UserRole.STUDENT -> getString(R.string.student)
                        else -> getString(R.string.not_selected)
                    }

                    if (role == UserRole.GURU) {
                        lifecycleScope.launch {
                            val profile = runCatching { app.guruProfileRepository.getProfile(uid) }.getOrNull()
                            val exp = profile?.experience ?: "0"
                            binding.experienceTextView.text = getString(R.string.experience_years_format, NimmaDateFormatter.localize(exp))
                            binding.experienceTextView.visibility = android.view.View.VISIBLE
                            binding.gradeTextView.visibility = android.view.View.GONE
                        }
                    } else {
                        val grade = data["gradeLevel"] as? String ?: ""
                        if (grade.isNotBlank()) {
                            binding.gradeTextView.text = getString(R.string.grade_format, NimmaDateFormatter.localize(grade))
                            binding.gradeTextView.visibility = android.view.View.VISIBLE
                        } else {
                            binding.gradeTextView.visibility = android.view.View.GONE
                        }
                        binding.experienceTextView.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        profileListener?.remove()
        super.onDestroy()
    }

    private fun setupRow(row: android.view.View, title: String, subtitle: String, icon: String, onClick: () -> Unit) {
        row.findViewById<TextView>(R.id.titleTextView)?.text = title
        row.findViewById<TextView>(R.id.subtitleTextView)?.text = subtitle
        val iconView = row.findViewById<TextView>(R.id.iconTextView)
        if (icon.isBlank()) {
            iconView?.visibility = android.view.View.GONE
        } else {
            iconView?.visibility = android.view.View.VISIBLE
            iconView?.text = icon
        }
        row.setOnClickListener { onClick() }
    }
}
