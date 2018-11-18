package com.alvinhkh.buseta.route.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import com.alvinhkh.buseta.model.Route


@Dao
interface RouteDao {

    @Query("DELETE FROM routes WHERE company_code = :companyCode AND name = :routeNo AND last_update < :lastUpdate")
    fun delete(companyCode: String, routeNo: String, lastUpdate: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: Route): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: List<Route>): List<Long>

    @Query("SELECT * FROM routes")
    fun liveData(): LiveData<MutableList<Route>>

    @Query("SELECT * FROM routes WHERE company_code = :companyCode AND name = :routeNo ORDER BY sequence ASC, service_type ASC")
    fun liveData(companyCode: String, routeNo: String): LiveData<MutableList<Route>>

    @Query("SELECT * FROM routes")
    fun list(): List<Route>

}