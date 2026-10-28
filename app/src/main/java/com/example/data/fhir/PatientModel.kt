package com.example.data.fhir

import com.example.data.PatientModel as CorePatientModel
import com.example.data.PatientObservations as CorePatientObservations
import com.example.data.HeartRateObservation as CoreHeartRateObservation
import com.example.data.OxygenSaturationObservation as CoreOxygenSaturationObservation
import com.example.data.BloodPressureObservation as CoreBloodPressureObservation

/**
 * Re-exports and typealiases for PatientModel in the com.example.data.fhir package
 * to provide maximum compatibility across the application's FHIR layers.
 */
typealias PatientModel = CorePatientModel
typealias PatientObservations = CorePatientObservations
typealias HeartRateObservation = CoreHeartRateObservation
typealias OxygenSaturationObservation = CoreOxygenSaturationObservation
typealias BloodPressureObservation = CoreBloodPressureObservation
