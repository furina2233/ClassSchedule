package com.lff.classschedule.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lff.classschedule.database.Adjustment
import com.lff.classschedule.database.AdjustmentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdjustmentsViewModel(
    private val adjustmentDao: AdjustmentDao
) : ViewModel() {

    private val _adjustments = MutableLiveData<List<Adjustment>>()
    val adjustments: LiveData<List<Adjustment>> = _adjustments

    fun refreshAdjustments() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { adjustmentDao.getAllAdjustments() }
            _adjustments.postValue(result)
        }
    }

    fun addAdjustment(adjustment: Adjustment) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { adjustmentDao.insert(adjustment) }
            refreshAdjustments()
        }
    }

    fun deleteAdjustment(adjustment: Adjustment) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { adjustmentDao.delete(adjustment) }
            refreshAdjustments()
        }
    }

    fun setAdjustmentActive(id: Int, active: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { adjustmentDao.setActive(id, active) }
            refreshAdjustments()
        }
    }
}
