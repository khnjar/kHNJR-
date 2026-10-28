package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalDao {
    // Prescriptions
    @Query("SELECT * FROM saved_prescriptions ORDER BY createdAt DESC")
    fun getAllPrescriptions(): Flow<List<SavedPrescription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: SavedPrescription)

    @Delete
    suspend fun deletePrescription(prescription: SavedPrescription)

    @Query("DELETE FROM saved_prescriptions WHERE id = :id")
    suspend fun deletePrescriptionById(id: Int)

    // Calculation Logs
    @Query("SELECT * FROM calculation_logs ORDER BY createdAt DESC")
    fun getAllCalculationLogs(): Flow<List<CalculationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculationLog(log: CalculationLog)

    @Delete
    suspend fun deleteCalculationLog(log: CalculationLog)

    @Query("DELETE FROM calculation_logs WHERE id = :id")
    suspend fun deleteCalculationLogById(id: Int)

    // Bookmarked Topics
    @Query("SELECT * FROM bookmarked_topics ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedTopic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedTopic)

    @Query("DELETE FROM bookmarked_topics WHERE topicType = :type AND topicId = :topicId")
    suspend fun deleteBookmark(type: String, topicId: String)

    @Query("SELECT EXISTS(SELECT * FROM bookmarked_topics WHERE topicType = :type AND topicId = :topicId)")
    fun isBookmarked(type: String, topicId: String): Flow<Boolean>

    // Body Systems
    @Query("SELECT * FROM body_systems ORDER BY createdAt DESC")
    fun getAllBodySystems(): Flow<List<BodySystemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodySystem(system: BodySystemEntity)

    @Delete
    suspend fun deleteBodySystem(system: BodySystemEntity)

    @Query("SELECT * FROM body_systems WHERE id = :id LIMIT 1")
    suspend fun getBodySystemById(id: String): BodySystemEntity?

    // Quiz Results
    @Query("SELECT * FROM quiz_results ORDER BY createdAt DESC")
    fun getAllQuizResults(): Flow<List<QuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity)

    @Query("DELETE FROM quiz_results WHERE id = :id")
    suspend fun deleteQuizResultById(id: Int)
}
