package shop.devosify.riya.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import shop.devosify.riya.ui.components.CameraPreview
import shop.devosify.riya.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    onViewHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val amplitudes by viewModel.audioAmplitudes.collectAsState()
    val cameraVision by viewModel.cameraVision.collectAsState()
    var showCamera by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = uiState) {
                is MainViewModel.UiState.Idle -> {
                    MicButton(
                        isRecording = false,
                        onClick = viewModel::startRecording
                    )
                }
                is MainViewModel.UiState.Recording -> {
                    MicButton(
                        isRecording = true,
                        onClick = viewModel::stopRecordingAndProcess
                    )
                }
                is MainViewModel.UiState.Processing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp)
                    )
                }
                is MainViewModel.UiState.Success -> {
                    Text(
                        text = state.response,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is MainViewModel.UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            if (uiState is MainViewModel.UiState.Recording) {
                Spacer(modifier = Modifier.height(32.dp))
                AudioVisualizer(amplitudes = amplitudes)
            }
        }
        
        IconButton(
            onClick = onViewHistory,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "View History"
            )
        }
        
        IconButton(
            onClick = { 
                showCamera = !showCamera
                if (showCamera) {
                    viewModel.startCameraVision(LocalLifecycleOwner.current)
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Toggle Camera"
            )
        }
        
        if (showCamera) {
            CameraPreview(
                onSwitchCamera = viewModel::switchCamera,
                visionAnalysis = cameraVision?.description,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MicButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(72.dp)
            .scale(scale),
        containerColor = if (isRecording) 
            MaterialTheme.colorScheme.error 
        else 
            MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun AudioVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        amplitudes.forEach { amplitude ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(amplitude.coerceIn(0f, 1f))
                    .padding(horizontal = 2.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
} 