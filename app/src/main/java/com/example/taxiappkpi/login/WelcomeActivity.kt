package com.example.taxiappkpi.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.taxiappkpi.References
import com.example.taxiappkpi.maps.DriverMapsActivity
import com.example.taxiappkpi.R
import com.example.taxiappkpi.maps.RiderMapsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class WelcomeActivity : AppCompatActivity() {

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser

        val driverBtn: Button = findViewById(R.id.driverButton)
        val riderBtn: Button = findViewById(R.id.riderButton)

        driverBtn.setOnClickListener {
            val driverIntent = Intent(this@WelcomeActivity, SignInActivity::class.java)
            driverIntent.putExtra("ROLE", "driver")
            if (currentUser!=null){
                val driverRef = FirebaseDatabase.getInstance().reference.child(References.DRIVERS_REFERENCE)
                driverRef.addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.child(currentUser.uid).exists()) {
                            startActivity(Intent(this@WelcomeActivity, DriverMapsActivity::class.java))
                    }else
                        startActivity(driverIntent)
                    }
                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }else
                startActivity(driverIntent)
        }

        riderBtn.setOnClickListener {
            val riderIntent = Intent(this@WelcomeActivity, SignInActivity::class.java)
            riderIntent.putExtra("ROLE", "rider")
            if (currentUser!=null){
                val riderRef = FirebaseDatabase.getInstance().reference.child(References.RIDERS_REFERENCE)
                riderRef.addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.child(currentUser.uid).exists()) {
                            startActivity(Intent(this@WelcomeActivity, RiderMapsActivity::class.java))
                        }else
                            startActivity(riderIntent)
                    }
                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }else
                startActivity(riderIntent)
        }

    }

}

