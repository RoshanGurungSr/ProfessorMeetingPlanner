package com.example.professormeetingplanner

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

data class Appointment(
    val studentName: String = "",
    val appointmentTime: String = "",
    val courseName: String = "",
    val email: String = "",
)

class AppointmentAdapter(
    context: Context,
    private val appointments: List<Appointment>,
    var isProfessor: Boolean
) : ArrayAdapter<Appointment>(context, 0, appointments) {

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_appointment,
            parent, false)

        val appointment = getItem(position)

        val studentNameTextView = view.findViewById<TextView>(R.id.student_name)
        val appointmentTimeTextView = view.findViewById<TextView>(R.id.appointment_time)
        val courseNameTextView = view.findViewById<TextView>(R.id.course_name)
        val cancelIcon = view.findViewById<ImageView>(R.id.cancel_icon)

        studentNameTextView.text = appointment?.studentName
        appointmentTimeTextView.text = appointment?.appointmentTime
        courseNameTextView.text = "Course Name: ${appointment?.courseName}"

        if (isProfessor) {
            cancelIcon.visibility = View.GONE
        } else {
            cancelIcon.visibility = View.VISIBLE
            cancelIcon.setOnClickListener {
                Toast.makeText(context, "Cancelled: ${appointment?.studentName}", Toast.LENGTH_SHORT).show()
                (context as MainActivity).removeAppointment(position)
            }
        }

        return view
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var adapter: AppointmentAdapter
    private val appointments = mutableListOf<Appointment>()
    private lateinit var databaseReference: DatabaseReference
    private var isProfessor: Boolean = false

    companion object {
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userEmail: String = intent.getStringExtra("EMAIL").toString()

        databaseReference = FirebaseDatabase.getInstance().getReference("appointments")

        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView: View = navView.getHeaderView(0)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> Toast.makeText(applicationContext, "Clicked Home", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> performLogout()
            }
            true
        }

        val navEmail: TextView = headerView.findViewById(R.id.user_email)
        navEmail.text = userEmail

        updateNavHeader(headerView, userEmail) { isProf ->
            isProfessor = isProf

            adapter = AppointmentAdapter(this, appointments, isProfessor)
            findViewById<ListView>(R.id.appointments_list_view).adapter = adapter

            loadAppointmentsFromFirebase(userEmail)
        }
    }

    private fun loadAppointmentsFromFirebase(userEmail: String) {
        val query = if (isProfessor) {
            databaseReference
        } else {
            databaseReference.orderByChild("email").equalTo(userEmail)
        }

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                appointments.clear()
                for (snapshot in dataSnapshot.children) {
                    val appointment = snapshot.getValue(Appointment::class.java)
                    appointment?.let { appointments.add(it) }
                }
                adapter.notifyDataSetChanged()
                scheduleAppointmentNotifications()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load appointments.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun scheduleAppointmentNotifications() {
        if (isNotificationPermissionGranted()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())

            for (appointment in appointments) {
                try {
                    val appointmentTime = Calendar.getInstance().apply {
                        time = dateFormat.parse(appointment.appointmentTime)
                    }
                    val currentTime = Calendar.getInstance()

                    if (appointmentTime.after(currentTime)) {
                        NotificationScheduler.scheduleNotification(
                            this,
                            appointmentTime,
                            "Upcoming Appointment",
                            "You have an appointment with ${appointment.studentName} for ${appointment.courseName}."
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            requestNotificationPermission()
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    scheduleAppointmentNotifications()
                } else {
                    // Permission denied
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateNavHeader(headerView: View, userEmail: String, callback: (Boolean) -> Unit) {
        val userNameTextView: TextView = headerView.findViewById(R.id.user_name)
        val usersDatabaseReference = FirebaseDatabase.getInstance().getReference("users")

        usersDatabaseReference.orderByChild("email").equalTo(userEmail).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val firstName = userSnapshot.child("firstName").getValue(String::class.java)
                        val lastName = userSnapshot.child("lastName").getValue(String::class.java)
                        val role = userSnapshot.child("role").getValue(String::class.java)

                        if (firstName != null && lastName != null && role != null) {
                            userNameTextView.text = "$firstName $lastName"
                            callback(role.equals("Professor", ignoreCase = true))
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load user details.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun removeAppointment(position: Int) {
        appointments.removeAt(position)
        adapter.notifyDataSetChanged()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
