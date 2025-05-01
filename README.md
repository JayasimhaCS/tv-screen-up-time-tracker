# TV Screen Uptime Tracker

An Android application designed to track TV screen uptime and generate reports. This application is particularly useful for digital signage, information displays, and other scenarios where monitoring screen uptime is important.

## Features

- **Continuous Uptime Tracking**: Records screen activity every 5 minutes
- **Automatic Aggregation**: Converts raw tracking data into meaningful uptime periods
- **Email Reporting**: Sends CSV reports with uptime statistics
- **Persistence**: Automatically starts on device boot
- **Low Resource Usage**: Designed to run efficiently in the background

## How It Works

1. The application runs as a foreground service to ensure it's not killed by the system
2. Every 5 minutes, it records the screen state (active, idle, or standby)
3. Daily, it processes these raw events into continuous uptime periods
4. When network connectivity is available, it sends email reports with the aggregated data
5. Weekly, it cleans up old data to prevent excessive storage usage

## Setup Instructions

### Prerequisites

- Android device running Android 10 (API level 29) or higher
- Email account for sending reports (Gmail recommended)
- If using Gmail, you'll need to create an "App Password" in your Google Account settings

### Installation

1. Install the APK on your Android device
2. Launch the application
3. Configure email settings:
   - Enter your email address
   - Enter your email password (or App Password for Gmail)
   - Enter the recipient email address
4. Tap "Save Settings"
5. Tap "Start Service" to begin tracking

### Email Configuration for Gmail

If using Gmail to send reports:

1. Go to your Google Account settings
2. Enable 2-Step Verification if not already enabled
3. Go to "Security" > "App passwords"
4. Select "Mail" and your device
5. Generate and use the 16-character password in the app instead of your regular Gmail password

## Data Collection

The application collects the following data:

- Screen name (device model)
- Timestamp of each uptime event
- Event type (active, idle, or standby)

This data is processed into daily aggregates that include:

- Screen name
- Start time of uptime period
- End time of uptime period
- Total uptime in minutes

## Privacy

All data is stored locally on the device and is only sent to the configured email address. No data is shared with third parties.

## Permissions

The application requires the following permissions:

- `RECEIVE_BOOT_COMPLETED`: To start the service when the device boots
- `FOREGROUND_SERVICE`: To run the service in the foreground
- `INTERNET`: To send email reports
- `ACCESS_NETWORK_STATE`: To detect network connectivity
- `WAKE_LOCK`: To keep the CPU running during tracking

## Technical Details

The application is built using:

- Kotlin
- Android Architecture Components (Room, WorkManager, LiveData)
- Coroutines for asynchronous operations
- JavaMail for email functionality
- OpenCSV for CSV generation

## Troubleshooting

If you're not receiving email reports:

1. Check that the email settings are correct
2. Ensure the device has internet connectivity
3. For Gmail, verify that the App Password is correct
4. Check if the email is in the spam folder

## License

This project is licensed under the MIT License - see the LICENSE file for details.
