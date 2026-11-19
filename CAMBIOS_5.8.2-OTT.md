# DavidLG IPTV 5.8.2 — adaptación OTT nativa

Esta entrega adapta la referencia visual STREAMX al apartado OTT Nuevo usando Jetpack Compose. No incorpora una WebView ni carga Tailwind/JavaScript dentro de la app.

## Cambios incluidos

- Barra superior OTT con acabado translúcido, logo DavidLG y navegación adaptada a TV, tablet y móvil.
- Reloj y fecha en escritorio; versión compacta en móvil.
- Selector desplegable de perfiles conectado a los perfiles reales de la aplicación, más acceso a administrar perfiles.
- Acceso directo a Ajustes.
- Selector de fuente conservado y accesible desde escritorio, tablet y móvil.
- Nueva entrada EPG en la navegación OTT. En móvil abre directamente la vista de programación; TV en vivo mantiene su vista de canales.
- Hero de Inicio con sinopsis en panel destacado y metadatos disponibles (año, valoración, duración y género), usando primero los metadatos locales y después los datos de la fuente.
- Se mantienen las pantallas nativas existentes de VOD, TV en vivo, perfiles y reproducción.

## Validación

Se verificó la integridad del ZIP. No fue posible ejecutar `:app:compileDebugKotlin` en este entorno porque Gradle no pudo descargar la distribución (`UnknownHostException: services.gradle.org`). Por tanto, esta entrega no se declara compilada; debe validarse en GitHub Actions o en Android Studio.
