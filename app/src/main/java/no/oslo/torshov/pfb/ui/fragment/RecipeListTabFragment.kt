package no.oslo.torshov.pfb.ui.fragment

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import no.oslo.torshov.pfb.R
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
                if (f >= itemAdapter.getItems().size || t >= itemAdapter.getItems().size) return false
                itemAdapter.moveItem(f, t)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(rv, viewHolder)
                viewModel.setItems(tabPosition, itemAdapter.getItems())
            }
        })

        val addLabel = getString(when (tabPosition) {
            0 -> no.oslo.torshov.pfb.R.string.add_ingredient
            1 -> no.oslo.torshov.pfb.R.string.add_step
            2 -> no.oslo.torshov.pfb.R.string.add_tip
            else -> no.oslo.torshov.pfb.R.string.add_common_mistake
        })

        itemAdapter = TabItemAdapter(
            addLabel = addLabel,
            onAddItem = {
                pendingFocusNewItem = true
                viewModel.addItem(tabPosition, "")
            },
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

        // Detect taps at the RecyclerView level — bypasses EditText touch-event consumption
        val rv = binding.recyclerView
        val tapDetector = GestureDetector(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    val child = rv.findChildViewUnder(e.x, e.y) ?: return false
                    val position = rv.getChildAdapterPosition(child)
                    if (position == RecyclerView.NO_POSITION ||
                        position >= itemAdapter.getItems().size) return false
                    // Don't toggle if the tap landed on a visible action button
                    val localX = (e.x - child.left).toInt()
                    val localY = (e.y - child.top).toInt()
                    val editBtn  = child.findViewById<View>(R.id.buttonEdit)
                    val deleteBtn = child.findViewById<View>(R.id.buttonDelete)
                    fun View?.hits() = this?.visibility == View.VISIBLE &&
                        localX in left..right && localY in top..bottom
                    if (editBtn.hits() || deleteBtn.hits()) return false
                    itemAdapter.toggleSelected(position, rv)
                    return true
                }
            })
        rv.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                tapDetector.onTouchEvent(e)
                return false
            }
        })

        var initialScrollDone = false
        viewModel.getItemsLiveData(tabPosition).observe(viewLifecycleOwner) { items ->
            val prevSize = itemAdapter.getItems().size
            itemAdapter.setItems(items)
            binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            if (!initialScrollDone) {
                initialScrollDone = true
                binding.recyclerView.post {
                    (binding.recyclerView.layoutManager
                        as? androidx.recyclerview.widget.LinearLayoutManager)
                        ?.scrollToPositionWithOffset(0, 0)
                }
            }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
