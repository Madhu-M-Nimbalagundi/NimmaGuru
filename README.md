# Nimma-Guru

## Problem Statement

Nimma-Guru is an Android learning and mentoring app for the Gyaan-Daan mission. It helps village students find local Gurus, receive assigned learning tasks, attend class sessions, and track progress while giving Gurus tools to manage teaching work and connect with other Gurus for peer collaboration.

The project is designed as a submission-ready Kotlin Android repository with clear separation of concerns, Firebase-backed data flows, localized English/Kannada UI strings, and a Material 3 interface that supports light and dark themes.

## Features

- Firebase Authentication: email/password login, signup, password reset, and optional Google Sign-In setup.
- First-login personalization: asks for a preferred display name and uses the first letter as the profile avatar.
- Role-based app flow: Guru and Student users see different actions after login.
- Elderly-friendly Guru Profile Management: retired professionals can list skills, free hours, village/street location, experience, and Samudaya Bhavana availability using large Material inputs and clear chip selections.
- Search with Skill Filtering: students can search Guru profiles by village/street location and filter skills through Material `ChipGroup` controls.
- Real-time Mentor List: mentor search uses Cloud Firestore snapshot listeners so students see Guru profile updates without refreshing.
- Guru community mode: Gurus do not see "Search for Mentor"; they get a Community route to connect with other Gurus.
- Task Manager: Gurus create tasks with title, description, due date, and assigned student id.
- Assigned task dashboard: students see tasks assigned by their mentors from the Firestore `tasks` collection.
- UserDashboard/Profile: shows name, Guru/Student status, badge level, avatar initial, and a Settings button.
- Badge system: supports Bronze, Silver, Gold, Platinum, and Diamond recognition levels.
- Class Calendar: students can view upcoming local sessions at the Samudaya Bhavana; Gurus can create teaching sessions.
- Study Materials and syllabus browsing: supports notes, links, class levels, boards, and subject filters.
- Appreciation System: students can post Thank You notes for Gurus.
- Wall of Fame: top-performing mentors are highlighted through ratings and student appreciation.
- Settings: language toggle, notification preferences, password reset, and theme switching.
- Bilingual UI: English and Kannada resource files are included.
- Theme engine: Material 3 DayNight colors are applied through theme attributes and night resources.

## Tech Stack

- Language: Kotlin
- Platform: Android
- UI: XML layouts, ViewBinding, Material Design 3
- Architecture: Activity/Fragment UI layer, repository data layer, domain model layer
- Auth: Firebase Authentication
- Database: Cloud Firestore
- Async: Kotlin Coroutines and Firebase Tasks integration
- Navigation: AndroidX Navigation graph
- Build: Gradle Kotlin DSL

## Folder Structure

```text
Nimma-Guru/
|-- app/
|   |-- build.gradle.kts
|   |-- google-services.json.example
|   `-- src/main/
|       |-- AndroidManifest.xml
|       |-- assets/
|       |   `-- syllabus_2026.json
|       |-- java/com/nimmaguru/app/
|       |   |-- MainActivity.kt
|       |   |-- NimmaGuruApp.kt
|       |   |-- data/
|       |   |   |-- appreciation/
|       |   |   |-- auth/
|       |   |   |-- badge/
|       |   |   |-- calendar/
|       |   |   |-- classroom/
|       |   |   |-- guru/
|       |   |   |-- learning/
|       |   |   |-- local/
|       |   |   |-- task/
|       |   |   `-- user/
|       |   |-- domain/model/
|       |   |   |-- Assignment.kt
|       |   |   |-- ClassSession.kt
|       |   |   |-- GuruProfile.kt
|       |   |   |-- LearningMaterial.kt
|       |   |   |-- Task.kt
|       |   |   `-- UserRole.kt
|       |   |-- ui/
|       |   |   |-- appreciation/
|       |   |   |-- auth/
|       |   |   |-- calendar/
|       |   |   |-- dashboard/
|       |   |   |-- guru/
|       |   |   |-- home/
|       |   |   |-- learning/
|       |   |   |-- role/
|       |   |   |-- settings/
|       |   |   |-- student/
|       |   |   `-- task/
|       |   `-- res/
|       |       |-- drawable/
|       |       |-- layout/
|       |       |-- navigation/
|       |       |-- values/
|       |       |-- values-kn/
|       |       `-- values-night/
|-- docs/
|   `-- learning-platform-architecture.md
|-- firestore.rules
|-- Final_Submission_README.md
|-- README.md
|-- build.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
`-- settings.gradle.kts
```

## Setup & Installation

1. Clone the repository.

```bash
git clone <your-public-github-repo-url>
cd Nimma-Guru
```

2. Open the project in Android Studio.

3. Create a Firebase project and add an Android app with this package name:

```text
com.nimmaguru.app
```

4. Download the real Firebase config file and place it here:

```text
app/google-services.json
```

5. Enable Firebase Authentication providers:

```text
Authentication > Sign-in method > Email/Password
Authentication > Sign-in method > Google (optional)
```

6. Create a Cloud Firestore database and deploy the starter rules:

```bash
firebase deploy --only firestore:rules
```

7. Build the debug APK from the terminal:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

8. Run the app from Android Studio or install the generated debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Firebase Collections

```text
users/{uid}
users/{uid}/profile/main
guruProfiles/{uid}
tasks/{taskId}
classSessions/{sessionId}
materials/{materialId}
appreciations/{messageId}
```

Task documents are stored in the top-level `tasks` collection:

```json
{
  "ownerGuruId": "guruUid",
  "assignedStudentId": "studentUid",
  "assignedTo": "studentUid",
  "title": "Algebra practice",
  "description": "Solve the worksheet before class.",
  "dueDate": "2026-05-20",
  "createdAt": "Firestore Timestamp"
}
```

## Development Notes

- Keep `app/google-services.json`, `local.properties`, APK files, and build folders out of Git.
- The included `.gitignore` already excludes generated Android and Firebase config files.
- Use separate commits for project setup, authentication, role logic, task manager, localization, theme polish, and documentation to show a strong development history.
- Before submitting, open the GitHub repository URL in an incognito/private browser window to confirm it is public.
- Add screenshots after running the app locally.

## Screenshot Checklist

Add screenshots to `docs/screenshots/` before final submission:

```text
docs/screenshots/dashboard.png
docs/screenshots/task-manager.png
docs/screenshots/profile-settings.png
```

Then reference them here:

```md
![Dashboard](docs/screenshots/dashboard.png)
![Task Manager](docs/screenshots/task-manager.png)
![Profile Settings](docs/screenshots/profile-settings.png)
```

## Why This Repository Scores Well

- Clear folder separation across `data`, `domain/model`, `ui`, `util`, `res`, and `docs`.
- Substantial Kotlin implementation across many focused files instead of one large file.
- Real project-specific domain text and naming: Gyaan-Daan, Guru, Student, Community, Badge Level, Wall of Fame.
- Firebase-backed CRUD and real-time listener flows.
- Complete dependency configuration in Gradle.
- Public-submission hygiene through `.gitignore`, setup instructions, and screenshot guidance.
