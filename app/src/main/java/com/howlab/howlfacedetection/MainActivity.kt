package com.howlab.howlfacedetection

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),0)
        setContentView(R.layout.activity_main)
        bindCameraUseCases()
    }

    fun bindCameraUseCases() {
        val rotation = 0

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(rotation)
                .build()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(120, 160))
                .setTargetRotation(rotation)
                .build()

            val luminosityAnalyzer = LuminosityAnalyzer(
                fireFaceOverlay,
                BitmapFactory.decodeResource(resources, R.drawable.clown_nose)
            )
            imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), luminosityAnalyzer)

            cameraProvider.unbindAll()


            val camera =
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview.setSurfaceProvider(viewFinder.createSurfaceProvider(camera.cameraInfo))
        }, ContextCompat.getMainExecutor(this))
    }
}

class LuminosityAnalyzer(var graphicOverlay: GraphicOverlay, var overlayBitmap: Bitmap) :
    ImageAnalysis.Analyzer {

    val options = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
        .build()

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val metadata = FirebaseVisionImageMetadata.Builder()
            .setWidth(image.width)
            .setHeight(image.height)
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(FirebaseVisionImageMetadata.ROTATION_270)
            .build()

        val bufferImage = FirebaseVisionImage.fromByteBuffer(buffer, metadata)


        FirebaseVision.getInstance()
            .getVisionFaceDetector(options)
            .detectInImage(bufferImage)
            .addOnSuccessListener { faces ->
                graphicOverlay.clear()

                for (i in 0 until faces.size) {
                    var face = faces[i]
                    val faceGraphic = FaceGraphic(
                        graphicOverlay,
                        face,
                        CameraSource.CAMERA_FACING_FRONT,
                        overlayBitmap,
                        image
                    )
                    graphicOverlay.add(faceGraphic)
                }
                graphicOverlay.postInvalidate()


                image.close()
            }
    }

}
