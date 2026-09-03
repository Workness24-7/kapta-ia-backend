# Login con Google y Apple — guía de configuración

La app ya trae el código cliente y servidor. Para activarlo faltan pasos
que solo el dueño puede hacer en las consolas (Google Cloud y Apple Developer).

## 1. Google (Android)

1. Entra a [Google Cloud Console](https://console.cloud.google.com/) → crea o elige el proyecto.
2. **APIs y servicios → Credenciales → Crear credenciales → ID de cliente de OAuth**:
   - Tipo **Android**: nombre del paquete `com.aistudio.kaptaiapos.xrzkhw`,
     huella SHA-1 de depuración `35:61:FE:0E:10:90:F5:C1:D0:66:9F:81:59:73:7F:62:8E:CD:B6:5C`
     (para release, genera la del keystore de firma y agrégala también).
   - Tipo **Aplicación web**: anota su **ID de cliente** (termina en `.apps.googleusercontent.com`).
3. En la app, crea/edita el archivo `.env` (raíz del proyecto) con:
   `GOOGLE_WEB_CLIENT_ID=TU_ID_DE_CLIENTE_WEB.apps.googleusercontent.com`
   y recompila. Sin este valor, el botón de Google se oculta solo.
4. En Railway → Variables del servicio backend, agrega:
   `GOOGLE_WEB_CLIENT_ID` con el mismo valor (acepta varios separados por coma).
5. **Importante:** el correo de Google debe corresponder a un **usuario ya creado
   por el administrador** en ese negocio (mismo correo). Si no existe, el
   servidor responde que pidas al administrador que te cree. El rol y permisos
   que obtienes son los de ese usuario.

## 2. Apple

Requiere cuenta **Apple Developer de pago** (99 USD/año).

1. En [developer.apple.com](https://developer.apple.com/) → Certificates, IDs & Profiles:
   - Crea un **Services ID** (ej. `com.kaptaia.pos.signin`) y actívale *Sign in with Apple*.
   - Crea una **Key** con *Sign in with Apple*, descarga el `.p8` y anota Key ID + Team ID.
2. En Railway → Variables del backend, agrega:
   - `APPLE_SERVICE_ID` = tu Services ID.
   - `APPLE_TEAM_ID` = tu Team ID (10 caracteres).
   - `APPLE_KEY_ID` = Key ID de la llave.
   - `APPLE_PRIVATE_KEY` = contenido del `.p8` (con `\n` literales en los saltos).
   - `APP_PUBLIC_URL` = URL pública del backend
     (por defecto `https://kapta-ia-backend-production.up.railway.app`).
   - En el Services ID, registra como Return URL: `<APP_PUBLIC_URL>/apple/callback`.
3. Sin estas variables, el botón de Apple muestra “no configurado”.
   Igual que Google: el correo de Apple debe existir como usuario del negocio.

## 3. Notas

- El login social **no crea usuarios**: solo entra quien ya tenga usuario
  (o sea el correo administrador del negocio). Así se conservan roles y permisos.
- La sesión social pasa por la misma puerta de **consentimiento legal** y por
  la **clave dinámica** igual que el login normal.
- Tras configurar variables en Railway, espera 1–2 min al redespliegue.
