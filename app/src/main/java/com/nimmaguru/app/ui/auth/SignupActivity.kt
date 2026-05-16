package com.nimmaguru.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivitySignupBinding
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.ui.role.RoleSelectionActivity

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    private val viewModel: AuthViewModel by viewModels {
        val app = application as NimmaGuruApp
        AuthViewModel.Factory(app.authRepository, app.userRoleRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signupButton.setOnClickListener {
            performSignup()
        }

        binding.confirmPasswordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                performSignup()
                true
            } else false
        }

        binding.loginButton.setOnClickListener {
            finish()
        }

        viewModel.uiState.observe(this) { state ->
            binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.signupButton.isEnabled = !state.isLoading
            binding.loginButton.isEnabled = !state.isLoading

            if (state.isSuccess) {
                val intent = Intent(this, PersonalizationActivity::class.java)
                    .putExtra(RoleSelectionActivity.EXTRA_ROLE, selectedRole().firestoreValue)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            state.errorMessage?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }
    }

    private fun performSignup() {
        viewModel.signup(
            binding.emailEditText.text?.toString().orEmpty(),
            binding.passwordEditText.text?.toString().orEmpty(),
            binding.confirmPasswordEditText.text?.toString().orEmpty()
        )
    }

    private fun selectedRole(): UserRole {
        return if (binding.roleToggleGroup.checkedButtonId == R.id.guruRoleButton) {
            UserRole.GURU
        } else {
            UserRole.STUDENT
        }
    }
}
