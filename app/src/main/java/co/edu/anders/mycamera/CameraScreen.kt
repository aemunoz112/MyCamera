package co.edu.anders.mycamera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateToGallery: () -> Unit
) {
    val context = LocalContext.current
    
    // Estado del permiso de cámara
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Solicitar permiso al cargar la pantalla
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Mostrar contenido según el estado del permiso
    when {
        cameraPermissionState.status.isGranted -> {
            // Permiso otorgado: mostrar cámara
            CameraContent(onNavigateToGallery = onNavigateToGallery)
        }
        cameraPermissionState.status.shouldShowRationale -> {
            // Mostrar explicación de por qué se necesita el permiso
            PermissionRationaleDialog(
                onDismiss = { cameraPermissionState.launchPermissionRequest() },
                onConfirm = { cameraPermissionState.launchPermissionRequest() }
            )
        }
        !cameraPermissionState.status.isGranted && !cameraPermissionState.status.shouldShowRationale -> {
            // Permiso denegado permanentemente o no solicitado aún
            // Si no está otorgado y no debe mostrar rationale, probablemente está permanentemente denegado
            PermissionDeniedScreen(
                isPermanentlyDenied = true,
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onOpenSettings = { openAppSettings(context) }
            )
        }
        else -> {
            // Permiso denegado: mostrar pantalla de solicitud
            PermissionDeniedScreen(
                isPermanentlyDenied = false,
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onOpenSettings = { openAppSettings(context) }
            )
        }
    }
}

/**
 * Contenido principal de la cámara con vista previa
 */
@Composable
fun CameraContent(onNavigateToGallery: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    // Estado para la captura de imágenes
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    // Estado para mensajes de confirmación
    var showMessage by remember { mutableStateOf<String?>(null) }
    
    // Estado para indicador de captura
    var isCapturing by remember { mutableStateOf(false) }
    
    // Estado para cámara frontal/trasera
    var isFrontCamera by remember { mutableStateOf(false) }
    
    // Estado para flash
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    
    // Estado para grid lines
    var showGrid by remember { mutableStateOf(false) }
    
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    // Estado para temporizador
    var timerSeconds by remember { mutableStateOf(0) } // 0 = sin temporizador, 3, 5, 10
    var countdown by remember { mutableStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }
    
    // Estado para modo ráfaga
    var burstMode by remember { mutableStateOf(false) }
    var isBursting by remember { mutableStateOf(false) }
    
    // Estado para modo retrato
    var portraitMode by remember { mutableStateOf(false) }
    var portraitModeAvailable by remember { mutableStateOf(false) }
    
    // Vista previa de la cámara
    val previewView = remember { PreviewView(context) }

    // Función para inicializar cámara
    suspend fun initCamera(useFrontCamera: Boolean, flash: Int, usePortrait: Boolean) {
        val provider = context.getCameraProvider()
        cameraProvider = provider
        
        // Configurar vista previa
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // Configurar captura de imágenes con flash
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flash)
            .build()

        // Selector según la cámara
        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            // Desenlazar casos de uso anteriores
            provider.unbindAll()
            
            // Enlazar casos de uso al ciclo de vida PRIMERO para mostrar la cámara rápidamente
            val cameraInstance = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            
            // Obtener instancia de cámara
            camera = cameraInstance
            
            Log.d("CameraScreen", "Cámara enlazada exitosamente (${if (useFrontCamera) "frontal" else "trasera"})")
            
            // Verificar disponibilidad de modo retrato EN SEGUNDO PLANO (no bloquea la UI)
            // Esta verificación se hace de forma asíncrona después de mostrar la cámara
            // para no retrasar la inicialización
        } catch (e: Exception) {
            Log.e("CameraScreen", "Error al iniciar la cámara", e)
            portraitModeAvailable = false
        }
    }

    // Inicializar la cámara cuando cambian los estados
    LaunchedEffect(isFrontCamera, flashMode, portraitMode) {
        initCamera(isFrontCamera, flashMode, portraitMode)
    }
    
    // Verificar disponibilidad de modo retrato en segundo plano (después de inicializar cámara)
    LaunchedEffect(camera, isFrontCamera) {
        if (camera != null && cameraProvider != null) {
            try {
                val extensionsManager = androidx.camera.extensions.ExtensionsManager.getInstanceAsync(
                    context,
                    cameraProvider!!
                ).get()
                
                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                
                val isPortraitAvailable = extensionsManager.isExtensionAvailable(
                    cameraSelector,
                    ExtensionMode.BOKEH
                )
                
                portraitModeAvailable = isPortraitAvailable
            } catch (e: Exception) {
                Log.d("CameraScreen", "Modo retrato no disponible", e)
                portraitModeAvailable = false
            }
        }
    }
    
    // Liberar recursos de cámara al salir
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                Log.d("CameraScreen", "Cámara desenlazada y recursos liberados")
            } catch (e: Exception) {
                Log.e("CameraScreen", "Error al desenlazar cámara", e)
            }
        }
    }

    // UI de la pantalla
    Box(modifier = Modifier.fillMaxSize()) {
        // Vista previa de la cámara
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Grid lines (regla de tercios)
        if (showGrid) {
            CameraGridOverlay()
        }

        // Botones de control superior con animaciones
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botón de flash con animación
            AnimatedFlashButton(
                flashMode = flashMode,
                onFlashModeChange = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                }
            )

            // Botón de grid con animación
            AnimatedGridButton(
                showGrid = showGrid,
                onGridToggle = { showGrid = !showGrid }
            )
            
            // Botón de temporizador
            AnimatedTimerButton(
                timerSeconds = timerSeconds,
                onTimerChange = {
                    timerSeconds = when (timerSeconds) {
                        0 -> 3
                        3 -> 5
                        5 -> 10
                        else -> 0
                    }
                }
            )
            
            // Botón de modo ráfaga
            AnimatedBurstButton(
                burstMode = burstMode,
                onBurstToggle = { burstMode = !burstMode }
            )
            
            // Botón de modo retrato (solo si está disponible)
            if (portraitModeAvailable) {
                AnimatedPortraitButton(
                    portraitMode = portraitMode,
                    onPortraitToggle = { portraitMode = !portraitMode }
                )
            }
        }

        // Botón para cambiar cámara con animación
        AnimatedCameraSwitchButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp),
            onCameraSwitch = { isFrontCamera = !isFrontCamera }
        )

        // Controles con funcionalidad de captura
        CameraControls(
            onNavigateToGallery = onNavigateToGallery,
            onTakePhoto = {
                if (isCountingDown || isBursting) return@CameraControls
                
                if (timerSeconds > 0) {
                    // Iniciar countdown
                    isCountingDown = true
                    countdown = timerSeconds
                } else {
                    // Capturar inmediatamente o en ráfaga
                    if (burstMode) {
                        isBursting = true
                        coroutineScope.launch {
                            takeBurstPhotos(context, imageCapture) { success ->
                                isBursting = false
                                showMessage = if (success) {
                                    "Ráfaga completada: 5 fotos"
                                } else {
                                    "Error en la ráfaga"
                                }
                            }
                        }
                    } else {
                        imageCapture?.let { capture ->
                            isCapturing = true
                            takePhoto(
                                context = context,
                                imageCapture = capture,
                                onPhotoTaken = { success ->
                                    isCapturing = false
                                    showMessage = if (success) {
                                        "Foto guardada correctamente"
                                    } else {
                                        "Error al guardar la foto"
                                    }
                                }
                            )
                        }
                    }
                }
            }
        )
        
        // Countdown visual
        if (isCountingDown && countdown > 0) {
            CountdownOverlay(
                countdown = countdown,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // LaunchedEffect para manejar el countdown
        LaunchedEffect(isCountingDown, countdown) {
            if (isCountingDown && countdown > 0) {
                delay(1000)
                if (countdown > 1) {
                    countdown--
                } else {
                    // Capturar foto
                    isCountingDown = false
                    countdown = 0
                    
                    if (burstMode) {
                        isBursting = true
                        coroutineScope.launch {
                            takeBurstPhotos(context, imageCapture) { success ->
                                isBursting = false
                                showMessage = if (success) {
                                    "Ráfaga completada: 5 fotos"
                                } else {
                                    "Error en la ráfaga"
                                }
                            }
                        }
                    } else {
                        imageCapture?.let { capture ->
                            isCapturing = true
                            takePhoto(
                                context = context,
                                imageCapture = capture,
                                onPhotoTaken = { success ->
                                    isCapturing = false
                                    showMessage = if (success) {
                                        "Foto guardada correctamente"
                                    } else {
                                        "Error al guardar la foto"
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Indicador de progreso al capturar
        if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        // Mensaje de confirmación con animación
        AnimatedVisibility(
            visible = showMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            showMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }

                // Auto-ocultar después de 3 segundos
                LaunchedEffect(message) {
                    delay(3000)
                    showMessage = null
                }
            }
        }
    }
}

/**
 * Función de extensión para obtener el ProcessCameraProvider
 */
private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

/**
 * Función para tomar una foto y guardarla en el dispositivo
 */
private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoTaken: (Boolean) -> Unit
) {
    // Crear archivo con nombre único basado en timestamp
    val photoFile = File(
        context.getExternalFilesDir(null),
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    // Configurar opciones de salida
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(photoFile)
        .build()

    // Capturar la foto
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d("CameraScreen", "Foto guardada: ${photoFile.absolutePath}")
                onPhotoTaken(true)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Error al guardar la foto", exception)
                onPhotoTaken(false)
            }
        }
    )
}

/**
 * Controles de cámara mejorados con animaciones
 */
@Composable
fun BoxScope.CameraControls(
    onNavigateToGallery: () -> Unit,
    onTakePhoto: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 40.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de galería con animación
            AnimatedGalleryButton(
                onClick = { onNavigateToGallery() }
            )

            // Botón de captura mejorado con animación
            AnimatedCaptureButton(
                onTakePhoto = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTakePhoto()
                }
            )

            // Espaciador para balance
            Spacer(modifier = Modifier.size(60.dp))
        }
    }
}

/**
 * Botón de flash animado
 */
@Composable
fun AnimatedFlashButton(
    flashMode: Int,
    onFlashModeChange: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "flash_scale"
    )

    IconButton(
        onClick = onFlashModeChange,
        modifier = Modifier
            .scale(scale)
            .shadow(8.dp, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                else -> Icons.Filled.FlashOff
            },
            contentDescription = "Flash",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Botón de grid animado
 */
@Composable
fun AnimatedGridButton(
    showGrid: Boolean,
    onGridToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "grid_scale"
    )

    val gridColor by animateColorAsState(
        targetValue = if (showGrid) Color.Yellow else Color.White,
        animationSpec = tween(300),
        label = "grid_color"
    )

    IconButton(
        onClick = onGridToggle,
        modifier = Modifier
            .scale(scale)
            .shadow(8.dp, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.Filled.GridOn,
            contentDescription = "Grid",
            tint = gridColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Botón de cambio de cámara animado
 */
@Composable
fun AnimatedCameraSwitchButton(
    modifier: Modifier = Modifier,
    onCameraSwitch: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "camera_switch_scale"
    )

    IconButton(
        onClick = onCameraSwitch,
        modifier = modifier
            .scale(scale)
            .shadow(8.dp, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.Filled.Cameraswitch,
            contentDescription = "Cambiar cámara",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Botón de galería animado
 */
@Composable
fun AnimatedGalleryButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gallery_scale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .shadow(12.dp, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoLibrary,
            contentDescription = "Galería",
            tint = Color.Black,
            modifier = Modifier.size(30.dp)
        )
    }
}

/**
 * Botón de captura mejorado con animación
 */
@Composable
fun AnimatedCaptureButton(
    onTakePhoto: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "capture_scale"
    )

    Box(
        modifier = Modifier
            .size(88.dp)
            .scale(scale)
            .shadow(16.dp, CircleShape)
            .border(5.dp, Color.White, CircleShape)
            .padding(10.dp)
            .background(Color.White, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTakePhoto
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = "Capturar foto",
            tint = Color.Black,
            modifier = Modifier.size(36.dp)
        )
    }
}

/**
 * Pantalla que se muestra cuando el permiso está denegado
 */
@Composable
fun PermissionDeniedScreen(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isPermanentlyDenied) {
                    "Permiso de Cámara Denegado"
                } else {
                    "Se necesita permiso para usar la cámara"
                },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = if (isPermanentlyDenied) {
                    "Has denegado el permiso de cámara permanentemente. " +
                    "Por favor, habilítalo en la configuración de la app."
                } else {
                    "Necesitamos acceso a tu cámara para tomar fotos."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (isPermanentlyDenied) {
                Button(onClick = onOpenSettings) {
                    Text("Abrir Configuración")
                }
            } else {
                Button(onClick = onRequestPermission) {
                    Text("Otorgar permiso")
                }
            }
        }
    }
}

/**
 * Diálogo que explica por qué se necesita el permiso
 */
@Composable
fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Permiso de Cámara Necesario")
        },
        text = {
            Text(
                "Necesitamos acceso a tu cámara para poder tomar fotos. " +
                "Este permiso es necesario para que la aplicación funcione correctamente."
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Entendido")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Abre la configuración de la app para que el usuario pueda habilitar permisos manualmente
 */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

/**
 * Overlay de grid lines (regla de tercios)
 */
@Composable
fun CameraGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Líneas verticales
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(width / 3, 0f),
            end = Offset(width / 3, height),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(width * 2 / 3, 0f),
            end = Offset(width * 2 / 3, height),
            strokeWidth = 2f
        )
        
        // Líneas horizontales
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(0f, height / 3),
            end = Offset(width, height / 3),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(0f, height * 2 / 3),
            end = Offset(width, height * 2 / 3),
            strokeWidth = 2f
        )
    }
}

/**
 * Función para capturar múltiples fotos en ráfaga
 */
private suspend fun takeBurstPhotos(
    context: Context,
    imageCapture: ImageCapture?,
    onBurstComplete: (Boolean) -> Unit
) {
    if (imageCapture == null) {
        onBurstComplete(false)
        return
    }
    
    val totalCount = 5 // Número de fotos en ráfaga
    var successCount = 0
    val lock = Mutex()
    
    repeat(totalCount) { index ->
        delay(index * 200L) // 200ms entre cada foto
        
        val photoFile = File(
            context.getExternalFilesDir(null),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis() + index) + ".jpg"
        )
        
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    kotlinx.coroutines.GlobalScope.launch {
                        lock.lock()
                        try {
                            successCount++
                            if (successCount == totalCount) {
                                onBurstComplete(true)
                            }
                        } finally {
                            lock.unlock()
                        }
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraScreen", "Error en ráfaga foto ${index + 1}", exception)
                    kotlinx.coroutines.GlobalScope.launch {
                        lock.lock()
                        try {
                            if (successCount == totalCount - 1) {
                                onBurstComplete(successCount > 0)
                            }
                        } finally {
                            lock.unlock()
                        }
                    }
                }
            }
        )
    }
}

/**
 * Overlay de countdown
 */
@Composable
fun CountdownOverlay(
    countdown: Int,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (countdown > 0) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "countdown_scale"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
        ) {
            Text(
                text = countdown.toString(),
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                color = Color.White,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Botón de temporizador animado
 */
@Composable
fun AnimatedTimerButton(
    timerSeconds: Int,
    onTimerChange: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "timer_scale"
    )
    
    val timerColor by animateColorAsState(
        targetValue = if (timerSeconds > 0) Color.Yellow else Color.White,
        animationSpec = tween(300),
        label = "timer_color"
    )
    
    IconButton(
        onClick = onTimerChange,
        modifier = Modifier
            .scale(scale)
            .shadow(8.dp, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        interactionSource = interactionSource
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = "Temporizador",
                tint = timerColor,
                modifier = Modifier.size(24.dp)
            )
            if (timerSeconds > 0) {
                Text(
                    text = "${timerSeconds}s",
                    color = timerColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Botón de modo ráfaga animado
 */
@Composable
fun AnimatedBurstButton(
    burstMode: Boolean,
    onBurstToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "burst_scale"
    )
    
    val burstColor by animateColorAsState(
        targetValue = if (burstMode) Color.Yellow else Color.White,
        animationSpec = tween(300),
        label = "burst_color"
    )
    
    IconButton(
        onClick = onBurstToggle,
        modifier = Modifier
            .scale(scale)
            .shadow(8.dp, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.Filled.BurstMode,
            contentDescription = "Modo ráfaga",
            tint = burstColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Botón de modo retrato animado
 */
@Composable
fun AnimatedPortraitButton(
    portraitMode: Boolean,
    onPortraitToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "portrait_scale"
    )
    
    val portraitColor by animateColorAsState(
        targetValue = if (portraitMode) Color.Yellow else Color.White,
        animationSpec = tween(300),
        label = "portrait_color"
    )
    
    IconButton(
        onClick = onPortraitToggle,
        modifier = Modifier
            .scale(scale)
            .shadow(8.dp, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.Filled.Portrait,
            contentDescription = "Modo retrato",
            tint = portraitColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
