# Firebase Integration for SmartAttendance App

## Overview
This document describes the Firebase integration implemented for the SmartAttendance application, including Firebase Authentication and Firestore for data persistence.

## Features Implemented

### 1. Firebase Authentication
- Email/password authentication
- User registration with role selection (student/admin)
- Email verification
- Password reset functionality
- User session management

### 2. Firestore Data Structure
- Users collection with role-based fields
- Students collection for student-specific data
- Events collection for attendance events
- Attendance records collection

### 3. Role-Based Access Control
- Student role: Can register, login, and mark attendance
- Admin role: Can manage students, events, and view reports

### 4. Authentication Flow
1. Users must register before using the app
2. Registration includes role selection (student/admin)
3. Students must provide student ID and course information
4. Admins have different permission levels (basic/super)
5. Email verification is sent upon registration
6. Users can login with email/password
7. Password reset functionality is available

## Implementation Details

### Domain Models
- `User`: Core user model with role-specific fields
- `UserRole`: Enum for STUDENT/ADMIN roles
- `AdminLevel`: Enum for admin permission levels
- `AuthResult`: Authentication result wrapper
- `RegisterRequest`: Registration data transfer object

### Services
- `AuthService`: Interface for authentication operations
- `FirestoreService`: Interface for Firestore operations
- `AuthServiceImpl`: Firebase Authentication implementation
- `FirestoreServiceImpl`: Firestore operations implementation

### Dependency Injection
- Hilt module for Firebase services
- Singleton providers for FirebaseAuth and FirebaseFirestore
- Service binding for AuthService and FirestoreService

### UI Components
- Login screen with email/password fields
- Registration screen with role selection
- Error handling and loading states
- Navigation based on authentication state

## Firebase Configuration
The app uses the provided `google-services.json` file for Firebase configuration.

## Security Considerations
- Passwords are securely handled by Firebase Authentication
- User data is stored in Firestore with proper structure
- Role-based access control prevents unauthorized access
- Email verification ensures valid user accounts

## Future Enhancements
- Implement real-time data synchronization
- Add offline support with Firestore persistence
- Enhance security rules for Firestore
- Add more admin features for user management