package com.example.taxiappkpi.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import com.example.taxiappkpi.Common
import com.example.taxiappkpi.Models.User.Birthdate
import com.example.taxiappkpi.Models.User.DriverInfo
import com.example.taxiappkpi.Models.User.RiderInfo
import com.example.taxiappkpi.Models.User.toBirthdate
import com.example.taxiappkpi.Models.User.toCarInfo
import com.example.taxiappkpi.Models.User.toDriverInfo
import com.example.taxiappkpi.Models.User.toRiderInfo
import com.example.taxiappkpi.Models.User.toUserInfo

import com.example.taxiappkpi.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso


class SettingsUserActivity : AppCompatActivity() {
    private lateinit var role: String
    private var isWorking = false
    private lateinit var mAuth: FirebaseAuth

    private lateinit var edtFirstName: EditText
    private lateinit var edtPhoneNumber: EditText
    private lateinit var edtBirthdate: EditText
    private lateinit var edtDriverLicense: EditText

    private lateinit var backBtn: ImageButton
    private lateinit var doneBtn: ImageButton

    private lateinit var exitBtn: Button
    private lateinit var goToCarBtn: Button

    private lateinit var driverInfo: DriverInfo
    private lateinit var riderInfo: RiderInfo

    private lateinit var imgView: ImageView
    private var imgUri: Uri = "".toUri()
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_user)

        mAuth = FirebaseAuth.getInstance()
        role = intent.getStringExtra("ROLE")!!
        isWorking = intent.getBooleanExtra("IN_TRIP", false)


        init()

    }

    private fun init(){
        edtFirstName = findViewById(R.id.edtFirstName)
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber)
        edtBirthdate = findViewById(R.id.edtBirthdate)
        edtDriverLicense = findViewById(R.id.edtDriverLicense)

        backBtn = findViewById(R.id.edtBackButton)
        doneBtn= findViewById(R.id.edtDoneButton)

        exitBtn = findViewById(R.id.edtExitButton)
        goToCarBtn = findViewById(R.id.edtCarBtn)

        imgView = findViewById(R.id.edtUserImg)

        doneBtn.setOnClickListener {
            updateUserInfo()
        }

        backBtn.setOnClickListener {
            Log.d("log_check", "closeActivity(0)")
            closeActivity(false)
        }
        exitBtn.setOnClickListener {
            if(!isWorking){
                closeActivity(true)
            }
            else{
                Toast.makeText(this@SettingsUserActivity, "Закінчіть поїздку щоб вийти", Toast.LENGTH_SHORT).show()
            }
        }

        imgView.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT

            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }

        if (role == "driver") {
            val driverID = mAuth.currentUser!!.uid
            FirebaseDatabase.getInstance().reference.child(Common.DRIVERS_REFERENCE).child(driverID).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val driverInfoMap = snapshot.value as Map<*, *>
                        driverInfo = driverInfoMap.toDriverInfo()
                        edtDriverLicense.visibility = View.VISIBLE
                        val birthdateStr = Birthdate.birthdateToStr(driverInfo.userInfo.birthdate)

                        edtFirstName.text = SpannableStringBuilder(driverInfo.userInfo.firstName)
                        edtPhoneNumber.text = SpannableStringBuilder(driverInfo.userInfo.phoneNumber)
                        edtBirthdate.text = SpannableStringBuilder(birthdateStr)
                        edtDriverLicense.text = SpannableStringBuilder(driverInfo.driverLicense)

                        try{
                            if(driverInfo.userInfo.avatar!=""){
                            val uriStr = driverInfo.userInfo.avatar
                            Picasso.get().load(uriStr).into(imgView) }
                        }catch(e: Exception) {
                        Log.e("log_check", "ошибка аватарки: ${e.message}")
                    }

                        // goSettingCar
                        goToCarBtn.visibility = View.VISIBLE
                        goToCarBtn.setOnClickListener {
                            startActivity(Intent(this@SettingsUserActivity, SettingsCarActivity::class.java))
                        }

                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        } else {
            val riderID = mAuth.currentUser!!.uid
            FirebaseDatabase.getInstance().reference.child(Common.RIDERS_REFERENCE).child(riderID).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val riderInfoMap = snapshot.value!! as Map<*, *>
                        riderInfo = riderInfoMap.toRiderInfo()


                        val birthdateStr = Birthdate.birthdateToStr(riderInfo.userInfo.birthdate)

                        edtFirstName.text = SpannableStringBuilder(riderInfo.userInfo.firstName)
                        edtPhoneNumber.text = SpannableStringBuilder(riderInfo.userInfo.phoneNumber)
                        edtBirthdate.text = SpannableStringBuilder(birthdateStr)
                        try{
                            if(riderInfo.userInfo.avatar!=""){
                                val uriStr = riderInfo.userInfo.avatar
                                Picasso.get().load(uriStr).into(imgView)
                            }
                        }catch(e: Exception) {
                            Log.e("log_check", "ошибка аватарки: ${e.message}")
                        }


                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    private fun updateUserInfo() {
        val firstName = edtFirstName.text.toString()
        val phoneNumber = edtPhoneNumber.text.toString()
        val birthdateStr = edtBirthdate.text.toString()
        val driverLicense = edtDriverLicense.text.toString()

        if (firstName.isEmpty() || phoneNumber.isEmpty() || birthdateStr.isEmpty()
        ) {
            Toast.makeText(this@SettingsUserActivity, "Заповніть усі поля", Toast.LENGTH_SHORT).show()
            return
        }

        val birthdate = Birthdate.strToBirthdate(birthdateStr)

        if (birthdate == null) {
            Toast.makeText(this@SettingsUserActivity, "Неправильний формат дати", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this@SettingsUserActivity, "Оновлюємо дані", Toast.LENGTH_SHORT).show()

        if(role=="driver"){
            driverInfo.userInfo.firstName = firstName
            driverInfo.userInfo.phoneNumber = phoneNumber
            driverInfo.userInfo.birthdate = birthdate
            driverInfo.userInfo.avatar = imgUri.toString()
            driverInfo.driverLicense = driverLicense

            val driverID = mAuth.currentUser!!.uid
            FirebaseDatabase.getInstance().reference.child(Common.DRIVERS_REFERENCE).child(driverID).updateChildren(driverInfo.toMap()).addOnSuccessListener {
                Toast.makeText(this@SettingsUserActivity, "Дані оновились", Toast.LENGTH_SHORT).show()
                closeActivity(false)
            }
        }else{
            riderInfo.userInfo.firstName=firstName
            riderInfo.userInfo.phoneNumber = phoneNumber
            riderInfo.userInfo.birthdate = birthdate
            riderInfo.userInfo.avatar = imgUri.toString()

            val riderID = mAuth.currentUser!!.uid
            FirebaseDatabase.getInstance().reference.child(Common.RIDERS_REFERENCE).child(riderID).updateChildren(riderInfo.toMap()).addOnSuccessListener {
                Toast.makeText(this@SettingsUserActivity, "Дані оновились", Toast.LENGTH_SHORT).show()
                closeActivity(false)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val path = if(role=="driver"){
            "Photos/Driver/"
        } else{
            "Photos/Rider/"
        }
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            Toast.makeText(this@SettingsUserActivity, "Зберігаємо фото", Toast.LENGTH_SHORT).show()
            if(data != null && data.data != null) {
                val imageUri = data.data!!
                imgView.setImageURI(imageUri)

                val carFolder = FirebaseStorage.getInstance().reference.child(path + mAuth.currentUser!!.uid)
                carFolder.putFile(imageUri)
                    .addOnFailureListener { e ->
                        Toast.makeText(this@SettingsUserActivity, e.message!!, Toast.LENGTH_SHORT).show()
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            carFolder.downloadUrl.addOnSuccessListener { uri ->
                                imgUri = uri
                                Toast.makeText(this@SettingsUserActivity, "Фото збережено!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else{
                            Toast.makeText(this@SettingsUserActivity, task.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }


    private fun closeActivity(code: Boolean) {
        Log.d("log_check_closeActivity", "$code")
        val resultIntent = Intent()
        resultIntent.putExtra("EXIT", code)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

}


