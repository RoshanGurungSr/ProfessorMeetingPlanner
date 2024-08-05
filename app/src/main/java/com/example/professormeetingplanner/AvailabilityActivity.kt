package com.example.professormeetingplanner

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class AvailabilityActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var startTimeTextView: TextView
    private lateinit var endTimeTextView: TextView

    private lateinit var userEmail: String
    private var isProfessor: Boolean = false

    private var startAvailable: String = ""
    private var endAvailable: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_availability)

        // INFO: Side navbar code starts here
        userEmail = intent.getStringExtra("professorEmail").toString()
        database = FirebaseDatabase.getInstance().getReference("users")

        // Code for navbar menu
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView: View = navView.getHeaderView(0)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navView.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.nav_home -> redirectHome()
                R.id.nav_availability -> redirectAvailability()
                R.id.nav_logout -> performLogout()
            }
            true
        }

        // Populate the navbar with user data
        val navEmail: TextView = headerView.findViewById(R.id.user_email)
        navEmail.text = userEmail

        updateNavHeader(headerView, userEmail) { isProf ->
            isProfessor = isProf
        }

        // INFO: Side navbar code ends here

        val startAvailableButton: Button = findViewById(R.id.startAvailable)
        val endAvailableButton: Button = findViewById(R.id.endAvailable)
        startTimeTextView = findViewById(R.id.startTimeTextView)
        endTimeTextView = findViewById(R.id.endTimeTextView)
        val courseNameList: EditText = findViewById(R.id.courseNameList)
        val btnSaveAvailability: Button = findViewById(R.id.btnSaveAvailability)

        val checkBoxes = listOf(
            findViewById<CheckBox>(R.id.checkBoxMonday),
            findViewById<CheckBox>(R.id.checkBoxTuesday),
            findViewById<CheckBox>(R.id.checkBoxWednesday),
            findViewById<CheckBox>(R.id.checkBoxThursday),
            findViewById<CheckBox>(R.id.checkBoxFriday),
            findViewById<CheckBox>(R.id.checkBoxSaturday),
            findViewById<CheckBox>(R.id.checkBoxSunday)
        )

        startAvailableButton.setOnClickListener { showTimePickerDialog(true) }
        endAvailableButton.setOnClickListener { showTimePickerDialog(false) }

        btnSaveAvailability.setOnClickListener {
            val courses = courseNameList.text.toString().split(",").map { it.trim() }
            val daysOfWeek = checkBoxes.filter { it.isChecked }.map { it.text.toString() }

            val email = userEmail
            updateProfessorAvailability(email, startAvailable, endAvailable, courses, daysOfWeek)
        }
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun showTimePickerDialog(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val time = String.format("%02d:%02d", selectedHour, selectedMinute)
            if (isStartTime) {
                startAvailable = time
                startTimeTextView.text = "Start Time: $time"
            } else {
                endAvailable = time
                endTimeTextView.text = "End Time: $time"
            }
        }, hour, minute, true).show() // Set is24HourView to true
    }

    private fun updateProfessorAvailability(email: String, start: String, end: String, courses: List<String>, daysOfWeek: List<String>) {
        val query: Query = database.orderByChild("email").equalTo(email)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val userKey = snapshot.key
                    if (userKey != null) {
                        val updates = mapOf(
                            "startAvailable" to start,
                            "endAvailable" to end,
                            "courseNameList" to courses,
                            "daysOfWeek" to daysOfWeek
                        )
                        database.child(userKey).updateChildren(updates)
                            .addOnSuccessListener {
                                Toast.makeText(this@AvailabilityActivity, "Availability updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@AvailabilityActivity, "Failed to update availability", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@AvailabilityActivity, "Error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateNavHeader(headerView: View, userEmail: String, callback: (Boolean) -> Unit) {
        val userNameTextView: TextView = headerView.findViewById(R.id.user_name)

        // Reference to the users collection
        val usersDatabaseReference = FirebaseDatabase.getInstance().getReference("users")

        // Query to find user by email
        usersDatabaseReference.orderByChild("email").equalTo(userEmail).addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val firstName = userSnapshot.child("firstName").getValue(String::class.java)
                        val lastName = userSnapshot.child("lastName").getValue(String::class.java)
                        val role = userSnapshot.child("role").getValue(String::class.java)

                        if (firstName != null && lastName != null && role != null) {
                            userNameTextView.text = "$firstName $lastName"

                            val isProf = role.lowercase() == "professor"
                            callback(isProf)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@AvailabilityActivity, "Failed to load user details.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()

        // Redirect to login screen
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun redirectAvailability() {
        val intent = Intent(this, AvailabilityActivity::class.java).apply {
            putExtra("professorEmail", userEmail)
        }
        startActivity(intent)
    }

    private fun redirectHome(){
        val intent = Intent(this, MainActivity::class.java).apply {putExtra("EMAIL", userEmail) }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Return to MainActivity when back button is pressed
        redirectHome()
    }
}