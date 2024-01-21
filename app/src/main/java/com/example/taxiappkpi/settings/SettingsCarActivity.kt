package com.example.taxiappkpi.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.core.net.toUri
import com.example.taxiappkpi.Common
import com.example.taxiappkpi.Models.User.CarInfo
import com.example.taxiappkpi.Models.User.toCarInfo
import com.example.taxiappkpi.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.Locale

class SettingsCarActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var driverID: String

    private lateinit var edtEngTypeRG: RadioGroup
    private lateinit var edtEngERB: RadioButton
    private lateinit var edtEngIceRB: RadioButton
    private lateinit var edtCarBrand: Spinner
    private lateinit var edtCarModel: Spinner
    private lateinit var edtCarBody: Spinner
    private lateinit var edtCarColor: Spinner
    private lateinit var edtCarNumber: EditText

    private lateinit var backBtn: ImageButton
    private lateinit var doneBtn: ImageButton

    private lateinit var carInfo: CarInfo

    private lateinit var imgView: ImageView
    private var imgUri: Uri = "".toUri()
    private val PICK_IMAGE_REQUEST = 1

    private var programSelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_car)

        mAuth = FirebaseAuth.getInstance()
        driverID = mAuth.currentUser!!.uid

        edtEngTypeRG = findViewById(R.id.edtEngType)
        edtEngERB = findViewById(R.id.edtEngE)
        edtEngIceRB = findViewById(R.id.edtEngICE)
        edtCarBrand = findViewById(R.id.edtCarBrandSpinner)
        edtCarModel = findViewById(R.id.edtCarModelSpinner)
        edtCarBody = findViewById(R.id.edtCarBodySpinner)
        edtCarColor = findViewById(R.id.edtCarColorSpinner)
        edtCarNumber = findViewById(R.id.edtCarNumber)

        backBtn = findViewById(R.id.edtbackButtonCar)
        doneBtn= findViewById(R.id.edtdoneButtonCar)

        imgView = findViewById(R.id.edtCarImg)

        edtCarBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, position: Int, id: Long) {
                if(!programSelection){
                    val selectedBrand = parentView.getItemAtPosition(position) as String
                    updateCarModels(selectedBrand)
                }
                programSelection = false

            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
            }
        }

        doneBtn.setOnClickListener {
            updateCarInfo()
        }
        backBtn.setOnClickListener {
            finish()
        }

        imgView.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT

            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }

        init()
    }


    private fun init() {
        FirebaseDatabase.getInstance().reference.child(Common.DRIVERS_REFERENCE).child(driverID).child("carInfo").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val carInfoMap = snapshot.value!! as Map<*, *>
                    val carInfo = carInfoMap.toCarInfo()

                    when (carInfo.engType) {
                        0 -> { edtEngIceRB.isChecked = true
                        }
                        1 -> { edtEngERB.isChecked = true
                        }
                    }

                    initializeBrandSpinner(carInfo.brand)
                    initializeModelSpinner(carInfo.brand, carInfo.model)
                    initializeBodySpinner(carInfo.bodyType)
                    initializeColorSpinner(carInfo.color)
                    edtCarNumber.text = SpannableStringBuilder(carInfo.number)

                    if(carInfo.photo!=""){
                        val uriStr = carInfo.photo

                        Picasso.get().load(uriStr).into(imgView)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

    }


    private fun updateCarInfo() {
        val carEngTypeStr = findViewById<RadioButton>(edtEngTypeRG.checkedRadioButtonId)?.text.toString()
        val carBrand = edtCarBrand.selectedItem.toString()
        val carModel = edtCarModel.selectedItem.toString()
        val carBody = edtCarBody.selectedItem.toString()
        val carColor = edtCarColor.selectedItem.toString()
        val carNumber = edtCarNumber.text.toString()

        if (carBrand.isEmpty() || carModel.isEmpty() || carBody.isEmpty() || carColor.isEmpty() || carNumber.isEmpty()
        ) {
            Toast.makeText(this@SettingsCarActivity, "Заповніть усі поля", Toast.LENGTH_SHORT).show()
            return
        }

        val carNumberPattern = Regex("[A-Z]{2}\\d{4}[A-Z]{2}")
        if (!carNumberPattern.matches(carNumber)) {
            Toast.makeText(this@SettingsCarActivity, "Номер авто некоректний", Toast.LENGTH_SHORT).show()
            return
        }

        val carEngTypInt = engTypeStringToInt(carEngTypeStr)

        val carBodyIndex = resources.getStringArray(R.array.car_body).indexOf(carBody)
        val carColorIndex = resources.getStringArray(R.array.car_color).indexOf(carColor)

        carInfo = CarInfo(
            engType = carEngTypInt,
            brand = carBrand,
            model = carModel,
            bodyType = carBodyIndex,
            color = carColorIndex,
            number= carNumber,
            photo = imgUri.toString()
        )
        FirebaseDatabase.getInstance().reference.child(Common.DRIVERS_REFERENCE).child(driverID).child("carInfo").updateChildren(carInfo.toMap()).addOnSuccessListener {
            Toast.makeText(this@SettingsCarActivity, "Дані оновились", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            Toast.makeText(this@SettingsCarActivity, "Зберігаємо фото", Toast.LENGTH_SHORT).show()
            if(data != null && data.data != null) {
                val imageUri = data.data!!
                imgView.setImageURI(imageUri)
                val carFolder = FirebaseStorage.getInstance().reference.child("Photos/Cars/" + FirebaseAuth.getInstance().currentUser!!.uid)
                carFolder.putFile(imageUri)
                    .addOnFailureListener { e ->
                        Toast.makeText(this@SettingsCarActivity, e.message!!, Toast.LENGTH_SHORT).show()
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            carFolder.downloadUrl.addOnSuccessListener { uri ->
                                imgUri = uri
                                Toast.makeText(this@SettingsCarActivity, "Фото збережено!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
        }
    }

    private fun engTypeStringToInt(type: String): Int {
        return when (type) {
            "ДВС" -> 0
            "Електрокар" -> 1
            else -> throw IllegalArgumentException("$type")
        }
    }

    private fun initializeBrandSpinner(selectedBrand: String) {
        val brandsArray = resources.getStringArray(R.array.car_brand)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, brandsArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtCarBrand.adapter = adapter
        val position = brandsArray.indexOf(selectedBrand)
        edtCarBrand.setSelection(if (position != -1) position else 0)
    }

    @SuppressLint("DiscouragedApi")
    private fun initializeModelSpinner(selectedBrand: String, selectedModel: String) {
        updateCarModels(selectedBrand)
        programSelection = true
        val modelsArrayName = selectedBrand.lowercase(Locale.ROOT).replace(" ", "").replace(" ", "")+"_models"
        val arrayResourceId = resources.getIdentifier(modelsArrayName, "array", packageName)
        if (arrayResourceId != 0) {
            val modelsArray = resources.getStringArray(arrayResourceId)
            Log.d("check_log_initializeModelSpinner", modelsArray.toString())
            val selectedModelIndex = modelsArray.indexOf(selectedModel)
            Log.d("check_log_initializeModelSpinner", selectedModelIndex.toString())
            edtCarModel.setSelection(if (selectedModelIndex != -1) selectedModelIndex else 0)
        } else {
            Toast.makeText(this, "Моделі не знайдені для марки $selectedBrand", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeBodySpinner(selectedBody: Int) {
        val bodyTypesArray = resources.getStringArray(R.array.car_body)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bodyTypesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtCarBody.adapter = adapter
        edtCarBody.setSelection(selectedBody)
    }

    private fun initializeColorSpinner(selectedColor: Int) {
        val colorsArray = resources.getStringArray(R.array.car_color)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colorsArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        edtCarColor.adapter = adapter
        edtCarColor.setSelection(selectedColor)
    }

    @SuppressLint("DiscouragedApi")
    private fun updateCarModels(selectedBrand: String) {
        val modelsArrayName = selectedBrand.lowercase(Locale.ROOT).replace(" ", "").replace(" ", "") + "_models"

        val arrayResourceId = resources.getIdentifier(modelsArrayName, "array", packageName)
        if (arrayResourceId != 0) {
            val modelsArray = resources.getStringArray(arrayResourceId)
            Log.d("check_log_updateCarModels", modelsArray.toString())

            val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelsArray)
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            edtCarModel.adapter = arrayAdapter
        } else {
            Toast.makeText(this, "Моделі не знайдені, оберіть 'Інше' в марці авто", Toast.LENGTH_SHORT).show()
        }
    }

}