package com.alvinhkh.buseta.follow.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import androidx.lifecycle.LiveData
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.follow.model.FollowGroup
import com.alvinhkh.buseta.utils.ConnectivityUtil
import java.lang.ref.WeakReference


class FollowFragment: Fragment() {

    private lateinit var arrivalTimeDatabase: ArrivalTimeDatabase
    private lateinit var followDatabase: FollowDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: FollowViewModel
    private var liveData: LiveData<MutableList<Follow>>? = null
    private var viewAdapter: FollowViewAdapter? = null
    private lateinit var emptyView: View
    private lateinit var snackbar: Snackbar
    private var groupId = FollowGroup.UNCATEGORISED

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_follow, container, false)

        groupId = arguments?.getString(C.EXTRA.GROUP_ID)?:FollowGroup.UNCATEGORISED
        arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(requireContext())!!
        followDatabase = FollowDatabase.getInstance(requireContext())!!

        snackbar = Snackbar.make(rootView?.findViewById(R.id.coordinator_layout)?:rootView, R.string.message_no_internet_connection, Snackbar.LENGTH_INDEFINITE)
        if (ConnectivityUtil.isConnected(context)) {
            snackbar.dismiss()
        } else {
            snackbar.show()
        }
        emptyView = rootView.findViewById(R.id.empty_view)
        emptyView.visibility = View.GONE
        recyclerView = rootView.findViewById(R.id.recycler_view)
        viewAdapter = FollowViewAdapter(WeakReference(requireActivity()))
        with(recyclerView) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
        }
        viewModel = ViewModelProvider(this).get(FollowViewModel::class.java)
        liveData = viewModel.liveData(groupId)
        liveData?.removeObservers(this)
        liveData?.observe(viewLifecycleOwner, { list ->
            viewAdapter?.replaceItems(list?: mutableListOf())
            emptyView.visibility = if (list?.size?:0 > 0) View.GONE else View.VISIBLE
        })
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (ConnectivityUtil.isConnected(context)) {
            snackbar.dismiss()
        } else {
            snackbar.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        liveData?.removeObservers(this)
    }

    companion object {

        fun newInstance(groupId: String): FollowFragment {
            val fragment = FollowFragment()
            val args = Bundle()
            args.putString(C.EXTRA.GROUP_ID, groupId)
            fragment.arguments = args
            return fragment
        }
    }
}
