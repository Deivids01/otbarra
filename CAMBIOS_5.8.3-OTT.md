# DavidLG IPTV 5.8.3 — Barra superior OTT Nuevo

## Alcance
Esta versión modifica exclusivamente `ui/ott/OttShell.kt` y los metadatos de versión. No se modifica `ClassicShell` ni los componentes visuales del estilo Clásico.

## Barra superior OTT Nuevo
- Identidad textual: **DavidLG IPTV Player**.
- Navegación horizontal integrada en la barra superior: TV en vivo, EPG, Películas, Series y Animes.
- Fecha en español y reloj de 12 horas con AM/PM y segundos, actualizado cada segundo.
- Selector desplegable de perfiles, con acceso a administración.
- Acceso de engranaje a Ajustes.
- En pantallas estrechas, la navegación permanece en la barra superior y se desplaza horizontalmente; se elimina la barra inferior exclusiva del OTT.
- Inicio sigue siendo la vista inicial y el nombre de la aplicación devuelve a Inicio.

## Conservación
Se mantienen las pantallas y funciones de contenido existentes, así como los motores de reproducción y la implementación Classic independiente.

## Validación
ZIP e integridad de archivos comprobados. La compilación Android debe validarse con GitHub Actions; no se declara compilación exitosa sin ejecutar Gradle con SDK disponible.
