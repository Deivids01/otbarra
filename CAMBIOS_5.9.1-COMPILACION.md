# DavidLG IPTV Player 5.9.1 — corrección de compilación

## Error corregido

Se eliminó la importación explícita `androidx.compose.foundation.layout.weight` de `ProfilesScreen.kt`. En Jetpack Compose, `weight` se resuelve como miembro de los scopes `RowScope` y `ColumnScope`; importar esa extensión desde el paquete de layout provocaba el error Kotlin `Cannot access ... RowColumnParentData?.weight: it is internal in file`.

Las llamadas `Modifier.weight(...)` se mantienen dentro de sus respectivos bloques `Column` y `Row`, por lo que conservan el reparto de espacio previsto en el formulario de perfiles.

## Alcance

- Cambio de compilación limitado a `ui/profiles/ProfilesScreen.kt` (eliminación de una línea de importación).
- Se conserva la corrección de visibilidad de Guardar y Cancelar de la versión 5.9.0.
- No se modifican `ClassicShell` ni los componentes del modo Clásico.

## Verificación

- ZIP validado con `unzip -t`.
- No se pudo ejecutar `assembleDebug` en este entorno: el wrapper no pudo resolver `services.gradle.org` (`UnknownHostException`). La compilación debe confirmarse en GitHub Actions.
