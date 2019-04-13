package com.alvinhkh.buseta.route.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import com.alvinhkh.buseta.route.model.LatLong
import com.alvinhkh.buseta.route.model.Route


@Dao
interface RouteDao {

    @Query("DELETE FROM routes")
    fun clear(): Int

    @Query("SELECT COUNT(*) FROM routes")
    fun count(): Int

    @Query("SELECT COUNT(*) FROM routes WHERE company_code = :companyCode AND name = :routeNo AND data_source = ''")
    fun count(companyCode: String, routeNo: String): Int

    @Query("DELETE FROM routes WHERE company_code = :companyCode AND last_update < :lastUpdate AND data_source = ''")
    fun delete(companyCode: String, lastUpdate: Long): Int

    @Query("DELETE FROM routes WHERE company_code = :companyCode AND name = :routeNo AND last_update < :lastUpdate AND data_source = ''")
    fun delete(companyCode: String, routeNo: String, lastUpdate: Long): Int

    @Query("DELETE FROM routes WHERE data_source = :dataSource AND last_update < :lastUpdate")
    fun deleteBySource(dataSource: String, lastUpdate: Long): Int

    @Query("SELECT * FROM routes WHERE company_code = :companyCode AND code = :routeId AND name = :routeNo AND sequence = :sequence AND service_type = :serviceType AND data_source = ''")
    fun get(companyCode: String, routeId: String, routeNo: String, sequence: String, serviceType: String): Route?

    @Query("SELECT * FROM routes WHERE company_code = :companyCode AND code = :routeCode AND data_source = ''")
    fun getByCode(companyCode: String, routeCode: String): Route?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: Route): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: List<Route>): List<Long>

    @Query("SELECT * FROM routes WHERE data_source = '' ORDER BY company_code ASC, name ASC, is_special ASC, sequence + 0 ASC, service_type + 0 ASC")
    fun liveData(): LiveData<MutableList<Route>>

    @Query("SELECT * FROM routes WHERE data_source = :dataSource ORDER BY company_code ASC, name ASC, is_special ASC, sequence + 0 ASC, service_type + 0 ASC")
    fun liveData(dataSource: String): LiveData<MutableList<Route>>

    @Query("SELECT * FROM routes WHERE data_source = :dataSource AND name LIKE :routeNo ORDER BY company_code ASC, name ASC, is_special ASC, sequence + 0 ASC, service_type + 0 ASC")
    fun liveData(dataSource: String, routeNo: String): LiveData<MutableList<Route>>

    @Query("SELECT * FROM routes WHERE data_source = :dataSource AND name = :routeNo AND company_code = :companyCode ORDER BY company_code ASC, name ASC, is_special ASC, sequence + 0 ASC, service_type + 0 ASC")
    fun liveData(dataSource: String, routeNo: String, companyCode: String): LiveData<MutableList<Route>>

    @Query("UPDATE routes SET map_coordinates = '[]' WHERE company_code = :companyCode AND code = :routeId AND name = :routeNo AND sequence = :sequence AND service_type = :serviceType")
    fun deleteCoordinates(companyCode: String, routeId: String, routeNo: String, sequence: String, serviceType: String): Int

    @Query("UPDATE routes SET map_coordinates = :coordinates WHERE company_code = :companyCode AND code = :routeId AND name = :routeNo AND sequence = :sequence AND service_type = :serviceType")
    fun updateCoordinates(companyCode: String, routeId: String, routeNo: String, sequence: String, serviceType: String, coordinates: List<LatLong>): Int

}