package no.oslo.torshov.pfb.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.databinding.ItemTabEntryBinding

class TabItemAdapter(
    private val addLabel: String,
    private val onAddItem: () -> Unit,
    private val onTextChanged: (Int, String) -> Unit,
    private val onSplit: (Int, List<String>) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ITEM    = 0
        private const val TYPE_ADD     = 1
        private val UNIT_PAYLOAD = Any()
    }

    private val items = mutableListOf<String>()
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    fun setItems(newItems: List<String>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(op: Int, np: Int) = items[op] == newItems[np]
            override fun areContentsTheSame(op: Int, np: Int) = items[op] == newItems[np]
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    fun moveItem(from: Int, to: Int) {
        if (from >= items.size || to >= items.size) return
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
    }

    fun getItems(): List<String> = items.toList()

    override fun getItemCount() = items.size + 1

    override fun getItemViewType(position: Int) =
        if (position == items.size) TYPE_ADD else TYPE_ITEM

    // Called from the fragment's RecyclerView-level gesture detector
    fun toggleSelected(position: Int, rv: RecyclerView) {
        if (position >= items.size) return
        val wasSelected = selectedPosition == position
        val prevPos = selectedPosition
        selectedPosition = if (wasSelected) RecyclerView.NO_POSITION else position
        // Collapse previous row directly on its live ViewHolder
        if (prevPos != RecyclerView.NO_POSITION && prevPos != position) {
            (rv.findViewHolderForAdapterPosition(prevPos) as? ViewHolder)
                ?.setButtonsVisible(false)
                ?: notifyItemChanged(prevPos, UNIT_PAYLOAD)
        }
        // Toggle current row directly
        (rv.findViewHolderForAdapterPosition(position) as? ViewHolder)
            ?.setButtonsVisible(!wasSelected)
            ?: notifyItemChanged(position, UNIT_PAYLOAD)
    }

    // Payload-only rebind: just updates button visibility, no EditText flicker
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>
    ) {
        if (payloads.isNotEmpty()) {
            (holder as? ViewHolder)?.setButtonsVisible(position == selectedPosition)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val label: TextView = itemView.findViewById(R.id.textAddLabel)
        init { itemView.setOnClickListener { onAddItem() } }
    }

    // ── Item row ─────────────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(val binding: ItemTabEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var watcher: TextWatcher? = null
        private var isSplitting = false

        fun setButtonsVisible(visible: Boolean) {
            val v = if (visible) View.VISIBLE else View.GONE
            binding.buttonEdit.visibility = v
            binding.buttonDelete.visibility = v
        }

        init {
            binding.root.setOnLongClickListener {
                onStartDrag(this)
                true
            }
            binding.buttonEdit.setOnClickListener {
                setEditable(true)
                binding.editItem.requestFocus()
                binding.editItem.selectAll()
                val imm = binding.editItem.context
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.editItem, InputMethodManager.SHOW_IMPLICIT)
            }
            binding.editItem.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    setEditable(false)
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION && selectedPosition == pos) {
                        selectedPosition = RecyclerView.NO_POSITION
                        setButtonsVisible(false)
                    }
                }
            }
        }

        private fun setEditable(editable: Boolean) {
            binding.editItem.isFocusable = editable
            binding.editItem.isFocusableInTouchMode = editable
            binding.editItem.isCursorVisible = editable
        }

        fun bind(text: String, isSelected: Boolean) {
            val btnVisibility = if (isSelected) View.VISIBLE else View.GONE
            binding.buttonEdit.visibility = btnVisibility
            binding.buttonDelete.visibility = btnVisibility

            watcher?.let { binding.editItem.removeTextChangedListener(it) }

            if (!binding.editItem.hasFocus()) {
                setEditable(false)
                binding.editItem.setText(text)
            }

            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isSplitting) return
                    val pos = bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION) return
                    val raw = s?.toString() ?: ""

                    if ('\n' in raw) {
                        isSplitting = true
                        val parts = raw.split('\n').map { it.trim() }
                        val first = parts.first()
                        val rest = parts.drop(1).filter { it.isNotEmpty() }
                        binding.editItem.setText(first)
                        binding.editItem.setSelection(first.length)
                        when {
                            rest.isNotEmpty() -> onSplit(pos, listOf(first) + rest)
                            else -> {
                                setEditable(false)
                                binding.editItem.clearFocus()
                                val imm = binding.editItem.context
                                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.hideSoftInputFromWindow(binding.editItem.windowToken, 0)
                            }
                        }
                        isSplitting = false
                    } else {
                        onTextChanged(pos, raw)
                    }
                }
            }
            binding.editItem.addTextChangedListener(watcher)

            binding.buttonDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onDelete(pos)
            }
        }

        fun requestEditFocus() {
            setEditable(true)
            binding.editItem.requestFocus()
        }
    }

    // ── RecyclerView overrides ───────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_ADD) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_add_entry, parent, false)
            AddViewHolder(view)
        } else {
            ViewHolder(
                ItemTabEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder  -> holder.bind(items[position], position == selectedPosition)
            is AddViewHolder -> holder.label.text = addLabel
        }
    }
}

