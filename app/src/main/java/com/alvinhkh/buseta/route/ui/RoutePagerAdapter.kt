package com.alvinhkh.buseta.route.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.alvinhkh.buseta.route.model.Route

class RoutePagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var fragmentList: MutableList<Fragment> = arrayListOf()

    internal var routeList: MutableList<Route> = arrayListOf()

    private var pageTitleList: MutableList<String> = arrayListOf()

    override fun getItem(position: Int) = fragmentList[position]

    override fun getCount() = fragmentList.size

    override fun getPageTitle(position: Int): CharSequence? {
        return pageTitleList[position]
    }

    fun addFragment(fragment: Fragment, pageTitle: String, route: Route) {
        fragmentList.add(fragment)
        pageTitleList.add(pageTitle)
        routeList.add(route)
        notifyDataSetChanged()
    }

    fun clear() {
        fragmentList.clear()
        pageTitleList.clear()
        routeList.clear()
        notifyDataSetChanged()
    }
}