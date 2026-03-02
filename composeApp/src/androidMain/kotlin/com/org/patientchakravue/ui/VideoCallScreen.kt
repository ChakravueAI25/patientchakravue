package com.org.patientchakravue.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.org.patientchakravue.data.ApiRepository
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import kotlin.math.absoluteValue

private const val TAG = "VideoCallScreen"

// Patient UID offset to ensure no collision with doctor
private const val PATIENT_UID_OFFSET = 50000

private fun generatePatientUid(channelName: String): Int {
    val hash = channelName.hashCode().absoluteValue
    return (hash % PATIENT_UID_OFFSET) + PATIENT_UID_OFFSET
}

@Composable
actual fun VideoCallScreen(
    channelName: String,
    doctorId: String,
    onCallEnded: () -> Unit
) {
    val context = LocalContext.current
    val api = remember { ApiRepository() }

    // Generate unique patient UID
    val patientUid = remember(channelName) { generatePatientUid(channelName) }

    // Agora Engine State
    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var remoteUid by remember { mutableStateOf<Int?>(null) }
    var isConnecting by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Call Control States
    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(true) }

    // Permission handling
    val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    // Initialize and join channel
    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
            return@LaunchedEffect
        }

        // 1. Get Token
        val tokenResponse = api.getCallToken(channelName)
        if (tokenResponse == null) {
            errorMessage = "Failed to get call token"
            isConnecting = false
            return@LaunchedEffect
        }

        try {
            // 2. Initialize Engine
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = tokenResponse.appId
                mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                        Log.d(TAG, "Join success: $channel, uid: $uid")
                        isConnecting = false
                    }

                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        Log.d(TAG, "Remote user joined: $uid")
                        // Trigger UI update to render remote video
                        remoteUid = uid
                    }

                    override fun onUserOffline(uid: Int, reason: Int) {
                        Log.d(TAG, "Remote user offline: $uid")
                        if (remoteUid == uid) {
                            remoteUid = null
                            // Optional: End call if doctor leaves
                            // onCallEnded()
                        }
                    }

                    override fun onError(err: Int) {
                        Log.e(TAG, "Agora error: $err")
                    }
                }
            }

            val engine = RtcEngine.create(config)
            engine.enableVideo()
            engine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

            // 3. Join Channel
            val options = ChannelMediaOptions()
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

            engine.joinChannel(tokenResponse.token, channelName, patientUid, options)
            rtcEngine = engine

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Agora", e)
            errorMessage = "Failed to initialize: ${e.message}"
            isConnecting = false
        }
    }

    // Cleanup on Dispose
    DisposableEffect(Unit) {
        onDispose {
            rtcEngine?.stopPreview()
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- REMOTE VIDEO (Full Screen) ---
        // This is the Doctor's feed. It sits at the bottom layer.
        if (remoteUid != null && rtcEngine != null) {
            @Suppress("DEPRECATION", "ComposeViewCompositionLocalNotFound")
            AndroidView(
                factory = { ctx ->
                    // Create SurfaceView inside the factory!
                    SurfaceView(ctx).apply {
                        setZOrderMediaOverlay(false) // BACKGROUND LAYER
                        // Set up the video rendering immediately
                        val canvas = VideoCanvas(this, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid!!)
                        rtcEngine?.setupRemoteVideo(canvas)
                    }
                },
                // Update block handles view updates if remoteUid changes (e.g. doctor reconnects)
                update = { surfaceView ->
                    if (remoteUid != null) {
                        val canvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid!!)
                        rtcEngine?.setupRemoteVideo(canvas)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Waiting UI
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text("Connecting...", color = Color.White, fontSize = 18.sp)
                } else if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red)
                    Button(onClick = onCallEnded) { Text("Go Back") }
                } else {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text("Waiting for doctor...", color = Color.White)
                }
            }
        }

        // --- LOCAL VIDEO (Floating PIP) ---
        // This is the Patient's feed. It floats on top.
        if (rtcEngine != null && isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp, 160.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                @Suppress("DEPRECATION", "ComposeViewCompositionLocalNotFound")
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            setZOrderMediaOverlay(true) // FOREGROUND LAYER (On Top)
                            val canvas = VideoCanvas(this, VideoCanvas.RENDER_MODE_HIDDEN, 0) // 0 = Local user
                            rtcEngine?.setupLocalVideo(canvas)
                            rtcEngine?.startPreview()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // --- CONTROLS ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute
            FloatingActionButton(
                onClick = {
                    isMuted = !isMuted
                    rtcEngine?.muteLocalAudioStream(isMuted)
                },
                containerColor = if (isMuted) Color.White else Color.DarkGray
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = if (isMuted) Color.Black else Color.White
                )
            }

            // End Call
            FloatingActionButton(
                onClick = {
                    rtcEngine?.leaveChannel()
                    onCallEnded()
                },
                containerColor = Color.Red,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.CallEnd, "End", tint = Color.White)
            }

            // Video Toggle
            FloatingActionButton(
                onClick = {
                    isVideoEnabled = !isVideoEnabled
                    rtcEngine?.muteLocalVideoStream(!isVideoEnabled)
                },
                containerColor = if (!isVideoEnabled) Color.White else Color.DarkGray
            ) {
                Icon(
                    imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Video",
                    tint = if (!isVideoEnabled) Color.Black else Color.White
                )
            }
        }
    }
}