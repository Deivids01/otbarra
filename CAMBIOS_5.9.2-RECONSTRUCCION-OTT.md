# Cambios 5.9.2 — reconstrucción OTT Nuevo

## Punto de partida

Se usa como base el proyecto original `DavidLG-IPTV-5.8.0-blue-streaming-rebuild.zip`. No se continúa editando el ZIP 5.9.1 porque sus cambios previos no representaban una reconstrucción integral y había que recuperar una separación estricta entre OTT Nuevo y Clásico.

## OTT Nuevo

- Barra superior nativa Compose con marca, cinco secciones principales, reloj 12 h en tiempo real, selector de perfiles y ajustes.
- Navegación adaptativa: menú desplegable en móvil, segunda fila desplazable en tablet y navegación en línea en pantallas amplias.
- `Inicio` desde la marca y `Mi lista` desde el menú de navegación/perfil.
- Hero de inicio con información tomada del catálogo y del perfil activo; sin títulos ni canales ficticios.
- TV en vivo con vista previa reproducible en pantallas amplias y compactas, respetando el motor elegido en Ajustes.
- EPG como sección directa de la navegación. En móvil abre la vista de programación; en pantallas amplias conserva la guía integrada.
- Películas, Series y Animes continúan usando `VodScreen` y el catálogo existente.

## Perfiles

- El formulario se presenta en un `Dialog` de pantalla adaptable.
- El cuerpo (nombre y avatares) es desplazable.
- Guardar y Cancelar permanecen fuera del área desplazable.
- El tamaño del diálogo se recalcula con el IME/teclado mediante `imePadding`; el manifiesto original ya configura `adjustResize`.
- Se quitaron las llamadas `Modifier.weight` de `ProfilesScreen.kt` para evitar el diagnóstico de acceso interno a `RowColumnParentData.weight`.

## Aislamiento del modo Clásico

`ClassicShell.kt` no se modifica. Tampoco se cambian `ClassicCommon.kt`, `ClassicVodTab.kt`, `VodScreen.kt` ni los ajustes visuales del modo Clásico.

## Estado de validación

- Sintaxis Kotlin: comprobación de análisis sintáctico sin diagnósticos de sintaxis en los archivos modificados.
- Integridad ZIP: se ejecuta `unzip -t`.
- Compilación Android/Gradle: pendiente de GitHub Actions, ya que el entorno de preparación no tiene Android SDK ni caché de dependencias Gradle.
