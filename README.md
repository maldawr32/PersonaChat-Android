# Chat Simulator

An Android 10+ local chat/call simulation app built entirely through GitHub Actions. Android Studio is not required on the user's computer.

## Safety and purpose

This project is a UI/testing simulator. The app deliberately labels generated conversations and calls as **SIMULATION / FICTIONAL DATA**. It does not connect to WhatsApp, does not write fake entries into the system phone/SMS apps, and should not be used to impersonate real people or fabricate evidence.

## Current features

- Android 10+ (`minSdk 29`), `compileSdk 36`.
- Fictional local bot contacts and editable phone numbers/calling-code prefix.
- Default `+963` fictional demo numbers using a deliberately non-real `000` range.
- Hundreds of unread counters and a resettable large demo dataset.
- Automatic routine replies with keyword-aware responses and configurable active hours.
- Bot profile image selection from the device.
- Scheduled incoming simulated messages using `AlarmManager`.
- Recent and older simulated call history.
- Scheduled simulated incoming calls with a full-screen/heads-up notification depending on Android permission policy.
- Separate selectable notification sound and simulated call ringtone.
- Three launcher icon choices.
- System light/dark theme support.
- Local-only storage with `SharedPreferences`/JSON; no server is required.

## Build the APK on GitHub

Every push to `main` or `feature/**` runs `.github/workflows/android.yml`.

1. Open the repository **Actions** tab.
2. Open the latest **Android APK** workflow run.
3. After it succeeds, download the `chat-simulator-debug` artifact.
4. Extract the ZIP and install `app-debug.apk` on Android 10 or newer. Android may ask you to allow installation from the browser/file manager you use.

The workflow uses Java 17, Gradle 8.13, Android Gradle Plugin 8.13.2, and Android SDK 36.

## Notes about newer Android versions

Full-screen call-style notifications are increasingly restricted by Android. If the OS does not allow the simulated call screen to open automatically, the high-priority call notification remains available to tap. Notification sounds are implemented with Android notification channels; choosing a new sound creates a new simulator channel because channel sound settings are immutable after creation.
