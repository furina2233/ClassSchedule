package com.lff.classschedule.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "adjustments_table")
data class Adjustment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val originalDayOfWeek: Int,
    val originalStartLesson: Int,
    val originalEndLesson: Int,
    val newDayOfWeek: Int,
    val newStartLesson: Int,
    val newEndLesson: Int,
    val newDate: String? = null,
    val isActive: Boolean = true
)
