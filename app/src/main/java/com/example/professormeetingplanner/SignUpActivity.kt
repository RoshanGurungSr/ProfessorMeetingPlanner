package com.example.professormeetingplanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmailSignUp: EditText
    private lateinit var etPasswordSignUp: EditText // Add this EditText for password input
    private lateinit var spinnerRole: Spinner
    private lateinit var btnRegister: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmailSignUp = findViewById(R.id.etEmailSignUp)
        etPasswordSignUp = findViewById(R.id.etPasswordSignUp) // Make sure you have this in your XML
        spinnerRole = findViewById(R.id.spinnerRole)
        btnRegister = findViewById(R.id.btnRegister)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString()
            val lastName = etLastName.text.toString()
            val email = etEmailSignUp.text.toString()
            val password = etPasswordSignUp.text.toString()
            val role = spinnerRole.selectedItem.toString()

            if (firstName.isNotEmpty() && lastName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Show the success message immediately
                            Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()

                            // Save user details to the database
                            val user = auth.currentUser
                            saveUserDetails(user?.uid, firstName, lastName, email, role)
                        } else {
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserDetails(userId: String?, firstName: String, lastName: String, email: String, role: String) {
        if (userId == null) {
            Toast.makeText(this, "User ID is null", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = database.getReference("users").child(userId)
        val userDetails = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "role" to role
        )

        userRef.setValue(userDetails).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Redirect to login page after saving user details
                redirectToLogin()
            } else {
                Toast.makeText(this, "Failed to save user details: " +
                        "${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
