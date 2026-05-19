package com.lff.classschedule.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AdjustmentDao {

    @Query("SELECT * FROM adjustments_table WHERE isActive = 1")
    suspend fun getActiveAdjustments(): List<Adjustment>

    @Query("SELECT * FROM adjustments_table ORDER BY id DESC")
    suspend fun getAllAdjustments(): List<Adjustment>

    @Insert
    suspend fun insert(adjustment: Adjustment)

    @Update
    suspend fun update(adjustment: Adjustment)

    @Delete
    suspend fun delete(adjustment: Adjustment)

    @Query("UPDATE adjustments_table SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Int, active: Boolean)
}
