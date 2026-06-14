# SharedPreferences Practical (Android)

## Aim
To implement SharedPreferences in Android to store and retrieve simple key-value data locally.

## Description
This application demonstrates how to use SharedPreferences in Android Studio. It allows the user to enter data, save it locally, and retrieve it even after closing and reopening the app.

## Tools Used
- Android Studio  
- Java 
- XML Layout  

## Working
- User enters input data in EditText  
- Data is saved using SharedPreferences  
- Data is retrieved and displayed after reopening the app  

## Methods Used
- getSharedPreferences()
- SharedPreferences.Editor
- putString()
- apply()
- getString()

## Output Screenshots

### Main Screen
![Main Screen](\LoginMemoryApp\screenshot\main_screen.png)

## Folder Structure
LoginMemoryApp/
├── app/
├── screenshot/
│   ├── main_screen.png
│   
└── README.md

## Result
SharedPreferences is successfully implemented to store and retrieve data in the Android application.

## Conclusion
SharedPreferences is useful for storing small amounts of persistent data such as user settings and simple app data.
