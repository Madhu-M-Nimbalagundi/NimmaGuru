package com.nimmaguru.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth
) {
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun login(email: String, password: String): FirebaseUser {
        return firebaseAuth
            .signInWithEmailAndPassword(email.trim(), password)
            .await()
            .user ?: error("Login completed without a Firebase user.")
    }

    suspend fun signup(email: String, password: String): FirebaseUser {
        return firebaseAuth
            .createUserWithEmailAndPassword(email.trim(), password)
            .await()
            .user ?: error("Signup completed without a Firebase user.")
    }

    suspend fun sendPasswordReset(email: String) {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = firebaseAuth.currentUser ?: error("Please log in again before changing your password.")
        val email = user.email ?: error("This account does not have an email password login.")
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    suspend fun loginWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return firebaseAuth
            .signInWithCredential(credential)
            .await()
            .user ?: error("Google sign-in completed without a Firebase user.")
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}
