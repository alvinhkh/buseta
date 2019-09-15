package com.alvinhkh.buseta.mtr.dao

import androidx.room.Dao
import androidx.room.Query
import com.alvinhkh.buseta.mtr.model.*


@Dao
interface MtrBusDao {

    @Query("SELECT * FROM busRoute")
    fun allRoutes(): List<MtrBusRoute>

    @Query("SELECT * FROM busRouteLine")
    fun allRouteLines(): List<MtrBusRouteLine>

    @Query("SELECT * FROM busStop ORDER BY routeLine_ID ASC, sort_order ASC")
    fun allStops(): List<MtrBusStop>

    @Query("SELECT * FROM busFare ORDER BY route_ID ASC")
    fun allFares(): List<MtrBusFare>

    @Query("SELECT * FROM busFrequence ORDER BY routeLine_ID ASC, sort_order ASC")
    fun allFrequences(): List<MtrBusFrequence>

}