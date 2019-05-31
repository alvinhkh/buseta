package com.alvinhkh.buseta.arrivaltime.dao

import androidx.lifecycle.LiveData
import androidx.room.*
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

    @Query("DELETE FROM eta WHERE company = :companyCode AND route_no = :routeNo AND updated_at < :updateAt")
    fun clear(companyCode: String, routeNo: String, updateAt: Long): Int

    @Query("DELETE FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq AND stop_id = :stopId AND stop_seq = :stopSeq AND updated_at < :updateAt")
    fun clear(companyCode: String, routeNo: String, routeSeq: String, stopId: String, stopSeq: String, updateAt: Long): Int

    @Query("SELECT * FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq AND stop_id = :stopId AND stop_seq = :stopSeq")
    fun getList(companyCode: String, routeNo: String, routeSeq: String, stopId: String, stopSeq: String): List<ArrivalTime>

    @Query("SELECT * FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq AND stop_id = :stopId AND stop_seq = :stopSeq AND updated_at > :updatedAt AND eta_expire > :expireAt ORDER BY eta_id ASC")
    fun getList(companyCode: String, routeNo: String, routeSeq: String, stopId: String,
                stopSeq: String, updatedAt: Long, expireAt: String): List<ArrivalTime>

    @Query("SELECT * FROM eta ORDER BY route_seq + 0 ASC, eta_id ASC")
    fun getLiveData(): LiveData<MutableList<ArrivalTime>>

    @Query("SELECT * FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq ORDER BY route_seq + 0 ASC, eta_id ASC")
    fun getLiveData(companyCode: String, routeNo: String, routeSeq: String): LiveData<MutableList<ArrivalTime>>

    @Query("SELECT * FROM eta WHERE company = :companyCode AND route_no = :routeNo AND route_seq = :routeSeq AND stop_id = :stopId AND stop_seq = :stopSeq ORDER BY eta_id ASC")
    fun getLiveData(companyCode: String, routeNo: String, routeSeq: String, stopId: String,
                    stopSeq: String): LiveData<MutableList<ArrivalTime>>
}