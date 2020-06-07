package com.alvinhkh.buseta.follow.ui

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.alvinhkh.buseta.follow.model.FollowGroup

class FollowGroupPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    internal var groupList: MutableList<FollowGroup> = arrayListOf()

    override fun getItem(position: Int): Fragment {
        return FollowFragment.newInstance(groupList[position].id)
    }

    override fun getCount() = groupList.size

    override fun getPageTitle(position: Int): CharSequence? {
        val followGroup = groupList[position]
        return when {
            followGroup.id == FollowGroup.UNCATEGORISED -> "未分類"
            followGroup.name.isNotEmpty() -> followGroup.name
            else -> followGroup.id
        }
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
    }

    fun add(followGroup: FollowGroup) {
        groupList.add(followGroup)
        notifyDataSetChanged()
    }

    fun clear() {
        groupList.clear()
        notifyDataSetChanged()
    }

    fun replace(list: List<FollowGroup>) {
        groupList.clear()
        groupList.addAll(list)
        notifyDataSetChanged()
    }
}