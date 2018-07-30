package com.alvinhkh.buseta.follow.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow

class EditFollowViewModel(application: Application) : AndroidViewModel(application) {

    private val followDatabase = FollowDatabase.getInstance(application)

    lateinit var list: LiveData<MutableList<Follow>>

    init {
        if (followDatabase != null) {
            list = followDatabase.followDao().getLiveData()
        }
    }

    fun getAsLiveData(): LiveData<MutableList<Follow>>{
        return list
    }
}