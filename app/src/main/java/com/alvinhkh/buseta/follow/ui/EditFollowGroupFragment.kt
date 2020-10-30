package com.alvinhkh.buseta.follow.ui

import androidx.lifecycle.ViewModelProvider
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.FollowGroup
import com.alvinhkh.buseta.ui.OnItemDragListener
import com.alvinhkh.buseta.ui.SimpleItemTouchHelperCallback
import com.alvinhkh.buseta.utils.ColorUtil
import java.lang.ref.WeakReference


class EditFollowGroupFragment: Fragment(), OnItemDragListener {

    private lateinit var followDatabase: FollowDatabase
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var viewAdapter: EditFollowGroupViewAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private var dragItemPosition = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_follow, container, false)
        setHasOptionsMenu(true)
        followDatabase = FollowDatabase.getInstance(rootView.context)!!
        emptyView = rootView.findViewById(R.id.empty_view)
        recyclerView = rootView.findViewById(R.id.recycler_view)
        with(recyclerView) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            viewAdapter = EditFollowGroupViewAdapter(WeakReference(requireActivity().supportFragmentManager), this@EditFollowGroupFragment)
            adapter = viewAdapter
            itemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(viewAdapter, false))
            itemTouchHelper.attachToRecyclerView(this)
        }
        val viewModel = ViewModelProvider(this).get(FollowGroupViewModel::class.java)
        viewModel.getAsLiveData().observe(viewLifecycleOwner, { list ->
            viewAdapter.replaceItems(list?: mutableListOf())
            emptyView.visibility = if (viewAdapter.itemCount > 0) View.GONE else View.VISIBLE
        })
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.title = getString(R.string.edit_follow_group)
            actionBar?.subtitle = null
            val color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            (activity as AppCompatActivity).supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                activity?.window?.statusBarColor = color
                activity?.window?.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.transparent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity?.window?.statusBarColor = ColorUtil.darkenColor(color)
                activity?.window?.navigationBarColor = ColorUtil.darkenColor(color)
            }
            activity?.findViewById<FrameLayout>(R.id.adView_container)?.setBackgroundColor(color)
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            fab?.hide()
        }
        viewAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_refresh) {
            viewAdapter.notifyDataSetChanged()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
        dragItemPosition = viewHolder.adapterPosition
    }

    override fun onItemStopDrag(viewHolder: RecyclerView.ViewHolder) {
        if (dragItemPosition >= 0 && dragItemPosition != viewHolder.adapterPosition) {
            val fromPosition = dragItemPosition
            val toPosition = viewHolder.adapterPosition
            val updateItems = arrayListOf<FollowGroup>()
            followDatabase.followGroupDao().list().forEachIndexed { index, followGroup ->
                var update = false
                if (fromPosition < toPosition) {  // down
                    if (index > fromPosition - 1) {
                        update = true
                    }
                } else if (fromPosition > toPosition) {  // up
                    if (index > toPosition - 1) {
                        update = true
                    }
                }
                if (update) {
                    var i = index
                    when {
                        index == fromPosition -> i = toPosition
                        fromPosition < toPosition -> // down
                            i = index - 1
                        fromPosition > toPosition -> // up
                            i = index + 1
                    }
                    followGroup.displayOrder = i
                    updateItems.add(followGroup)
                }
            }
            followDatabase.followGroupDao().updateAll(*updateItems.toTypedArray())
        }
        dragItemPosition = -1
    }

    companion object {

        fun newInstance(): EditFollowGroupFragment {
            val fragment = EditFollowGroupFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
