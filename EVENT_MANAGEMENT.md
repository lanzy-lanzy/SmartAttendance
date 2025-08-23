# Event Management in SmartAttendance

## Overview

This document provides an overview of the event management functionality in the SmartAttendance application, focusing on the admin features for managing events and tracking student attendance.

## Features Implemented

### 1. Event Creation and Management

- **Create Events**: Administrators can create new events with name, date/time, location, and geofence radius
- **Activate/Deactivate Events**: Events can be toggled between active and inactive states
- **Edit Event Details**: Event properties can be modified (name, time, location, etc.)
- **Event Listing**: View all events with filtering options for active/inactive status

### 2. Attendance Tracking

- **View Attendance Records**: Detailed view of all attendance records for a specific event
- **Attendance Statistics**: Visual summary of attendance metrics (present, late, absent, excused)
- **Manual Attendance Management**: Admins can manually mark or modify attendance status for students

### 3. Geofence Integration

- **Location-Based Attendance**: Events include geofence boundaries (50m radius by default)
- **Attendance Verification**: Student attendance is verified by their physical presence within the geofence
- **Location Tracking**: Attendance records include location coordinates for verification

## Implementation Details

### Event Management Flow

1. **Admin Dashboard**: The central hub for all administrative functions
2. **Event Management Screen**: Lists all events with activation controls
3. **Event Detail Screen**: Comprehensive view of a specific event with attendance data
4. **Attendance Records**: Detailed information about each student's attendance status

### Data Structure

- **Event Entity**: Contains event details (name, time, location, radius, status)
- **Attendance Records**: Links students to events with attendance status
- **Time Windows**: Configurable periods for sign-in and sign-out

### User Roles and Permissions

- **Administrators**: Full access to create, edit, and manage events and attendance records
- **Students**: Can only mark attendance for active events when physically present

## Technical Implementation

### Architecture Components

- **EventManagementScreen**: UI for listing and managing events
- **EventDetailScreen**: Comprehensive event management and attendance tracking
- **EventManagementViewModel**: Business logic for event operations
- **EventDetailViewModel**: Handles detailed event data and attendance records

### Event Activation System

Events can be activated or deactivated by administrators:
- **Active Events**: Available for students to mark attendance
- **Inactive Events**: Hidden from students, attendance marking disabled
- **Toggle Control**: Simple UI control to switch event status

### Attendance Management System

The application provides comprehensive attendance management:
- **Status Tracking**: Records attendance as Present, Late, Absent, or Excused
- **Statistics**: Visual metrics of attendance rates
- **Manual Override**: Admin capability to adjust attendance records

## User Flows

### Administrator Flow

1. Admin logs in and navigates to the Admin Dashboard
2. From the bottom navigation, selects "Events" tab
3. Views list of all events with activation status
4. Can toggle event activation/deactivation directly from the list
5. Taps an event to view detailed information
6. On the Event Detail screen, can:
   - View comprehensive event information
   - See attendance statistics
   - View individual attendance records
   - Edit event details
   - Toggle event activation status

### Student Flow

1. Student logs in and sees dashboard with active events
2. Selects an event to mark attendance
3. System verifies the student is within the event's geofence
4. Biometric authentication confirms student identity
5. Attendance is recorded with timestamp and location

## Compliance with Requirements

This implementation satisfies the requirements from the project documentation:

1. **Requirement 2**: Admins can create events with geo-fence boundaries (50m radius)
2. **Requirement 3**: The system works offline with local storage
3. **Requirement 5**: Automatic penalty calculation for late/absent students
4. **Requirement 7**: Role-based access control for admins and students
5. **Requirement 8**: Comprehensive security measures for data protection