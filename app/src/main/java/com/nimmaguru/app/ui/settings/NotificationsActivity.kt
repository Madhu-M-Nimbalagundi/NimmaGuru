package com.nimmaguru.app.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preferences = (application as NimmaGuruApp).userPreferences
        val enabled = preferences.areNotificationsEnabled()
        binding.classesCheckBox.isChecked = enabled
        binding.assignmentsCheckBox.isChecked = enabled
        binding.messagesCheckBox.isChecked = enabled
        binding.backButton.setOnClickListener { finish() }
        val listener = android.widget.CompoundButton.OnCheckedChangeListener { _, _ ->
            preferences.setNotificationsEnabled(
                binding.classesCheckBox.isChecked ||
                    binding.assignmentsCheckBox.isChecked ||
                    binding.messagesCheckBox.isChecked
            )
        }
        binding.classesCheckBox.setOnCheckedChangeListener(listener)
        binding.assignmentsCheckBox.setOnCheckedChangeListener(listener)
        binding.messagesCheckBox.setOnCheckedChangeListener(listener)
    }
}
