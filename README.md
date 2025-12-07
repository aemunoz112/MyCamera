# MyCamera - Aplicación de Cámara para Android

## Descripción del Proyecto

MyCamera es una aplicación móvil completa para Android que integra funcionalidades de captura fotográfica y gestión de galería de imágenes. La aplicación permite a los usuarios capturar fotos, visualizarlas en una galería organizada y gestionar sus imágenes con funciones avanzadas como filtros, modo ráfaga y temporizador.

## Características Principales

### Funcionalidades Básicas
- ✅ **Captura Fotográfica**: Vista previa en tiempo real y captura de fotos en alta calidad
- ✅ **Galería de Fotos**: Visualización en grid con carga eficiente de imágenes
- ✅ **Vista Detallada**: Navegación horizontal entre fotos con funciones de compartir y eliminar
- ✅ **Gestión de Permisos**: Manejo completo de permisos de cámara y almacenamiento
- ✅ **Navegación Fluida**: Implementación con Navigation Compose

### Funcionalidades Avanzadas
- ✅ **Control de Flash**: Modos OFF, ON y AUTO
- ✅ **Cambio de Cámara**: Alternancia entre cámara frontal y trasera
- ✅ **Grid Lines**: Regla de tercios para composición fotográfica
- ✅ **Temporizador**: Opciones de 3, 5 y 10 segundos
- ✅ **Modo Ráfaga**: Captura de 5 fotos en secuencia
- ✅ **Modo Retrato**: Efecto bokeh cuando está disponible en el dispositivo
- ✅ **Filtros de Imagen**: Normal, Escala de grises, Sepia y Blur en vista detallada
- ✅ **Compartir Fotos**: Integración con el sistema de compartir de Android

## Requisitos del Sistema

- **Android**: API 26 (Android 8.0 Oreo) o superior
- **SDK Objetivo**: API 35
- **Kotlin**: Versión 2.0.21 o superior
- **Android Studio**: Hedgehog (2023.1.1) o superior
- **Hardware**: Cámara (frontal o trasera)

## Instalación y Configuración

### Prerrequisitos
1. Android Studio instalado
2. SDK de Android configurado
3. Dispositivo Android o emulador con API 26+

### Pasos de Instalación

1. **Clonar el repositorio**
   ```bash
   git clone [URL_DEL_REPOSITORIO]
   cd MyCamera
   ```

2. **Abrir en Android Studio**
   - Abrir Android Studio
   - Seleccionar "Open an Existing Project"
   - Navegar a la carpeta del proyecto

3. **Sincronizar Gradle**
   - Android Studio sincronizará automáticamente las dependencias
   - Si no, ir a: File → Sync Project with Gradle Files

4. **Ejecutar la aplicación**
   - Conectar un dispositivo Android o iniciar un emulador
   - Presionar el botón "Run" o usar Shift+F10
   - La aplicación se instalará y ejecutará automáticamente

## Estructura del Proyecto

```
app/src/main/java/co/edu/anders/mycamera/
├── MainActivity.kt              # Activity principal con navegación
├── CameraScreen.kt              # Pantalla de captura fotográfica
├── GalleryScreen.kt             # Pantalla de galería y vista detallada
└── ui/
    └── theme/                  # Tema Material Design 3
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## Tecnologías Utilizadas

### Stack Principal
- **Kotlin 2.0.21**: Lenguaje de programación
- **Jetpack Compose**: Framework de UI declarativo
- **Material Design 3**: Sistema de diseño

### Bibliotecas
- **CameraX 1.3.4**: Integración con la cámara del dispositivo
- **Coil 2.5.0**: Carga y visualización eficiente de imágenes
- **Navigation Compose 2.7.7**: Navegación entre pantallas
- **Accompanist Permissions 0.34.0**: Gestión de permisos

## Capturas de Pantalla

> **Nota**: Agregar capturas de pantalla de la aplicación funcionando:
> - Pantalla de cámara con controles
> - Galería con grid de fotos
> - Vista detallada de foto
> - Selector de filtros

## Funcionalidades Detalladas

### Captura Fotográfica
- Vista previa en tiempo real de la cámara
- Botón de captura grande y accesible
- Guardado automático en formato JPEG
- Almacenamiento en directorio interno de la aplicación
- Feedback visual tras captura exitosa
- Manejo de errores durante la captura

### Galería
- Grid responsive con 3 columnas
- Carga lazy de imágenes para optimizar memoria
- Ordenamiento por fecha (más recientes primero)
- Indicador cuando no hay fotos disponibles
- Actualización automática al capturar nuevas fotos

### Vista Detallada
- Visualización en pantalla completa
- Navegación horizontal con swipe (HorizontalPager)
- Eliminación con confirmación
- Compartir fotos con otras aplicaciones
- Aplicación de filtros en tiempo real

### Gestión de Permisos
- Solicitud automática de permisos al iniciar
- Diálogos explicativos cuando se deniegan
- Redirección a configuración si es necesario
- Manejo de estados: concedido, denegado, permanentemente denegado

## Problemas Conocidos

- El modo retrato (bokeh) solo está disponible en dispositivos compatibles
- Los filtros se aplican solo en la vista detallada, no se guardan permanentemente
- En dispositivos con poca memoria, la carga de muchas fotos puede ser lenta

## Mejoras Futuras

- [ ] Guardar filtros aplicados permanentemente
- [ ] Edición básica de imágenes (recorte, rotación)
- [ ] Sincronización con almacenamiento en la nube
- [ ] Modo video
- [ ] Configuración de calidad de imagen
- [ ] Exportación de fotos a galería del sistema

## Autor

**Nombre del Estudiante**
- Asignatura: Desarrollo de Aplicaciones Móviles
- Universidad: [Nombre de la Universidad]

## Licencia

Este proyecto es parte de una actividad académica y está destinado únicamente para fines educativos.

## Referencias

- [Documentación de Android](https://developer.android.com)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io)

