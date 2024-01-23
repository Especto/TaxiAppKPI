package com.example.taxiappkpi.registration

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.taxiappkpi.References
import com.example.taxiappkpi.Models.User.Birthdate
import com.example.taxiappkpi.Models.User.CarInfo
import com.example.taxiappkpi.Models.User.DriverInfo
import com.example.taxiappkpi.Models.User.RiderInfo
import com.example.taxiappkpi.Models.User.UserInfo

import com.example.taxiappkpi.maps.DriverMapsActivity

import com.example.taxiappkpi.R
import com.example.taxiappkpi.maps.RiderMapsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDate
import java.time.Period
import java.util.Calendar
import java.util.Locale

class RegistrationActivity : AppCompatActivity() {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var mAuth: FirebaseAuth
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var imgCar: ImageView
    private var imgCarUri: String = ""
    private lateinit var carBrandSP: Spinner
    private lateinit var carModelSP: Spinner
    private lateinit var role: String
    private lateinit var driverLicense: EditText

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        mAuth = FirebaseAuth.getInstance()
        role = intent.getStringExtra("ROLE")!!

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        init()

    }

    private fun init() {
        if (role == "driver") {
            carBrandSP = findViewById(R.id.regCarBrandSpinner)
            carModelSP = findViewById(R.id.regCarModelSpinner)
            imgCar = findViewById(R.id.regCarPhoto)
            driverLicense = findViewById(R.id.regDriverLicense)
            driverLicense.visibility = View.VISIBLE

            carBrandSP.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, position: Int, id: Long) {
                    val selectedBrand = parentView.getItemAtPosition(position) as String
                    updateCarModels(selectedBrand, carModelSP)
                }

                override fun onNothingSelected(parentView: AdapterView<*>) {
                }
            }

            imgCar.setOnClickListener {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT

                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
            }
        } else {
            findViewById<LinearLayout>(R.id.regCarLayout).visibility = View.GONE
        }

        val registerButton: Button = findViewById(R.id.regButton)
        registerButton.setOnClickListener {
            val firstName = findViewById<EditText>(R.id.regFirstName).text.toString()
            val phoneNumber = findViewById<EditText>(R.id.regPhoneNumber).text.toString()
            val birthdateStr = findViewById<EditText>(R.id.regBirthdate).text.toString()
            val gender = findViewById<RadioButton>(findViewById<RadioGroup>(R.id.regGenderGroup).checkedRadioButtonId)?.text.toString()

            if (firstName.isEmpty() || phoneNumber.isEmpty() || birthdateStr.isEmpty() || gender.isEmpty()
            ) {
                Toast.makeText(this@RegistrationActivity, "Заповніть усі поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val birthdate = Birthdate.strToBirthdate(birthdateStr)

            if (birthdate == null) {
                Toast.makeText(this@RegistrationActivity, "Неправильний формат дати", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role == "driver") {
                val carEngType = findViewById<RadioButton>(findViewById<RadioGroup>(R.id.regEngType).checkedRadioButtonId)?.text.toString()
                val carBrandSelected = carBrandSP.selectedItem.toString()
                val carModelSelected = carModelSP.selectedItem.toString()
                val carBody = findViewById<Spinner>(R.id.regCarBodySpinner).selectedItem.toString()
                val carColor = findViewById<Spinner>(R.id.regCarColorSpinner).selectedItem.toString()
                val carNumber = findViewById<EditText>(R.id.regCarNumber).text.toString()
                val driverLicenseText = driverLicense.text.toString()


                if(carBrandSelected.isEmpty() || carModelSelected.isEmpty() || carColor.isEmpty() || carNumber.isEmpty() || driverLicenseText.isEmpty()){
                    Toast.makeText(this@RegistrationActivity, "Заповніть усі поля", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (imgCarUri.isEmpty()){
                    Toast.makeText(this@RegistrationActivity, "Завантажте фото вашого авто", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val birthday = Calendar.getInstance()
                birthday.set(birthdate.year, birthdate.month, birthdate.day)
                val age = Calendar.getInstance().get(Calendar.YEAR) - birthday.get(Calendar.YEAR)

                if (age < 20) {
                    Toast.makeText(this@RegistrationActivity, "Водію повинно бути більше 20 років", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }


                registerDriver(firstName, phoneNumber, birthdate, gender, carEngType, carBrandSelected, carModelSelected, carBody, carColor, carNumber, driverLicenseText)
            } else {
                registerRider(firstName, phoneNumber, birthdate, gender)
            }
        }
    }


    private fun registerDriver(firstName: String, phoneNumber: String, birthdate: Birthdate, gender: String, carEngTypeStr: String,
                               carBrand: String, carModel: String, carBody: String, carColor: String, carNumber: String, driverLicense: String) {


        val genderInt = genderStringToInt(gender)
        val carEngTypeInt = engTypeStringToInt(carEngTypeStr)

        val carBodyIndex = resources.getStringArray(R.array.car_body).indexOf(carBody)
        val carColorIndex = resources.getStringArray(R.array.car_color).indexOf(carColor)

        val userInfo = UserInfo(
            firstName = firstName,
            phoneNumber = phoneNumber,
            gender = genderInt,
            birthdate = birthdate
        )

        val carInfo = CarInfo(
            engType = carEngTypeInt,
            brand = carBrand,
            model = carModel,
            bodyType = carBodyIndex,
            color = carColorIndex,
            number = carNumber,
            photo = imgCarUri
        )

        val driverInfo = DriverInfo(userInfo, carInfo, driverLicense)
        val driverID = mAuth.currentUser!!.uid
        FirebaseDatabase.getInstance().reference.child(References.DRIVERS_REFERENCE).child(driverID).updateChildren(driverInfo.toMap()).addOnSuccessListener {
            Toast.makeText(this@RegistrationActivity, "Реєстрація закінчена", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@RegistrationActivity, DriverMapsActivity::class.java))
        }

    }


    private fun registerRider(firstName: String, phoneNumber: String, birthdate: Birthdate, gender: String) {
        val genderInt = genderStringToInt(gender)

        val userInfo = UserInfo(
            firstName = firstName,
            phoneNumber = phoneNumber,
            gender = genderInt,
            birthdate = birthdate
        )

        val riderInfo = RiderInfo(userInfo)
        val riderID = mAuth.currentUser!!.uid
        FirebaseDatabase.getInstance().reference.child(References.RIDERS_REFERENCE).child(riderID).updateChildren(riderInfo.toMap()).addOnSuccessListener {
            Toast.makeText(this@RegistrationActivity, "Реєстрація закінчена", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@RegistrationActivity, RiderMapsActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            Toast.makeText(this@RegistrationActivity, "Зберігаємо фото, зачекайте", Toast.LENGTH_SHORT).show()
            if(data != null && data.data != null) {
                val imageUri = data.data!!
                imgCar.setImageURI(imageUri)

                val carFolder = FirebaseStorage.getInstance().reference.child("Photos/Cars/" + FirebaseAuth.getInstance().currentUser!!.uid)
                carFolder.putFile(imageUri)
                    .addOnFailureListener { e ->
                        Toast.makeText(this@RegistrationActivity, e.message!!, Toast.LENGTH_SHORT).show()
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            carFolder.downloadUrl.addOnSuccessListener { uri ->
                                imgCarUri = uri.toString()
                                Toast.makeText(this@RegistrationActivity, "Фото збережено!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
        }
    }

    private fun genderStringToInt(gender: String): Int {
        return when (gender) {
            "Чоловік" -> 0
            "Жінка" -> 1
            else -> throw IllegalArgumentException("$gender")
        }
    }

    private fun engTypeStringToInt(type: String): Int {
        return when (type) {
            "ДВС" -> 0
            "Електрокар" -> 1
            else -> throw IllegalArgumentException("$type")
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun updateCarModels(selectedBrand: String, carModelSP: Spinner) {
        val modelsArrayName = selectedBrand.lowercase(Locale.ROOT).replace(" ", "").replace(" ", "") + "_models"

        val arrayResourceId = resources.getIdentifier(modelsArrayName, "array", packageName)
        if (arrayResourceId != 0) {
            val modelsArray = resources.getStringArray(arrayResourceId)

            val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelsArray)
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            carModelSP.adapter = arrayAdapter
        } else {
            Toast.makeText(this, "Моделі не знайдені, оберіть 'Інше' в марці авто", Toast.LENGTH_SHORT).show()
        }
    }
}