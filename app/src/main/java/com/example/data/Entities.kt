package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "saved_prescriptions")
data class SavedPrescription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val condition: String,
    val regimen: String,
    val duration: String,
    val monitoring: String,
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "calculation_logs")
data class CalculationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientName: String,
    val modelUsed: String, // "Framingham" or "CHA₂DS₂-VASc" or "eGFR"
    val inputsSummary: String, // Short summary of inputs like "M, 54, SBP: 140, Chol: 200, Smoker"
    val calculatedResult: String, // e.g., "12.5% (Medium Risk)" or "Stage 3 (eGFR: 45)"
    val scoreText: String, // Raw score or index
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "bookmarked_topics")
data class BookmarkedTopic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicType: String, // "system" or "solidarity" or "prediction"
    val topicId: String, // system id (e.g. "circulatory") or model name
    val topicTitle: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "body_systems")
data class BodySystemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latinName: String,
    val icon: String,
    val scientificDescription: String,
    val relevantHadith: String,
    val mechanism: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val systemId: String,
    val systemTitle: String,
    val score: Int,
    val totalQuestions: Int,
    val percentage: Int,
    val feedback: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

