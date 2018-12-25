package com.alvinhkh.buseta.search.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.database.Cursor
import com.alvinhkh.buseta.search.model.Suggestion


@Dao
interface SuggestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: Suggestion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: List<Suggestion>)

    @Query("DELETE FROM suggestions WHERE type = :type AND company = :companyCode AND text = :routeNo")
    fun delete(type: String, companyCode: String, routeNo: String): Int

    @Query("DELETE FROM suggestions WHERE type = :type AND company = :companyCode AND date < :lastUpdate")
    fun delete(type: String, companyCode: String, lastUpdate: Long): Int

    @Query("SELECT COUNT(*) FROM suggestions WHERE type = 'default'")
    fun countDefault(): Int

    @Query("SELECT COUNT(*) FROM suggestions WHERE type = 'history'")
    fun countHistory(): Int

    @Query("DELETE FROM suggestions WHERE type = 'history'")
    fun clearHistory(): Int

    @Query("DELETE FROM suggestions WHERE type = 'default'")
    fun clearDefault()

    @Query("SELECT * FROM suggestions WHERE type = 'default' ORDER BY company DESC, text ASC")
    fun suggestionLiveData(): LiveData<MutableList<Suggestion>>

    @Query("SELECT * FROM suggestions WHERE text LIKE :text AND type = 'default' ORDER BY company DESC, text ASC")
    fun get(text: String): LiveData<MutableList<Suggestion>>

    @Query("SELECT _id, * FROM suggestions WHERE text LIKE :text AND type = 'default' ORDER BY text ASC, company DESC")
    fun defaultCursor(text: String): Cursor

    @Query("SELECT _id, * FROM suggestions WHERE type = 'history' ORDER BY date DESC LIMIT :limit")
    fun historyCursor(limit: Int): Cursor

    @Query("SELECT * FROM suggestions WHERE type = 'history' ORDER BY date DESC, company DESC, text ASC")
    fun historyLiveData(): LiveData<MutableList<Suggestion>>

    @Query("SELECT * FROM suggestions WHERE type = 'history' ORDER BY date DESC, company DESC, text ASC LIMIT :limit")
    fun historyList(limit: Int): List<Suggestion>

}