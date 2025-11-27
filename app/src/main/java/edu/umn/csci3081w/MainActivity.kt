package edu.umn.csci3081w

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.umn.csci3081w.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RestaurantPicker(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun RestaurantPicker(modifier: Modifier = Modifier) {
    val restaurants = listOf(
        "Kings Hawaiian Grill",
        "Global Kitchen",
        "Holy Guacamole",
        "Campus Club",
        "Coffman Market",
        "Starbucks Coffee",
        "Einstein Bros. Bagels",
        "Erbert & Gerbert's",
        "Wild Blue Sushi",
        "Panda Express"
    )

    var selectedRestaurant by remember { mutableStateOf("Spin the wheel!") }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var showConfetti by remember { mutableStateOf(false) }
    var isSpinning by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = selectedRestaurant, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(contentAlignment = Alignment.Center) {
                SpinningWheel(
                    items = restaurants,
                    rotation = rotation.value
                )
                // Pointer
                Canvas(modifier = Modifier.size(30.dp).align(Alignment.TopCenter)) {
                     val path = Path().apply {
                         moveTo(size.width / 2, size.height)
                         lineTo(0f, 0f)
                         lineTo(size.width, 0f)
                         close()
                     }
                     drawPath(path, Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = {
                scope.launch {
                    showConfetti = false
                    isSpinning = true
                    val targetRotation = rotation.value + 360f * 15 + (0..360).random() // Increased spins
                    rotation.animateTo(
                        targetValue = targetRotation,
                        animationSpec = tween(durationMillis = 15000, easing = FastOutSlowInEasing) // 15 seconds
                    )
                    
                    val anglePerItem = 360f / restaurants.size
                    val normalizedRotation = targetRotation % 360f
                    val index = (((360f - normalizedRotation) % 360f) / anglePerItem).toInt() % restaurants.size
                    selectedRestaurant = restaurants[index]
                    isSpinning = false
                    showConfetti = true
                }
            }, enabled = !isSpinning) {
                Text(text = "Spin")
            }
        }

        if (isSpinning) {
            SnakeGameOverlay()
        }

        if (showConfetti) {
            ConfettiExplosion(onDismiss = { showConfetti = false })
        }
    }
}

@Composable
fun SpinningWheel(
    items: List<String>,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFFFFC107), Color(0xFFFF5722), Color(0xFF4CAF50),
        Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFE91E63)
    )

    Canvas(modifier = modifier.size(300.dp)) {
        val anglePerItem = 360f / items.size
        val radius = size.minDimension / 2
        
        rotate(rotation) {
            items.forEachIndexed { index, item ->
                val startAngle = index * anglePerItem - 90f
                val sweepAngle = anglePerItem
                
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                
                val textAngle = startAngle + sweepAngle / 2
                
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    translate(center.x, center.y)
                    rotate(textAngle)
                    
                    val paint = Paint().apply {
                        color = if (index % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        textSize = 40f
                        textAlign = Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                    
                    // Dynamic text sizing
                    val maxTextWidth = radius * 0.85f
                    var currentSize = 40f
                    paint.textSize = currentSize
                    while (paint.measureText(item) > maxTextWidth && currentSize > 12f) {
                        currentSize -= 2f
                        paint.textSize = currentSize
                    }
                    
                    drawText(item, radius - 20f, paint.textSize / 3, paint)
                    restore()
                }
            }
        }
    }
}

@Composable
fun SnakeGameOverlay() {
    var snake by remember { mutableStateOf(listOf(Pair(10, 10))) }
    var food by remember { mutableStateOf(Pair(15, 15)) }
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var gameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while(!gameOver) {
            delay(200)
            val head = snake.first()
            val newHead = when(direction) {
                Direction.UP -> head.copy(second = head.second - 1)
                Direction.DOWN -> head.copy(second = head.second + 1)
                Direction.LEFT -> head.copy(first = head.first - 1)
                Direction.RIGHT -> head.copy(first = head.first + 1)
            }
            
            if (newHead.first < 0 || newHead.first >= 20 || newHead.second < 0 || newHead.second >= 20 || snake.contains(newHead)) {
                gameOver = true
            } else {
                val newSnake = snake.toMutableList()
                newSnake.add(0, newHead)
                if (newHead == food) {
                    food = Pair(Random.nextInt(20), Random.nextInt(20))
                    score++
                } else {
                    newSnake.removeAt(newSnake.size - 1)
                }
                snake = newSnake
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        if (gameOver) {
             Text("Game Over! Score: $score", color = Color.White, fontSize = 30.sp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Snake Game (Score: $score)", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
                
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(Color.DarkGray)
                        .border(2.dp, Color.White)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cellSize = size.width / 20
                        
                        drawRect(
                            color = Color.Red,
                            topLeft = Offset(food.first * cellSize, food.second * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                        
                        snake.forEach { segment ->
                            drawRect(
                                color = Color.Green,
                                topLeft = Offset(segment.first * cellSize, segment.second * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { if(direction != Direction.DOWN) direction = Direction.UP }) { Text("▲") }
                    Row {
                        Button(onClick = { if(direction != Direction.RIGHT) direction = Direction.LEFT }) { Text("◄") }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = { if(direction != Direction.LEFT) direction = Direction.RIGHT }) { Text("►") }
                    }
                    Button(onClick = { if(direction != Direction.UP) direction = Direction.DOWN }) { Text("▼") }
                }
            }
        }
    }
}

enum class Direction { UP, DOWN, LEFT, RIGHT }

data class Particle(
    val color: Color,
    val vx: Float,
    val vy: Float
)

@Composable
fun ConfettiExplosion(onDismiss: () -> Unit) {
    val particles = remember {
        val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan)
        List(100) {
            val angle = Math.random() * 2 * Math.PI
            val speed = Math.random() * 20 + 10
            Particle(
                color = colors.random(),
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat()
            )
        }
    }

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(2000, easing = LinearEasing))
        onDismiss()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val time = anim.value * 50

        particles.forEach { p ->
            val x = centerX + p.vx * time
            val y = centerY + p.vy * time + 0.8f * time * time // Gravity
            
            if (y < size.height) {
                 drawCircle(
                     color = p.color,
                     radius = 10f,
                     center = Offset(x, y),
                     alpha = 1f - anim.value
                 )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RestaurantPickerPreview() {
    MyApplicationTheme {
        RestaurantPicker()
    }
}
