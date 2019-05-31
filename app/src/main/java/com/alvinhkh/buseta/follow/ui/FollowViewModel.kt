package com.alvinhkh.buseta.follow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow

class FollowViewModel(application: Application) : AndroidViewModel(application) {

    private val followDatabase = FollowDatabase.getInstance(application)

    lateinit var list: LiveData<MutableList<Follow>>

    init {
        if (followDatabase != null) {
            list = followDatabase.followDao().liveData()
        }
    }

    fun getAsLiveData(groupId: String): LiveData<MutableList<Follow>>{
        return followDatabase?.followDao()?.liveData(groupId)?:list
    }
}