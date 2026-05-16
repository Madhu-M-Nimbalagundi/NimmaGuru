package com.nimmaguru.app.ui.guru

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityGuruProfileBinding
import com.nimmaguru.app.domain.model.GuruProfile
import com.nimmaguru.app.domain.model.UserRole
import android.content.Intent

class GuruProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuruProfileBinding
    private var profileLoaded = false

    private val viewModel: GuruProfileViewModel by viewModels {
        GuruProfileViewModel.Factory((application as NimmaGuruApp).guruProfileRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuruProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        val app = application as NimmaGuruApp
        val user = app.authRepository.currentUser
        val role = user?.uid?.let(app.userRoleRepository::getCachedRole)

        if ((user == null) || (role != UserRole.GURU)) {
            Snackbar.make(binding.root, getString(R.string.only_guru_profile_error), Snackbar.LENGTH_LONG).show()
            finish()
            return
        }

        binding.samudayaBhavanaSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.samudayaAddressInputLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.saveButton.setOnClickListener {
            viewModel.saveProfile(
                GuruProfile(
                    uid = user.uid,
                    name = binding.nameEditText.text?.toString().orEmpty(),
                    skills = selectedSkills(),
                    experience = binding.experienceEditText.text?.toString().orEmpty(),
                    availableTime = binding.availableTimeEditText.text?.toString().orEmpty(),
                    location = binding.locationEditText.text?.toString().orEmpty(),
                    samudayaBhavanaAvailable = binding.samudayaBhavanaSwitch.isChecked,
                    samudayaBhavanaAddress = binding.samudayaAddressEditText.text?.toString().orEmpty()
                )
            )
        }

        viewModel.uiState.observe(this) { state ->
            binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.saveButton.isEnabled = !state.isLoading

            state.profile?.let { profile ->
                if (!profileLoaded) {
                    bindProfile(profile)
                    profileLoaded = true
                }
            }

            if (state.isSaved) {
                Snackbar.make(binding.root, getString(R.string.guru_profile_saved), Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }

            state.errorMessage?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }

        viewModel.loadProfile(user.uid)
    }

    private fun bindProfile(profile: GuruProfile) {
        binding.nameEditText.setText(profile.name)
        binding.experienceEditText.setText(profile.experience)
        binding.availableTimeEditText.setText(profile.availableTime)
        binding.locationEditText.setText(profile.location)
        binding.samudayaBhavanaSwitch.isChecked = profile.samudayaBhavanaAvailable
        binding.samudayaAddressEditText.setText(profile.samudayaBhavanaAddress)
        binding.samudayaAddressInputLayout.visibility = if (profile.samudayaBhavanaAvailable) View.VISIBLE else View.GONE

        for (index in 0 until binding.skillsChipGroup.childCount) {
            val child = binding.skillsChipGroup.getChildAt(index) as? Chip ?: continue
            child.isChecked = profile.skills.contains(child.skillValue())
        }
    }

    private fun selectedSkills(): List<String> {
        return binding.skillsChipGroup.checkedChipIds.mapNotNull { chipId ->
            binding.skillsChipGroup.findViewById<Chip>(chipId)?.skillValue()
        }
    }

    private fun Chip.skillValue(): String {
        return tag?.toString().orEmpty().ifBlank { text.toString() }
    }
}
