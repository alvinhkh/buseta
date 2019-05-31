package com.alvinhkh.buseta.follow.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.alvinhkh.buseta.follow.model.Follow


@Dao
interface FollowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: Follow): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: List<Follow>): List<Long>

    @Query("DELETE FROM follow")
    fun clear(): Int

    @Query("DELETE FROM follow WHERE `company` = :companyCode AND `no` = :routeNo AND `bound` = :routeSeq AND `route_service_type` = :routeServiceType AND `stop_code` = :stopId AND `stop_seq` = :stopSeq")
    fun delete(companyCode: String, routeNo: String, routeSeq: String, routeServiceType: String, stopId: String, stopSeq: String): Int

    @Query("DELETE FROM follow WHERE `category_id` = :groupId")
    fun delete(groupId: String): Int

    @Query("DELETE FROM follow WHERE `category_id` = :groupId AND `company` = :companyCode AND `no` = :routeNo AND `bound` = :routeSeq AND `route_service_type` = :routeServiceType AND `stop_code` = :stopId AND `stop_seq` = :stopSeq")
    fun delete(groupId: String, companyCode: String, routeNo: String, routeSeq: String, routeServiceType: String, stopId: String, stopSeq: String): Int

    @Query("SELECT COUNT(*) FROM follow")
    fun count(): Int

    @Query("SELECT COUNT(*) FROM follow WHERE `category_id` = :groupId")
    fun count(groupId: String): Int

    @Query("SELECT COUNT(*) FROM follow WHERE `company` = :companyCode AND `no` = :routeNo AND `bound` = :routeSeq AND `route_service_type` = :routeServiceType AND `stop_code` = :stopId AND `stop_seq` = :stopSeq")
    fun count(companyCode: String, routeNo: String, routeSeq: String, routeServiceType: String, stopId: String, stopSeq: String): Int

    @Query("SELECT COUNT(*) FROM follow WHERE `category_id` = :groupId AND `company` = :companyCode AND `no` = :routeNo AND `bound` = :routeSeq AND `route_service_type` = :routeServiceType AND `stop_code` = :stopId AND `stop_seq` = :stopSeq")
    fun count(groupId: String, companyCode: String, routeNo: String, routeSeq: String, routeServiceType: String, stopId: String, stopSeq: String): Int

    @Query("SELECT COUNT(*) FROM follow WHERE `company` = :companyCode AND `no` = :routeNo AND `bound` = :routeSeq AND `route_service_type` = :routeServiceType AND `stop_code` = :stopId AND `stop_seq` = :stopSeq")
    fun liveCount(companyCode: String, routeNo: String, routeSeq: String, routeServiceType: String, stopId: String, stopSeq: String): LiveData<Int>

    @Update
    fun updateAll(vararg entity: Follow)

    @Query("SELECT * FROM follow LEFT JOIN follow_category WHERE `follow`.`category_id` = `follow_category`.`id` ORDER BY `follow_category`.`display_order` ASC, `follow`.`display_order` ASC, `follow`.`date` DESC")
    fun list(): List<Follow>

    @Query("SELECT * FROM follow WHERE `category_id` = :groupId ORDER BY display_order ASC, date DESC")
    fun list(groupId: String): List<Follow>

    @Query("SELECT * FROM follow LEFT JOIN follow_category WHERE `follow`.`category_id` = `follow_category`.`id` ORDER BY `follow_category`.`display_order` ASC, `follow`.`display_order` ASC, `follow`.`date` DESC")
    fun liveData(): LiveData<MutableList<Follow>>

    @Query("SELECT * FROM follow WHERE `category_id` = :groupId ORDER BY display_order ASC, date DESC")
    fun liveData(groupId: String): LiveData<MutableList<Follow>>

}