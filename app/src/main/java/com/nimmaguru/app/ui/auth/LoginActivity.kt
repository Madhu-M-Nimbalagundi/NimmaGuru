package com.nimmaguru.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityLoginBinding
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.ui.home.HomeActivity
import com.nimmaguru.app.ui.role.RoleSelectionActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            task.getResult(ApiException::class.java)
        }.onSuccess { account ->
            val token = account.idToken
            if (token.isNullOrBlank()) {
                binding.progressIndicator.visibility = View.GONE
                binding.loginButton.isEnabled = true
                Toast.makeText(this, getString(R.string.google_sign_in_failed), Toast.LENGTH_LONG).show()
            } else {
                finishGoogleLogin(token)
            }
        }.onFailure { error ->
            binding.progressIndicator.visibility = View.GONE
            binding.loginButton.isEnabled = true
            Toast.makeText(this, getString(R.string.login_failed, error.localizedMessage.orEmpty()), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // 1. SMART CHECK: If user is already logged in, check their role!
        if (auth.currentUser != null) {
            checkUserRoleAndNavigate(enforcePortalRole = false)
        }

        // Setup Login Button Click Listener
        binding.loginButton.setOnClickListener {
            performLogin()
        }

        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                performLogin()
                true
            } else false
        }

        binding.googleButton.setOnClickListener {
            startGoogleLogin()
        }

        binding.forgotPasswordTextView.setOnClickListener {
            sendPasswordReset()
        }

        // Setup Create Account Click Listener
        binding.createAccountButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun startGoogleLogin() {
        val webClientId = getString(R.string.google_web_client_id)
        if (webClientId.isBlank()) {
            Toast.makeText(this, getString(R.string.google_client_id_missing), Toast.LENGTH_LONG).show()
            return
        }
        binding.progressIndicator.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInLauncher.launch(GoogleSignIn.getClient(this, options).signInIntent)
    }

    private fun finishGoogleLogin(idToken: String) {
        lifecycleScope.launch {
            runCatching {
                (application as NimmaGuruApp).authRepository.loginWithGoogle(idToken)
            }.onSuccess {
                auth = FirebaseAuth.getInstance()
                checkUserRoleAndNavigate(enforcePortalRole = true)
            }.onFailure { error ->
                binding.progressIndicator.visibility = View.GONE
                binding.loginButton.isEnabled = true
                Toast.makeText(this@LoginActivity, getString(R.string.login_failed, error.localizedMessage.orEmpty()), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendPasswordReset() {
        val email = binding.emailEditText.text.toString().trim()
        if (email.isBlank()) {
            binding.emailInputLayout.error = getString(R.string.email_required)
            binding.emailEditText.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = getString(R.string.invalid_email)
            binding.emailEditText.requestFocus()
            return
        }

        binding.emailInputLayout.error = null
        binding.progressIndicator.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false
        
        lifecycleScope.launch {
            runCatching {
                (application as NimmaGuruApp).authRepository.sendPasswordReset(email)
            }.onSuccess {
                binding.progressIndicator.visibility = View.GONE
                binding.loginButton.isEnabled = true
                Toast.makeText(this@LoginActivity, getString(R.string.password_reset_sent_check_inbox), Toast.LENGTH_LONG).show()
            }.onFailure { error ->
                binding.progressIndicator.visibility = View.GONE
                binding.loginButton.isEnabled = true
                Toast.makeText(this@LoginActivity, getString(R.string.login_failed, error.localizedMessage.orEmpty()), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performLogin() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        // Basic Validation
        if (email.isEmpty()) {
            binding.emailInputLayout.error = getString(R.string.email_required)
            return
        }
        if (password.isEmpty()) {
            binding.passwordInputLayout.error = getString(R.string.password_required)
            return
        }

        // Clear errors if valid
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null

        // Show Loading State
        binding.loginButton.isEnabled = false
        binding.progressIndicator.visibility = View.VISIBLE

        // Connect to Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkUserRoleAndNavigate(enforcePortalRole = true)
                } else {
                    // Failed! Hide loading state and show error
                    binding.progressIndicator.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    Toast.makeText(this, getString(R.string.login_failed, task.exception?.message.orEmpty()), Toast.LENGTH_LONG).show()
                }
            }
    }

    // 3. THE MAGIC ROUTER FUNCTION
    private fun checkUserRoleAndNavigate(enforcePortalRole: Boolean) {
        val user = auth.currentUser ?: return

        binding.progressIndicator.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false

        lifecycleScope.launch {
            runCatching {
                (application as NimmaGuruApp).userRoleRepository.loadRole(user.uid)
            }.onSuccess { actualRole ->
                if (actualRole == null) {
                    binding.progressIndicator.visibility = View.GONE
                    val intent = Intent(this@LoginActivity, PersonalizationActivity::class.java).apply {
                        putExtra(RoleSelectionActivity.EXTRA_ROLE, selectedRole().firestoreValue)
                    }
                    startActivity(intent)
                    finish()
                    return@onSuccess
                }
                val displayName = (application as NimmaGuruApp).userRoleRepository.loadDisplayName(user.uid)
                if (displayName.isBlank()) {
                    binding.progressIndicator.visibility = View.GONE
                    val intent = Intent(this@LoginActivity, PersonalizationActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return@onSuccess
                }
                if (enforcePortalRole && actualRole != selectedRole()) {
                    val message = if (selectedRole() == UserRole.STUDENT) {
                        getString(R.string.no_student_profile_found)
                    } else {
                        getString(R.string.no_guru_profile_found)
                    }
                    auth.signOut()
                    (application as NimmaGuruApp).userRoleRepository.clearCache()
                    binding.progressIndicator.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                    return@onSuccess
                }
                binding.progressIndicator.visibility = View.GONE
                Toast.makeText(this@LoginActivity, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                openHome()
                finish()
            }.onFailure { e ->
                binding.progressIndicator.visibility = View.GONE
                binding.loginButton.isEnabled = true
                Toast.makeText(this@LoginActivity, getString(R.string.role_check_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectedRole(): UserRole {
        return if (binding.roleToggleGroup.checkedButtonId == R.id.teacherRoleButton) {
            UserRole.GURU
        } else {
            UserRole.STUDENT
        }
    }

    private fun openHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
