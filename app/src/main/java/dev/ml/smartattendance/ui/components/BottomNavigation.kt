package dev.ml.smartattendance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ml.smartattendance.domain.model.UserRole
import dev.ml.smartattendance.presentation.navigation.Screen
import dev.ml.smartattendance.ui.theme.*

data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val hasNews: Boolean = false,
    val badgeCount: Int? = null
)

@Composable
fun ModernBottomNavigation(
    currentRoute: String?,
    userRole: UserRole,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(userRole) {
        when (userRole) {
            UserRole.STUDENT -> listOf(
                BottomNavItem(
                    route = "dashboard",
                    selectedIcon = Icons.Filled.Dashboard,
                    unselectedIcon = Icons.Outlined.Dashboard,
                    label = "Dashboard"
                ),
                BottomNavItem(
                    route = "attendance_history",
                    selectedIcon = Icons.Filled.Fingerprint,
                    unselectedIcon = Icons.Outlined.Fingerprint,
                    label = "Attendance"
                ),
                BottomNavItem(
                    route = "events",
                    selectedIcon = Icons.Filled.Event,
                    unselectedIcon = Icons.Outlined.Event,
                    label = "Events"
                ),
                BottomNavItem(
                    route = "profile",
                    selectedIcon = Icons.Filled.Person,
                    unselectedIcon = Icons.Outlined.Person,
                    label = "Profile"
                )
            )
            UserRole.ADMIN -> listOf(
                BottomNavItem(
                    route = "admin_main",
                    selectedIcon = Icons.Filled.Dashboard,
                    unselectedIcon = Icons.Outlined.Dashboard,
                    label = "Dashboard"
                ),
                BottomNavItem(
                    route = Screen.StudentManagement.route,
                    selectedIcon = Icons.Filled.People,
                    unselectedIcon = Icons.Outlined.People,
                    label = "Students"
                ),
                BottomNavItem(
                    route = Screen.EventManagement.route,
                    selectedIcon = Icons.Filled.Event,
                    unselectedIcon = Icons.Outlined.Event,
                    label = "Events"
                ),
                BottomNavItem(
                    route = "reports",
                    selectedIcon = Icons.Filled.Assessment,
                    unselectedIcon = Icons.Outlined.Assessment,
                    label = "Reports"
                ),
                BottomNavItem(
                    route = "settings",
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    label = "Settings"
                )
            )
        }
    }
    
    NavigationBar(
        modifier = modifier
            .fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                        
                        // Badge for notifications
                        if (item.hasNews || item.badgeCount != null) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 12.dp, y = (-12).dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                if (item.badgeCount != null) {
                                    Text(
                                        text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                        color = MaterialTheme.colorScheme.onError,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (currentRoute == item.route) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                selected = currentRoute == item.route,
                onClick = {
                    try {
                        onNavigate(item.route)
                    } catch (e: Exception) {
                        // Prevent crashes from navigation errors
                        e.printStackTrace()
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun BottomNavItemComponent(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "color"
    )
    
    val animatedBackgroundAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "backgroundAlpha"
    )
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = animatedBackgroundAlpha),
                shape = RoundedCornerShape(12.dp)
            )
            .selectable(
                selected = isSelected,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = animatedColor,
                modifier = Modifier.scale(animatedScale)
            )
            
            // Badge for notifications
            if (item.hasNews || item.badgeCount != null) {
                Box(
                    modifier = Modifier
                        .offset(x = 12.dp, y = (-12).dp)
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    if (item.badgeCount != null) {
                        Text(
                            text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = item.label,
            color = animatedColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}