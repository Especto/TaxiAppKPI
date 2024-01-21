package com.example.taxiappkpi.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.taxiappkpi.Models.Filters.RiderFilter
import com.example.taxiappkpi.Models.Filters.UserFilter
import com.example.taxiappkpi.R

class FiltersForDriverActivity : AppCompatActivity() {
    private lateinit var genderRG: RadioGroup
    private lateinit var genderMaleRB: RadioButton
    private lateinit var genderFemaleRB: RadioButton
    private lateinit var genderAnywayRB: RadioButton
    private lateinit var minAgeET: EditText
    private lateinit var maxAgeET: EditText
    private lateinit var resetUserFilterB: Button

    private lateinit var riderFilter: RiderFilter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filters)

        val receivedIntent = intent
        if (receivedIntent != null && receivedIntent.hasExtra("riderFilter")) {
            riderFilter = receivedIntent.getParcelableExtra<RiderFilter>("riderFilter")!!
        }

        genderRG = findViewById(R.id.filGenderGroup)
        genderMaleRB = findViewById(R.id.filMale)
        genderFemaleRB = findViewById(R.id.filFemale)
        genderAnywayRB = findViewById(R.id.filAnywayGender)
        minAgeET = findViewById(R.id.filMinAge)
        maxAgeET = findViewById(R.id.filMaxAge)
        resetUserFilterB = findViewById(R.id.filResetUserBtn)

        findViewById<TextView>(R.id.filUserTitle).text = getString(R.string.filterFor) + getString(R.string.rider)
        findViewById<ConstraintLayout>(R.id.filterCar).isVisible = false
        init()

        findViewById<ImageButton>(R.id.filBackButton).setOnClickListener {
            closeActivity(false)
        }
        findViewById<ImageButton>(R.id.filDoneButton).setOnClickListener {
            updateFilter()
        }


    }

    private fun updateFilter() {
        val gender = when {
            genderMaleRB.isChecked -> 0
            genderFemaleRB.isChecked -> 1
            genderAnywayRB.isChecked -> 2
            else -> -1
        }

        val minAgeText = minAgeET.text.toString()
        val maxAgeText = maxAgeET.text.toString()

        var minAge: Int = -1
        var maxAge: Int = -1

        if(minAgeText.isNotEmpty()){
            minAge = minAgeText.toInt()
            if(minAge < 20){
                Toast.makeText(this@FiltersForDriverActivity, "Поле з мінімальним віком некоректне", Toast.LENGTH_SHORT).show()
                return
            }
        }
        if(maxAgeText.isNotEmpty()){
            maxAge = maxAgeText.toInt()
            if(maxAge > 75){
                Toast.makeText(this@FiltersForDriverActivity, "Поле з максимальним віком некоректне", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (maxAge!=-1){
            if (minAge >= maxAge){
                Toast.makeText(this@FiltersForDriverActivity, "Діапазон віку некоректний", Toast.LENGTH_SHORT).show()
                return
            }
        }

        riderFilter = RiderFilter(UserFilter(gender, minAge, maxAge))
        closeActivity(true)
    }


    private fun init() {
        val riderFilterInfo = riderFilter.userFilter

        when (riderFilterInfo.gender) {
            0 -> { genderMaleRB.isChecked = true
            }
            1 -> { genderFemaleRB.isChecked = true
            }
            2 -> { genderAnywayRB.isChecked = true
            }
            -1 ->{
                genderMaleRB.isChecked = false
                genderAnywayRB.isChecked = false
                genderFemaleRB.isChecked = false
            }
        }

        if(riderFilterInfo.minAge != -1){
            minAgeET.text = SpannableStringBuilder(riderFilterInfo.minAge.toString())
        }
        else{
            minAgeET.text = SpannableStringBuilder("")

        }
        if(riderFilterInfo.maxAge != -1){
            maxAgeET.text = SpannableStringBuilder(riderFilterInfo.maxAge.toString())
        }
        else{
            maxAgeET.text = SpannableStringBuilder("")
        }

        resetUserFilterB.setOnClickListener {
            riderFilter = RiderFilter(UserFilter())
            init()
        }


    }

    private fun closeActivity(code: Boolean) {
        val resultIntent = Intent()
        resultIntent.putExtra("FILTER", riderFilter)
        if(code){
            setResult(Activity.RESULT_OK, resultIntent)
        }
        else{
            setResult(Activity.RESULT_CANCELED, resultIntent)
        }
        finish()
    }

}