package com.example.professormeetingplanner

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.professormeetingplanner.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import java.text.SimpleDateFormat
import java.util.*

class StudentAppointmentActivity : AppCompatActivity() {

    private lateinit var professorSpinner: Spinner
    private lateinit var courseSpinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var datePicker: DatePicker
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private lateinit var databaseReference: DatabaseReference

    private var availableTimes = mutableListOf<String>()
    private var professorEmailToCourse = mutableMapOf<String, String>()
    private var selectedProfessor: String? = null
    private var selectedProfessorEncodedEmail: String? = null
    private var selectedCourse: String? = null
    private var selectedDate: Date? = null
    private var studentEmail: String? = null
    private var studentName: String? = null
    private val availableDaysOfWeek = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_appointment)

        professorSpinner = findViewById(R.id.professor_spinner)
        courseSpinner = findViewById(R.id.course_spinner)
        timeSpinner = findViewById(R.id.time_spinner)
        datePicker = findViewById(R.id.date_picker)
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
                    fetchAvailableDaysOfWeek(it)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        datePicker.init(Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH) { _, year, monthOfYear, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, monthOfYear, dayOfMonth)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val selectedDateCalendar = Calendar.getInstance().apply {
                set(year, monthOfYear, dayOfMonth)
            }
            selectedDate = selectedDateCalendar.time

            // Check if the selected day is available
            if (availableDaysOfWeek.contains(dayOfWeek)) {
                fetchAvailableTimes()
            } else {
                Toast.makeText(this, "Selected date is not available", Toast.LENGTH_SHORT).show()
                availableTimes.clear()
                timeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableTimes)
            }
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
                        Log.d("StudentAppointment", "No courses found for professor: $professorName")
                    }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                courseSpinner.adapter = adapter
            }.addOnFailureListener {
                Log.e("StudentAppointment", "Error fetching courses", it)
            }
    }

    private fun fetchAvailableDaysOfWeek(professorName: String) {
        val professorEncodedEmail = professorEmailToCourse[professorName] ?: return

        databaseReference.child("users").orderByChild("encodedEmail").equalTo(professorEncodedEmail)
            .get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val userSnapshot = dataSnapshot.children.first()

                    val daysOfWeekTypeIndicator = object : GenericTypeIndicator<List<String>>() {}
                    val daysOfWeek = userSnapshot.child("daysOfWeek").getValue(daysOfWeekTypeIndicator)

                    if (daysOfWeek != null) {
                        availableDaysOfWeek.clear()
                        val dayOfWeekMap = mapOf(
                            "Sunday" to Calendar.SUNDAY,
                            "Monday" to Calendar.MONDAY,
                            "Tuesday" to Calendar.TUESDAY,
                            "Wednesday" to Calendar.WEDNESDAY,
                            "Thursday" to Calendar.THURSDAY,
                            "Friday" to Calendar.FRIDAY,
                            "Saturday" to Calendar.SATURDAY
                        )

                        for (day in daysOfWeek) {
                            dayOfWeekMap[day]?.let {
                                availableDaysOfWeek.add(it)
                            }
                        }
                    } else {
                        Log.d("StudentAppointment", "daysOfWeek field is null")
                    }
                } else {
                    Log.d("StudentAppointment", "No data found for professor: $professorName")
                }
            }.addOnFailureListener {
                Log.e("StudentAppointment", "Error fetching available days", it)
            }
    }

    private fun fetchAvailableTimes() {
        val professorName = selectedProfessor ?: return
        val professorEncodedEmail = professorEmailToCourse[professorName] ?: return
        if (selectedDate == null) return

        databaseReference.child("users").orderByChild("encodedEmail").equalTo(professorEncodedEmail)
            .get().addOnSuccessListener { dataSnapshot ->
                selectedProfessorEncodedEmail = professorEncodedEmail
                if (dataSnapshot.exists()) {
                    val availabilityStart = dataSnapshot.children.first().child("startAvailable").getValue(String::class.java)
                    val availabilityEnd = dataSnapshot.children.first().child("endAvailable").getValue(String::class.java)

                    if (availabilityStart != null && availabilityEnd != null) {
                        val startTime = convertStringToDate(availabilityStart)
                        val endTime = convertStringToDate(availabilityEnd)

                        availableTimes = splitToHalfHourIntervals(startTime, endTime)
                        removeBookedTimes(professorEncodedEmail, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate!!))

                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableTimes)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        timeSpinner.adapter = adapter
                    }
                }
            }.addOnFailureListener {
                Log.e("StudentAppointment", "Error fetching available times", it)
            }
    }

    private fun removeBookedTimes(professorEncodedEmail: String, date: String) {
        databaseReference.child("appointments").orderByChild("professorEmail").equalTo(professorEncodedEmail)
            .get().addOnSuccessListener { dataSnapshot ->
                val bookedTimes = mutableListOf<Date>()
                for (appointmentSnapshot in dataSnapshot.children) {
                    val appointmentTime = appointmentSnapshot.child("appointmentTime").getValue(String::class.java)
                    val appointmentDate = appointmentSnapshot.child("appointmentDate").getValue(String::class.java)
                    if (appointmentTime != null && appointmentDate == date) {
                        val bookedDate = convertStringToDate(appointmentTime)
                        bookedTimes.add(bookedDate)
                    }
                }

                availableTimes = availableTimes.filter { time ->
                    val timeDate = convertStringToDate(time)
                    !bookedTimes.contains(timeDate)
                }.toMutableList()

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableTimes)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                timeSpinner.adapter = adapter
            }.addOnFailureListener {
                Log.e("StudentAppointment", "Error fetching booked times", it)
            }
    }

    private fun saveAppointment() {
        val selectedTime = timeSpinner.selectedItem.toString()
        val appointment = mapOf(
            "studentName" to studentName,
            "email" to studentEmail,
            "professorEmail" to selectedProfessorEncodedEmail,
            "courseName" to selectedCourse,
            "appointmentDate" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate!!),
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

    private fun convertStringToDate(timeString: String): Date {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.parse(timeString)!!
    }

    private fun splitToHalfHourIntervals(startTime: Date, endTime: Date): MutableList<String> {
        val intervals = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.time = startTime

        while (calendar.time.before(endTime)) {
            intervals.add(SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time))
            calendar.add(Calendar.MINUTE, 30)
        }

        return intervals
    }
}
