package no.oslo.torshov.pfb.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import no.oslo.torshov.pfb.databinding.FragmentRecipeTabBinding
import no.oslo.torshov.pfb.ui.adapter.TabItemAdapter
import no.oslo.torshov.pfb.ui.viewmodel.RecipeDetailViewModel

class RecipeListTabFragment : Fragment() {

    companion object {
        private const val ARG_TAB_POSITION = "tab_position"

        fun newInstance(tabPosition: Int) = RecipeListTabFragment().apply {
            arguments = Bundle().apply { putInt(ARG_TAB_POSITION, tabPosition) }
        }
    }

    private var _binding: FragmentRecipeTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeDetailViewModel by activityViewModels()
    private lateinit var itemAdapter: TabItemAdapter
    private var tabPosition: Int = 0
    private var pendingFocusNewItem = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tabPosition = arguments?.getInt(ARG_TAB_POSITION) ?: 0

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                val f = from.bindingAdapterPosition
                val t = to.bindingAdapterPosition
                if (f == RecyclerView.NO_POSITION || t == RecyclerView.NO_POSITION) return false
                itemAdapter.moveItem(f, t)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(rv, viewHolder)
                viewModel.setItems(tabPosition, itemAdapter.getItems())
            }
        })

        itemAdapter = TabItemAdapter(
            onTextChanged = { position, text ->
                viewModel.updateItemSilent(tabPosition, position, text)
            },
            onSplit = { position, parts ->
                viewModel.insertItemsAt(tabPosition, position, parts)
            },
            onDelete = { position -> viewModel.removeItem(tabPosition, position) },
            onStartDrag = { vh -> itemTouchHelper.startDrag(vh) }
        )

        binding.recyclerView.adapter = itemAdapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.getItemsLiveData(tabPosition).observe(viewLifecycleOwner) { items ->
            val prevSize = itemAdapter.getItems().size
            itemAdapter.setItems(items)
            binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            if (items.size > prevSize && pendingFocusNewItem) {
                pendingFocusNewItem = false
                val newPos = items.size - 1
                binding.recyclerView.scrollToPosition(newPos)
                binding.recyclerView.post {
                    (binding.recyclerView.findViewHolderForAdapterPosition(newPos)
                        as? TabItemAdapter.ViewHolder)?.requestEditFocus()
                }
            }
        }

        binding.fab.setOnClickListener {
            pendingFocusNewItem = true
            viewModel.addItem(tabPosition, "")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
