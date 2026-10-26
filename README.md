# Cyber Dates

An offline, cyberpunk-styled Android app for tracking any label with any number of custom-named dates
(bought on, expires on, renewal due…), with local expiry notifications. No accounts, no cloud, no ads,
no analytics, no network access.

> **Build status: NOT compiled yet.** This project was written without access to an Android toolchain
> (no JDK/SDK/Gradle on the authoring machine). Nothing has been built or run. The first build
> (GitHub Actions or local) is the first compile, so expect that a small fix may be needed.
>

## Get the APK for free using GitHub (no installs)

1. Create a new GitHub repository and upload everything in this folder
   (keep the `.github/workflows/build-apk.yml` path exactly; hidden folders must be included).
   With git: `git init && git add . && git commit -m "Cyber Dates" && git remote add origin <url> && git push -u origin main`.
2. Open the repo's **Actions** tab → **Build APK** → wait about 5–8 minutes (it starts on push; or use *Run workflow*).
3. Open the finished run → **Artifacts** → download `cyber-date-tracker-debug-apk` (a zip containing `app-debug.apk`).
4. Unit tests run after the APK upload (failures don't block the APK); their report is the `unit-test-report` artifact.

## Build locally

Requirements: JDK 17, Android SDK (platform 35, build-tools), Gradle 8.9 (or open the folder in Android Studio,
which supplies all of this). A Gradle wrapper is not committed because it couldn't be generated here; create one with
`gradle wrapper --gradle-version 8.9` if you want `./gradlew`.

macOS / Linux:
```bash
gradle assembleDebug          # or ./gradlew assembleDebug after generating the wrapper
gradle testDebugUnitTest      # JVM unit tests
```
Windows (PowerShell / cmd):
```powershell
gradle assembleDebug
gradle testDebugUnitTest
```
Output: `app/build/outputs/apk/debug/app-debug.apk`.
Instrumented tests (need a device/emulator): `gradle connectedDebugAndroidTest`.

## Install on your phone

1. Copy `app-debug.apk` to the phone (USB, Drive, email…).
2. Open it from a file manager; allow "Install unknown apps" for that app when Android asks.
3. Debug-signed APKs install fine for personal use. Updating later requires the same signing key
   (GitHub builds use a fresh debug key each run, so uninstall first, or export a backup from Settings and restore it).

## Features

- Entries with a title, optional remarks, and zero or more date rows. Each row has your own label, a date, a type
  (ordinary or expiry/reminder) and, for expiry rows, a notification switch.
- Home dashboard: totals, a "Needs attention" section, search over titles/labels/remarks, animated list, empty state.
- Status (EXPIRED / EXPIRES TODAY / EXPIRING SOON ≤ 7 days / SAFE) is computed from `LocalDate` comparisons and
  refreshed when the app resumes. Only expiry rows show a status, and status always has a text label.
- Details screen with edit and confirmed delete. Editor warns before discarding and keeps drafts across process death.
- Settings: JSON export/restore (Storage Access Framework), notification status and shortcut to system settings.

### Shopping lists and timed reminders (v1.1)

The home screen has three tabs: **DATES**, **LISTS**, **REMINDERS**.
- **Lists:** create any number of named lists, add items, tick them off with checkboxes (ticked items move to a "Done" section
  with strike-through), remove single items, "Clear checked" (confirmed), rename or delete a list (confirmed).
  Changes save instantly; there is no Save button.
- **Reminders:** a title, optional note, a date and a time. You get a notification at that exact local time
  (Android may delay it a few minutes). Editing moves the alarm, deleting (confirmed) cancels it, and past reminders are listed
  under "Past" and never fire. They're rebuilt after reboot/time-zone change like the expiry reminders.
- Database moved from version 1 to 2 with a real migration (`AppDatabase.MIGRATION_1_2`) that only adds tables.
  This migration has not been run on a device.
- Backup is now format v2 and includes lists and reminders. Older v1 backups still restore (they replace entries only and
  leave lists/reminders alone).

## Architecture

MVVM with Kotlin coroutines/Flow, manual DI (`CyberApp.container`), Room (SQLite) with a foreign key from
`entry_dates.entryId` to `entries.id` (cascade) and indices on both. Dates are stored as ISO `yyyy-MM-dd` text.
Schema is exported to `app/schemas`; `AppDatabase.MIGRATIONS` is where future migrations go, and there is no destructive fallback.

```
data/{entity,dao,database,repository}   domain/ (status + ordering logic)   notifications/   backup/
ui/{theme,components,home,editor,details,settings}
```

Sorting: needs-attention entries first (soonest/most overdue date first), then entries with an upcoming date,
then the rest; ties break by title then id.

## Notifications

- Mechanism: `AlarmManager.setAndAllowWhileIdle` (inexact, Doze-friendly) → non-exported `ReminderReceiver` → notification.
  Chosen over WorkManager because reminders are tied to a calendar time of day, and over exact alarms because
  `SCHEDULE_EXACT_ALARM` is a restricted special access on Android 12+ that a 9 AM reminder doesn't justify.
  Android may deliver a few minutes late, and more so in battery saver / Doze or with aggressive vendor battery managers.
  If reminders never show, exclude the app from battery optimisation in system settings.
- For each expiry row with notifications on: 7 days before and on the day, at 9:00 AM in the device's *current* time zone.
  A 7-day reminder whose day is today or earlier is skipped; nothing is scheduled for past dates or for a 9:00 that already passed.
- Alarm identity: the row id + type (`<rowId>:ADVANCE|DUE`) encoded in the PendingIntent data URI, so rows can't collide.
  Editing re-syncs; disabling or deleting a row/entry cancels; restore cancels everything old then reschedules.
- Rescheduling happens on process start and on boot, app update, time change and time-zone change (`BootReceiver`, not exported).
  The alarm payload only holds the row id; the notification text is read from the database when it fires, so it's never stale.
- Tapping a notification opens that entry. Android 13+: the permission is requested only after a rationale dialog when you
  save an entry that has reminders. If denied the app works normally and Settings shows "Reminders are disabled".

## Backup format (version 1)

```json
{ "app": "cyber-date-tracker", "version": 1, "exportedAt": "2026-10-24T06:00:00Z",
  "entries": [ { "id": "…", "title": "Mirchi", "remarks": "…", "createdAt": 0, "updatedAt": 0,
      "dates": [ { "id": "…", "label": "Expires on", "date": "2026-10-31", "type": "EXPIRY",
                   "notificationEnabled": true, "createdAt": 0, "updatedAt": 0 } ] } ] }
```
Restore parses and validates the whole file first (JSON syntax, app id, version, required fields and types, ISO dates,
date types, duplicate ids, length/count limits, 5 MB cap) and only then replaces all data in one Room transaction. It asks for confirmation first.
Uses Android's built-in `org.json`; the file is only parsed as data. The app declares no INTERNET permission.

## Tests (written, not yet run)

JVM (`app/src/test`): expiry boundaries (past, today, +1, +7, +8), optional dates/remarks, multi-row entries, ordering, search,
reminder planning, stable IDs, date-change/disable/delete cancellation, time-zone recomputation, backup round-trip and rejection of
malformed/invalid/oversized files.
Instrumented (`app/src/androidTest`): Room insert/update/delete, alarm cancellation on delete, a failed restore leaving the
database unchanged, and two Compose checks (status badge text, empty state). There are no full-screen Compose flow tests yet.
