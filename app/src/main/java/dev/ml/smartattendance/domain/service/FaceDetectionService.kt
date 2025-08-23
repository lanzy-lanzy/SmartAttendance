package dev.ml.smartattendance.domain.service

import dev.ml.smartattendance.domain.model.biometric.FaceDetectionResult
import dev.ml.smartattendance.domain.model.biometric.LivenessResult

interface FaceDetectionService {
    suspend fun detectFace(imageData: ByteArray): FaceDetectionResult
    suspend fun verifyLiveness(imageData: ByteArray): LivenessResult
}