package dev.ml.smartattendance.data.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dev.ml.smartattendance.domain.model.biometric.FaceDetectionResult
import dev.ml.smartattendance.domain.model.biometric.LivenessResult
import dev.ml.smartattendance.domain.service.FaceDetectionService
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class FaceDetectionServiceImpl @Inject constructor() : FaceDetectionService {
    
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )
    
    override suspend fun detectFace(imageData: ByteArray): FaceDetectionResult = 
        suspendCancellableCoroutine { continuation ->
            try {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                val image = InputImage.fromBitmap(bitmap, 0)
                
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        when {
                            faces.isEmpty() -> {
                                continuation.resume(FaceDetectionResult.NoFaceDetected)
                            }
                            faces.size > 1 -> {
                                continuation.resume(FaceDetectionResult.MultipleFacesDetected)
                            }
                            else -> {
                                // Return the original image data for successful single face detection
                                continuation.resume(FaceDetectionResult.Success(imageData))
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(FaceDetectionResult.Error(exception.message ?: "Unknown error"))
                    }
            } catch (e: Exception) {
                continuation.resume(FaceDetectionResult.Error(e.message ?: "Failed to process image"))
            }
        }
    
    override suspend fun verifyLiveness(imageData: ByteArray): LivenessResult = 
        suspendCancellableCoroutine { continuation ->
            try {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                val image = InputImage.fromBitmap(bitmap, 0)
                
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isEmpty()) {
                            continuation.resume(LivenessResult.Error("No face detected"))
                            return@addOnSuccessListener
                        }
                        
                        val face = faces.first()
                        
                        // Basic liveness detection based on eye open probability and smile probability
                        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 0f
                        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 0f
                        val smileProb = face.smilingProbability ?: 0f
                        
                        // Simple heuristic: both eyes should be reasonably open
                        val isLive = leftEyeOpenProb > 0.1f && rightEyeOpenProb > 0.1f
                        
                        if (isLive) {
                            continuation.resume(LivenessResult.Live)
                        } else {
                            continuation.resume(LivenessResult.NotLive)
                        }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(LivenessResult.Error(exception.message ?: "Liveness detection failed"))
                    }
            } catch (e: Exception) {
                continuation.resume(LivenessResult.Error(e.message ?: "Failed to verify liveness"))
            }
        }
}