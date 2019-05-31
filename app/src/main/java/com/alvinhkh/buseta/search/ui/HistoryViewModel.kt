package com.alvinhkh.buseta.search.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val suggestionDatabase = SuggestionDatabase.getInstance(application)

    lateinit var list: LiveData<MutableList<Suggestion>>

    init {
        if (suggestionDatabase != null) {
            list = suggestionDatabase.suggestionDao().historyLiveData()
        }
    }

    fun getAsLiveData(): LiveData<MutableList<Suggestion>>{
        return list
    }
}