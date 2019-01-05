package com.alvinhkh.buseta.follow.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.FollowGroup

class FollowGroupViewModel(application: Application) : AndroidViewModel(application) {

    private val followDatabase = FollowDatabase.getInstance(application)

    lateinit var list: LiveData<MutableList<FollowGroup>>

    fun getAsLiveData(): LiveData<MutableList<FollowGroup>> {
        if (followDatabase != null) {
            list = followDatabase.followGroupDao().liveData()
        }
        return list
    }
}