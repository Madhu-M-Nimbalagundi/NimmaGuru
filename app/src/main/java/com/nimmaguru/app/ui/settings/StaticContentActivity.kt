package com.nimmaguru.app.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityStaticContentBinding

class StaticContentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStaticContentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaticContentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Information"
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: "Content coming soon..."

        binding.topAppBar.title = title
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.contentTextView.text = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
    }
}
