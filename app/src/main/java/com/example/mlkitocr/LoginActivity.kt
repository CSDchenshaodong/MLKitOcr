package com.example.mlkitocr

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import com.example.mlkitocr.util.LocaleHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class LoginActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var errorText: TextView
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
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        avatarManager = AvatarManager(this)

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        errorText = findViewById(R.id.errorText)
        avatarView = findViewById(R.id.loginAvatar)

        loadAvatar()

        avatarView.setOnClickListener { showAvatarPickerDialog() }

        loginButton.setOnClickListener {
            performLogin()
        }
    }

    private fun loadAvatar() {
        val bitmap = avatarManager.load()
        if (bitmap != null) {
            avatarView.setImageBitmap(bitmap)
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

    private fun performLogin() {
        val username = usernameInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString().orEmpty()

        if (username == "admin" && password == "123456") {
            errorText.visibility = TextView.GONE
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            errorText.text = getString(R.string.login_error)
            errorText.visibility = TextView.VISIBLE
        }
    }
}
