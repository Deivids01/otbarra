# DavidLG IPTV Player 5.9.3 — corrección de compilación

## Error corregido

GitHub Actions reportó:
`ClassicShell.kt:236:34 Unresolved reference 'size'`.

Se agregó la importación explícita:
`androidx.compose.foundation.layout.size`

La expresión `Modifier.size(6.dp)` ya existía en el archivo y no se modificó su comportamiento ni su diseño. El cambio es únicamente de importación.

## Alcance

- No se modificó la lógica ni la interfaz del modo Clásico.
- No se modificaron perfiles, reproducción, catálogo, EPG ni pantallas OTT en esta corrección.
- Esta versión parte del ZIP 5.9.2 y aplica exclusivamente el import faltante.

## Verificación

- ZIP: integridad comprobada con `unzip -t`.
- Compilación Gradle: no verificada en este entorno. `./gradlew assembleDebug --no-daemon` no pudo descargar Gradle 8.14.3 por `UnknownHostException: services.gradle.org`.
