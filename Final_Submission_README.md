# Nimma-Guru Final Submission

## Gyaan-Daan Mission

Nimma-Guru is built around Gyaan-Daan: making knowledge sharing simple, local, and dignified. Students can discover Gurus, attend learning sessions, receive assignments, and track progress. Gurus can teach, upload materials, manage tasks, and collaborate with peers.

## Badge System

The app uses a Bronze to Diamond badge journey to recognize contribution and learning progress:

- Bronze: first profile setup and early participation.
- Silver: consistent learning, teaching, or material sharing.
- Gold: reliable mentoring and strong student engagement.
- Platinum: high community contribution and repeat impact.
- Diamond: exceptional Gyaan-Daan leadership.

## Peer-Guru Connection

Guru users no longer see "Search for Mentor." Instead, their Connect area becomes Community, a peer directory for connecting with other Gurus. This supports collaboration, shared teaching materials, and local mentorship planning.

## Submission-Ready Features

- Bottom navigation supports Dashboard, Tasks/Connect, and Profile.
- Profile opens a UserDashboard with preferred name, Guru/Student status, badge level, avatar initial, and Settings access.
- Student dashboards show assigned mentor tasks from Firestore.
- Guru Task Manager creates tasks with title, description, due date, and assigned student id in the `tasks` collection.
- First-login personalization captures preferred name and stores it in Firestore through the configured Firebase app.
- Language resources include English and Kannada strings for the new submission surfaces.
