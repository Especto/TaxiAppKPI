package com.example.taxiappkpi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.taxiappkpi.login.WelcomeActivity


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val thread: Thread = object : Thread() {
            override fun run() {
                super.run()
                run {
                    try {
                        sleep(2000)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        val welcomeIntent = Intent(this@MainActivity, WelcomeActivity::class.java)
                        startActivity(welcomeIntent)
                    }
                }
            }
        }
        thread.start()
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}

