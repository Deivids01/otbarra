# DavidLG IPTV — reconstrucción Blue Streaming 5.8.0

Esta versión inicia una reconstrucción de la experiencia de usuario sobre la base funcional nativa existente. No es una actualización cosmética: cambia la composición de Inicio, el catálogo VOD, la ficha de contenido, la guía móvil/TV y el diseño compacto del modo Clásico.

## Cambios de 5.8.0

- **Inicio OTT editorial:** portada basada en contenido real del catálogo, sinopsis/metadatos cuando existen, carruseles de películas, series, anime, continuar viendo y TV en vivo.
- **Adaptación por dispositivo:** teléfono vertical, tablet y Android TV tienen medidas, jerarquías y densidad diferentes; TV mantiene interacción por D-pad y teléfono usa controles táctiles.
- **VOD reconstruido:** portada cinematográfica, carruseles de póster, ficha ampliada, continuar reproducción y Mi lista.
- **Metadatos propios:** el usuario puede guardar sinopsis, póster, fondo, año, género, duración y calificación en el almacenamiento local por perfil. Estos datos no dependen de que la lista M3U incluya esos campos.
- **Enriquecimiento opcional TMDB:** desde Editar información se puede buscar el título usando la API key configurada en Ajustes. Los metadatos locales existentes tienen prioridad y no se reemplazan.
- **Parser M3U:** conserva atributos EXTINF para aprovechar descripción, poster, backdrop, año, género, duración y rating cuando la fuente los proporciona.
- **Guía OTT:** en teléfono muestra programación como tarjetas horizontales por canal; en Android TV muestra bloques de programas en una línea de tiempo, resaltando el programa actual y su progreso.
- **Modo Clásico móvil:** nueva vista destacada de canal con logotipo, información EPG y cuadrícula de canales; la guía compacta usa tarjetas horizontales de programación.
- **Identidad visual:** paleta OTT unificada azul eléctrico, azul marino y superficies oscuras.
- Se conserva la selección ExoPlayer/Shaka, el pipeline de reproducción, los perfiles, las listas, los favoritos y el comportamiento de pantalla encendida durante reproducción.

## Compilación y verificación

Abrir el proyecto en Android Studio con JDK 17 y ejecutar *Build > Build APK(s)*, o usar el workflow de GitHub Actions incluido. La distribución Gradle configurada es 8.14.3. En el entorno de preparación no está disponible Android SDK ni se pudo descargar Gradle desde `services.gradle.org`, por lo que esta entrega es código fuente empaquetado y todavía no se afirma que el APK compile. Debe probarse en Android TV, teléfono y tablet antes de considerarla estable.

---

## 4.3.0 – interfaz adaptable
- Android móvil usa diseño nativo vertical sin lienzo 16:9 forzado.
- Android TV conserva el lienzo horizontal 16:9.
- El diálogo Añadir/Editar lista solicita foco inicial en el primer campo y agrupa el recorrido de foco para mando.


## 4.4.0 – experiencia móvil B
- TV en vivo en móvil reorganizada: filtros de categoría, panel superior de canal/previa, y pestañas independientes Canales y Programación.
- Se conserva la identidad DavidLG: fondo oscuro, acentos dorados, tipografía Poppins y tarjetas de alto contraste.
- La vista Android TV y su guía horizontal se mantienen separadas de la composición móvil.
- El panel superior funciona como acceso directo al reproductor nativo; la reproducción sigue usando la pantalla PlayerScreen existente.


## 4.5.0 — experiencia OTT móvil

- Reproductor en vivo integrado en la pantalla de TV en vivo para móviles y tablets compactas, usando Media3/ExoPlayer y el mismo `PlaybackFactory` del reproductor de pantalla completa.
- Seleccionar un canal desde la lista cambia la reproducción en el panel sin abandonar la pantalla.
- Acción de ampliar abre el reproductor de pantalla completa existente.
- Pestañas separadas para Canales y Programación; se conserva el tema oscuro, los acentos dorados, Poppins y los recursos de marca DavidLG.
- El reproductor integrado libera ExoPlayer al salir de la pantalla y respeta los perfiles de búfer y bitrate configurados.

La compilación debe verificarse en un entorno Android/Gradle conectado; no se pudo descargar Gradle en el entorno de preparación por falta de acceso a `services.gradle.org`.


## Motor Shaka experimental (5.0.0)

En Ajustes > Reproductor se puede seleccionar Automático, ExoPlayer/Media3 o Shaka Player. ExoPlayer sigue siendo el motor predeterminado y el único motor nativo. Shaka se ejecuta en un Android WebView con JavaScript habilitado y carga la biblioteca Shaka desde CDN, por lo que requiere conectividad a Internet. En la versión 5.0.0 inicial, la configuración avanzada DRM y las cabeceras todavía no estaban integradas; la versión 5.2.0 añade el envío de metadatos Widevine/ClearKey y cabeceras descrito abajo. La compatibilidad real sigue dependiendo del dispositivo y requiere pruebas. La UI y navegación de la aplicación permanecen en Jetpack Compose.


## 5.1.0 — sesión compartida de reproducción

- La selección del canal en TV en vivo se conserva en el estado compartido del ViewModel al cambiar entre motores y al recomponer la pantalla OTT.
- ShakaSurface detecta cambios de URL en la misma vista WebView y vuelve a cargar el contenido; esto evita que la vista previa conserve la URL del canal anterior.
- ExoPlayer sigue siendo el motor nativo predeterminado; Shaka continúa siendo experimental y requiere conexión para cargar la biblioteca desde CDN.
- La compilación APK debe verificarse en un entorno Android/Gradle con la distribución Gradle 8.14.3 disponible.


## 5.2.0 — metadatos DRM y cabeceras para ambos motores

- Shaka recibe cabeceras de flujo desde `inputstream.adaptive.stream_headers`, `referer`, `origin` y `user_agent`; las cabeceras se aplican a manifiestos y segmentos.
- Para Widevine, Shaka configura `com.widevine.alpha` con la URL de licencia y añade las cabeceras indicadas en el formato Kodi `URL|cabeceras|...`, además de las cabeceras generales del canal.
- Para ClearKey, Shaka convierte pares `kid:key` y los campos `kid`/`key` a claves JWK en formato Base64URL, sin enviar claves locales a un servidor externo.
- ExoPlayer conserva su configuración DRM Media3: Widevine con URL/cabeceras de licencia y ClearKey local o mediante URL de licencia.
- La compatibilidad Widevine de Shaka depende del soporte EME/CDM que ofrezca la versión de Android System WebView y el dispositivo. Esta integración no puede garantizar reproducción de contenido protegido en todos los equipos. DRM, licencias y fuentes deben estar autorizadas por el proveedor.
- No se implementa evasión de DRM, extracción de claves ni descifrado de contenido protegido.

## 5.3.0 — Playback reliability and keep-awake

- Shaka WebView now tries two CDN endpoints for its JavaScript library and displays actionable startup/playback errors instead of leaving a silent black screen.
- The Shaka document uses the stream's origin as its base URL rather than always using `https://localhost/`, which can avoid unnecessary cross-origin/CORS rejection when the stream host serves its own manifests and segments.
- WebView playback settings explicitly allow network loading, hardware rendering, inline playback, and compatibility-mode mixed content. Console and main-frame load errors are logged with the `DavidLG-Shaka` tag.
- The app requests that Android keep the display on while the full-screen player is open. The OTT live preview also requests keep-awake while a channel is selected; the normal system timeout is restored when leaving those screens.
- This is a code-level correction, not a claim that every Shaka stream/DRM combination is supported. The stream host must still permit the WebView origin through CORS where required, and Android System WebView must support the necessary MSE/EME capabilities. Widevine playback depends on the device's supported DRM/CDM and an authorized license.

## 5.4.0 — Responsive classic interface

- Adapted the classic navigation header for narrow screens; tabs scroll horizontally instead of overflowing.
- Added a dedicated compact live-TV layout for phones and portrait tablets, with channel categories, logos, current programme and progress.
- Reworked the classic EPG into a time-based programme grid on wide screens and a compact schedule on narrow screens.
- Made the profile carousel horizontally scrollable on TV so the Add Profile card remains reachable when there are several profiles.
- Added unique profile IDs and visible feedback after successful or failed profile creation.
- Settings options now use clearer card surfaces and focus feedback for remote navigation.

The changes are source-level updates. APK compilation and device testing still need to be completed in an Android build environment.


## 5.5.0 — LG-inspired live player information panel

- Reworked the full-screen player information overlay to resemble the reference LG presentation: channel logo and number, channel name, LIVE/VOD and stream-quality badges, current and next EPG programme, current-programme progress, and clock.
- The overlay now uses separate sizing rules for phones, intermediate/tablet widths, and Android TV / wide screens. Phone mode keeps the information compact; TV mode gives the programme information more room and adds remote-control hints.
- The panel appears automatically when a channel is opened or changed and then hides after the existing timeout; touching the screen or using the remote can show it again. Playback remains started by the playback engine and does not depend on pressing OK to start.
- The existing ExoPlayer / Shaka selection, stream playback pipeline, and keep-screen-on behavior are retained.
- This is a source update. ZIP integrity can be checked here, but APK compilation and real-device playback still require an Android build environment and authorized test streams.
