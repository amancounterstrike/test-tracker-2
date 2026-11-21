# Test Tracker — Native Android App (Kotlin + Jetpack Compose)

A native, standalone Android application built with **Kotlin + Jetpack Compose (Material 3)**, offline-first **SQLite** storage, **90Hz–120Hz** high-refresh-rate display support, and zero WebView wrappers.

## How to Get Your Ready-to-Install `.apk` File (No Coding Needed)

1. **Unzip** `TestTracker-Android.zip` on your computer or phone.
2. Go to [github.com](https://github.com) and sign in (or create a free account).
3. Click the **`+`** icon in the top-right corner and select **New repository**.
   - Name it `test-tracker-app` (leave it **Public** or **Private**).
   - Click **Create repository**.
4. On the next page, click **uploading an existing file**.
5. Open the unzipped `TestTracker` folder, select **everything inside it** (including the hidden `.github` folder), and drag-and-drop all files into GitHub.
   - Tip: On Windows, turn on *View → Show → Hidden items* if you don't see `.github`. On Mac, press `Cmd + Shift + .` to show hidden folders.
6. Scroll down and click the green **Commit changes** button.
7. Click the **Actions** tab at the top of your repository.
   - You will see **Build Test Tracker APK** running automatically (takes ~2–4 minutes).
8. When it shows a green checkmark, click the run name, scroll to **Artifacts**, and click **Test-Tracker-APK** to download the zip containing **`Test-Tracker.apk`**.
9. Transfer **`Test-Tracker.apk`** to your Android phone, tap it, allow *Install from unknown sources* if prompted, and tap **Install**.

## Included Features
- **Dashboard**: GATE Civil Engineering hero banner, 6 live KPI cards, spaced repetition progress bar, Today/Overdue/Upcoming revisions with 1-tap Done, and Recent Tests.
- **My Tests & Test Details**: Fast 1-tap `+ Add Test` bottom sheet with collapsible optional score/accuracy fields, search/filter/sort, and strict **R0–R7** calendar repetition chart (`0, 3, 8, 16, 30, 45, 60, 75 days`).
- **Subjects**: Color-coded Spreadsheet View (all 12 GATE CE subject colors) and Chronological Repetition Schedule.
- **Revisions**: Master R1–R7 tracker with status filters, completion date, difficulty (Easy/Medium/Hard), and notes.
- **Question Bank (Mistakes)**: C / F / A / E / T error classification, repeated-question detection, Single & Bulk (`Q3: C, Q8: F`) entry.
- **Analytics**: 4 KPI cards and 6 native Compose Canvas charts (Score Trend, Accuracy Trend, Subject Avg Score, Error Type Distribution, Revision Breakdown, 7-Day Activity).
- **Calendar**: Monthly grid with color-coded test & revision dots and day-by-day schedule.
- **Settings & Backup**: Light/Dark theme, web-compatible JSON export/import (file picker or paste), CSV export, 6 Sample GATE CE Tests loader, and Clear All Data.
