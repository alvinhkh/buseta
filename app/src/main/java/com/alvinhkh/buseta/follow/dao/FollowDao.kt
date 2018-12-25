package com.alvinhkh.buseta.follow.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.database.Cursor
import com.alvinhkh.buseta.follow.model.Follow


@Dao
interface FollowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: Follow): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: List<Follow>): List<Long>

    @Query("DELETE FROM follow")
    fun clear(): Int

    @Query("DELETE FROM follow WHERE `type` = :type AND `company` = :companyCode AND `no` = :routeNo AND `bound` = :routeSeq AND `route_service_type` = :routeServiceType AND `stop_code` = :stopId AND `stop_seq` = :stopSeq")
    fun delete(type: String, companyCode: String, routeNo: String, routeSeq: String, routeServiceType: String, stopId: String, stopSeq: String): Int

    @Query("SELECT COUNT(*) FROM follow")
    fun count(): Int

    @Query("SELECT COUNT(*) FROM follow WHERE `type` = :type AND `company` = :companyCode AND `no` = :routeNo AND `bound` = :routeSeq AND `route_service_type` = :routeServiceType AND `stop_code` = :stopId AND `stop_seq` = :stopSeq")
    fun count(type: String, companyCode: String, routeNo: String, routeSeq: String, routeServiceType: String, stopId: String, stopSeq: String): Int

    @Query("SELECT COUNT(*) FROM follow WHERE `type` = :type AND `company` = :companyCode AND `no` = :routeNo AND `bound` = :routeSeq AND `route_service_type` = :routeServiceType AND `stop_code` = :stopId AND `stop_seq` = :stopSeq")
    fun liveCount(type: String, companyCode: String, routeNo: String, routeSeq: String, routeServiceType: String, stopId: String, stopSeq: String): LiveData<Int>

    @Update
    fun updateAll(vararg entity: Follow)

    @Query("SELECT * FROM follow ORDER BY display_order ASC, date DESC")
    fun getList(): List<Follow>

    @Query("SELECT * FROM follow WHERE `type` = :type ORDER BY display_order ASC, date DESC")
    fun getList(type: String): List<Follow>

    @Query("SELECT * FROM follow ORDER BY display_order ASC, date DESC")
    fun getLiveData(): LiveData<MutableList<Follow>>

    @Query("SELECT * FROM follow WHERE `type` = :type ORDER BY display_order ASC, date DESC")
    fun getLiveData(type: String): LiveData<MutableList<Follow>>

}