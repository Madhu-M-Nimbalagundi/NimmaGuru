# Nimma-Guru PRD

## Project Title

Nimma-Guru

## Short Description

Nimma-Guru is a bilingual Android app that connects village students with retired professionals and local mentors, called Gurus. Students can search for nearby mentors by skill and location, view local class sessions, post Thank You notes, and follow learning tasks. Gurus can create profiles, list their skills and available hours, manage teaching tasks, and connect with other Gurus through real-time community chat.

## Problem Statement

Many rural students need academic support but do not always have easy access to nearby teachers, tutors, or subject experts. At the same time, retired professionals and skilled community members often have valuable knowledge and free time but no simple local platform to offer guidance.

Nimma-Guru solves this gap by creating a digital community mentoring platform where students can find trusted local Gurus in their village or street, attend sessions at community spaces such as the Samudaya Bhavana, and appreciate mentors publicly through Thank You notes and Wall of Fame recognition.

## Technologies Used

- Kotlin
- Android SDK
- XML layouts
- ViewBinding
- Material Design Components
- Firebase Authentication
- Cloud Firestore
- Firestore real-time snapshot listeners
- Google Sign-In integration
- AndroidX AppCompat
- AndroidX RecyclerView
- AndroidX Lifecycle ViewModel and LiveData
- Kotlin Coroutines
- Gradle Kotlin DSL
- English and Kannada Android string resources

## Product Goals

- Help students discover Gurus in their own village, street, or nearby community.
- Allow retired professionals to easily register their skills and free teaching hours.
- Support real-time mentor discovery using Firestore.
- Encourage community learning through class sessions at Samudaya Bhavana.
- Build respect and motivation through Thank You notes and Wall of Fame recognition.
- Support both Kannada and English users.
- Keep the interface simple, readable, and elderly-friendly.

## Target Users

- Village students who need help with subjects such as Mathematics, Science, English, Kannada, Coding, Music, Yoga, or vocational skills.
- Retired teachers, professionals, and skilled elders who want to volunteer time as Gurus.
- Community organizers who want to encourage local learning and mentorship.

## Core Features

### 1. Guru Profile Management

Gurus can create and update a profile with:

- Name
- Skills
- Experience
- Available free hours
- Village or street location
- Samudaya Bhavana availability
- Samudaya Bhavana address

The screen uses large Material input fields and chip selections so elderly users can navigate it comfortably.

### 2. Search With Skill Filtering

Students can search for mentors using:

- Village or street location
- Skill filters implemented with Material `ChipGroup`

This helps students quickly find Gurus for specific subjects or practical skills.

### 3. Real-Time Mentor List

The mentor list is powered by Cloud Firestore real-time listeners. When Guru profiles are added or updated, students can see the latest mentor list without manually refreshing.

### 4. Class Calendar

Students can view upcoming sessions, including local sessions at Samudaya Bhavana. Gurus can create teaching sessions with subject, date, time, location, class level, and board type.

### 5. Appreciation System

Students can post Thank You notes to show gratitude for Gurus. These notes support a culture of respect and recognition.

### 6. Bilingual UI

The app supports:

- English
- Kannada

Android string resources are provided for both languages.

### 7. Wall of Fame

The Wall of Fame highlights top-performing Gurus using appreciation and rating-related data. This helps recognize mentors who contribute strongly to the community.

### 8. Guru Community Chat

Gurus can connect with other Gurus in real time. Each Guru card in the community screen has a Message action that opens a Firestore-backed chat screen.

## User Flow

### Student Flow

1. Student opens the app.
2. Student signs up or logs in.
3. Student selects the Student role.
4. Student searches for Gurus by skill and location.
5. Student views real-time mentor results.
6. Student checks upcoming classes at Samudaya Bhavana.
7. Student follows assigned tasks and materials.
8. Student posts Thank You notes for Gurus.

### Guru Flow

1. Guru opens the app.
2. Guru signs up or logs in.
3. Guru selects the Guru role.
4. Guru creates a profile with skills, free hours, and location.
5. Guru creates class sessions or tasks.
6. Guru views community activity.
7. Guru connects and chats with other Gurus.
8. Guru receives appreciation and Wall of Fame recognition.

## Functional Requirements

- The app must allow email/password authentication.
- The app must support role-based navigation for Student and Guru users.
- The app must allow Gurus to save profile details in Firestore.
- The app must allow skill selection using `ChipGroup`.
- The app must display mentors in real time using Firestore snapshot listeners.
- The app must allow students to search by village or street location.
- The app must show upcoming class sessions.
- The app must include Samudaya Bhavana session/location support.
- The app must allow Thank You note posting.
- The app must include a Wall of Fame screen.
- The app must include English and Kannada UI resources.
- The app must allow Gurus to send real-time messages to other Gurus.

## Non-Functional Requirements

- The app should build successfully using Gradle.
- The interface should be readable and easy for elderly users.
- The app should support Android 6.0 and newer.
- The app should support different screen sizes, including phones and tablets.
- The repository should include real Android source code, XML layouts, Gradle files, and README documentation.
- Code should be organized into modular packages such as `data`, `domain/model`, `ui`, and `util`.

## Success Criteria

- A Guru can create a profile with skills and free hours.
- A student can filter mentors using skill chips.
- Mentor search results update in real time from Firestore.
- Students can view sessions at Samudaya Bhavana.
- Students can post Thank You notes.
- The app can switch between English and Kannada resources.
- Wall of Fame is accessible from the UI.
- Gurus can message other Gurus in real time.
- The project builds successfully without errors.

## Repository Deliverables

- Kotlin source files
- XML layout files
- Gradle build files
- Android manifest
- Firebase configuration support
- Firestore rules
- README documentation
- PRD documentation

## Current Build Status

The project has been verified with a successful debug build using:

```bash
gradlew assembleDebug
```

The generated APK is available in:

```text
app/build/outputs/apk/debug/app-debug.apk
```
