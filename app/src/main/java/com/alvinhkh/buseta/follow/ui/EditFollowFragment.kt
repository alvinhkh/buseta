package com.alvinhkh.buseta.follow.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.ui.OnItemDragListener
import com.alvinhkh.buseta.ui.SimpleItemTouchHelperCallback


class EditFollowFragment: Fragment(), OnItemDragListener {

    private lateinit var followDatabase: FollowDatabase
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var viewModel: EditFollowViewModel
    private lateinit var viewAdapter: EditFollowViewAdapter
    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null
    private var dragItemPosition = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_follow_edit, container, false)
        setHasOptionsMenu(true)

        followDatabase = FollowDatabase.getInstance(rootView.context)!!
        emptyView = rootView.findViewById(R.id.empty_view)
        recyclerView = rootView.findViewById(R.id.recycler_view)
        if (recyclerView != null) {
            with(recyclerView!!) {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                viewAdapter = EditFollowViewAdapter(this, null, this@EditFollowFragment)
                adapter = viewAdapter
                itemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(viewAdapter))
                itemTouchHelper.attachToRecyclerView(this)
                viewModel = ViewModelProviders.of(this@EditFollowFragment).get(EditFollowViewModel::class.java)
                viewModel.getAsLiveData().observe(this@EditFollowFragment, Observer {
                    viewAdapter.clear()
                    it?.forEach {
                        viewAdapter.addItem(it)
                    }
                    emptyView?.visibility = if (viewAdapter.itemCount > 0) View.GONE else View.VISIBLE
                })
            }
        }

        if (activity != null) {
            val fab = activity!!.findViewById<FloatingActionButton>(R.id.fab)
            fab?.hide()
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.setTitle(R.string.edit_follow_list)
        }
        if (view != null) {
            Snackbar.make(view!!, R.string.swipe_to_remove_follow_stop, Snackbar.LENGTH_SHORT).show()
        }
        viewAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        if (id == R.id.action_refresh) {
            viewAdapter.notifyDataSetChanged()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemStartDrag(viewHolder: RecyclerView.ViewHolder?) {
        if (viewHolder != null) {
            itemTouchHelper.startDrag(viewHolder)
            dragItemPosition = viewHolder.adapterPosition
        }
    }

    override fun onItemStopDrag(viewHolder: RecyclerView.ViewHolder?) {
        if (dragItemPosition >= 0 && dragItemPosition != viewHolder?.adapterPosition) {
            val fromPosition = dragItemPosition
            val toPosition = viewHolder?.adapterPosition?:0
            val updateItems = arrayListOf<Follow>()
            followDatabase.followDao().getList().forEachIndexed { index, follow ->
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
                    if (index == fromPosition) {
                        i = toPosition
                    } else if (fromPosition < toPosition) {  // down
                        i = index - 1
                    } else if (fromPosition > toPosition) {  // up
                        i = index + 1
                    }
                    follow.order = i
                    updateItems.add(follow)
                }
            }
            followDatabase.followDao().updateAll(*updateItems.toTypedArray())
        }
        dragItemPosition = -1
    }
}
