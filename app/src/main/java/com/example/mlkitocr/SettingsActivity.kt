package com.example.mlkitocr

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mlkitocr.util.AvatarManager
import com.google.android.material.imageview.ShapeableImageView
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var avatarView: ShapeableImageView
    private lateinit var avatarManager: AvatarManager
    private var cameraPhotoUri: Uri? = null

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraPhotoUri?.let { uri ->
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                if (bitmap != null) {
                    avatarManager.save(bitmap)
                    avatarView.setImageBitmap(bitmap)
                }
            }
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(it))
            if (bitmap != null) {
                avatarManager.save(bitmap)
                avatarView.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        avatarManager = AvatarManager(this)

        avatarView = findViewById(R.id.settingsAvatar)

        loadAvatar()
        setupVersionInfo()

        avatarView.setOnClickListener { showAvatarPickerDialog() }
    }

    private fun loadAvatar() {
        val bitmap = avatarManager.load()
        if (bitmap != null) {
            avatarView.setImageBitmap(bitmap)
        }
    }

    private fun setupVersionInfo() {
        val versionText = findViewById<TextView>(R.id.versionInfo)
        try {
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = getString(R.string.settings_version, pkgInfo.versionName ?: "1.0")
        } catch (e: PackageManager.NameNotFoundException) {
            versionText.text = getString(R.string.settings_version, "1.0")
        }
    }

    private fun showAvatarPickerDialog() {
        val options = arrayOf(getString(R.string.avatar_take_photo), getString(R.string.avatar_choose_from_gallery))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.avatar_change_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickPhoto()
                }
            }
            .show()
    }

    private fun takePhoto() {
        val photoFile = File(cacheDir, "camera_avatar.jpg")
        cameraPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        takePhotoLauncher.launch(cameraPhotoUri!!)
    }

    private fun pickPhoto() {
        pickPhotoLauncher.launch("image/*")
    }
}
