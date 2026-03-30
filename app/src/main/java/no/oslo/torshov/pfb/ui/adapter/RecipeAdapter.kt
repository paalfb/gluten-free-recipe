package no.oslo.torshov.pfb.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.data.model.Recipe
import no.oslo.torshov.pfb.databinding.ItemRecipeBinding

class RecipeAdapter(
    private val onClick: (Recipe) -> Unit,
    private val onLongClick: (Recipe) -> Unit,
    private val onExperiencesClick: (Recipe) -> Unit,
    private val onFavouriteClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeAdapter.ViewHolder>(DiffCallback()) {

    var recipesWithExperiences: Set<Long> = emptySet()
        set(value) {
            val changed = field.union(value) - field.intersect(value)
            field = value
            val current = currentList
            changed.forEach { id ->
                val pos = current.indexOfFirst { it.id == id }
                if (pos != -1) notifyItemChanged(pos)
            }
        }

    inner class ViewHolder(private val binding: ItemRecipeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: Recipe) {
            val name = recipe.name
            val displayEmoji: String
            val displayName: String
            if (recipe.emoji.isNotEmpty()) {
                displayEmoji = recipe.emoji
                displayName = name
            } else {
                val prefixEnd = emojiPrefixEnd(name)
                displayEmoji = name.substring(0, prefixEnd).trim()
                displayName = name.substring(prefixEnd)
            }
            binding.textEmoji.text = displayEmoji
            binding.textRecipeName.text = displayName
            binding.iconTested.visibility = if (recipe.tested && recipe.id !in recipesWithExperiences) android.view.View.VISIBLE else android.view.View.GONE
            binding.iconExperiences.visibility = if (recipe.id in recipesWithExperiences) android.view.View.VISIBLE else android.view.View.GONE
            binding.iconExperiences.setOnClickListener { onExperiencesClick(recipe) }
            binding.iconFavourite.visibility = if (recipe.favourite) android.view.View.VISIBLE else android.view.View.GONE
            binding.iconFavourite.setImageResource(R.drawable.ic_star)
            binding.iconFavourite.setColorFilter(android.graphics.Color.parseColor("#FFC107"))
            binding.iconFavourite.setOnClickListener { onFavouriteClick(recipe) }
            binding.root.setOnClickListener { onClick(recipe) }
            binding.root.setOnLongClickListener {
                if (recipe.id !in recipesWithExperiences) { onLongClick(recipe) }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun emojiPrefixEnd(name: String): Int {
        var i = 0
        while (i < name.length) {
            val cp = name.codePointAt(i)
            if (Character.isLetter(cp) || Character.isDigit(cp)) break
            i += Character.charCount(cp)
        }
        return i
    }

    class DiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe) =
            oldItem == newItem
    }
}

