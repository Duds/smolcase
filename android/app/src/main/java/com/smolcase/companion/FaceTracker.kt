package com.smolcase.companion

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

/**
 * Front-camera face tracking via CameraX ImageAnalysis + ML Kit.
 * No preview is rendered — the screen is the creature's face, not a viewfinder.
 *
 * Reports the largest detected face as normalized coordinates (-1..1),
 * mirrored so the eyes track intuitively (you move left, eyes move left).
 */
class FaceTracker(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onFace: (nx: Float, ny: Float) -> Unit
) {
    private val analyzerExecutor = Executors.newSingleThreadExecutor()

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.1f)
            .build()
    )

    fun start() {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(
                    mediaImage, imageProxy.imageInfo.rotationDegrees
                )
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        // Largest face = the person at the desk
                        val face = faces.maxByOrNull {
                            it.boundingBox.width() * it.boundingBox.height()
                        }
                        if (face != null) {
                            val iw = image.width.toFloat()
                            val ih = image.height.toFloat()
                            // Map to -1..1, mirror X for the front camera
                            val nx = -( (face.boundingBox.centerX() / iw) * 2f - 1f )
                            val ny = (face.boundingBox.centerY() / ih) * 2f - 1f
                            onFace(nx, ny)
                        }
                        // No face: EyesView times out into DROWSY/SLEEPING on its own
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                analysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        detector.close()
        analyzerExecutor.shutdown()
    }
}
