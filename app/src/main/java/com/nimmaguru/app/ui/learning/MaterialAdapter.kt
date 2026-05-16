package com.nimmaguru.app.ui.learning

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ItemMaterialBinding
import com.nimmaguru.app.domain.model.LearningMaterial

class MaterialAdapter(
    private val currentUid: String,
    private val onOpen: (LearningMaterial) -> Unit,
    private val onSave: (LearningMaterial) -> Unit
) : ListAdapter<LearningMaterial, MaterialAdapter.MaterialViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        return MaterialViewHolder(
            ItemMaterialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MaterialViewHolder(
        private val binding: ItemMaterialBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(material: LearningMaterial) {
            val context = binding.root.context
            binding.titleTextView.text = material.title
            binding.metaTextView.text = listOf(material.subject, material.classLevel, material.type, material.size)
                .filter(String::isNotBlank)
                .joinToString(" • ")
            binding.descriptionTextView.text = material.description
            binding.openButton.isEnabled = material.resourceUrl.isNotBlank()
            binding.openButton.setOnClickListener { onOpen(material) }
            binding.saveButton.text = if (material.isSavedBy(currentUid)) {
                context.getString(R.string.saved_material)
            } else {
                context.getString(R.string.save_material)
            }
            binding.saveButton.setOnClickListener { onSave(material) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<LearningMaterial>() {
        override fun areItemsTheSame(oldItem: LearningMaterial, newItem: LearningMaterial): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LearningMaterial, newItem: LearningMaterial): Boolean = oldItem == newItem
    }
}
