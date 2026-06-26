# SMS App Android

A simple Android SMS application built using Java and Android SDK.

The application demonstrates sending SMS messages using Android's SmsManager API and handling runtime permissions.

## Features

- Send SMS messages
- Runtime SMS permission handling
- Phone number validation
- Display sent messages on screen
- Simple Android UI

## Screenshots

<img src="screenshot/img.png" width="300">

## Tech Stack

- Java
- Android SDK
- Android Studio
- Gradle

## Android Components Used

### Activity

Handles UI interaction and user actions.

### SmsManager

Used to send SMS messages through the device SIM.

### Runtime Permissions

Handles dangerous permissions:

- SEND_SMS
- RECEIVE_SMS
- READ_SMS

## Project Structure
app
└── src
└── main
├── java
├── res
└── AndroidManifest.xml
