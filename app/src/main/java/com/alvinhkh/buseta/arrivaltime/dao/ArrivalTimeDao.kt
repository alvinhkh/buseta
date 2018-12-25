package com.alvinhkh.buseta.arrivaltime.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.database.Cursor
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime


@Dao
interface ArrivalTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: ArrivalTime)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: List<ArrivalTime>)

    @Query("DELETE FROM eta")
    fun clear(): Int

    @Query("DELETE FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq")
    fun clear(companyCode: String, routeNo: String, routeSeq: String): Int

    @Query("DELETE FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq AND stop_id = :stopId AND stop_seq = :stopSeq")
    fun clear(companyCode: String, routeNo: String, routeSeq: String, stopId: String, stopSeq: String): Int

    @Query("SELECT * FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq AND stop_id = :stopId AND stop_seq = :stopSeq")
    fun getList(companyCode: String, routeNo: String, routeSeq: String, stopId: String, stopSeq: String): List<ArrivalTime>

    @Query("SELECT * FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq AND stop_id = :stopId AND stop_seq = :stopSeq AND updated_at > :updatedAt AND eta_expire > :expireAt ORDER BY eta_id ASC")
    fun getList(companyCode: String, routeNo: String, routeSeq: String, stopId: String,
                stopSeq: String, updatedAt: Long, expireAt: String): List<ArrivalTime>

    @Query("SELECT * FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq ORDER BY route_seq + 0 ASC, eta_id ASC")
    fun getLiveData(companyCode: String, routeNo: String, routeSeq: String): LiveData<MutableList<ArrivalTime>>

    @Query("SELECT * FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq AND stop_id = :stopId AND stop_seq = :stopSeq ORDER BY eta_id ASC")
    fun getLiveData(companyCode: String, routeNo: String, routeSeq: String, stopId: String,
                    stopSeq: String): LiveData<MutableList<ArrivalTime>>
}