package com.example.taxiappkpi.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taxiappkpi.References
import com.example.taxiappkpi.maps.DriverMapsActivity
import com.example.taxiappkpi.R
import com.example.taxiappkpi.maps.RiderMapsActivity
import com.example.taxiappkpi.registration.RegistrationActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class SignInActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        val role = intent.getStringExtra("ROLE")!!

        val question: TextView = findViewById(R.id.signUpQuestion)
        val title: TextView = findViewById(R.id.titleLayout)
        val signInBtn:Button = findViewById(R.id.signIn)
        val signUpBtn:Button = findViewById(R.id.signUp)
        val emailET:EditText = findViewById(R.id.email)
        val passwordET:EditText = findViewById(R.id.password)

        signUpBtn.isEnabled = false

        if (role=="driver")
            title.text = getString(R.string.SignIn) + getString(R.string.driver)

        else
            title.text = getString(R.string.SignIn) + getString(R.string.rider)

        question.setOnClickListener {
            signUpBtn.visibility = View.VISIBLE
            signUpBtn.isEnabled = true
            if (role=="driver")
                title.text = getString(R.string.SignUp) + getString(R.string.driver)
            else
                title.text = getString(R.string.SignUp) + getString(R.string.rider)
        }

        signUpBtn.setOnClickListener {
            val email = emailET.text.toString()
            val password = passwordET.text.toString()
            register(email, password, role)
        }

        signInBtn.setOnClickListener {
            val email = emailET.text.toString()
            val password = passwordET.text.toString()
            auth(email, password, role)
        }
    }


    private fun register(email: String, password: String, role: String) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this@SignInActivity, RegistrationActivity::class.java).putExtra("ROLE", role))
                Toast.makeText(this@SignInActivity, getText(R.string.successSignUp), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SignInActivity, "Помилка ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun auth(email: String, password: String, role: String) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if(role=="driver"){
                    val driverRef = FirebaseDatabase.getInstance().reference.child(References.DRIVERS_REFERENCE)
                    driverRef.addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if(snapshot.child(mAuth.currentUser!!.uid).exists()) {
                                startActivity(Intent(this@SignInActivity, DriverMapsActivity::class.java))
                            }else
                                startActivity(Intent(this@SignInActivity, RegistrationActivity::class.java).putExtra("ROLE", "driver"))
                        }
                        override fun onCancelled(error: DatabaseError) {
                        }
                    })

                } else{
                    val riderRef = FirebaseDatabase.getInstance().reference.child(References.RIDERS_REFERENCE)
                    riderRef.addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if(snapshot.child(mAuth.currentUser!!.uid).exists()) {
                                startActivity(Intent(this@SignInActivity, RiderMapsActivity::class.java))
                            }else
                                startActivity(Intent(this@SignInActivity, RegistrationActivity::class.java).putExtra("ROLE", "rider"))
                        }
                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
                Toast.makeText(this@SignInActivity, getText(R.string.successSignIn), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SignInActivity, "Помилка ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}