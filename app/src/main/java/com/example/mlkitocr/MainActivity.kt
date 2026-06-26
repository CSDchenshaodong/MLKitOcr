package com.example.mlkitocr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mlkitocr.camera.BitmapCropper
import com.example.mlkitocr.camera.IdCardOverlayView
import com.example.mlkitocr.idcard.ChinaIdNumberValidator
import com.example.mlkitocr.idcard.IdCardFrontParser
import com.example.mlkitocr.idcard.IdCardFrontRecognizer
import com.example.mlkitocr.idcard.RecognitionConfidence
import com.example.mlkitocr.mlkit.MlKitOcrEngine
import com.example.mlkitocr.review.ReviewActivity
import com.example.mlkitocr.util.LocaleHelper
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: IdCardOverlayView
    private lateinit var captureButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var settingsButton: ImageButton

    private val cropper = BitmapCropper()
    private val recognizer by lazy {
        IdCardFrontRecognizer(
            ocrEngine = MlKitOcrEngine(),
            parser = IdCardFrontParser(ChinaIdNumberValidator())
        )
    }

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val uiScope = CoroutineScope(Job() + Dispatchers.Main)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            statusText.text = getString(R.string.capture_permission_required)
            captureButton.isEnabled = false
        }
    }

    private val reviewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        setLoading(false)
        if (result.resultCode == RESULT_CANCELED) {
            statusText.text = getString(R.string.capture_retry_hint)
        } else {
            statusText.text = getString(R.string.review_confidence_high)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        captureButton = findViewById(R.id.captureButton)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        cameraExecutor = Executors.newSingleThreadExecutor()

        settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        captureButton.setOnClickListener {
            captureImage()
        }
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        ensureCameraPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                    captureButton.isEnabled = true
                    statusText.text = ""
                }.onFailure {
                    captureButton.isEnabled = false
                    statusText.text = getString(R.string.capture_no_camera)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun captureImage() {
        val capture = imageCapture ?: run {
            statusText.text = getString(R.string.capture_no_camera)
            return
        }
        setLoading(true)
        statusText.text = getString(R.string.capture_processing)
        val outputFile = File.createTempFile("id-card-", ".jpg", cacheDir)
        capture.takePicture(
            OutputFileOptions.Builder(outputFile).build(),
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                    if (bitmap == null) {
                        runOnUiThread {
                            setLoading(false)
                            statusText.text = getString(R.string.capture_retry_hint)
                        }
                        return
                    }
                    val cropped = cropper.crop(bitmap, overlayView.framingRectFraction)
                    uiScope.launch {
                        runCatching {
                            recognizer.recognize(cropped)
                        }.onSuccess { result ->
                            statusText.text = when (result.confidence) {
                                RecognitionConfidence.HIGH -> ""
                                RecognitionConfidence.PARTIAL -> getString(R.string.capture_low_confidence)
                                RecognitionConfidence.LOW -> result.failureReason ?: getString(R.string.capture_retry_hint)
                            }
                            if (result.confidence == RecognitionConfidence.LOW) {
                                setLoading(false)
                            } else {
                                openReview(result.confidence, result.fields)
                            }
                        }.onFailure {
                            setLoading(false)
                            statusText.text = getString(R.string.capture_retry_hint)
                        }
                        outputFile.delete()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        setLoading(false)
                        statusText.text = getString(R.string.capture_retry_hint)
                    }
                }
            }
        )
    }

    private fun openReview(
        confidence: RecognitionConfidence,
        fields: com.example.mlkitocr.idcard.IdCardFrontFields
    ) {
        val intent = Intent(this, ReviewActivity::class.java).apply {
            putExtra(ReviewActivity.EXTRA_CONFIDENCE, confidence.name)
            putExtra(ReviewActivity.EXTRA_NAME, fields.name.value)
            putExtra(ReviewActivity.EXTRA_GENDER, fields.gender.value)
            putExtra(ReviewActivity.EXTRA_ETHNICITY, fields.ethnicity.value)
            putExtra(ReviewActivity.EXTRA_BIRTH_DATE, fields.birthDate.value)
            putExtra(ReviewActivity.EXTRA_ADDRESS, fields.address.value)
            putExtra(ReviewActivity.EXTRA_ID_NUMBER, fields.idNumber.value)
        }
        reviewLauncher.launch(intent)
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        captureButton.isEnabled = !loading
    }
}
