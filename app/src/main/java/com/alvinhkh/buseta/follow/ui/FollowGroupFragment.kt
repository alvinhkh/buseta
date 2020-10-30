package com.alvinhkh.buseta.follow.ui

import androidx.lifecycle.ViewModelProvider
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.widget.FrameLayout
import androidx.lifecycle.LiveData
import com.alvinhkh.buseta.C

import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.FollowGroup
import com.alvinhkh.buseta.search.ui.SearchActivity
import com.alvinhkh.buseta.service.EtaService
import com.alvinhkh.buseta.utils.ColorUtil
import com.alvinhkh.buseta.utils.ConnectivityUtil


class FollowGroupFragment : Fragment() {

    private lateinit var followDatabase: FollowDatabase
    private lateinit var viewPager: ViewPager
    private lateinit var pagerAdapter: FollowGroupPagerAdapter
    private lateinit var viewModel: FollowGroupViewModel
    private var liveData: LiveData<MutableList<FollowGroup>>? = null
    private lateinit var fab: FloatingActionButton

    private val fetchEtaHandler = Handler()
    private val fetchEtaRunnable = object : Runnable {
        override fun run() {
            if (ConnectivityUtil.isConnected(context)) {
                try {
                    val intent = Intent(context, EtaService::class.java)
                    intent.putExtra(C.EXTRA.FOLLOW, true)
                    context?.startService(intent)
                } catch (e: Throwable) {
                }
            }
            fetchEtaHandler.postDelayed(this, 30000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_follow_group, container, false)
        setHasOptionsMenu(true)
        followDatabase = FollowDatabase.getInstance(requireContext())!!
        viewPager = rootView.findViewById(R.id.viewPager)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }
            override fun onPageSelected(position: Int) {
                if (pagerAdapter.groupList.size > position) {
                    val liveData = followDatabase.followGroupDao().liveData(pagerAdapter.groupList[position].id)
                    liveData.removeObservers(this@FollowGroupFragment)
                    liveData.observe(viewLifecycleOwner, { group ->
                        setPageColor(group)
                    })
                }
            }
        })
        pagerAdapter = FollowGroupPagerAdapter(childFragmentManager)
        viewPager.adapter = pagerAdapter
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.visibility = View.GONE
        fab = rootView.findViewById(R.id.fab)
        fab.setOnClickListener {
            startActivity(Intent(context, SearchActivity::class.java))
        }
        viewModel = ViewModelProvider(this).get(FollowGroupViewModel::class.java)
        liveData = viewModel.getAsLiveData()
        liveData?.observe(viewLifecycleOwner, { list ->
            val groupList = mutableListOf<FollowGroup>()
            list?.forEach { item ->
                val count = followDatabase.followDao().count(item.id)
                if (count > 0) {
                    groupList.add(item)
                }
            }
            if (pagerAdapter.groupList.size < 1 && groupList.size > 0) {
                val liveData = followDatabase.followGroupDao().liveData(groupList[0].id)
                liveData.removeObservers(this@FollowGroupFragment)
                liveData.observe(viewLifecycleOwner, { group ->
                    setPageColor(group)
                })
            }
            pagerAdapter.replace(groupList)
            tabLayout.visibility = if (pagerAdapter.groupList.size > 1) View.VISIBLE else View.GONE
        })
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.title = getString(R.string.app_name)
            actionBar?.subtitle = null
        }
        fab.show()
        fetchEtaHandler.postDelayed(fetchEtaRunnable, 500)
    }

    override fun onPause() {
        super.onPause()
        fetchEtaHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        liveData?.removeObservers(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_follow, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> fetchEtaHandler.postDelayed(fetchEtaRunnable, 100)
            R.id.action_edit_follow -> {
                val fragmentManager = activity?.supportFragmentManager!!
                val fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragment_container, EditFollowGroupFragment.newInstance())
                fragmentTransaction.addToBackStack("edit_follow_list")
                fragmentTransaction.commit()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setPageColor(group: FollowGroup?) {
        val color = if (!group?.colour.isNullOrEmpty())
            Color.parseColor(group?.colour)
        else
            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        (activity as AppCompatActivity).supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            activity?.window?.statusBarColor = color
            activity?.window?.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.bottomNavigationBackground)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.window?.statusBarColor = ColorUtil.darkenColor(color)
            activity?.window?.navigationBarColor = ColorUtil.darkenColor(color)
        }
        activity?.findViewById<FrameLayout>(R.id.adView_container)?.setBackgroundColor(color)
        activity?.findViewById<TabLayout>(R.id.tabs)?.background = ColorDrawable(color)
        fab.backgroundTintList = ColorStateList.valueOf(color)
    }
}
