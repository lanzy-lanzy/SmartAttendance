package dev.ml.smartattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.ml.smartattendance.data.entity.Student
import dev.ml.smartattendance.domain.model.AttendanceStatus
import dev.ml.smartattendance.domain.model.PenaltyType
import dev.ml.smartattendance.presentation.viewmodel.admin.StudentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun StatisticItem(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CourseFilterChips(
    courses: List<String>,
    selectedCourse: String?,
    onCourseSelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            FilterChip(
                onClick = { onCourseSelected(null) },
                label = { Text("All") },
                selected = selectedCourse == null
            )
        }
        
        items(courses) { course ->
            FilterChip(
                onClick = { onCourseSelected(course) },
                label = { Text(course) },
                selected = selectedCourse == course
            )
        }
    }
}

@Composable
fun EmptyStudentsCard() {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.PersonOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Students Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No students match your current filter criteria",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedAddStudentDialog(
    isLoading: Boolean,
    courses: List<String>,
    onDismiss: () -> Unit,
    onAddStudent: (String, String, String, String) -> Unit
) {
    var studentId by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("") }
    var studentEmail by remember { mutableStateOf("") }
    var showCourseDropdown by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        ModernCard {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add New Student",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("Student ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("Student Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = studentEmail,
                    onValueChange = { studentEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = showCourseDropdown,
                    onExpandedChange = { showCourseDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedCourse,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Course") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCourseDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showCourseDropdown,
                        onDismissRequest = { showCourseDropdown = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course) },
                                onClick = {
                                    selectedCourse = course
                                    showCourseDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (studentId.isNotEmpty() && studentName.isNotEmpty() && 
                                selectedCourse.isNotEmpty() && studentEmail.isNotEmpty()) {
                                onAddStudent(studentId, studentName, selectedCourse, studentEmail)
                            }
                        },
                        enabled = !isLoading && studentId.isNotEmpty() && studentName.isNotEmpty() && 
                                selectedCourse.isNotEmpty() && studentEmail.isNotEmpty()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Add Student")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    courses: List<String>,
    selectedCourse: String?,
    selectedStatus: StudentStatus?,
    onCourseFilterChanged: (String?) -> Unit,
    onStatusFilterChanged: (StudentStatus?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Filter Students",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Course",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            CourseFilterChips(
                courses = courses,
                selectedCourse = selectedCourse,
                onCourseSelected = onCourseFilterChanged
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { onStatusFilterChanged(null) },
                    label = { Text("All") },
                    selected = selectedStatus == null
                )
                
                FilterChip(
                    onClick = { onStatusFilterChanged(StudentStatus.ACTIVE) },
                    label = { Text("Active") },
                    selected = selectedStatus == StudentStatus.ACTIVE
                )
                
                FilterChip(
                    onClick = { onStatusFilterChanged(StudentStatus.INACTIVE) },
                    label = { Text("Inactive") },
                    selected = selectedStatus == StudentStatus.INACTIVE
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkActionsBottomSheet(
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onGenerateReport: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Bulk Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ListItem(
                headlineContent = { Text("Export Data") },
                supportingContent = { Text("Export student data to CSV") },
                leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable { onExportData(); onDismiss() }
            )
            
            ListItem(
                headlineContent = { Text("Import Data") },
                supportingContent = { Text("Import students from CSV") },
                leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                modifier = Modifier.clickable { onImportData(); onDismiss() }
            )
            
            ListItem(
                headlineContent = { Text("Generate Report") },
                supportingContent = { Text("Generate attendance report") },
                leadingContent = { Icon(Icons.Default.Assessment, contentDescription = null) },
                modifier = Modifier.clickable { onGenerateReport(); onDismiss() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Event Detail Screen Components

@Composable
fun EventDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AttendanceWindowInfo(
    signInStart: Long,
    signInEnd: Long,
    signOutStart: Long,
    signOutEnd: Long
) {
    Column {
        Text(
            text = "Attendance Windows",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Sign-in: ${signInStart}min before - ${signInEnd}min after start",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Sign-out: ${signOutStart}min before - ${signOutEnd}min after end",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PenaltyOverviewCard(
    penalties: Map<PenaltyType, Int>,
    onViewPenalties: () -> Unit
) {
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Penalty Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                TextButton(onClick = onViewPenalties) {
                    Text("View Details")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (penalties.isEmpty()) {
                Text(
                    text = "No penalties recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    penalties.forEach { (penalty, count) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (penalty) {
                                    PenaltyType.WARNING -> MaterialTheme.colorScheme.secondary
                                    PenaltyType.MINOR -> MaterialTheme.colorScheme.tertiary
                                    PenaltyType.MAJOR -> MaterialTheme.colorScheme.error
                                    PenaltyType.CRITICAL -> MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = penalty.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceStatusFilter(
    selectedStatus: AttendanceStatus?,
    onStatusChanged: (AttendanceStatus?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            FilterChip(
                onClick = { onStatusChanged(null) },
                label = { Text("All") },
                selected = selectedStatus == null
            )
        }
        
        item {
            FilterChip(
                onClick = { onStatusChanged(AttendanceStatus.PRESENT) },
                label = { Text("Present") },
                selected = selectedStatus == AttendanceStatus.PRESENT
            )
        }
        
        item {
            FilterChip(
                onClick = { onStatusChanged(AttendanceStatus.LATE) },
                label = { Text("Late") },
                selected = selectedStatus == AttendanceStatus.LATE
            )
        }
        
        item {
            FilterChip(
                onClick = { onStatusChanged(AttendanceStatus.ABSENT) },
                label = { Text("Absent") },
                selected = selectedStatus == AttendanceStatus.ABSENT
            )
        }
        
        item {
            FilterChip(
                onClick = { onStatusChanged(AttendanceStatus.EXCUSED) },
                label = { Text("Excused") },
                selected = selectedStatus == AttendanceStatus.EXCUSED
            )
        }
    }
}

@Composable
fun EmptyAttendanceCard() {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Attendance Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No attendance records found for this event",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAttendanceDialog(
    availableStudents: List<Student>,
    selectedStudent: dev.ml.smartattendance.data.dao.DetailedAttendanceRecord? = null,
    onDismiss: () -> Unit,
    onMarkAttendance: (String, AttendanceStatus, String?) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(selectedStudent?.studentId ?: "") }
    var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var notes by remember { mutableStateOf("") }
    var showStudentDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        ModernCard {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedStudent != null) "Edit Attendance" else "Mark Attendance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedStudent == null) {
                    ExposedDropdownMenuBox(
                        expanded = showStudentDropdown,
                        onExpandedChange = { showStudentDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = availableStudents.find { it.id == selectedStudentId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Student") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStudentDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showStudentDropdown,
                            onDismissRequest = { showStudentDropdown = false }
                        ) {
                            availableStudents.forEach { student ->
                                DropdownMenuItem(
                                    text = { Text("${student.name} (${student.id})") },
                                    onClick = {
                                        selectedStudentId = student.id
                                        showStudentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(
                        text = "Student: ${selectedStudent.studentName} (${selectedStudent.studentId})",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                ExposedDropdownMenuBox(
                    expanded = showStatusDropdown,
                    onExpandedChange = { showStatusDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedStatus.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showStatusDropdown,
                        onDismissRequest = { showStatusDropdown = false }
                    ) {
                        AttendanceStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name) },
                                onClick = {
                                    selectedStatus = status
                                    showStatusDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val studentId = selectedStudent?.studentId ?: selectedStudentId
                            if (studentId.isNotEmpty()) {
                                onMarkAttendance(studentId, selectedStatus, notes.takeIf { it.isNotBlank() })
                            }
                        },
                        enabled = (selectedStudent != null) || selectedStudentId.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenaltyDetailsDialog(
    penalties: List<dev.ml.smartattendance.presentation.viewmodel.admin.StudentPenaltyData>,
    onDismiss: () -> Unit,
    onUpdatePenalty: (String, PenaltyType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ModernCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "Penalty Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (penalties.isEmpty()) {
                    Text(
                        text = "No penalties recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Show penalty list here
                    penalties.forEach { penaltyData ->
                        Text(
                            text = "${penaltyData.studentName}: ${penaltyData.totalPenaltyPoints} points",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}