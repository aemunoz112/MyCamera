package co.edu.anders.mycamera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import java.io.File

/**
 * Enum para los filtros de imagen disponibles
 */
enum class ImageFilter {
    NONE,
    GRAYSCALE,
    SEPIA,
    BLUR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedPhoto by remember { mutableStateOf<File?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Cargar fotos al abrir la pantalla
    LaunchedEffect(Unit) {
        photos = loadPhotos(context)
        Log.d("GalleryScreen", "Fotos cargadas: ${photos.size}")
        photos.forEach { photo ->
            Log.d("GalleryScreen", "Foto: ${photo.name}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galería (${photos.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (photos.isEmpty()) {
            EmptyGalleryMessage(modifier = Modifier.padding(paddingValues))
        } else {
            PhotoGrid(
                photos = photos,
                modifier = Modifier.padding(paddingValues),
                onPhotoClick = { photo ->
                    selectedPhoto = photo
                }
            )
        }
    }

    // Vista detallada
    selectedPhoto?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            photos = photos,
            onDismiss = { selectedPhoto = null },
            onDelete = {
                showDeleteDialog = true
            }
        )
    }

    // Diálogo de confirmación de eliminación
    if (showDeleteDialog && selectedPhoto != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar foto") },
            text = { Text("¿Estás seguro de que quieres eliminar esta foto?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val photoToDelete = selectedPhoto
                        if (photoToDelete != null && isValidPhotoFile(context, photoToDelete)) {
                            val deleted = photoToDelete.delete()
                            Log.d("GalleryScreen", "Foto eliminada: $deleted")
                            photos = loadPhotos(context)
                            selectedPhoto = null
                            showDeleteDialog = false
                        } else {
                            Log.w("GalleryScreen", "Intento de eliminar archivo inválido")
                            showDeleteDialog = false
                        }
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Función para cargar todas las fotos del almacenamiento
 */
private fun loadPhotos(context: Context): List<File> {
    val directory = context.getExternalFilesDir(null)
    
    // Debug: Verificar directorio
    Log.d("GalleryScreen", "Directorio: ${directory?.absolutePath}")
    Log.d("GalleryScreen", "Archivos totales: ${directory?.listFiles()?.size ?: 0}")
    
    return directory?.listFiles { file ->
        file.extension.lowercase() in listOf("jpg", "jpeg", "png")
    }?.sortedByDescending { it.lastModified() } ?: emptyList()
}

/**
 * Valida que un archivo está en el directorio de la app (seguridad)
 */
private fun isValidPhotoFile(context: Context, file: File): Boolean {
    val appDir = context.getExternalFilesDir(null)
    val appDirPath = appDir?.absolutePath ?: return false
    
    // Verificar que el archivo está dentro del directorio de la app
    if (!file.absolutePath.startsWith(appDirPath)) {
        Log.w("GalleryScreen", "Intento de eliminar archivo fuera del directorio de la app")
        return false
    }
    
    // Verificar que es un archivo de imagen válido
    val extension = file.extension?.lowercase() ?: return false
    if (extension !in listOf("jpg", "jpeg", "png")) {
        Log.w("GalleryScreen", "Intento de eliminar archivo que no es una imagen")
        return false
    }
    
    return true
}

/**
 * Mensaje que se muestra cuando la galería está vacía
 */
@Composable
fun EmptyGalleryMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No hay fotos",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Toma algunas fotos para verlas aquí",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

/**
 * Cuadrícula de fotos en formato 3 columnas
 */
@Composable
fun PhotoGrid(
    photos: List<File>,
    modifier: Modifier = Modifier,
    onPhotoClick: (File) -> Unit
) {
    val context = LocalContext.current
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(
            items = photos,
            key = { it.absolutePath }
        ) { photo ->
            AnimatedPhotoItem(
                photo = photo,
                context = context,
                onPhotoClick = { onPhotoClick(photo) }
            )
        }
    }
}

/**
 * Vista detallada de foto con navegación horizontal
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoDetailDialog(
    photo: File,
    photos: List<File>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    // Encontrar índice de la foto actual
    val initialPage = photos.indexOf(photo).takeIf { it >= 0 } ?: 0

    // Estado del pager
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { photos.size }
    )
    
    // Estado para el filtro seleccionado
    var selectedFilter by remember { mutableStateOf(ImageFilter.NONE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onDismiss)
    ) {
        // Pager horizontal para navegar entre fotos
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val currentPhoto = photos[page]
            val context = LocalContext.current
            
            // Cargar imagen con filtro aplicado
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = {}) // Evitar que el click cierre
            ) {
                when (selectedFilter) {
                    ImageFilter.NONE -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentPhoto)
                                .build(),
                            contentDescription = "Foto detallada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    else -> {
                        // Aplicar filtro usando Canvas
                        FilteredImage(
                            photoFile = currentPhoto,
                            filter = selectedFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Botones de control mejorados con animaciones
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selector de filtros
            FilterSelector(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón compartir animado
                AnimatedDetailButton(
                    onClick = { sharePhoto(context, photos[pagerState.currentPage]) },
                    icon = Icons.Filled.Share,
                    contentDescription = "Compartir"
                )

                // Botón eliminar animado
                AnimatedDetailButton(
                    onClick = onDelete,
                    icon = Icons.Filled.Delete,
                    contentDescription = "Eliminar",
                    tint = Color(0xFFFF5252) // Rojo para eliminar
                )

                // Botón cerrar animado
                AnimatedDetailButton(
                    onClick = onDismiss,
                    icon = Icons.Filled.Close,
                    contentDescription = "Cerrar"
                )
            }
        }

        // Indicador de página mejorado
        if (photos.size > 1) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.padding(20.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = Color.Black.copy(alpha = 0.7f),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

/**
 * Item de foto animado para el grid
 */
@Composable
fun AnimatedPhotoItem(
    photo: File,
    context: Context,
    onPhotoClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "photo_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = tween(200),
        label = "photo_alpha"
    )

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(photo)
            .crossfade(true)
            .size(Size.ORIGINAL)
            .memoryCacheKey(photo.absolutePath)
            .diskCacheKey(photo.absolutePath)
            .build(),
        contentDescription = "Foto",
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .alpha(alpha)
            .shadow(4.dp, MaterialTheme.shapes.small)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPhotoClick
            ),
        contentScale = ContentScale.Crop,
        onError = {
            Log.e("GalleryScreen", "Error cargando: ${photo.name}")
        }
    )
}

/**
 * Botón animado para la vista detallada
 */
@Composable
fun AnimatedDetailButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "detail_button_scale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .shadow(8.dp, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Componente para mostrar imagen con filtro aplicado
 */
@Composable
fun FilteredImage(
    photoFile: File,
    filter: ImageFilter,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(photoFile, filter) {
        try {
            val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            when (filter) {
                ImageFilter.GRAYSCALE -> applyGrayscaleFilter(originalBitmap)
                ImageFilter.SEPIA -> applySepiaFilter(originalBitmap)
                ImageFilter.BLUR -> applyBlurFilter(context, originalBitmap)
                ImageFilter.NONE -> originalBitmap
            }
        } catch (e: Exception) {
            Log.e("GalleryScreen", "Error aplicando filtro", e)
            null
        }
    }
    
    bitmap?.let { bmp ->
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(bmp)
                .build(),
            contentDescription = "Foto con filtro",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } ?: run {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photoFile)
                .build(),
            contentDescription = "Foto detallada",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Aplica filtro de escala de grises
 */
private fun applyGrayscaleFilter(bitmap: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(result)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

/**
 * Aplica filtro sepia
 */
private fun applySepiaFilter(bitmap: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(result)
    val paint = Paint()
    val colorMatrix = ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

/**
 * Aplica filtro de desenfoque (blur) simple usando box blur
 */
private fun applyBlurFilter(context: Context, bitmap: Bitmap): Bitmap {
    // Implementación simple de box blur
    val radius = 10
    val width = bitmap.width
    val height = bitmap.height
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    
    // Aplicar box blur horizontal
    for (y in 0 until height) {
        for (x in 0 until width) {
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            
            for (dx in -radius..radius) {
                val px = (x + dx).coerceIn(0, width - 1)
                val pixel = pixels[y * width + px]
                r += android.graphics.Color.red(pixel)
                g += android.graphics.Color.green(pixel)
                b += android.graphics.Color.blue(pixel)
                count++
            }
            
            pixels[y * width + x] = android.graphics.Color.rgb(
                r / count,
                g / count,
                b / count
            )
        }
    }
    
    // Aplicar box blur vertical
    for (x in 0 until width) {
        for (y in 0 until height) {
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            
            for (dy in -radius..radius) {
                val py = (y + dy).coerceIn(0, height - 1)
                val pixel = pixels[py * width + x]
                r += android.graphics.Color.red(pixel)
                g += android.graphics.Color.green(pixel)
                b += android.graphics.Color.blue(pixel)
                count++
            }
            
            pixels[y * width + x] = android.graphics.Color.rgb(
                r / count,
                g / count,
                b / count
            )
        }
    }
    
    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}

/**
 * Selector de filtros para la vista detallada
 */
@Composable
fun FilterSelector(
    selectedFilter: ImageFilter,
    onFilterSelected: (ImageFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ImageFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = when (filter) {
                            ImageFilter.NONE -> "Normal"
                            ImageFilter.GRAYSCALE -> "B&N"
                            ImageFilter.SEPIA -> "Sepia"
                            ImageFilter.BLUR -> "Blur"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

/**
 * Comparte una foto usando el sistema de compartir de Android
 */
fun sharePhoto(context: Context, file: File) {
    try {
        // Crear URI del archivo usando FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        // Crear intent de compartir
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // Lanzar selector de compartir
        context.startActivity(Intent.createChooser(intent, "Compartir foto"))
    } catch (e: Exception) {
        Log.e("GalleryScreen", "Error al compartir foto", e)
    }
}
