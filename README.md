# Reto 9 · Accediendo al GPS

Aplicación Android nativa en Java para consultar hospitales y lugares turísticos alrededor de una ubicación. Utiliza OpenStreetMap con osmdroid, Overpass para los puntos de interés y Nominatim para resolver direcciones. No requiere una clave de Google Maps.

**Versión 1.1:** búsqueda hospitalaria corregida y comprobada en el emulador: 160 puntos hospitalarios dentro de 20 km del origen de prueba. Consulta [las pruebas actuales y las capturas](evidence/hospital-fix/PRUEBAS.md).

## Funcionalidades

- Ubicación GPS con permisos de Android en tiempo de ejecución; admite permiso aproximado.
- Origen por dirección o por coordenadas `latitud,longitud`.
- Mapa interactivo con marcadores, nombre del lugar y distancia al origen.
- Círculo de búsqueda y filtro por hospitales, turismo o ambos.
- Radio entre 0.1 y 20 km guardado en `SharedPreferences` y conservado al cerrar la app.
- Cálculo de distancia Haversine y exclusión de centros de lugares fuera del radio.
- Mensajes para permiso denegado, GPS desactivado, falta de señal, errores de conexión y resultados vacíos.
- Consultas POST por área, tres servidores y espera del cliente superior al límite de ejecución del servidor.
- Hospitales por etiquetas `amenity=hospital` y `healthcare=hospital`, con nombres alternativos y exclusión de veterinarias.
- Cancelación de búsquedas reemplazadas y rechazo de respuestas parciales; una búsqueda antigua no reemplaza la actual.
- Últimos resultados de la misma búsqueda guardados durante siete días, con fecha y aviso cuando no se pueden actualizar.
- El mapa ajusta el zoom al radio y distingue hospitales, turismo y origen.

La vista inicial muestra Bogotá como referencia; no se presenta como ubicación obtenida por GPS. Pulsa **Mi GPS** o define un origen para consultar lugares. Toca un nombre de la lista para centrar y abrir el marcador.

## Abrir y ejecutar

Proyecto local solicitado: `C:\Users\Usuario\AndroidStudioProjects\Reto9AccediendoalGPS`.

1. Abre esa carpeta en Android Studio y sincroniza Gradle.
2. Usa el JDK incluido con Android Studio, Android SDK 36 y un emulador o dispositivo con Android 8.0 (API 26) o superior.
3. Selecciona la configuración `app` y pulsa Run.
4. Activa la ubicación y concede el permiso, o escribe `4.60971,-74.08175` y pulsa **Buscar**.
5. Usa **Radio** para configurar los kilómetros y el selector para filtrar los lugares.

Versiones: Android Gradle Plugin 9.1.0, Gradle 9.5.0 y osmdroid 6.1.20. `local.properties` se genera para cada equipo y no se publica.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
python scripts\emulator_checks.py
```

Las pruebas funcionales requieren un emulador con ADB, conexión a Internet y la app instalada; usan coordenadas simuladas públicas de Bogotá. El script recorre permisos, GPS, consulta de lugares, filtro, validación del radio, persistencia y coordenadas. El informe distingue `PASS` y `FAIL`: un error de servidor no se considera una prueba aprobada.

## Evidencia

Consulta [el informe actual de la versión 1.1](evidence/hospital-fix/PRUEBAS.md). El informe anterior en `evidence/PRUEBAS.md` se conserva como registro histórico de la versión 1.0. El APK actualizado está en `dist/Reto9AccediendoalGPS-debug.apk` y se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Servicios externos y límites

El mapa, las direcciones y las consultas nuevas necesitan Internet y dependen de servicios públicos. Si Overpass está saturado o no responde, la app distingue el error de un resultado vacío y permite reintentar con **Buscar** conservando el origen. Si existe una respuesta guardada para exactamente el mismo origen, radio y filtro, la muestra indicando su fecha. No sustituye resultados con datos inventados. La cobertura y los nombres dependen de OpenStreetMap; para polígonos se usa el centro informado por Overpass. El conteo corresponde a puntos/objetos registrados en OSM, no a un censo oficial de instituciones independientes.

La app envía el origen y radio a Overpass y el texto de dirección a Nominatim al buscar. Las teselas de mapa se almacenan en la caché local de osmdroid. No hay servidor propio ni claves privadas en el proyecto.

## Referencias

- [osmdroid](https://github.com/osmdroid/osmdroid)
- [Consulta por distancia de Overpass](https://dev.overpass-api.de/overpass-doc/en/full_data/polygon.html)
- [OpenStreetMap y atribución](https://www.openstreetmap.org/copyright)
- [Política de Nominatim](https://operations.osmfoundation.org/policies/nominatim/)
