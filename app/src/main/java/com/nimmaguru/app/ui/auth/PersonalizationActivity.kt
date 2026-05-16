package com.nimmaguru.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityPersonalizationBinding
import com.nimmaguru.app.ui.home.HomeActivity
import com.nimmaguru.app.ui.role.RoleSelectionActivity
import kotlinx.coroutines.launch

import com.google.android.material.chip.Chip

class PersonalizationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPersonalizationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as NimmaGuruApp
        val user = app.authRepository.currentUser
        if (binding.nameEditText.text.isNullOrBlank()) {
            binding.nameEditText.setText(user?.displayName)
        }

        binding.continueButton.setOnClickListener {
            val name = binding.nameEditText.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                binding.nameInputLayout.error = getString(R.string.name_required)
                return@setOnClickListener
            }
            binding.nameInputLayout.error = null
            
            val selectedChipId = binding.avatarChipGroup.checkedChipId
            val avatar = if (selectedChipId != View.NO_ID) {
                binding.avatarChipGroup.findViewById<Chip>(selectedChipId)?.text?.toString().orEmpty()
            } else ""
            
            saveName(name, avatar)
        }

        binding.nameEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                binding.continueButton.performClick()
                true
            } else false
        }
    }

    private fun saveName(name: String, avatar: String) {
        val app = application as NimmaGuruApp
        val user = app.authRepository.currentUser ?: return
        binding.progressIndicator.visibility = View.VISIBLE
        binding.continueButton.isEnabled = false
        lifecycleScope.launch {
            runCatching {
                app.userRoleRepository.saveDisplayName(user.uid, user.email.orEmpty(), name, avatar)
                app.userRoleRepository.loadRole(user.uid)
            }.onSuccess {
                val nextIntent = if (it == null) {
                    Intent(this@PersonalizationActivity, RoleSelectionActivity::class.java)
                        .putExtra(RoleSelectionActivity.EXTRA_ROLE, intent.getStringExtra(RoleSelectionActivity.EXTRA_ROLE).orEmpty())
                } else {
                    Intent(this@PersonalizationActivity, HomeActivity::class.java)
                }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(nextIntent)
                finish()
            }.onFailure { error ->
                binding.progressIndicator.visibility = View.GONE
                binding.continueButton.isEnabled = true
                Snackbar.make(binding.root, error.localizedMessage.orEmpty(), Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
