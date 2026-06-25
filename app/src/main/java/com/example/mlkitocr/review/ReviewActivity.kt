package com.example.mlkitocr.review

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mlkitocr.R
import com.example.mlkitocr.idcard.RecognitionConfidence
import com.google.android.material.button.MaterialButton

class ReviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_review)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reviewRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val confidence = RecognitionConfidence.valueOf(
            intent.getStringExtra(EXTRA_CONFIDENCE) ?: RecognitionConfidence.LOW.name
        )
        findViewById<TextView>(R.id.confidenceSummary).setText(
            when (confidence) {
                RecognitionConfidence.HIGH -> R.string.review_confidence_high
                RecognitionConfidence.PARTIAL -> R.string.review_confidence_partial
                RecognitionConfidence.LOW -> R.string.review_confidence_low
            }
        )

        bindField(R.id.nameInput, EXTRA_NAME)
        bindField(R.id.genderInput, EXTRA_GENDER)
        bindField(R.id.ethnicityInput, EXTRA_ETHNICITY)
        bindField(R.id.birthDateInput, EXTRA_BIRTH_DATE)
        bindField(R.id.addressInput, EXTRA_ADDRESS)
        bindField(R.id.idNumberInput, EXTRA_ID_NUMBER)

        findViewById<MaterialButton>(R.id.confirmButton).setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
        findViewById<MaterialButton>(R.id.retakeButton).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun bindField(viewId: Int, extraKey: String) {
        findViewById<EditText>(viewId).setText(intent.getStringExtra(extraKey).orEmpty())
    }

    companion object {
        const val EXTRA_CONFIDENCE = "extra_confidence"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_GENDER = "extra_gender"
        const val EXTRA_ETHNICITY = "extra_ethnicity"
        const val EXTRA_BIRTH_DATE = "extra_birth_date"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_ID_NUMBER = "extra_id_number"
    }
}
