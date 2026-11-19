# DavidLG IPTV Player 5.9.2

Proyecto Android nativo con Jetpack Compose, compatible con Android TV, teléfonos y tablets.

## Base de trabajo

Esta versión parte del código fuente original **5.8.0 Blue Streaming Rebuild**. Se portaron de forma selectiva las mejoras de catálogo y guía de las iteraciones posteriores, sin reemplazar el proyecto por una maqueta web.

## Cambios 5.9.2

- Reconstrucción de la navegación exclusiva de **OTT Nuevo**.
- Barra superior con DavidLG IPTV Player, TV en vivo, EPG, Películas, Series, Animes, fecha y hora en formato de 12 horas con segundos, selector de perfil y ajustes.
- Menú compacto para móviles; navegación horizontal en tablets; navegación completa en pantallas amplias/Android TV.
- Inicio con hero alimentado por el catálogo real, metadatos disponibles, perfil activo y recuentos reales de VOD/canales.
- Vista previa de TV en vivo con el motor seleccionado en Ajustes (ExoPlayer o Shaka), también en pantallas amplias.
- Acceso directo a la guía EPG desde la barra superior.
- Formulario de perfiles dentro de una ventana que se redimensiona con el teclado: el contenido puede desplazarse y Guardar/Cancelar quedan en una zona fija. Se eliminaron los usos de `Modifier.weight` de `ProfilesScreen.kt` para evitar el error de compilación reportado.

## Archivos modificados

- `app/build.gradle.kts` (versión 5.9.2 / versionCode 16)
- `MainViewModel.kt` (pestaña EPG)
- `ui/ott/OttShell.kt`
- `ui/ott/OttStart.kt`
- `ui/ott/OttLive.kt`
- `ui/profiles/ProfilesScreen.kt` (ventana y acciones fijas del formulario)

## Archivos preservados

- `ui/classic/ClassicShell.kt` y todos los componentes visuales del modo Clásico.
- Repositorio, parser M3U, modelos, almacenamiento local, EPG, metadatos TMDB y configuración.
- `PlaybackFactory.kt`, `PlayerScreen.kt` y los motores ExoPlayer/Shaka.
- Pantallas de VOD, ajustes y recursos gráficos.

## Compilación

Ejecuta desde la raíz del proyecto:

```bash
./gradlew assembleDebug --no-daemon
```

El entorno donde se preparó este ZIP no dispone de Android SDK ni de las dependencias Gradle descargadas, por lo que **no se declara la compilación como verificada**. El ZIP sí se valida por integridad antes de su entrega.
