package com.example.professormeetingplanner

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import java.text.SimpleDateFormat
import java.util.*

class StudentAppointmentActivity : AppCompatActivity() {

    private lateinit var professorSpinner: Spinner
    private lateinit var courseSpinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private lateinit var databaseReference: DatabaseReference

    private var availableTimes = mutableListOf<String>()
    private var professorEmailToCourse = mutableMapOf<String, String>()
    private var selectedProfessor: String? = null
    private var selectedProfessorEncodedEmail: String? = null
    private var selectedCourse: String? = null
    private var studentEmail: String? = null
    private var studentName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_appointment)

        professorSpinner = findViewById(R.id.professor_spinner)
        courseSpinner = findViewById(R.id.course_spinner)
        timeSpinner = findViewById(R.id.time_spinner)
        saveButton = findViewById(R.id.save_appointment_button)
        cancelButton = findViewById(R.id.cancel_button)

        databaseReference = FirebaseDatabase.getInstance().reference

        // Get student email from intent
        studentEmail = intent.getStringExtra("Student email")
        if (studentEmail == null) {
            Toast.makeText(this, "Student email not found", Toast.LENGTH_SHORT).show()
            return
        }

        fetchStudentName(studentEmail!!)
        fetchProfessors()

        professorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedProfessor = parent.getItemAtPosition(position) as? String
                selectedProfessor?.let {
                    fetchCourseForProfessor(it)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCourse = parent.getItemAtPosition(position) as? String
                selectedProfessor?.let {
                    fetchAvailableTimes(it)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        timeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Handle time selection if needed
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        saveButton.setOnClickListener {
            saveAppointment()
        }

        cancelButton.setOnClickListener {
            finish() // Close the activity
        }
    }

    private fun fetchStudentName(email: String) {
        databaseReference.child("users").orderByChild("email").equalTo(email)
            .get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        studentName = userSnapshot.child("firstName").getValue(String::class.java) +
                                " " + userSnapshot.child("lastName").getValue(String::class.java)
                    }
                } else {
                    Log.d("StudentAppointment", "No student found with email: $email")
                }
            }.addOnFailureListener {
                Log.e("StudentAppointment", "Error fetching student name", it)
            }
    }

    private fun fetchProfessors() {
        databaseReference.child("users").orderByChild("role").equalTo("Professor")
            .get().addOnSuccessListener { dataSnapshot ->
                val professors = mutableListOf<String>()
                for (userSnapshot in dataSnapshot.children) {
                    val email = userSnapshot.child("email").getValue(String::class.java)
                    val name = userSnapshot.child("firstName").getValue(String::class.java) + " " +
                            userSnapshot.child("lastName").getValue(String::class.java)
                    val encodedEmail = userSnapshot.child("encodedEmail").getValue(String::class.java)
                    if (email != null && encodedEmail != null) {
                        professors.add(name)
                        // Map professor names to their encoded email for further use
                        professorEmailToCourse[name] = encodedEmail
                    }
                }
                if (professors.isEmpty()) {
                    Log.d("StudentAppointment", "No professors found")
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, professors)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                professorSpinner.adapter = adapter
            }.addOnFailureListener {
                Log.e("StudentAppointment", "Error fetching professors", it)
            }
    }

    private fun fetchCourseForProfessor(professorName: String) {
        val professorEncodedEmail = professorEmailToCourse[professorName] ?: return

        databaseReference.child("users").orderByChild("encodedEmail").equalTo(professorEncodedEmail)
            .get().addOnSuccessListener { dataSnapshot ->
                val courses = mutableListOf<String>()
                for (userSnapshot in dataSnapshot.children) {
                    val courseNamesSnapshot = userSnapshot.child("courseNameList")
                    val courseNames = courseNamesSnapshot.getValue(object : GenericTypeIndicator<List<String>>() {})
                    if (courseNames != null) {
                        courses.addAll(courseNames)
                    } else {
                        Log.d("StudentAppointment", "Course names not found or not a list")
                    }
                }
                if (courses.isEmpty()) {
                    Log.d("StudentAppointment", "No courses found for professor: $professorName")
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                courseSpinner.adapter = adapter
            }.addOnFailureListener { exception ->
                Log.e("StudentAppointment", "Error fetching courses", exception)
            }
    }

    private fun fetchAvailableTimes(professorName: String) {
        val professorEncodedEmail = professorEmailToCourse[professorName] ?: return

        databaseReference.child("users").orderByChild("encodedEmail").equalTo(professorEncodedEmail)
            .get().addOnSuccessListener { dataSnapshot ->
                selectedProfessorEncodedEmail=professorEncodedEmail
                if (dataSnapshot.exists()) {
                    val availabilityStart = dataSnapshot.children.first().child("startAvailable").getValue(String::class.java)
                    val availabilityEnd = dataSnapshot.children.first().child("endAvailable").getValue(String::class.java)

                    if (availabilityStart != null && availabilityEnd != null) {
                        val startTime = convertStringToDate(availabilityStart)
                        val endTime = convertStringToDate(availabilityEnd)

                        availableTimes = splitToHalfHourIntervals(startTime, endTime)
                        removeBookedTimes(professorEncodedEmail)

                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableTimes)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        timeSpinner.adapter = adapter
                    } else {
                        Log.d("StudentAppointment", "Availability data not found for professor: $professorName")
                    }
                } else {
                    Log.d("StudentAppointment", "No data found for encoded email: $professorEncodedEmail")
                }
            }.addOnFailureListener {
                Log.e("StudentAppointment", "Error fetching available times", it)
            }
    }

    private fun removeBookedTimes(professorEncodedEmail: String) {
        databaseReference.child("appointments").orderByChild("professorEmail").equalTo(professorEncodedEmail)
            .get().addOnSuccessListener { dataSnapshot ->
                val bookedTimes = mutableListOf<Date>()
                for (appointmentSnapshot in dataSnapshot.children) {
                    val appointmentTime = appointmentSnapshot.child("appointmentTime").getValue(String::class.java)
                    if (appointmentTime != null) {
                        val bookedDate = convertStringToDate(appointmentTime)
                        bookedTimes.add(bookedDate)
                    }
                }

                // Remove booked times from availableTimes
                availableTimes = availableTimes.filter { time ->
                    val timeDate = convertStringToDate(time)
                    !bookedTimes.contains(timeDate)
                }.toMutableList()

                // Update spinner after removing booked times
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableTimes)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                timeSpinner.adapter = adapter
            }.addOnFailureListener {
                Log.e("StudentAppointment", "Error fetching booked times", it)
            }
    }

    private fun convertStringToDate(dateString: String): Date {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.parse(dateString) ?: Date()
    }

    private fun splitToHalfHourIntervals(start: Date, end: Date): MutableList<String> {
        val intervals = mutableListOf<String>()
        val calendar = Calendar.getInstance().apply {
            time = start
        }

        while (calendar.time.before(end)) {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
            intervals.add(time)
            calendar.add(Calendar.MINUTE, 30)
        }
        return intervals
    }

    private fun saveAppointment() {
        val selectedTime = timeSpinner.selectedItem.toString()
        val professorEmail=professorSpinner.selectedItem.toString()
        val appointment = mapOf(
            "studentName" to studentName,
            "email" to studentEmail,
            "professorEmail" to selectedProfessorEncodedEmail,
            "courseName" to selectedCourse,
            "appointmentTime" to selectedTime
        )

        databaseReference.child("appointments").push().setValue(appointment)
            .addOnSuccessListener {
                Toast.makeText(this, "Appointment saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save appointment", Toast.LENGTH_SHORT).show()
            }
    }
}
