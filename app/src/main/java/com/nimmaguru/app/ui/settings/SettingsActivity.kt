package com.nimmaguru.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivitySettingsBinding
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.util.NimmaDateFormatter
import com.nimmaguru.app.ui.auth.LoginActivity
import com.nimmaguru.app.ui.guru.GuruProfileActivity
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as NimmaGuruApp
        val currentUser = app.authRepository.currentUser
        val role = currentUser?.uid?.let(app.userRoleRepository::getCachedRole)

        binding.backButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            currentUser?.uid?.let { uid ->
                val preferred = runCatching { app.userRoleRepository.loadDisplayName(uid) }.getOrDefault("")
                val avatar = runCatching { app.userRoleRepository.loadAvatar(uid) }.getOrNull()
                val roleName = when (role) {
                    UserRole.GURU -> getString(R.string.teacher)
                    UserRole.STUDENT -> getString(R.string.student)
                    else -> getString(R.string.not_selected)
                }
                
                binding.roleTextView.text = roleName
                
                if (preferred.isNotBlank()) {
                    binding.nameTextView.text = preferred
                    if (!avatar.isNullOrBlank()) {
                        binding.avatarTextView.text = avatar
                    } else {
                        binding.avatarTextView.text = preferred.first().uppercaseChar().toString()
                    }
                } else {
                    val email = currentUser.email.orEmpty()
                    binding.nameTextView.text = email.substringBefore("@").replaceFirstChar(Char::titlecase)
                    binding.avatarTextView.text = binding.nameTextView.text.toString().trim().firstOrNull()?.uppercaseChar()?.toString() ?: "N"
                }

                if (role == UserRole.GURU) {
                    val profile = runCatching { app.guruProfileRepository.getProfile(uid) }.getOrNull()
                    val exp = profile?.experience ?: "0"
                    binding.experienceTextView.text = getString(R.string.experience_years_format, NimmaDateFormatter.localize(exp))
                    binding.experienceTextView.visibility = android.view.View.VISIBLE
                } else {
                    binding.experienceTextView.visibility = android.view.View.GONE
                }
            }
        }

        // Make headers "work" by showing info or toggling
        binding.accountSettingsHeader.setOnClickListener {
            Toast.makeText(this, "Manage your personal account details", Toast.LENGTH_SHORT).show()
        }
        binding.supportOthersHeader.setOnClickListener {
            Toast.makeText(this, "Get help and view legal information", Toast.LENGTH_SHORT).show()
        }

        binding.editProfileRow.root.setOnClickListener {
            if (role == UserRole.GURU) {
                startActivity(Intent(this, GuruProfileActivity::class.java))
            } else {
                showGradeDialog(app, currentUser?.uid.orEmpty())
            }
        }

        binding.changePasswordRow.root.setOnClickListener {
            showChangePasswordDialog(app)
        }

        binding.notificationsRow.notificationSwitch.isChecked = app.userPreferences.areNotificationsEnabled()
        binding.notificationsRow.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            app.userPreferences.setNotificationsEnabled(isChecked)
            val msg = if (isChecked) "Notifications enabled" else "Notifications turned off"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        binding.notificationsRow.root.setOnClickListener {
            binding.notificationsRow.notificationSwitch.toggle()
        }

        binding.languageRow.root.setOnClickListener {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        binding.themeRow.root.setOnClickListener {
            val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            val nextMode = if (isNight) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            AppCompatDelegate.setDefaultNightMode(nextMode)
            app.userPreferences.setThemeMode(if (nextMode == AppCompatDelegate.MODE_NIGHT_YES) "dark" else "light")
            Toast.makeText(this, getString(R.string.theme_updated), Toast.LENGTH_SHORT).show()
        }

        binding.helpRow.root.setOnClickListener {
            startActivity(
                Intent(this, StaticContentActivity::class.java)
                    .putExtra(StaticContentActivity.EXTRA_TITLE, getString(R.string.help_support))
                    .putExtra(StaticContentActivity.EXTRA_CONTENT, getString(R.string.help_note)),
            )
        }

        binding.privacyRow.root.setOnClickListener {
            startActivity(
                Intent(this, StaticContentActivity::class.java)
                    .putExtra(StaticContentActivity.EXTRA_TITLE, getString(R.string.privacy_policy))
                    .putExtra(StaticContentActivity.EXTRA_CONTENT, getString(R.string.privacy_note))
            )
        }

        binding.termsRow.root.setOnClickListener {
            startActivity(
                Intent(this, StaticContentActivity::class.java)
                    .putExtra(StaticContentActivity.EXTRA_TITLE, getString(R.string.terms_conditions))
                    .putExtra(StaticContentActivity.EXTRA_CONTENT, getString(R.string.terms_note))
            )
        }

        binding.reportRow.root.setOnClickListener {
            showReportDialog()
        }

        binding.logoutRow.root.setOnClickListener {
            app.authRepository.logout()
            app.userRoleRepository.clearCache()
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
        }
    }

    private fun showChangePasswordDialog(app: NimmaGuruApp) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }

        val currentPasswordLayout = passwordInputLayout(getString(R.string.current_password))
        val currentPasswordInput = passwordInput(currentPasswordLayout)
        currentPasswordLayout.addView(currentPasswordInput)

        val newPasswordLayout = passwordInputLayout(getString(R.string.new_password)).apply {
            setPadding(0, 12, 0, 0)
        }
        val newPasswordInput = passwordInput(newPasswordLayout)
        newPasswordLayout.addView(newPasswordInput)

        val confirmPasswordLayout = passwordInputLayout(getString(R.string.confirm_new_password)).apply {
            setPadding(0, 12, 0, 0)
        }
        val confirmPasswordInput = passwordInput(confirmPasswordLayout)
        confirmPasswordLayout.addView(confirmPasswordInput)

        container.addView(currentPasswordLayout)
        container.addView(newPasswordLayout)
        container.addView(confirmPasswordLayout)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.change_password)
            .setView(container)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val currentPassword = currentPasswordInput.text?.toString().orEmpty()
                val newPassword = newPasswordInput.text?.toString().orEmpty()
                val confirmPassword = confirmPasswordInput.text?.toString().orEmpty()

                currentPasswordLayout.error = null
                newPasswordLayout.error = null
                confirmPasswordLayout.error = null

                when {
                    currentPassword.isBlank() -> {
                        currentPasswordLayout.error = getString(R.string.current_password_required)
                    }
                    newPassword.length < 6 -> {
                        newPasswordLayout.error = getString(R.string.password_minimum_error)
                    }
                    newPassword != confirmPassword -> {
                        confirmPasswordLayout.error = getString(R.string.passwords_do_not_match)
                    }
                    else -> {
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                        lifecycleScope.launch {
                            runCatching { app.authRepository.changePassword(currentPassword, newPassword) }
                                .onSuccess {
                                    Toast.makeText(this@SettingsActivity, R.string.password_changed, Toast.LENGTH_LONG).show()
                                    dialog.dismiss()
                                }
                                .onFailure { error ->
                                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                    Toast.makeText(
                                        this@SettingsActivity,
                                        error.localizedMessage ?: getString(R.string.password_change_failed),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun passwordInputLayout(hintText: String): TextInputLayout {
        return TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox).apply {
            hint = hintText
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
    }

    private fun passwordInput(parent: TextInputLayout): TextInputEditText {
        return TextInputEditText(parent.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            minHeight = resources.getDimensionPixelSize(R.dimen.button_height)
        }
    }

    private fun showReportDialog() {
        val layout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox)
            .apply {
                hint = "Describe the issue or profile"
                setPadding(32, 16, 32, 0)
            }
        val input = com.google.android.material.textfield.TextInputEditText(layout.context).apply {
            minLines = 3
            gravity = android.view.Gravity.TOP
        }
        layout.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.report_issue)
            .setView(layout)
            .setPositiveButton("Submit") { _, _ ->
                val text = input.text?.toString().orEmpty()
                if (text.isNotBlank()) {
                    Toast.makeText(this, getString(R.string.report_sent), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showGradeDialog(app: NimmaGuruApp, uid: String) {
        if (uid.isBlank()) return
        val layout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox_ExposedDropdownMenu)
            .apply {
                hint = getString(R.string.select_grade)
                setPadding(32, 8, 32, 0)
            }
        val input = MaterialAutoCompleteTextView(layout.context)
        val grades = (5..10).map(Int::toString)
        input.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, grades))
        layout.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.select_grade)
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                val gradeText = input.text?.toString().orEmpty()
                val gradeInt = gradeText.toIntOrNull()
                if (gradeInt == null || gradeInt !in 5..10) {
                    Toast.makeText(this, getString(R.string.choose_grade_error), Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    runCatching { app.userRoleRepository.updateGradeLevel(uid, gradeText) }
                        .onSuccess { Toast.makeText(this@SettingsActivity, getString(R.string.grade_updated), Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(this@SettingsActivity, it.localizedMessage.orEmpty(), Toast.LENGTH_LONG).show() }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
