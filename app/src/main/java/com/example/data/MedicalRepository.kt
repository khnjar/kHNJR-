package com.example.data

import kotlinx.coroutines.flow.Flow

class MedicalRepository(private val medicalDao: MedicalDao) {
    val allPrescriptions: Flow<List<SavedPrescription>> = medicalDao.getAllPrescriptions()
    val allCalculationLogs: Flow<List<CalculationLog>> = medicalDao.getAllCalculationLogs()
    val allBookmarks: Flow<List<BookmarkedTopic>> = medicalDao.getAllBookmarks()

    suspend fun insertPrescription(prescription: SavedPrescription) {
        medicalDao.insertPrescription(prescription)
    }

    suspend fun deletePrescriptionById(id: Int) {
        medicalDao.deletePrescriptionById(id)
    }

    suspend fun insertCalculationLog(log: CalculationLog) {
        medicalDao.insertCalculationLog(log)
    }

    suspend fun deleteCalculationLogById(id: Int) {
        medicalDao.deleteCalculationLogById(id)
    }

    suspend fun insertBookmark(bookmark: BookmarkedTopic) {
        medicalDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(type: String, topicId: String) {
        medicalDao.deleteBookmark(type, topicId)
    }

    fun isBookmarked(type: String, topicId: String): Flow<Boolean> {
        return medicalDao.isBookmarked(type, topicId)
    }

    val allBodySystems: Flow<List<BodySystemEntity>> = medicalDao.getAllBodySystems()

    suspend fun insertBodySystem(system: BodySystemEntity) {
        medicalDao.insertBodySystem(system)
    }

    suspend fun deleteBodySystem(system: BodySystemEntity) {
        medicalDao.deleteBodySystem(system)
    }

    suspend fun getBodySystemById(id: String): BodySystemEntity? {
        return medicalDao.getBodySystemById(id)
    }

    val allQuizResults: Flow<List<QuizResultEntity>> = medicalDao.getAllQuizResults()

    suspend fun insertQuizResult(result: QuizResultEntity) {
        medicalDao.insertQuizResult(result)
    }

    suspend fun deleteQuizResultById(id: Int) {
        medicalDao.deleteQuizResultById(id)
    }
}
