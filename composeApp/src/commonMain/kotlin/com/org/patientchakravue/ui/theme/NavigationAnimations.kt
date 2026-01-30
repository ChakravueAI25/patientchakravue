package com.org.patientchakravue.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.tween

object NavigationAnimations {
    private const val DURATION = 300

    // ===== LEFT NAVIGATION (Home, Care - left side icons) =====
    val enterFromLeft = slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = tween(DURATION)
    ) + fadeIn(animationSpec = tween(DURATION))

    val exitToLeft = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(DURATION)
    ) + fadeOut(animationSpec = tween(DURATION))

    // ===== RIGHT NAVIGATION (Vision, Notifications - right side icons) =====
    val enterFromRight = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(DURATION)
    ) + fadeIn(animationSpec = tween(DURATION))

    val exitToRight = slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = tween(DURATION)
    ) + fadeOut(animationSpec = tween(DURATION))

    // ===== BACK NAVIGATION =====
    val popEnter = slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = tween(DURATION)
    ) + fadeIn(animationSpec = tween(DURATION))

    val popExit = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(DURATION)
    ) + fadeOut(animationSpec = tween(DURATION))

    // ===== FORWARD NAVIGATION (nested screens like Profile, AmslerGrid, etc) =====
    val enterForward = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(DURATION)
    ) + fadeIn(animationSpec = tween(DURATION))

    val exitForward = slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(DURATION)
    ) + fadeOut(animationSpec = tween(DURATION))
}
