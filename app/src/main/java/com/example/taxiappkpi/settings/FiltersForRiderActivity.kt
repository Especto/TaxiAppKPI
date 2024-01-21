package com.example.taxiappkpi.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.MultiAutoCompleteTextView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.taxiappkpi.Models.Filters.CarFilter
import com.example.taxiappkpi.Models.Filters.DriverFilter
import com.example.taxiappkpi.Models.Filters.UserFilter
import com.example.taxiappkpi.R

class FiltersForRiderActivity : AppCompatActivity() {
    private lateinit var genderRG: RadioGroup
    private lateinit var genderMaleRB: RadioButton
    private lateinit var genderFemaleRB: RadioButton
    private lateinit var genderAnywayRB: RadioButton
    private lateinit var minAgeET: EditText
    private lateinit var maxAgeET: EditText
    private lateinit var resetUserFilterB: Button

    private lateinit var engTypeRG: RadioGroup
    private lateinit var engIceRB: RadioButton
    private lateinit var engERB: RadioButton
    private lateinit var engAnywayRB: RadioButton
    private lateinit var carBodyType: Spinner
    private lateinit var resetCarFilterB: Button

    private lateinit var driverFilter: DriverFilter
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filters)

        val receivedIntent = intent
        if (receivedIntent != null && receivedIntent.hasExtra("driverFilter")) {
            driverFilter = receivedIntent.getParcelableExtra<DriverFilter>("driverFilter")!!
        }

        genderRG = findViewById(R.id.filGenderGroup)
        genderMaleRB = findViewById(R.id.filMale)
        genderFemaleRB = findViewById(R.id.filFemale)
        genderAnywayRB = findViewById(R.id.filAnywayGender)
        minAgeET = findViewById(R.id.filMinAge)
        maxAgeET = findViewById(R.id.filMaxAge)
        resetUserFilterB = findViewById(R.id.filResetUserBtn)

        engTypeRG = findViewById(R.id.filEngTypeGroup)
        engIceRB = findViewById(R.id.filIceType)
        engERB = findViewById(R.id.filEType)
        engAnywayRB = findViewById(R.id.filAnywayType)
        carBodyType = findViewById(R.id.filCarBodySpinner)
        resetCarFilterB = findViewById(R.id.filResetCarBtn)

        findViewById<TextView>(R.id.filUserTitle).text = getString(R.string.filterFor) + getString(R.string.driver)
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
                Toast.makeText(this@FiltersForRiderActivity, "Поле з мінімальним віком некоректне", Toast.LENGTH_SHORT).show()
                return
            }
        }
        if(maxAgeText.isNotEmpty()){
            maxAge = maxAgeText.toInt()
            if(maxAge > 75){
                Toast.makeText(this@FiltersForRiderActivity, "Поле з максимальним віком некоректне", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (maxAge!=-1){
            if (minAge >= maxAge){
                Toast.makeText(this@FiltersForRiderActivity, "Діапазон віку некоректний", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val engType = when {
            engIceRB.isChecked -> 0
            engERB.isChecked-> 1
            engAnywayRB.isChecked -> 2
            else -> -1
        }
        val carBody = carBodyType.selectedItem.toString()
        val carBodyIndex = resources.getStringArray(R.array.car_body).indexOf(carBody)

        driverFilter = DriverFilter(UserFilter(gender, minAge, maxAge), CarFilter(engType, carBodyIndex))

        closeActivity(true)
    }

    private fun init() {
        val driverFilterInfo = driverFilter.userFilter
        val carFilterInfo = driverFilter.carFilter

        when (driverFilterInfo.gender) {
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

        if(driverFilterInfo.minAge != -1){
            minAgeET.text = SpannableStringBuilder(driverFilterInfo.minAge.toString())
        }
        else{
            minAgeET.text = SpannableStringBuilder("")

        }
        if(driverFilterInfo.maxAge != -1){
            maxAgeET.text = SpannableStringBuilder(driverFilterInfo.maxAge.toString())
        }
        else{
            maxAgeET.text = SpannableStringBuilder("")
        }

        when (carFilterInfo.engType) {
            0 -> { engIceRB.isChecked = true
            }
            1 -> { engERB.isChecked = true
            }
            2 -> { engAnywayRB.isChecked = true
            }
            -1 ->{
                engIceRB.isChecked = false
                engAnywayRB.isChecked = false
                engERB.isChecked = false
            }
        }

        if(carFilterInfo.bodyType != -1){
            initializeBodySpinner(carFilterInfo.bodyType)
        }
        else{
            initializeBodySpinner(0)
        }


        resetUserFilterB.setOnClickListener {
            driverFilter = DriverFilter(UserFilter(), driverFilter.carFilter)
            Log.d("log_check", "resetUser${driverFilter}")
            init()
        }

        resetCarFilterB.setOnClickListener {
            driverFilter = DriverFilter(driverFilter.userFilter, CarFilter())
            init()
        }
    }

    private fun closeActivity(code: Boolean) {
        val resultIntent = Intent()
        resultIntent.putExtra("FILTER", driverFilter)
        if(code){
            setResult(Activity.RESULT_OK, resultIntent)
        }
        else{
            setResult(Activity.RESULT_CANCELED, resultIntent)
        }

        finish()
    }

    private fun initializeBodySpinner(selectedBody: Int) {
        val bodyTypesArray = resources.getStringArray(R.array.car_body)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bodyTypesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        carBodyType.adapter = adapter
        carBodyType.setSelection(selectedBody)
    }
}