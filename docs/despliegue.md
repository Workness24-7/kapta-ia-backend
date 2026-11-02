# Despliegue: PWA en InfinityFree + API en Render + Postgres en Neon

InfinityFree solo ejecuta PHP, así que la parte Python vive en Render.
El frontend (HTML/JS/imágenes) es estático y sí puede vivir en InfinityFree.

```
kapta.ifree.page        → PWA estática (InfinityFree)
kapta-ia-api.onrender.com → API FastAPI (/exec, y también /app/ por si acaso)
Neon (Postgres)         → base de datos
```

## 1. Base de datos (Neon, gratis)

1. neon.com → crea proyecto (región US East).
2. Copia el **Connection string** (`postgresql://usuario:clave@ep-xxx.neon.tech/neondb?sslmode=require`).
3. No hay que crear tablas: `init_db()` las crea al arrancar.

## 2. API (Render, gratis)

1. render.com → New **Blueprint** → conecta el repo `Workness24-7/kapta-ia-backend`.
   Render lee `render.yaml` y crea el servicio Docker `kapta-ia-api` (plan free).
2. En el servicio → Environment → pega `DATABASE_URL` con la cadena de Neon.
3. Deploy. La URL queda en `https://kapta-ia-api.onrender.com` (si Render asigna
   otra, usa esa y ajusta la línea de abajo).
4. Comprueba:
   - `https://kapta-ia-api.onrender.com/exec?action=ping`
   - `https://kapta-ia-api.onrender.com/app/` (la PWA también se sirve aquí)

El plan free se duerme tras 15 minutos sin uso: el primer request tarda ~1 minuto.

## 3. PWA (InfinityFree)

1. Panel de InfinityFree → agrega el dominio `kapta.ifree.page` (o créalo como
   subdominio) y activa el SSL gratuito.
2. Sube **el contenido** de la carpeta `pwa/` dentro de `htdocs/`
   (index.html en la raíz, más `app.js`, `sw.js`, `manifest.webmanifest`,
   `plantilla_factura.xlsx` e `img/`). Son ~3,2 MB.
3. Abre `https://kapta.ifree.page/` y en Safari: Compartir → Añadir a pantalla de inicio.

## 4. Si cambia el dominio de la API

Una sola línea en `pwa/index.html`:

```html
window.KAPTA_API = "https://kapta-ia-api.onrender.com";
```

Se vuelve a subir solo `index.html` (el HTML siempre se pide a la red, no queda en caché).
Si la dejas vacía (`""`), la PWA llama a `/exec` en su mismo dominio.

## 5. Cambiar de motor de base de datos

`DATABASE_URL` decide todo (Postgres, MySQL o SQLite). Ver `docs/base-de-datos.md`.
Al cambiar de motor la base arranca vacía: los datos no se migran solos.

## Avisos

- El MySQL de InfinityFree **no** acepta conexiones remotas: no sirve para el backend
  en Render, solo para PHP alojado ahí.
- Render free (512 MB) se duerme; Neon free (1 GB) escala a cero en 5 min y despierta en
  milisegundos. Los datos de prueba caben de sobra.
- `APP_PUBLIC_URL` (opcional) define el dominio público para el login con Apple,
  que está pausado.
