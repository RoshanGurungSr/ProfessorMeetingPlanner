package com.example.professormeetingplanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity: AppCompatActivity () {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEdit: EditText
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        auth = FirebaseAuth.getInstance()

        emailEdit = findViewById(R.id.emailAddress)
        resetButton = findViewById(R.id.btnResetPassword)

        resetButton.setOnClickListener{
            val email = emailEdit.text.toString().trim()

            if (email.isEmpty()){
                Toast.makeText(this, "Please enter email address", Toast.LENGTH_SHORT).show()
            } else {
                resetPassword(email)
            }
        }
    }

    private fun resetPassword(email: String){
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful){
                Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                redirectToLogin()
            } else {
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}