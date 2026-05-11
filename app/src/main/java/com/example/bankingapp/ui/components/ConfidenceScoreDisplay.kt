package com.example.bankingapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankingapp.network.AuthenticationStatus
import com.example.bankingapp.ui.theme.ABHEDColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Confidence Score Display Component
 * 
 * Displays the ABHED authentication confidence score with visual indicators
 * and status information for the user.
 */
@Composable
fun ConfidenceScoreDisplay(
    confidenceScore: Double,
    authenticationStatus: AuthenticationStatus,
    dataCollectionProgress: Pair<Int, Int> = 0 to 100,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = confidenceScore.toFloat(),
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "confidence_animation"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "ABHED Security Score",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ABHEDColors.Charcoal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Circular Progress Indicator
            ConfidenceCircularProgress(
                score = animatedScore,
                status = authenticationStatus,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Score Text
            Text(
                text = "${(animatedScore * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = getScoreColor(animatedScore)
            )
            
            // Status Text
            Text(
                text = getStatusText(authenticationStatus),
                style = MaterialTheme.typography.bodyMedium,
                color = getStatusColor(authenticationStatus),
                textAlign = TextAlign.Center
            )
            
            // Data Collection Progress (if collecting data)
            if (authenticationStatus == AuthenticationStatus.CollectingData) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Collecting behavioral data...",
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.Charcoal
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = dataCollectionProgress.first.toFloat() / dataCollectionProgress.second.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ABHEDColors.LightSeaGreen,
                    trackColor = ABHEDColors.GlaucousMoonstone
                )
                
                Text(
                    text = "${dataCollectionProgress.first}/${dataCollectionProgress.second} strokes",
                    style = MaterialTheme.typography.bodySmall,
                    color = ABHEDColors.Charcoal
                )
            }
            
            // Security Level Indicator
            Spacer(modifier = Modifier.height(12.dp))
            
            SecurityLevelIndicator(
                score = animatedScore,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConfidenceCircularProgress(
    score: Float,
    status: AuthenticationStatus,
    modifier: Modifier = Modifier
) {
    val sweepAngle by animateFloatAsState(
        targetValue = score * 360f,
        animationSpec = tween(1500, easing = EaseOutCubic),
        label = "sweep_animation"
    )
    
    Canvas(modifier = modifier) {
        val strokeWidth = 12.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)
        
        // Background circle
        drawCircle(
            color = ABHEDColors.GlaucousMoonstone,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        
        // Progress arc
        val progressColor = getScoreColor(score)
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Status indicator dot
        if (status != AuthenticationStatus.CollectingData) {
            val dotAngle = Math.toRadians((sweepAngle - 90).toDouble()).toFloat()
            val dotRadius = 8.dp.toPx()
            val dotX = center.x + radius * cos(dotAngle.toDouble()).toFloat()
            val dotY = center.y + radius * sin(dotAngle.toDouble()).toFloat()
            
            drawCircle(
                color = progressColor,
                radius = dotRadius,
                center = Offset(dotX, dotY)
            )
        }
    }
}

@Composable
private fun SecurityLevelIndicator(
    score: Float,
    modifier: Modifier = Modifier
) {
    val securityLevel = when {
        score >= 0.8f -> "High Security"
        score >= 0.6f -> "Medium Security"
        score >= 0.4f -> "Low Security"
        else -> "Suspicious Activity"
    }
    
    val levelColor = when {
        score >= 0.8f -> ABHEDColors.LightSeaGreen
        score >= 0.6f -> Color(0xFFFFA726) // Orange
        score >= 0.4f -> Color(0xFFFF7043) // Deep Orange
        else -> Color(0xFFE53935) // Red
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(levelColor)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = securityLevel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = levelColor
        )
    }
}

private fun getScoreColor(score: Float): Color {
    return when {
        score >= 0.8f -> ABHEDColors.LightSeaGreen
        score >= 0.6f -> Color(0xFFFFA726) // Orange
        score >= 0.4f -> Color(0xFFFF7043) // Deep Orange
        else -> Color(0xFFE53935) // Red
    }
}

private fun getStatusText(status: AuthenticationStatus): String {
    return when (status) {
        AuthenticationStatus.Disconnected -> "Disconnected from server"
        AuthenticationStatus.NotAuthenticated -> "Not authenticated"
        AuthenticationStatus.CollectingData -> "Learning your behavior..."
        AuthenticationStatus.LowConfidence -> "Low confidence - continue using app"
        AuthenticationStatus.Authenticated -> "Authenticated user"
        AuthenticationStatus.Suspicious -> "Suspicious activity detected"
    }
}

private fun getStatusColor(status: AuthenticationStatus): Color {
    return when (status) {
        AuthenticationStatus.Disconnected -> Color(0xFF757575) // Grey
        AuthenticationStatus.NotAuthenticated -> Color(0xFF757575) // Grey
        AuthenticationStatus.CollectingData -> ABHEDColors.LightSeaGreen
        AuthenticationStatus.LowConfidence -> Color(0xFFFFA726) // Orange
        AuthenticationStatus.Authenticated -> ABHEDColors.LightSeaGreen
        AuthenticationStatus.Suspicious -> Color(0xFFE53935) // Red
    }
}

/**
 * Compact confidence score display for smaller spaces
 */
@Composable
fun CompactConfidenceScore(
    confidenceScore: Double,
    authenticationStatus: AuthenticationStatus,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = confidenceScore.toFloat(),
        animationSpec = tween(500),
        label = "compact_confidence_animation"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Small circular indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(getScoreColor(animatedScore))
        )
        
        Text(
            text = "${(animatedScore * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = getScoreColor(animatedScore)
        )
        
        Text(
            text = getStatusText(authenticationStatus),
            style = MaterialTheme.typography.bodySmall,
            color = getStatusColor(authenticationStatus)
        )
    }
}
