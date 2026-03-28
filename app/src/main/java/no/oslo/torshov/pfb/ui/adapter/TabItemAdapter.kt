package no.oslo.torshov.pfb.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import no.oslo.torshov.pfb.databinding.ItemTabEntryBinding

class TabItemAdapter(
    private val onTextChanged: (Int, String) -> Unit,
    private val onSplit: (Int, List<String>) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<TabItemAdapter.ViewHolder>() {

    private val items = mutableListOf<String>()

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
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
    }

    fun getItems(): List<String> = items.toList()

    override fun getItemCount() = items.size

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(private val binding: ItemTabEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var watcher: TextWatcher? = null
        private var isSplitting = false

        init {
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(this)
                false
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
                if (!hasFocus) setEditable(false)
            }
        }

        private fun setEditable(editable: Boolean) {
            binding.editItem.isFocusable = editable
            binding.editItem.isFocusableInTouchMode = editable
            binding.editItem.isCursorVisible = editable
        }

        fun bind(text: String) {
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
                                // Enter pressed → exit edit mode
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTabEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])
}
