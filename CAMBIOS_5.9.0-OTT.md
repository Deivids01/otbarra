# DavidLG IPTV Player 5.9.0 — OTT Nuevo y perfiles

## Rediseño OTT Nuevo
- Se conserva la identidad visual azul oscuro/azul eléctrico y la composición editorial conectada al catálogo real.
- La barra superior mantiene la marca DavidLG IPTV Player, reloj en vivo de 12 horas con segundos y AM/PM, selector de perfil y acceso a Ajustes.
- En pantallas amplias, TV en vivo, EPG, Películas, Series y Animes aparecen como navegación superior.
- En móviles, las secciones se agrupan en un menú desplegable superior para evitar una fila horizontal incómoda; Mi lista también queda accesible desde ese menú.
- El nombre de la aplicación regresa a Inicio. No se agrega navegación inferior.
- Las vistas de Inicio, TV en vivo, EPG y VOD continúan usando el catálogo, metadatos, progreso y reproducción de la aplicación; no se incorporan datos de demostración del HTML.

## Corrección del editor de perfiles
- Guardar y Cancelar quedan fuera del área desplazable del formulario, siempre en una zona de acciones fija.
- En móviles, Guardar y Cancelar comparten una fila con espacio suficiente; Eliminar perfil, cuando aplica, pasa a una fila independiente.
- El formulario respeta el espacio del teclado en pantalla y lo oculta al salir del campo de nombre para facilitar el acceso a las acciones.
- Se conserva la selección de avatar, validación de nombre, edición, creación y eliminación existentes.
- Los controles mantienen foco visible y son utilizables por toque y control remoto.

## Conservación
- No se modificó `ui/classic/ClassicShell.kt` ni los componentes visuales del modo Clásico.
- Se mantienen ExoPlayer/Shaka, repositorios, perfiles y persistencia existentes.

## Validación
- La integridad del ZIP se verificó con `unzip -t`.
- No se pudo ejecutar `git diff --check` porque esta copia de trabajo no contiene metadatos de Git.
- No se pudo ejecutar Gradle en este entorno: el wrapper requiere descargar Gradle desde `services.gradle.org` y la conexión DNS no está disponible.
- La compilación debe confirmarse con el workflow GitHub Actions incluido. No se declara APK compilado ni prueba de ejecución satisfactoria.
