package com.nimmaguru.app.ui.learning

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivitySyllabusListBinding
import com.nimmaguru.app.domain.model.CurriculumType
import com.nimmaguru.app.domain.model.SyllabusTopic
import kotlinx.coroutines.launch

class SyllabusListActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySyllabusListBinding
    private var allTopics: List<SyllabusTopic> = emptyList()
    private var activeBoard: CurriculumType? = null
    private var selectedClassLevel = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySyllabusListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as NimmaGuruApp
        val uid = app.authRepository.currentUser?.uid.orEmpty()
        binding.topAppBar.title = getString(R.string.view_syllabus)
        binding.topAppBar.setNavigationOnClickListener { finish() }
        bindClassGrid()

        lifecycleScope.launch {
            binding.progressIndicator.visibility = View.VISIBLE
            activeBoard = uid.takeIf { it.isNotBlank() }?.let { app.userRoleRepository.loadCurriculumType(it) }
            allTopics = runCatching { app.learningRepository.loadCurriculum(activeBoard) }.getOrDefault(emptyList())
            binding.syllabusTextView.text = getString(R.string.select_class_for_syllabus)
            binding.progressIndicator.visibility = View.GONE
        }
    }

    private fun bindClassGrid() {
        binding.classGrid.removeAllViews()
        (5..10).forEach { grade ->
            val card = MaterialCardView(this).apply {
                radius = 8f * resources.displayMetrics.density
                isClickable = true
                isFocusable = true
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = resources.getDimensionPixelSize(R.dimen.quick_access_cell_height)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(6, 6, 6, 6)
                }
                addView(TextView(context).apply {
                    text = getString(R.string.class_label, grade)
                    gravity = android.view.Gravity.CENTER
                    textSize = 18f
                    setTextColor(resources.getColor(R.color.brand_green, theme))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                setOnClickListener { selectClass("Class $grade") }
            }
            binding.classGrid.addView(card)
        }
    }

    private fun selectClass(classLevel: String) {
        selectedClassLevel = classLevel
        val classTopics = allTopics.filter { it.classLevel == classLevel }
        val subjects = classTopics.map { it.subject }.distinct().sorted()
        binding.subjectInputLayout.visibility = View.VISIBLE
        binding.subjectEditText.setText("", false)
        binding.subjectEditText.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, subjects)
        )
        binding.subjectEditText.setOnItemClickListener { _, _, position, _ ->
            renderSyllabus(classTopics.filter { it.subject == subjects[position] })
        }
        binding.syllabusTextView.text = if (subjects.isEmpty()) {
            getString(R.string.no_syllabus_available)
        } else {
            getString(R.string.select_subject_for_syllabus)
        }
    }

    private fun renderSyllabus(topics: List<SyllabusTopic>) {
        binding.syllabusTextView.text = if (topics.isEmpty()) {
            getString(R.string.no_syllabus_available)
        } else {
            topics.groupBy { "${it.board} - $selectedClassLevel - ${it.subject}" }
                .entries.joinToString("\n\n") { (heading, chapters) ->
                    val body = chapters.joinToString("\n") { item ->
                        "- ${item.chapter}: ${item.topics.joinToString(", ")}"
                    }
                    "$heading\n$body"
                }
        }
    }
}
