package com.nimmaguru.app.ui.learning

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.data.learning.SubjectCatalog
import com.nimmaguru.app.data.learning.SyllabusCatalog
import com.nimmaguru.app.databinding.ActivityMaterialsBinding
import com.nimmaguru.app.domain.model.CurriculumType
import com.nimmaguru.app.domain.model.LearningMaterial
import com.nimmaguru.app.ui.common.EmptyStateComponent
import kotlinx.coroutines.launch
import android.widget.ArrayAdapter

class MaterialsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMaterialsBinding
    private lateinit var materialAdapter: MaterialAdapter
    private var selectedFileUri: Uri? = null
    private var selectedFileNameTextView: TextView? = null
    private var currentCurriculumType: CurriculumType? = null
    private var currentGradeLevel: String = ""

    private val viewModel: MaterialsViewModel by viewModels {
        MaterialsViewModel.Factory((application as NimmaGuruApp).learningRepository)
    }
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileNameTextView?.text = getString(R.string.selected_file_format, getDisplayName(uri))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaterialsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = (application as NimmaGuruApp).authRepository.currentUser
        val uid = user?.uid.orEmpty()

        binding.topAppBar.setNavigationOnClickListener { finish() }
        materialAdapter = MaterialAdapter(
            currentUid = uid,
            onOpen = { material ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(material.resourceUrl)))
            },
            onSave = { material ->
                if (uid.isBlank()) {
                    Snackbar.make(binding.root, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG).show()
                } else {
                    viewModel.toggleSaved(material, uid)
                }
            }
        )

        binding.materialsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.materialsRecyclerView.adapter = materialAdapter
        binding.searchButton.setOnClickListener {
            viewModel.startListening(binding.subjectEditText.text?.toString().orEmpty(), currentCurriculumType)
        }
        binding.clearButton.setOnClickListener {
            binding.subjectEditText.setText("")
            viewModel.startListening(curriculumType = currentCurriculumType)
        }
        binding.uploadNotesButton.setOnClickListener {
            if (uid.isBlank()) {
                Snackbar.make(binding.root, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG).show()
            } else {
                showUploadChoiceDialog(uid)
            }
        }
        binding.syllabusButton.setOnClickListener {
            lifecycleScope.launch {
                val firestoreTopics = runCatching {
                    (application as NimmaGuruApp).learningRepository.loadCurriculum(currentCurriculumType)
                }.getOrDefault(emptyList())
                val message = if (firestoreTopics.isNotEmpty()) {
                    SyllabusCatalog.formatted(firestoreTopics)
                } else {
                    SyllabusCatalog.formatted(currentCurriculumType)
                }
                AlertDialog.Builder(this@MaterialsActivity)
                    .setTitle(R.string.view_syllabus)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }

        viewModel.uiState.observe(this) { state ->
            binding.progressIndicator.visibility = if (state.isLoading || state.isSaving) View.VISIBLE else View.GONE
            materialAdapter.submitList(state.materials)
            EmptyStateComponent.bind(
                binding.emptyStateTextView,
                !state.isLoading && state.materials.isEmpty(),
                "${getString(R.string.empty_notes_bitmoji)}\n${getString(R.string.no_materials_found)}"
            )

            state.infoMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
            state.errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }

        lifecycleScope.launch {
            currentCurriculumType = uid.takeIf { it.isNotBlank() }
                ?.let { (application as NimmaGuruApp).userRoleRepository.loadCurriculumType(it) }
            currentGradeLevel = uid.takeIf { it.isNotBlank() }
                ?.let { (application as NimmaGuruApp).userRoleRepository.loadGradeLevel(it) }
                .orEmpty()
            binding.subjectEditText.setAdapter(
                ArrayAdapter(
                    this@MaterialsActivity,
                    android.R.layout.simple_list_item_1,
                    SubjectCatalog.subjects(this@MaterialsActivity, currentCurriculumType, currentGradeLevel)
                )
            )
            viewModel.startListening(curriculumType = currentCurriculumType)
        }
    }

    private fun showUploadChoiceDialog(uid: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.upload_notes)
            .setItems(arrayOf(getString(R.string.upload_link), getString(R.string.upload_document))) { _, which ->
                showUploadNotesDialog(uid, uploadDocument = which == 1)
            }
            .show()
    }

    private fun showUploadNotesDialog(uid: String, uploadDocument: Boolean) {
        selectedFileUri = null
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 0)
        }
        fun field(hint: String, value: String = ""): TextInputEditText {
            val layout = TextInputLayout(this).apply { this.hint = hint }
            val editText = TextInputEditText(layout.context).apply { setText(value) }
            layout.addView(editText)
            container.addView(layout)
            return editText
        }
        fun subjectDropdown(): MaterialAutoCompleteTextView {
            val layout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox_ExposedDropdownMenu)
                .apply { hint = getString(R.string.subject) }
            val editText = MaterialAutoCompleteTextView(layout.context)
            editText.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, SubjectCatalog.subjects(this, currentCurriculumType, currentGradeLevel)))
            layout.addView(editText)
            container.addView(layout)
            return editText
        }
        val title = field(getString(R.string.name))
        val subject = subjectDropdown()
        val curriculum = field(getString(R.string.board), currentCurriculumType?.displayName ?: "CBSE")
        val classLevel = field(getString(R.string.class_level), "Class 10")
        val link = field(getString(R.string.notes_link))
        link.visibility = if (uploadDocument) View.GONE else View.VISIBLE
        val description = field(getString(R.string.thank_you_message))
        selectedFileNameTextView = TextView(this).apply {
            text = getString(R.string.upload_file_required)
            setPadding(0, 12, 0, 8)
        }
        if (uploadDocument) container.addView(selectedFileNameTextView)
        val selectFileButton = com.google.android.material.button.MaterialButton(this).apply {
            text = getString(R.string.select_file)
            setOnClickListener {
                filePicker.launch(
                    arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "image/*"
                    )
                )
            }
        }
        if (uploadDocument) container.addView(selectFileButton)

        AlertDialog.Builder(this)
            .setTitle(R.string.upload_notes)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val fileUri = selectedFileUri
                val validationError = if (uploadDocument) {
                    validateUploadFile(fileUri)
                } else if (link.text?.toString().orEmpty().isBlank()) {
                    getString(R.string.notes_link_required)
                } else {
                    null
                }
                if (validationError != null) {
                    Snackbar.make(binding.root, validationError, Snackbar.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                viewModel.uploadMaterial(
                    LearningMaterial(
                        title = title.text?.toString().orEmpty(),
                        subject = subject.text?.toString().orEmpty(),
                        curriculumType = currentCurriculumType?.firestoreValue
                            ?: CurriculumType.fromStorage(curriculum.text?.toString())?.firestoreValue
                            ?: CurriculumType.CBSE.firestoreValue,
                        classLevel = classLevel.text?.toString().orEmpty(),
                        type = "Notes",
                        size = if (uploadDocument) formatSize(getFileSize(fileUri)) else "Link",
                        description = description.text?.toString().orEmpty(),
                        resourceUrl = link.text?.toString().orEmpty().ifBlank { fileUri.toString() }
                    ),
                    uid
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun validateUploadFile(uri: Uri?): String? {
        if (uri == null) return getString(R.string.upload_file_required)
        val mimeType = contentResolver.getType(uri).orEmpty()
        val name = getDisplayName(uri).lowercase()
        val isAllowed = mimeType.startsWith("image/") ||
            mimeType in ALLOWED_DOCUMENT_MIME_TYPES ||
            ALLOWED_EXTENSIONS.any { name.endsWith(it) }
        if (!isAllowed) return getString(R.string.upload_file_type_invalid)
        if (getFileSize(uri) > MAX_UPLOAD_BYTES) return getString(R.string.upload_file_too_large)
        return null
    }

    private fun getDisplayName(uri: Uri): String {
        return queryOpenable(uri, OpenableColumns.DISPLAY_NAME) ?: uri.lastPathSegment.orEmpty()
    }

    private fun getFileSize(uri: Uri?): Long {
        if (uri == null) return 0L
        return queryOpenable(uri, OpenableColumns.SIZE)?.toLongOrNull() ?: 0L
    }

    private fun queryOpenable(uri: Uri, column: String): String? {
        val cursor: Cursor? = contentResolver.query(uri, arrayOf(column), null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    private fun formatSize(bytes: Long): String {
        return if (bytes >= 1024 * 1024) {
            "${bytes / (1024 * 1024)} MB"
        } else {
            "${bytes / 1024} KB"
        }
    }

    private companion object {
        const val MAX_UPLOAD_BYTES = 10L * 1024L * 1024L
        val ALLOWED_EXTENSIONS = setOf(".pdf", ".doc", ".docx", ".jpg", ".jpeg", ".png", ".webp", ".gif")
        val ALLOWED_DOCUMENT_MIME_TYPES = setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    }
}
