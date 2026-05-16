package com.nimmaguru.app.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.NimmaGuruApp
import com.nimmaguru.app.R
import com.nimmaguru.app.databinding.ActivityHomeBinding
import com.nimmaguru.app.domain.model.ClassSession
import com.nimmaguru.app.domain.model.LearningMaterial
import com.nimmaguru.app.domain.model.UserRole
import com.nimmaguru.app.ui.appreciation.WallOfFameActivity
import com.nimmaguru.app.ui.calendar.ClassCalendarActivity
import com.nimmaguru.app.ui.dashboard.DashboardActivity
import com.nimmaguru.app.ui.guru.GuruModule
import com.nimmaguru.app.ui.student.StudentModule
import com.nimmaguru.app.ui.student.StudentSearchActivity
import com.nimmaguru.app.ui.task.TaskManagerActivity
import com.nimmaguru.app.util.NimmaDateFormatter
import kotlinx.coroutines.launch

import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var sessionListener: ListenerRegistration? = null
    private var materialListener: ListenerRegistration? = null
    private var progressListener: ListenerRegistration? = null
    private var tasksListener: ListenerRegistration? = null
    private var profileListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as NimmaGuruApp
        val user = app.authRepository.currentUser
        val role = user?.uid?.let(app.userRoleRepository::getCachedRole)
        val roleName = when (role) {
            UserRole.GURU -> getString(R.string.guru)
            UserRole.STUDENT -> getString(R.string.student)
            null -> getString(R.string.not_selected)
        }

        binding.welcomeTextView.text = getString(
            R.string.home_greeting,
            getGreeting(this),
            user?.email?.substringBefore("@") ?: getString(R.string.friend),
        )
        binding.avatarTextView.text = (user?.email?.substringBefore("@") ?: "N")
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "N"
        binding.userTextView.text = user?.email ?: getString(R.string.signed_in_user)
        binding.roleTextView.text = getString(R.string.role_label, roleName)

        user?.uid?.let { uid ->
            profileListener = app.userRoleRepository.listenProfile(uid) { result ->
                result.onSuccess { data ->
                    val displayName = data["displayName"] as? String
                    val avatar = data["avatar"] as? String
                    if (!displayName.isNullOrBlank()) {
                        binding.welcomeTextView.text = getString(
                            R.string.home_greeting,
                            getGreeting(this@HomeActivity),
                            displayName,
                        )
                        if (!avatar.isNullOrBlank()) {
                            binding.avatarTextView.text = avatar
                        } else {
                            binding.avatarTextView.text = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "N"
                        }
                    }
                }
            }
        }

        binding.wallOfFameButton.visibility = android.view.View.VISIBLE
        if (role == UserRole.GURU) {
            binding.classCalendarButton.text = getString(R.string.continue_teaching)
            binding.searchMentorsButton.visibility = android.view.View.VISIBLE
            binding.searchMentorsText.text = getString(R.string.connect_with_gurus)
            binding.navTasksConnectButton.text = getString(R.string.nav_community)
        } else {
            binding.classCalendarButton.text = getString(R.string.continue_learning)
            binding.searchMentorsButton.visibility = android.view.View.VISIBLE
            binding.searchMentorsText.text = getString(R.string.search_mentors)
            binding.navTasksConnectButton.text = getString(R.string.nav_tasks_connect)
        }

        binding.myClassesButton.setOnClickListener { startActivity(Intent(this, ClassCalendarActivity::class.java)) }
        binding.materialsButton.setOnClickListener {
            if (role == UserRole.GURU) GuruModule.openMaterials(this) else StudentModule.openMaterials(this)
        }
        binding.assignmentsButton.setOnClickListener {
            if (role == UserRole.GURU) {
                startActivity(Intent(this, TaskManagerActivity::class.java))
            } else {
                StudentModule.openAssignments(this)
            }
        }
        binding.progressButton.setOnClickListener { StudentModule.openProgress(this) }
        binding.searchMentorsButton.setOnClickListener {
            if (role == UserRole.GURU) {
                startActivity(Intent(this, StudentSearchActivity::class.java).putExtra(StudentSearchActivity.EXTRA_COMMUNITY_MODE, true))
            } else {
                StudentModule.openMentors(this)
            }
        }
        binding.wallOfFameButton.setOnClickListener { startActivity(Intent(this, WallOfFameActivity::class.java)) }
        binding.classCalendarButton.setOnClickListener { startActivity(Intent(this, ClassCalendarActivity::class.java)) }
        binding.navDashboardButton.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }
        binding.navTasksConnectButton.setOnClickListener {
            if (role == UserRole.GURU) {
                startActivity(Intent(this, StudentSearchActivity::class.java).putExtra(StudentSearchActivity.EXTRA_COMMUNITY_MODE, true))
            } else {
                startActivity(Intent(this, TaskManagerActivity::class.java))
            }
        }
        binding.profileNavButton.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }

        binding.welcomeTextView.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }
        binding.avatarTextView.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }

        binding.openProgressAction.setOnClickListener { StudentModule.openProgress(this) }
        binding.currentTasksAction.setOnClickListener {
            if (role == UserRole.GURU) {
                startActivity(Intent(this, TaskManagerActivity::class.java))
            } else {
                StudentModule.openAssignments(this)
            }
        }

        listenUpcomingClasses(app)
        listenRecentMaterials(app)
        listenRealTimeProgress(app, user?.uid, role)
    }

    override fun onDestroy() {
        sessionListener?.remove()
        materialListener?.remove()
        progressListener?.remove()
        tasksListener?.remove()
        profileListener?.remove()
        super.onDestroy()
    }

    private fun listenRealTimeProgress(app: NimmaGuruApp, uid: String?, role: UserRole?) {
        if (uid == null) return
        
        // Listen for tasks
        lifecycleScope.launch {
            val grade = app.userRoleRepository.loadGradeLevel(uid)
            tasksListener = if (role == UserRole.GURU) {
                app.taskRepository.listenGuruTasks(uid) { result ->
                    result.onSuccess { tasks ->
                        binding.progressTasksCount.text = tasks.size.toString()
                    }
                }
            } else {
                app.taskRepository.listenAssignedTasks(grade) { result ->
                    result.onSuccess { tasks ->
                        binding.progressTasksCount.text = tasks.size.toString()
                    }
                }
            }
        }
    }

    private fun listenUpcomingClasses(app: NimmaGuruApp) {
        sessionListener = app.classSessionRepository.listenUpcomingSessions { result ->
            result.onSuccess { sessions -> 
                bindUpcomingSession(sessions.firstOrNull())
                binding.progressClassesCount.text = sessions.size.toString()
            }
        }
    }

    private fun listenRecentMaterials(app: NimmaGuruApp) {
        materialListener = app.learningRepository.listenRecentMaterials { result ->
            result.onSuccess { materials -> bindRecentMaterial(materials.firstOrNull()) }
        }
    }

    private fun bindRecentMaterial(material: LearningMaterial?) {
        if (material == null) {
            binding.recentMaterialIconTextView.text = getString(R.string.material_badge)
            binding.recentMaterialTitleTextView.text = getString(R.string.no_materials_found)
            binding.recentMaterialMetaTextView.text = ""
            return
        }
        binding.recentMaterialIconTextView.text = fileIcon(material.resourceUrl, material.type)
        binding.recentMaterialTitleTextView.text = material.title
        binding.recentMaterialMetaTextView.text = material.let {
            listOf(it.subject, it.classLevel, it.type, it.size)
                .asSequence()
                .filter(String::isNotBlank)
                .joinToString(" - ")
        }
    }

    private fun bindUpcomingSession(session: ClassSession?) {
        val app = application as NimmaGuruApp
        val currentUser = app.authRepository.currentUser
        val role = currentUser?.uid?.let(app.userRoleRepository::getCachedRole)

        if (session == null) {
            binding.upcomingDateTextView.text = ""
            binding.upcomingTimeTextView.text = ""
            binding.upcomingTitleTextView.text = getString(R.string.no_live_classes)
            binding.upcomingMentorTextView.text = ""
            binding.enrollButton.visibility = android.view.View.GONE
        } else {
            binding.upcomingDateTextView.text = NimmaDateFormatter.localize(session.startsAt?.toDate()?.let(NimmaDateFormatter::format).orEmpty())
            binding.upcomingTimeTextView.text = NimmaDateFormatter.localize(session.time)
            binding.upcomingTitleTextView.text = when(session.subject.lowercase()) {
                "science" -> getString(R.string.science_subject)
                "mathematics", "math" -> getString(R.string.mathematics_subject)
                "english" -> getString(R.string.english_subject)
                "kannada" -> getString(R.string.kannada_subject)
                "social science" -> getString(R.string.social_science_subject)
                else -> session.subject
            }
            binding.upcomingMentorTextView.text = listOf(session.mentor, session.location)
                .asSequence()
                .filter(String::isNotBlank)
                .joinToString(" - ")

            if (role == UserRole.STUDENT) {
                val isEnrolled = session.enrolledStudentIds.contains(currentUser.uid)
                binding.enrollButton.visibility = android.view.View.VISIBLE
                binding.enrollButton.text = if (isEnrolled) getString(R.string.enrolled_check) else getString(R.string.enroll_now)
                binding.enrollButton.isEnabled = !isEnrolled
                binding.enrollButton.setOnClickListener {
                    lifecycleScope.launch {
                        runCatching { app.classSessionRepository.enrollInSession(session.id, currentUser.uid) }
                    }
                }
            } else if (role == UserRole.GURU) {
                binding.enrollButton.visibility = android.view.View.VISIBLE
                binding.enrollButton.text = getString(R.string.students_enrolled_count, NimmaDateFormatter.localize(session.enrolledStudentIds.size))
                binding.enrollButton.isEnabled = true
                binding.enrollButton.setOnClickListener {
                    showStudentsDialog(session.enrolledStudentIds)
                }
            } else {
                binding.enrollButton.visibility = android.view.View.GONE
            }
        }
    }

    private fun showStudentsDialog(studentIds: List<String>) {
        if (studentIds.isEmpty()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.view_students)
                .setMessage(R.string.no_students_enrolled)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        lifecycleScope.launch {
            val app = application as NimmaGuruApp
            val names = studentIds.map { uid ->
                async {
                    val name = runCatching { app.userRoleRepository.loadDisplayName(uid) }.getOrNull()
                    if (name.isNullOrBlank()) {
                        // If name is missing, try to get a fallback from the user document data
                        val data = runCatching {
                            val snap = app.userRoleRepository.profileDocumentForDialog(uid).get().await()
                            snap.getString("displayName") ?: snap.getString("email")?.substringBefore("@")
                        }.getOrNull()
                        data ?: "Student"
                    } else name
                }
            }.awaitAll()
            androidx.appcompat.app.AlertDialog.Builder(this@HomeActivity)
                .setTitle(R.string.view_students)
                .setItems(names.toTypedArray(), null)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun fileIcon(url: String, type: String): String {
        val value = "$url $type".lowercase()
        return when {
            value.contains(".pdf") || value.contains("pdf") -> "PDF"
            value.contains(".doc") || value.contains("word") -> "DOC"
            value.contains(".jpg") || value.contains(".jpeg") || value.contains(".png") || value.contains(".webp") -> "IMG"
            else -> "FILE"
        }
    }
}
