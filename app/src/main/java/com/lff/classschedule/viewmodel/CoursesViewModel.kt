package com.lff.classschedule.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lff.classschedule.database.CourseDao
import com.lff.classschedule.database.CourseMapper.toCourse
import com.lff.classschedule.database.CourseMapper.toEntity
import com.lff.classschedule.pojo.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoursesViewModel(
    private val courseDao: CourseDao
) : ViewModel() {

    private val _courses = MutableLiveData<Map<Int, Course>>()
    val courses: LiveData<Map<Int, Course>> = _courses

    fun refreshCourses() {
        viewModelScope.launch {
            val entities = withContext(Dispatchers.IO) { courseDao.getAllCourses() }
            _courses.postValue(entities.associate { it.id to it.toCourse() })
        }
    }

    fun addCourse(course: Course) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { courseDao.insert(course.toEntity()) }
            refreshCourses()
        }
    }

    fun addCourses(courseList: List<Course>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { courseDao.insertAll(courseList.map { it.toEntity() }) }
            refreshCourses()
        }
    }

    fun updateCourse(courseId: Int, course: Course) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                courseDao.update(course.toEntity().copy(id = courseId))
            }
            refreshCourses()
        }
    }

    fun deleteCourse(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { courseDao.deleteById(id) }
            refreshCourses()
        }
    }

    fun deleteCourses(ids: List<Int>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { courseDao.deleteByIds(ids) }
            refreshCourses()
        }
    }
}
