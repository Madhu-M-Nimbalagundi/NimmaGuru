package com.nimmaguru.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nimmaguru.app.data.FirestoreRepository
import com.nimmaguru.app.data.appreciation.AppreciationRepository
import com.nimmaguru.app.data.auth.AuthRepository
import com.nimmaguru.app.data.badge.BadgeManager
import com.nimmaguru.app.data.calendar.ClassSessionRepository
import com.nimmaguru.app.data.classroom.ClassroomRepository
import com.nimmaguru.app.data.guru.GuruChatRepository
import com.nimmaguru.app.data.guru.GuruProfileRepository
import com.nimmaguru.app.data.learning.LearningRepository
import com.nimmaguru.app.data.local.UserPreferences
import com.nimmaguru.app.data.task.TaskRepository
import com.nimmaguru.app.data.user.UserRoleRepository

class NimmaGuruApp : Application() {

    lateinit var authRepository: AuthRepository
        private set

    lateinit var guruProfileRepository: GuruProfileRepository
        private set

    lateinit var guruChatRepository: GuruChatRepository
        private set

    lateinit var firestoreRepository: FirestoreRepository
        private set

    lateinit var classSessionRepository: ClassSessionRepository
        private set

    lateinit var classroomRepository: ClassroomRepository
        private set

    lateinit var appreciationRepository: AppreciationRepository
        private set

    lateinit var userRoleRepository: UserRoleRepository
        private set

    lateinit var learningRepository: LearningRepository
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var badgeManager: BadgeManager
        private set

    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Local Data
        userPreferences = UserPreferences(this)

        // 2. Initialize Firebase (This guarantees it's ready before grabbing instances)
        FirebaseApp.initializeApp(this)

        // 3. Get Firebase Instances
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // 4. Initialize Repositories
        authRepository = AuthRepository(auth)
        userRoleRepository = UserRoleRepository(firestore, userPreferences)
        firestoreRepository = FirestoreRepository(firestore)
        guruProfileRepository = GuruProfileRepository(firestore)
        guruChatRepository = GuruChatRepository(firestore)
        classSessionRepository = ClassSessionRepository(firestore)
        classroomRepository = ClassroomRepository(firestore)
        appreciationRepository = AppreciationRepository(firestore)
        learningRepository = LearningRepository(firestore)
        taskRepository = TaskRepository(firestore)
        badgeManager = BadgeManager(firestore)
    }
}
