package com.lff.classschedule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lff.classschedule.database.CourseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSettingsViewModel(
    private val courseDao: CourseDao
) : ViewModel() {

    fun syncSemesterLengthened(oldMax: Int, newMax: Int, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                courseDao.syncSemesterLengthened(oldMax, newMax)
            }
            onResult(count)
        }
    }

    fun syncSemesterShortened(newMax: Int, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                courseDao.syncSemesterShortened(newMax)
            }
            onResult(count)
        }
    }
}
