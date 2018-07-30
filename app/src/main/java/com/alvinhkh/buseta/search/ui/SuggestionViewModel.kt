package com.alvinhkh.buseta.search.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion

class SuggestionViewModel(application: Application) : AndroidViewModel(application) {

    private val suggestionDatabase = SuggestionDatabase.getInstance(application)

    lateinit var list: LiveData<MutableList<Suggestion>>

    init {
        if (suggestionDatabase != null) {
            list = suggestionDatabase.suggestionDao().getAll()
        }
    }

    fun getAsLiveData(route: String): LiveData<MutableList<Suggestion>>{
        if (suggestionDatabase != null) {
            if (route.isNotEmpty()) {
                list = suggestionDatabase.suggestionDao().get(route)
            } else {
                list = suggestionDatabase.suggestionDao().getAll()
            }
        }
        return list
    }
}