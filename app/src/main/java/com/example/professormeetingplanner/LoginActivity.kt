package com.example.professormeetingplanner

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var cbShowPassword: CheckBox
    private lateinit var tvSignUp: TextView
    private lateinit var forgetPassword: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        cbShowPassword = findViewById(R.id.cbShowPassword)
        tvSignUp = findViewById(R.id.tvSignUp)
        forgetPassword = findViewById(R.id.forgetPassword)

        auth = FirebaseAuth.getInstance()

        cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show password
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                // Hide password
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            // Move the cursor to the end of the text
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, MainActivity::class.java).apply {putExtra("EMAIL", email) }
                            startActivity(intent)
                            finish()

                        } else {
                            Toast.makeText(this, "Authentication failed: " +
                                    "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        forgetPassword.setOnClickListener{
            val intent = Intent(this, ForgetPasswordActivity::class.java)
            startActivity(intent)
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}