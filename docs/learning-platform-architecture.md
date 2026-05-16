# Learning Platform Architecture

This app should use one canonical learning data model instead of storing class progress and assignments inside each student document. The current Firebase stack can support the required backend through Cloud Firestore collections, repositories, and optional Cloud Functions that expose REST endpoints.

## Database Schema

Use Firebase Auth UID as the primary user id. Store all timestamps as Firestore `Timestamp`.

### `users/{userId}`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `uid` | string | yes | Firebase Auth UID. |
| `email` | string | yes | Unique through Firebase Auth. |
| `displayName` | string | no | Public name. |
| `role` | string | yes | `student`, `guru`, or `admin`. |
| `phone` | string | no | Optional contact. |
| `createdAt` | timestamp | yes | Created once. |
| `updatedAt` | timestamp | yes | Updated on profile changes. |

Indexes:

- `role ASC`

### `guruProfiles/{guruId}`

Document id must match `users/{guruId}`.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `uid` | string | yes | FK to `users.uid`. |
| `bio` | string | no | Guru introduction. |
| `specializations` | array<string> | yes | Searchable skills/subjects. |
| `experienceYears` | number | no | Teaching experience. |
| `ratingAverage` | number | yes | Denormalized from `reviews`. |
| `ratingCount` | number | yes | Denormalized from `reviews`. |
| `availableTime` | string | no | Display text. |
| `location` | string | no | Display text. |
| `locationNormalized` | string | no | Lowercase exact-search value. |
| `updatedAt` | timestamp | yes | Last profile update. |

Indexes:

- `specializations ARRAY_CONTAINS`
- `locationNormalized ASC`

### `studentProfiles/{studentId}`

Document id must match `users/{studentId}`.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `uid` | string | yes | FK to `users.uid`. |
| `gradeLevel` | string | no | Example: `Class 8`. |
| `learningGoals` | array<string> | no | Student goals/interests. |
| `guardianName` | string | no | Optional. |
| `guardianPhone` | string | no | Optional. |
| `updatedAt` | timestamp | yes | Last profile update. |

### `classes/{classId}`

This replaces the overloaded `classSessions` model. A class is the scheduled learning unit created by a guru.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | yes | Document id copied into field for client mapping. |
| `guruId` | string | yes | FK to `users.uid`; creator must be a guru. |
| `title` | string | yes | Class title. |
| `subject` | string | yes | Search/filter subject. |
| `description` | string | no | Class details. |
| `scheduledStartAt` | timestamp | yes | Planned start. |
| `scheduledEndAt` | timestamp | yes | Planned end. |
| `durationMinutes` | number | yes | Derived or validated from start/end. |
| `maxStudents` | number | yes | Enrollment cap. |
| `isLive` | boolean | yes | True while a live session is active. |
| `status` | string | yes | `upcoming`, `ongoing`, `completed`, `cancelled`. |
| `createdAt` | timestamp | yes | Created time. |
| `updatedAt` | timestamp | yes | Last update. |

Indexes:

- `guruId ASC, scheduledStartAt DESC`
- `status ASC, scheduledStartAt ASC`
- `isLive ASC, scheduledStartAt ASC`
- `subject ASC, scheduledStartAt ASC`

### `enrollments/{enrollmentId}`

Use deterministic id `${classId}_${studentId}` to prevent duplicate enrollments.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | yes | `${classId}_${studentId}`. |
| `classId` | string | yes | FK to `classes.id`. |
| `studentId` | string | yes | FK to `users.uid`. |
| `enrollmentDate` | timestamp | yes | Enrollment time. |
| `status` | string | yes | `enrolled`, `completed`, `cancelled`. |

Indexes:

- Unique by deterministic document id.
- `classId ASC, status ASC`
- `studentId ASC, status ASC`

### `assignments/{assignmentId}`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | yes | Document id. |
| `classId` | string | yes | FK to `classes.id`. |
| `guruId` | string | yes | Denormalized owner for rules and queries. |
| `title` | string | yes | Assignment title. |
| `description` | string | yes | Assignment instructions. |
| `dueDate` | timestamp | yes | Due date/time. |
| `maxScore` | number | yes | Default `100`. |
| `createdAt` | timestamp | yes | Created time. |
| `updatedAt` | timestamp | yes | Last update. |

Indexes:

- `classId ASC, dueDate ASC`
- `guruId ASC, dueDate DESC`

### `assignmentSubmissions/{submissionId}`

Use deterministic id `${assignmentId}_${studentId}` to allow one active submission per assignment/student.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | yes | `${assignmentId}_${studentId}`. |
| `assignmentId` | string | yes | FK to `assignments.id`. |
| `classId` | string | yes | Denormalized for progress queries. |
| `studentId` | string | yes | FK to `users.uid`. |
| `submittedAt` | timestamp | yes | Last submitted time. |
| `submissionText` | string | no | Text answer or link. |
| `attachmentUrl` | string | no | Optional uploaded work URL. |
| `status` | string | yes | `submitted`, `graded`, `returned`. |
| `score` | number | no | Null until graded. |
| `feedback` | string | no | Guru feedback. |
| `gradedAt` | timestamp | no | Score time. |

Indexes:

- Unique by deterministic document id.
- `studentId ASC, classId ASC`
- `assignmentId ASC, studentId ASC`
- `classId ASC, studentId ASC`

### `liveSessions/{liveSessionId}`

Usually one active live session per class. If classes can recur, create one live session document per meeting.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | yes | Document id. |
| `classId` | string | yes | FK to `classes.id`. |
| `guruId` | string | yes | Denormalized owner. |
| `startTime` | timestamp | yes | Actual live start. |
| `endTime` | timestamp | yes | Actual or planned end. |
| `streamUrl` | string | no | Video URL. |
| `roomId` | string | no | Meeting room id. |
| `isActive` | boolean | yes | True only while joinable. |
| `createdAt` | timestamp | yes | Created time. |
| `updatedAt` | timestamp | yes | Last update. |

Indexes:

- `classId ASC, startTime DESC`
- `guruId ASC, isActive ASC`
- `isActive ASC, startTime ASC, endTime ASC`

### `attendance/{attendanceId}`

Use deterministic id `${liveSessionId}_${studentId}`. If attendance is tracked per class only, use `${classId}_${studentId}`, but per live session is more accurate.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | yes | `${liveSessionId}_${studentId}`. |
| `classId` | string | yes | FK to `classes.id`. |
| `liveSessionId` | string | yes | FK to `liveSessions.id`. |
| `studentId` | string | yes | FK to `users.uid`. |
| `status` | string | yes | `present` or `absent`. |
| `joinedAt` | timestamp | no | Set when student joins. |
| `leftAt` | timestamp | no | Optional. |
| `updatedAt` | timestamp | yes | Last update. |

Indexes:

- Unique by deterministic document id.
- `classId ASC, studentId ASC`
- `liveSessionId ASC, status ASC`
- `studentId ASC, status ASC`

### `reviews/{reviewId}`

Use deterministic id `${guruId}_${studentId}_${classId}` if reviews should be one per class; otherwise generated ids allow multiple reviews.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | yes | Document id. |
| `guruId` | string | yes | FK to guru user. |
| `studentId` | string | yes | FK to student user. |
| `classId` | string | no | Optional class context. |
| `rating` | number | yes | Integer 1-5. |
| `comment` | string | no | Review text. |
| `createdAt` | timestamp | yes | Created time. |
| `updatedAt` | timestamp | yes | Last update. |

Indexes:

- `guruId ASC, createdAt DESC`
- `studentId ASC, createdAt DESC`

## Backend Logic

If the app stays Firebase-first, implement these as repository methods in Android plus Firestore rules. If a REST backend is added, expose the same operations through Cloud Functions or an Express/Ktor service.

### Auth and Users

| Endpoint | Method | Role | Behavior |
| --- | --- | --- | --- |
| `/users/me` | GET | signed-in | Return current user and role profile. |
| `/users/{id}/role` | PATCH | admin or self during onboarding | Set `student`, `guru`, or `admin`. |
| `/gurus/{id}/profile` | PUT | guru owner/admin | Upsert `guruProfiles/{id}`. |
| `/students/{id}/profile` | PUT | student owner/admin | Upsert `studentProfiles/{id}`. |

### Classes

| Endpoint | Method | Role | Behavior |
| --- | --- | --- | --- |
| `/classes` | POST | guru/admin | Create class with `guruId`, schedule, capacity, and `status=upcoming`. |
| `/classes` | GET | signed-in | List classes; filter by `guruId`, `subject`, `status`, `isLive`. |
| `/classes/{id}` | GET | signed-in | Return class detail, guru, assignments, live session summary. |
| `/classes/{id}` | PATCH | class guru/admin | Update schedule/details/status. |

Validation:

- `scheduledEndAt > scheduledStartAt`.
- `maxStudents > 0`.
- Creator role must be `guru` or `admin`.

### Enrollments

| Endpoint | Method | Role | Behavior |
| --- | --- | --- | --- |
| `/classes/{id}/enrollments` | POST | student/admin | Create `enrollments/{classId_studentId}` if capacity allows. |
| `/classes/{id}/enrollments` | GET | class guru/admin | List enrolled students. |
| `/students/{id}/enrollments` | GET | student owner/admin | List joined classes. |

Enrollment count must come from `enrollments where classId == id and status in ['enrolled','completed']`, not arrays on class documents.

### Assignments

| Endpoint | Method | Role | Behavior |
| --- | --- | --- | --- |
| `/classes/{id}/assignments` | POST | class guru/admin | Create assignment. |
| `/classes/{id}/assignments` | GET | enrolled student/class guru/admin | List assignments. |
| `/assignments/{id}/submissions` | POST | enrolled student | Create/update own submission. |
| `/submissions/{id}/score` | PATCH | class guru/admin | Record `score`, `feedback`, `status=graded`. |

Progress calculation:

```text
progressPercent = totalAssignments == 0
  ? 0
  : round(submittedAssignments / totalAssignments * 100)

averageScore = average(non-null submission.score for that student and class)
```

### Live Sessions and Attendance

| Endpoint | Method | Role | Behavior |
| --- | --- | --- | --- |
| `/classes/{id}/live-sessions/start` | POST | class guru/admin | Create active live session and set `classes/{id}.isLive=true`, `status=ongoing`. |
| `/live-sessions/{id}/end` | POST | session guru/admin | Set `isActive=false`, set end time, set class `isLive=false`, `status=completed`. |
| `/students/{id}/live-sessions/ongoing` | GET | student owner/admin | Return only enrolled classes whose live session is active and current time is between `startTime` and `endTime`. |
| `/live-sessions/{id}/join` | POST | enrolled student | Create/update attendance as `present`, set `joinedAt`, return `streamUrl` or `roomId`. |
| `/classes/{id}/attendance` | GET | class guru/admin | List present/absent students per live session. |

Ongoing live-session filter:

```text
enrolledClassIds = enrollments where studentId == currentUser and status == enrolled
liveSessions where isActive == true
  and startTime <= now
  and endTime >= now
  and classId in enrolledClassIds
```

Firestore limits `in` queries to a bounded number of ids, so chunk class ids when needed.

### Reviews

| Endpoint | Method | Role | Behavior |
| --- | --- | --- | --- |
| `/gurus/{id}/reviews` | POST | enrolled/completed student | Create review with rating 1-5. |
| `/gurus/{id}/reviews` | GET | signed-in | Return average and review list. |

On review write, update `guruProfiles/{guruId}.ratingAverage` and `ratingCount` in a transaction or Cloud Function.

### Admin Dashboard

| Endpoint | Method | Role | Behavior |
| --- | --- | --- | --- |
| `/admin/gurus` | GET | admin | List all `users where role == guru` plus profiles. |
| `/admin/students` | GET | admin | List all `users where role == student` plus profiles. |
| `/admin/classes` | GET | admin | List all classes with enrollment count and enrolled students. |

Admin class summary:

```text
totalClassesCreated = count(classes)
for each class:
  enrollmentCount = count(enrollments where classId == class.id and status == enrolled)
  enrolledStudents = join enrollments.studentId -> users/studentProfiles
```

### Guru Dashboard

Repository/service should produce:

| Metric | Source |
| --- | --- |
| Classes created | `classes where guruId == currentGuruId` |
| Classes taken live | `classes where guruId == currentGuruId and isLive == true and scheduledEndAt < now`, or better `liveSessions where guruId == currentGuruId and isActive == false and endTime < now` |
| Total students enrolled | Count unique `studentId` across enrollments for guru class ids. |
| Attendance per class | `attendance where classId in guruClassIds`, joined to `users`. |
| Average rating and reviews | `reviews where guruId == currentGuruId`, plus denormalized profile rating fields. |

Replace the guru “My progress” area with:

- Reviews header: `ratingAverage`, `ratingCount`.
- Review list: student name, rating, comment, created date.
- Classes taken count.
- Attendance table: class title, student name, present/absent, joined time.

### Student Dashboard

Repository/service should produce:

| Section | Source |
| --- | --- |
| Per-class progress | Enrollments -> assignments -> submissions. |
| Classes joined | `enrollments where studentId == currentStudentId`, joined to `classes`. |
| Assignments | Assignments for enrolled classes, left-joined to submissions. |
| Average score | Average of non-null graded submission scores. |
| Ongoing live sessions | Active `liveSessions` for enrolled class ids where `startTime <= now <= endTime`. |

Class status:

```text
if liveSession.isActive && now between startTime/endTime -> ongoing
else if class.scheduledEndAt < now || enrollment.status == completed -> completed
else -> upcoming
```

Assignment status:

```text
submitted if assignmentSubmissions/{assignmentId_studentId} exists
pending otherwise
```

## Android Code Structure

Keep the current Activity + XML + ViewModel pattern.

```text
app/src/main/java/com/nimmaguru/app/
|-- domain/model/
|   |-- User.kt
|   |-- GuruProfile.kt
|   |-- StudentProfile.kt
|   |-- LearningClass.kt
|   |-- Enrollment.kt
|   |-- Assignment.kt
|   |-- AssignmentSubmission.kt
|   |-- LiveSession.kt
|   |-- Attendance.kt
|   |-- Review.kt
|   |-- StudentDashboard.kt
|   |-- GuruDashboard.kt
|   `-- AdminDashboard.kt
|-- data/
|   |-- user/UserRepository.kt
|   |-- profile/ProfileRepository.kt
|   |-- classes/ClassRepository.kt
|   |-- enrollment/EnrollmentRepository.kt
|   |-- assignments/AssignmentRepository.kt
|   |-- live/LiveSessionRepository.kt
|   |-- attendance/AttendanceRepository.kt
|   |-- reviews/ReviewRepository.kt
|   `-- dashboard/DashboardRepository.kt
|-- ui/
|   |-- admin/
|   |   |-- AdminDashboardActivity.kt
|   |   |-- AdminDashboardViewModel.kt
|   |   |-- AdminGuruAdapter.kt
|   |   |-- AdminStudentAdapter.kt
|   |   `-- AdminClassAdapter.kt
|   |-- guru/
|   |   |-- GuruDashboardActivity.kt
|   |   |-- GuruDashboardViewModel.kt
|   |   |-- GuruReviewAdapter.kt
|   |   `-- GuruAttendanceAdapter.kt
|   `-- student/
|       |-- StudentDashboardActivity.kt
|       |-- StudentDashboardViewModel.kt
|       |-- StudentClassAdapter.kt
|       |-- StudentAssignmentAdapter.kt
|       `-- OngoingLiveSessionAdapter.kt
```

## Firestore Repository Responsibilities

### `ClassRepository`

- `createClass(input, guruId)`
- `listenClasses(filter)`
- `listenGuruClasses(guruId)`
- `getClassesByIds(classIds)`
- `markLive(classId, isLive, status)`

### `EnrollmentRepository`

- `enrollStudent(classId, studentId)`
- `listenEnrollmentsForClass(classId)`
- `listenEnrollmentsForStudent(studentId)`
- `countEnrollments(classId)`
- `getUniqueStudentCountForClasses(classIds)`

### `AssignmentRepository`

- `createAssignment(classId, guruId, input)`
- `listenAssignmentsForClass(classId)`
- `listenAssignmentsForClasses(classIds)`
- `submitAssignment(assignmentId, classId, studentId, text, attachmentUrl)`
- `gradeSubmission(submissionId, score, feedback)`
- `listenSubmissionsForStudent(studentId)`

### `LiveSessionRepository`

- `startSession(classId, guruId, streamUrl, roomId, startTime, endTime)`
- `endSession(liveSessionId)`
- `listenOngoingSessionsForStudent(studentId)`
- `listenGuruLiveHistory(guruId)`

### `AttendanceRepository`

- `markPresent(liveSessionId, classId, studentId)`
- `markAbsentForMissingStudents(liveSessionId, classId)`
- `listenAttendanceForClass(classId)`
- `listenAttendanceForStudent(studentId)`

### `ReviewRepository`

- `submitReview(guruId, studentId, classId, rating, comment)`
- `listenReviewsForGuru(guruId)`
- `recalculateGuruRating(guruId)`

### `DashboardRepository`

Compose the above repositories into UI-ready objects:

- `listenStudentDashboard(studentId): StudentDashboard`
- `listenGuruDashboard(guruId): GuruDashboard`
- `listenAdminDashboard(): AdminDashboard`

## Frontend Dashboard Components

### Guru dashboard UI

Screen blocks:

- Header cards: classes created, classes taken, total students, average rating.
- Reviews section: average rating and list of `Review`.
- Attendance section: grouped by class, with student name and present/absent status.
- Class list section: class title, schedule, enrolled count, live/completed status.

### Student dashboard UI

Screen blocks:

- Average score card across graded submissions.
- Ongoing live sessions list with `Join Live Session` button.
- Joined classes list with `upcoming`, `ongoing`, or `completed`.
- Progress list per enrolled class.
- Assignment list with due date, pending/submitted status, score, and feedback.

Join button behavior:

1. Call `LiveSessionRepository.markPresent(...)`.
2. Open `streamUrl` if present.
3. Otherwise open an in-app room route keyed by `roomId`.

### Admin dashboard UI

Screen blocks:

- Summary: total gurus, total students, total classes.
- Guru list.
- Student list.
- Class table/list: class title, guru, schedule, enrollment count, enrolled students.

## Migration From Current Data

1. Keep existing `users` and `guruProfiles`.
2. Create `studentProfiles` for student users.
3. Rename or migrate `classSessions` into `classes`:
   - `mentor` -> `guruName` only if needed for display.
   - Add `guruId`, `title`, `durationMinutes`, `maxStudents`, `status`, `isLive`.
4. Stop using `users/{uid}/assignments` for class assignments.
5. Stop using `users/{uid}/progress/summary` as a source of truth.
6. Compute progress from `assignments`, `assignmentSubmissions`, `enrollments`, and `attendance`.

## Security Rules Shape

Rules should enforce:

- Students can read their own enrollments, submissions, attendance, and enrolled class assignments.
- Gurus can create classes, assignments, live sessions, and attendance only for their own classes.
- Admins can read all dashboard data and manage any class/profile.
- Reviews can be created by students, but rating aggregates should be updated by trusted server logic.

The existing `firestore.rules` should be expanded for the new top-level collections before these screens are shipped.
