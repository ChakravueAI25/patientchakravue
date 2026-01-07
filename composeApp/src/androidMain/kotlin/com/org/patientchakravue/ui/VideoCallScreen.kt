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

private const val TAG = "VideoCallScreen"

@Composable
actual fun VideoCallScreen(
    channelName: String,
    doctorId: String,
    onCallEnded: () -> Unit
) {
    val context = LocalContext.current
    val api = remember { ApiRepository() }

    // Agora Engine State
    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var remoteUid by remember { mutableStateOf<Int?>(null) }
    var isJoined by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Call Control States
    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(true) }

    // Surface views
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

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

        // Fetch token from backend
        val tokenResponse = api.getCallToken(channelName)
        if (tokenResponse == null) {
            errorMessage = "Failed to get call token"
            isConnecting = false
            return@LaunchedEffect
        }

        try {
            // Initialize Agora Engine
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = tokenResponse.appId
                mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                        Log.d(TAG, "Join channel success: $channel, uid: $uid")
                        isJoined = true
                        isConnecting = false
                    }

                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        Log.d(TAG, "Remote user joined: $uid")
                        remoteUid = uid
                    }

                    override fun onUserOffline(uid: Int, reason: Int) {
                        Log.d(TAG, "Remote user offline: $uid")
                        if (remoteUid == uid) {
                            remoteUid = null
                        }
                    }

                    override fun onError(err: Int) {
                        Log.e(TAG, "Agora error: $err")
                        errorMessage = "Call error: $err"
                    }
                }
            }

            rtcEngine = RtcEngine.create(config)
            rtcEngine?.apply {
                enableVideo()
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

                // Setup local video
                val localView = SurfaceView(context)
                localSurfaceView = localView
                setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                startPreview()

                // Join channel
                joinChannel(tokenResponse.token, channelName, 0, ChannelMediaOptions())
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Agora", e)
            errorMessage = "Failed to initialize video call: ${e.message}"
            isConnecting = false
        }
    }

    // Setup remote video when remote user joins
    LaunchedEffect(remoteUid) {
        if (remoteUid != null && rtcEngine != null) {
            val remoteView = SurfaceView(context)
            remoteSurfaceView = remoteView
            rtcEngine?.setupRemoteVideo(VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid!!))
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            rtcEngine?.apply {
                stopPreview()
                leaveChannel()
            }
            RtcEngine.destroy()
            rtcEngine = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Remote Video (Full Screen)
        if (remoteUid != null && remoteSurfaceView != null) {
            AndroidView(
                factory = { remoteSurfaceView!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Waiting for remote user
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
                    Text(errorMessage!!, color = Color.Red, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onCallEnded) {
                        Text("Go Back")
                    }
                } else if (isJoined) {
                    Text("Waiting for doctor to join...", color = Color.White, fontSize = 18.sp)
                }
            }
        }

        // Local Video (Small Overlay - Top Right)
        if (localSurfaceView != null && isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp, 160.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { localSurfaceView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Call Info Bar (Top)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isJoined) "In Call" else "Connecting",
                color = Color.White,
                fontSize = 14.sp
            )
        }

        // Control Buttons (Bottom)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute Button
            FloatingActionButton(
                onClick = {
                    isMuted = !isMuted
                    rtcEngine?.muteLocalAudioStream(isMuted)
                },
                containerColor = if (isMuted) Color.Red else Color.DarkGray,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White
                )
            }

            // End Call Button (Larger)
            FloatingActionButton(
                onClick = {
                    rtcEngine?.leaveChannel()
                    onCallEnded()
                },
                containerColor = Color.Red,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Video Toggle Button
            FloatingActionButton(
                onClick = {
                    isVideoEnabled = !isVideoEnabled
                    rtcEngine?.muteLocalVideoStream(!isVideoEnabled)
                },
                containerColor = if (!isVideoEnabled) Color.Red else Color.DarkGray,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (isVideoEnabled) "Disable Video" else "Enable Video",
                    tint = Color.White
                )
            }
        }
    }
}
