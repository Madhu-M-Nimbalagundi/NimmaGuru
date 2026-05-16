package com.nimmaguru.app.ui.role

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityRoleSelectionBinding
import com.nimmaguru.app.domain.model.CurriculumType
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.ui.home.HomeActivity

class RoleSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRoleSelectionBinding
    private var hasNavigated = false
    private var fixedRole: UserRole? = null

    private val viewModel: RoleViewModel by viewModels {
        RoleViewModel.Factory((application as NimmaGuruApp).userRoleRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🚨 NEW: Intercept the physical back button so they cannot escape!
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Snackbar.make(
                    binding.root,
                    "You must select a role to continue.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

        fixedRole = UserRole.fromStorage(intent.getStringExtra(EXTRA_ROLE))
        binding.roleToggleGroup.check(if (fixedRole == UserRole.GURU) R.id.guruButton else R.id.studentButton)
        if (fixedRole != null) {
            binding.roleToggleGroup.visibility = View.GONE
        }
        binding.curriculumToggleGroup.check(R.id.cbseButton)
        binding.gradeEditText.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, (5..10).map(Int::toString))
        )

        val user = (application as NimmaGuruApp).authRepository.currentUser
        binding.roleToggleGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) updateCurriculumOptions()
        }
        updateCurriculumOptions()

        binding.continueButton.setOnClickListener {
            val role = fixedRole ?: when (binding.roleToggleGroup.checkedButtonId) {
                R.id.guruButton -> UserRole.GURU
                R.id.studentButton -> UserRole.STUDENT
                else -> null
            }
            val curriculumType = when (binding.curriculumToggleGroup.checkedButtonId) {
                R.id.karnatakaButton -> CurriculumType.KARNATAKA_STATE_BOARD
                R.id.cbseButton -> CurriculumType.CBSE
                R.id.bothButton -> CurriculumType.BOTH
                else -> null
            }
            val gradeLevel = binding.gradeEditText.text?.toString().orEmpty().trim()

            if (role == null) {
                Snackbar.make(binding.root, getString(R.string.choose_role_error), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (curriculumType == null) {
                Snackbar.make(binding.root, getString(R.string.choose_curriculum_error), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val grade = gradeLevel.toIntOrNull()
            if (role == UserRole.STUDENT && (grade == null || grade !in 5..10)) {
                Snackbar.make(binding.root, getString(R.string.choose_grade_error), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user == null) {
                Snackbar.make(binding.root, getString(R.string.sign_in_to_choose_role), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.saveRole(user.uid, user.email.orEmpty(), role, curriculumType, gradeLevel)
        }

        viewModel.uiState.observe(this) { state ->
            binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.continueButton.isEnabled = !state.isLoading
            binding.roleToggleGroup.isEnabled = !state.isLoading
            binding.curriculumToggleGroup.isEnabled = !state.isLoading

            if (state.isSaved && !hasNavigated) {
                hasNavigated = true
                val intent = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }

            state.errorMessage?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }
    }

    private fun updateCurriculumOptions() {
        val isGuru = (fixedRole ?: if (binding.roleToggleGroup.checkedButtonId == R.id.guruButton) UserRole.GURU else UserRole.STUDENT) == UserRole.GURU
        binding.bothButton.visibility = if (isGuru) View.VISIBLE else View.GONE
        binding.gradeInputLayout.visibility = if (isGuru) View.GONE else View.VISIBLE
        if (!isGuru && binding.curriculumToggleGroup.checkedButtonId == R.id.bothButton) {
            binding.curriculumToggleGroup.check(R.id.cbseButton)
        }
    }

    companion object {
        const val EXTRA_ROLE = "extra_role"
    }
}
