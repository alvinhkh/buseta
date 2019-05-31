package com.alvinhkh.buseta.follow.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.alvinhkh.buseta.follow.model.FollowGroup


@Dao
interface FollowGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: FollowGroup): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: List<FollowGroup>): List<Long>

    @Query("DELETE FROM follow_category WHERE `id` = :id")
    fun delete(id: String): Int

    @Query("SELECT * FROM follow_category WHERE `id` = :id")
    fun get(id: String): FollowGroup

    @Update
    fun updateAll(vararg entity: FollowGroup)

    @Query("SELECT * FROM follow_category ORDER BY display_order ASC, updated_at DESC")
    fun list(): MutableList<FollowGroup>

    @Query("SELECT * FROM follow_category ORDER BY display_order ASC, updated_at DESC")
    fun liveData(): LiveData<MutableList<FollowGroup>>

    @Query("SELECT * FROM follow_category WHERE `id` = :id")
    fun liveData(id: String): LiveData<FollowGroup>

}