# main.py — KAPTA IA Backend FastAPI (contrato idéntico a api.gs)
# Reemplaza Google Apps Script. La app Android solo cambia APPS_SCRIPT_WEB_APP_URL.
import datetime
import hashlib
import hmac
import json
import re
import os
import uuid
import time as _time
import urllib.request as _urlreq
import urllib.parse as _urlparse

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

import db

app = FastAPI(title="KAPTA IA API", docs_url=None, redoc_url=None)
# PWA web (iPhone/Android/PC) consume la API desde otro origen.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

# PWA web (usable en iPhone/Android/PC) servida en /app/.
try:
    app.mount("/app", StaticFiles(directory="pwa", html=True), name="pwa")
except Exception:
    pass

VERSION_API = "KAPTA-1.0.0"

# ===================================================
# TABLAS INTERNAS DE CADA EMPRESA (ESPEJO DE config.gs)
# Por negocio: ventas, inventario, deudores, movimientos.
# Globales (una sola tabla física con Código_Empresa al inicio):
# gastos, auditoria_gastos, usuarios, config_negocio.
# Eliminadas: estadisticas, ia.
# ===================================================
TABLAS = {
    "INVENTARIO": {"INICIO": 1, "FILA_INICIO": 3, "COLUMNAS": 13},
    "VENTAS": {"INICIO": 14, "FILA_INICIO": 3, "COLUMNAS": 22},
    "DEUDORES": {"INICIO": 36, "FILA_INICIO": 3, "COLUMNAS": 11},
    "GASTOS": {"INICIO": 45, "FILA_INICIO": 3, "COLUMNAS": 14},
    "AUDITORIA_GASTOS": {"INICIO": 60, "FILA_INICIO": 3, "COLUMNAS": 8},
    "USUARIOS": {"INICIO": 69, "FILA_INICIO": 3, "COLUMNAS": 11},
    "CONFIG_NEGOCIO": {"INICIO": 81, "FILA_INICIO": 3, "COLUMNAS": 6},
    "MOVIMIENTOS": {"INICIO": 110, "FILA_INICIO": 3, "COLUMNAS": 10},
}

CABECERAS = {
    "INVENTARIO": ["Id_Producto", "Codigo_Barras", "Nom_Producto", "Categoria",
                   "Cantidad", "Costo", "Precio_Venta", "Precio_Minimo",
                   "Alerta_Stock", "Estado", "Fecha_Creacion", "Ultima_Modificacion"],
    "VENTAS": ["Id_Venta", "Fecha", "Hora", "Cliente", "Id_Producto", "Producto",
               "Cantidad", "Precio_Unitario", "Subtotal", "Descuento",
               "Transferencia", "Efectivo", "Total", "Usuario", "Estado",
               "Fecha_Modificacion", "Hora_Modificacion", "Modificado_Por",
               "Fecha_Anulacion", "Hora_Anulacion", "Anulado_Por", "Tipo"],
    "DEUDORES": ["Fecha_Registro", "Nom_Cliente", "Producto", "Cantidad",
                 "Minimo", "Transferencia", "Efectivo", "Total_Pendiente", "Tipo", "Perdedor", "Chico"],
    "MOVIMIENTOS": ["Id_Movimiento", "Fecha", "Id_Producto", "Nom_Producto",
                    "Tipo", "Cantidad", "Stock_Anterior", "Stock_Nuevo",
                    "Usuario", "Observacion"],
}

# Vista tenant de las tablas globales (sin la columna Código_Empresa,
# que el backend agrega/quita automáticamente al guardar/leer).
CABECERAS_GLOBALES = {
    "GASTOS": ["Id_Gasto", "Fecha", "Hora", "Categoría", "Concepto",
               "Descripción", "Proveedor", "Monto", "Método_Pago",
               "Referencia", "Usuario", "Estado", "Fecha_Modificación", "Modificado_Por"],
    "AUDITORIA_GASTOS": ["Id_Evento", "Id_Empresa", "Id_Gasto", "Accion",
                         "Usuario", "Fecha_Hora", "Detalles", "Estado"],
    "USUARIOS": ["Id_Usuario", "Nombre", "Correo", "Contraseña", "Rol",
                  "Estado", "Fecha_Creacion", "Ultimo_Acceso",
                  "Fecha_Cambio_Estado", "Motivo_Cambio", "Cambiado_Por", "Funciones"],
    "CONFIG_NEGOCIO": ["Parametro", "Valor", "Descripcion", "Fecha_Actualizacion",
                       "Usuario", "Observaciones"],
}

# ===================================================
# HELPERS
# ===================================================
def respuesta_success(data):
    return JSONResponse({"status": "success", "data": data})


def respuesta_error(mensaje):
    return JSONResponse({"status": "error", "message": mensaje})


def fecha_actual():
    return datetime.datetime.now().strftime("%Y-%m-%d %H:%M")


def _normalize_key(key):
    return db._normalize(key).strip().replace("ñ", "n")


def identificar_tabla(nombre):
    alias = {
        "inventario": "INVENTARIO",
        "ventas": "VENTAS",
        "venta": "VENTAS",
        "deudores": "DEUDORES",
        "deudor": "DEUDORES",
        "gastos": "GASTOS",
        "gasto": "GASTOS",
        "auditoria_gastos": "AUDITORIA_GASTOS",
        "auditoria_gasto": "AUDITORIA_GASTOS",
        "usuarios": "USUARIOS",
        "usuario": "USUARIOS",
        "config_negocio": "CONFIG_NEGOCIO",
        "config": "CONFIG_NEGOCIO",
        "movimientos": "MOVIMIENTOS",
        "movimiento": "MOVIMIENTOS",
        "kardex": "MOVIMIENTOS",
    }
    key = _normalize_key(nombre).replace(" ", "_")
    return alias.get(key)


def prefijo_id_tabla(nombre):
    mapa = {
        "inventario": "Ky_", "producto": "Ky_",
        "ventas": "V-", "venta": "V-",
        "gastos": "G-", "gasto": "G-",
        "usuarios": "USR-", "usuario": "USR-",
        "auditoria_gastos": "AG-", "auditoria_gasto": "AG-",
    }
    return mapa.get(_normalize_key(nombre).replace(" ", "_"))


def prefijo_inventario(empresa):
    """Código de producto: 2 primeras letras del nombre del negocio + '-' (ej. CO-)."""
    emp = db.buscar_empresa(empresa) or {}
    nombre = _normalize_key(str(emp.get("nombre") or "").strip())
    letras = "".join(ch for ch in nombre if ch.isalpha())[:2].upper()
    return (letras or "PR") + "-"


def asegurar_admin():
    """Bootstrap env-driven: garantiza un usuario admin en la tabla unificada.
    ADMIN_COMPANY (def KAPT), ADMIN_EMAIL (def AdminMauricio@kaptaia.com),
    ADMIN_PASSWORD (obligatoria; si falta no se crea nada)."""
    empresa = (os.getenv("ADMIN_COMPANY") or "KAPT").strip().upper()
    correo = (os.getenv("ADMIN_EMAIL") or "AdminMauricio@kaptaia.com").strip().lower()
    clave = (os.getenv("ADMIN_PASSWORD") or "").strip()
    if not clave or not db.buscar_empresa(empresa):
        return
    existente = any(
        str(d[2] or "").lower() == correo
        for (_, d) in db.leer_tabla(empresa, "usuarios")
    )
    if existente:
        db.actualizar_contrasena(empresa, correo, _hash_password(clave))
        return
    hoy = fecha_actual()
    fila = db.siguiente_fila_libre(empresa, "usuarios", 3)
    valores = [
        "USR-" + uuid.uuid4().hex[:8].upper(), "Administrador KAPTA",
        correo, _hash_password(clave), "Administrador", "Activo",
        hoy, hoy, "", "", "",
    ]
    db.guardar_fila(empresa, "usuarios", fila, valores)


def resolver_hoja(clave):
    """Replica buscarHojaEmpresa: devuelve el codigo de la empresa o None."""
    if not clave:
        return None
    emp = db.buscar_empresa(clave)
    if not emp:
        return None
    return emp["codigo"]


def leer_hoja_rows(empresa, tabla_key, fila_inicio=2):
    """rows[0]=encabezados (fila 2), fila 3+ = datos."""
    filas = db.leer_tabla(empresa, tabla_key.lower())
    filas = sorted(filas, key=lambda f: f[0])
    filas = [d for (n, d) in filas if n >= fila_inicio]
    columnas = TABLAS[tabla_key]["COLUMNAS"]
    rows = [list(r[:columnas]) + [""] * max(0, columnas - len(r)) for r in filas]
    return rows


# ===================================================
# ACCIONES
# ===================================================
def action_listar_empresas(params=None):
    try:
        db.purgar_empresas_eliminadas()
    except Exception:
        pass  # la purga diferida nunca debe romper el listado
    empresas = db.listar_empresas_db()
    # Excluir las eliminadas (soft-delete) para que conteo y vista coincidan
    empresas = [e for e in empresas if str(e.get("estado") or "").strip().upper() != "ELIMINADO"]
    lista = []
    for e in empresas:
        lista.append({
            "id": e["id"], "codigo": e["codigo"] or "", "nombre": e["nombre"] or "",
            "nit": e["nit"] or "", "tipo": e["tipo"] or "", "pais": e["pais"] or "",
            "ciudad": e["ciudad"] or "", "direccion": e["direccion"] or "",
            "correo": e["correo"] or "", "celular1": e["celular1"] or "",
            "celular2": e["celular2"] or "", "estado": e["estado"] or "",
            "plan": e["plan"] or "", "tiempo": e["tiempo"] or "",
            "fechaCreacion": e["fecha_creacion"] or "",
            "ultimoAcceso": e["ultimo_acceso"] or "",
            "fechaVencimiento": e["fecha_vencimiento"] or "",
            "observaciones": e["observaciones"] or "",
            "tipoSistema": e["tipo_sistema"] or "",
            "tipoPlataforma": e["tipo_plataforma"] or "",
            "logoUrl": e["logo_url"] or "",
            "listIconUrl": e["list_icon_url"] or "",
            "colorPrimario": e["color_primario"] or "",
            "colorSecundario": e["color_secundario"] or "",
            "colorTerciario": e["color_terciario"] or "",
            "colorNeutro": e["color_neutro"] or "",
            "tipoFuente": e["tipo_fuente"] or "",
            "funciones": e["funciones"] or "",
        })
    return respuesta_success({"empresas": lista})


def action_read(params):
    clave = str(params.get("sheetName") or params.get("idEmpresa") or "").strip()
    table_name = str(params.get("tableName") or params.get("targetTable") or "").strip()
    if not clave:
        return respuesta_error("No se recibió sheetName.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)

    tabla_key = identificar_tabla(table_name)
    if tabla_key:
        rows = leer_hoja_rows(empresa, tabla_key)
        return respuesta_success({
            "sheetName": empresa,
            "tableName": table_name,
            "rows": rows,
        })

    # Sin tabla específica -> todas las tablas concatenadas por fila
    todas = []
    for nombre, t in TABLAS.items():
        todas.extend(leer_hoja_rows(empresa, nombre))
    return respuesta_success({"sheetName": empresa, "rows": todas})


def action_dedup_inventario(params):
    """Mantenimiento: elimina duplicados de inventario por Nom_Producto."""
    clave = str(params.get("sheetName") or params.get("idEmpresa") or "").strip()
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)
    antes = len(db.leer_tabla(empresa, "inventario"))
    eliminadas, err = db.dedup_inventario(empresa)
    despues = len(db.leer_tabla(empresa, "inventario"))
    return respuesta_success({"antes": antes, "eliminadas": eliminadas, "despues": despues, "error": err})


# ===================================================
# SEGURIDAD: hash PBKDF2 de contraseñas + bloqueo por intentos
# ===================================================
MSG_CREDENCIALES = "Usuario o Contraseña incorrecta."


def _hash_password(password):
    salt = uuid.uuid4().hex
    iteraciones = 100_000
    hash_hex = hashlib.pbkdf2_hmac(
        "sha256", password.encode("utf-8"), bytes.fromhex(salt), iteraciones
    ).hex()
    return f"pbkdf2${iteraciones}${salt}${hash_hex}"


def _verificar_password(password, almacenado):
    """Compara contra hash pbkdf2 o contraseña legada en texto plano (migrable)."""
    almacenado = str(almacenado or "")
    try:
        if almacenado.startswith("pbkdf2$"):
            _, iteraciones, salt, hash_hex = almacenado.split("$", 3)
            calc = hashlib.pbkdf2_hmac(
                "sha256", password.encode("utf-8"), bytes.fromhex(salt), int(iteraciones)
            ).hex()
            return hmac.compare_digest(calc, hash_hex)
        return hmac.compare_digest(almacenado, password)
    except (ValueError, TypeError):
        return False


def _limpiar(valor, max_len):
    """Sanitiza entradas: sin caracteres de control y longitud acotada."""
    limpio = "".join(c for c in str(valor or "") if c.isprintable()).strip()
    return limpio[:max_len]


def _mensaje_bloqueo(segundos):
    if segundos == -1:
        return "Demasiados intentos fallidos. Contacta al Administrador para recuperar tu acceso."
    if segundos < 60:
        return ("Cuenta bloqueada temporalmente por múltiples intentos fallidos. "
                f"Intenta de nuevo en {segundos} segundo(s).")
    mins = (segundos + 59) // 60
    return ("Cuenta bloqueada temporalmente por múltiples intentos fallidos. "
            f"Intenta de nuevo en {mins} minuto(s).")


def action_login(params):
    codigo = _limpiar(params.get("codigo"), 20).upper()
    correo = _limpiar(params.get("correo") or params.get("Correo_Admin"), 120).lower()
    password = _limpiar(params.get("password") or params.get("Contraseña_Admin")
                        or params.get("Contrasena_Admin"), 128)
    if not codigo:
        return respuesta_error(MSG_CREDENCIALES)
    if not correo or not re.match(r"^[^@\s]+@[^@\s]+\.[^@\s]+$", correo):
        return respuesta_error(MSG_CREDENCIALES)
    if not password:
        return respuesta_error(MSG_CREDENCIALES)

    empresa = resolver_hoja(codigo)
    if not empresa:
        return respuesta_error("No existe empresa con ese código.")

    # Bloqueo por membresía: suspendida o vencida no entra
    emp = db.buscar_empresa(codigo) or {}
    if str(emp.get("estado") or "").strip().lower() == "suspendido":
        return respuesta_error("Empresa suspendida. Para reactivar contactanos.")
    fv = str(emp.get("fecha_vencimiento") or "").strip()
    if fv:
        try:
            if datetime.date.fromisoformat(fv[:10]) < datetime.date.today():
                return respuesta_error("Membresía vencida. Para renovar contactanos.")
        except ValueError:
            pass

    # Bloqueo temporal por intentos fallidos previos
    restantes = db.segundos_bloqueo_restantes(codigo, correo)
    if restantes != 0:
        return respuesta_error(_mensaje_bloqueo(restantes))

    usuario = None
    for (n, d) in db.leer_tabla(empresa, "usuarios"):
        if str(d[2] or "").lower().strip() == correo:
            usuario = d
            break
    if not usuario:
        db.registrar_fallo_login(codigo, correo)
        return respuesta_error(MSG_CREDENCIALES)
    if str(usuario[5] or "") in ("Suspendido", "Bloqueado"):
        return respuesta_error("Usuario " + str(usuario[5]).lower() + ".")
    if not _verificar_password(password, usuario[3]):
        segundos = db.registrar_fallo_login(codigo, correo)
        if segundos != 0:
            return respuesta_error(_mensaje_bloqueo(segundos))
        return respuesta_error(MSG_CREDENCIALES)

    # Migración transparente: texto plano -> hash PBKDF2
    if not str(usuario[3] or "").startswith("pbkdf2$"):
        try:
            db.actualizar_contrasena(codigo, correo, _hash_password(password))
        except Exception:
            pass

    db.reset_fallos_login(codigo, correo)
    db.actualizar_ultimo_acceso(empresa, correo, fecha_actual())

    admin_datos = {
        "idUsuario": usuario[0], "nombre": usuario[1], "correo": usuario[2],
        "rol": usuario[4] or "Administrador", "estado": usuario[5] or "Activo",
        "login": True,
    }
    return JSONResponse({
        "status": "success",
        "idEmpresa": empresa, "id_empresa": empresa, "Id_Negocio": empresa,
        "id_negocio": empresa, "idEmpresaSesion": empresa,
        "codigo": codigo,
        "empresaNombre": params.get("nombre") or empresa,
        "correo": correo, "rol": admin_datos["rol"],
        "estado": admin_datos["estado"], "login": True,
        "version": VERSION_API,
        "data": dict(admin_datos, idEmpresa=empresa, codigo=codigo,
                     empresaNombre=params.get("nombre") or empresa),
    })


# ===================================================
# LOGIN SOCIAL (Google / Apple)
# ===================================================
_CANJES_APPLE = {}  # canje -> {"empresa": codigo, "correo": email, "expira": epoch}


def _google_ids_permitidos():
    return [x.strip() for x in str(os.getenv("GOOGLE_WEB_CLIENT_ID") or "").split(",") if x.strip()]


def _verificar_id_token_google(id_token):
    """Valida el ID token contra Google. Retorna el payload o None."""
    try:
        url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + _urlparse.quote(id_token, safe="")
        with _urlreq.urlopen(url, timeout=10) as resp:
            datos = json.loads(resp.read().decode("utf-8"))
    except Exception:
        return None
    if str(datos.get("email_verified") or "").lower() not in ("true", "1"):
        return None
    if str(datos.get("aud") or "") not in _google_ids_permitidos():
        return None
    try:
        if int(datos.get("exp") or 0) < int(_time.time()):
            return None
    except (TypeError, ValueError):
        return None
    if str(datos.get("iss") or "") not in ("accounts.google.com", "https://accounts.google.com"):
        return None
    return datos


def _respuesta_sesion_social(empresa_codigo, codigo_cli, correo, rol, estado, nombre):
    rol = rol or "Empleado"
    estado = estado or "Activo"
    return JSONResponse({
        "status": "success",
        "idEmpresa": empresa_codigo, "id_empresa": empresa_codigo, "Id_Negocio": empresa_codigo,
        "id_negocio": empresa_codigo, "idEmpresaSesion": empresa_codigo,
        "codigo": codigo_cli,
        "empresaNombre": empresa_codigo,
        "correo": correo, "rol": rol,
        "nombre": nombre or "",
        "estado": estado, "login": True,
        "version": VERSION_API,
        "data": {"idEmpresa": empresa_codigo, "codigo": codigo_cli,
                 "empresaNombre": empresa_codigo, "idUsuario": "",
                 "nombre": nombre or "", "correo": correo,
                 "rol": rol, "estado": estado, "login": True},
    })


def _login_social_por_correo(codigo_cli, correo, nombre_sugerido=""):
    """Sesión social por correo ya verificado (Google/Apple)."""
    empresa = resolver_hoja(codigo_cli)
    if not empresa:
        return respuesta_error("No existe empresa con ese código.")
    emp = db.buscar_empresa(codigo_cli) or {}
    if str(emp.get("estado") or "").strip().lower() == "suspendido":
        return respuesta_error("Empresa suspendida. Para reactivar contactanos.")
    fv = str(emp.get("fecha_vencimiento") or "").strip()
    if fv:
        try:
            if datetime.date.fromisoformat(fv[:10]) < datetime.date.today():
                return respuesta_error("Membresía vencida. Para renovar contactanos.")
        except ValueError:
            pass

    usuario = None
    for (n, d) in db.leer_tabla(empresa, "usuarios"):
        if str(d[2] or "").lower().strip() == correo:
            usuario = d
            break
    if usuario is not None:
        if str(usuario[5] or "") in ("Suspendido", "Bloqueado"):
            return respuesta_error("Usuario " + str(usuario[5]).lower() + ".")
        db.actualizar_ultimo_acceso(empresa, correo, fecha_actual())
        return _respuesta_sesion_social(empresa, codigo_cli, correo,
                                        usuario[4], usuario[5], usuario[1])
    correo_admin = str(emp.get("correo") or "").lower().strip()
    if correo and correo == correo_admin:
        return _respuesta_sesion_social(empresa, codigo_cli, correo, "Administrador",
                                        "Activo", str(emp.get("nombre") or "") or nombre_sugerido)
    return respuesta_error("Tu correo no tiene usuario en este negocio. Pide al administrador que te cree uno.")


def action_login_google(params):
    codigo_cli = _limpiar(params.get("codigo") or params.get("idEmpresa"), 20).upper()
    id_token = str(params.get("idToken") or "").strip()
    if not codigo_cli or not id_token:
        return respuesta_error("Faltan datos para el ingreso con Google.")
    if not _google_ids_permitidos():
        return respuesta_error("Ingreso con Google no configurado en el servidor.")
    datos = _verificar_id_token_google(id_token)
    if not datos:
        return respuesta_error("No se pudo verificar tu cuenta de Google. Intenta de nuevo.")
    correo = str(datos.get("email") or "").lower().strip()
    if not correo:
        return respuesta_error("Google no devolvió un correo válido.")
    return _login_social_por_correo(codigo_cli, correo, str(datos.get("name") or ""))


def _apple_env():
    return {
        "service_id": str(os.getenv("APPLE_SERVICE_ID") or "").strip(),
        "team_id": str(os.getenv("APPLE_TEAM_ID") or "").strip(),
        "key_id": str(os.getenv("APPLE_KEY_ID") or "").strip(),
        "private_key": str(os.getenv("APPLE_PRIVATE_KEY") or "").replace("\\n", "\n"),
        "redirect": str(os.getenv("APP_PUBLIC_URL") or "https://kapta-ia-backend-production.up.railway.app").rstrip("/") + "/apple/callback",
    }


def action_apple_auth_url(params):
    """Devuelve la URL de autorización de Apple para el negocio indicado."""
    cfg = _apple_env()
    if not cfg["service_id"]:
        return respuesta_error("Ingreso con Apple no configurado en el servidor.")
    codigo_cli = _limpiar(params.get("codigo") or params.get("idEmpresa"), 20).upper()
    if not codigo_cli:
        return respuesta_error("Falta el código del negocio.")
    if not resolver_hoja(codigo_cli):
        return respuesta_error("No existe empresa con ese código.")
    url = ("https://appleid.apple.com/auth/authorize?response_type=code"
           "&response_mode=form_post"
           "&client_id=" + _urlparse.quote(cfg["service_id"], safe="") +
           "&redirect_uri=" + _urlparse.quote(cfg["redirect"], safe="") +
           "&scope=" + _urlparse.quote("name email", safe="") +
           "&state=" + _urlparse.quote(codigo_cli, safe=""))
    return respuesta_success({"url": url})


def _apple_client_secret(cfg):
    """Firma el client_secret ES256 que exige Apple (requiere PyJWT)."""
    try:
        import jwt as _pyjwt
    except Exception:
        return None
    ahora = int(_time.time())
    payload = {"iss": cfg["team_id"], "iat": ahora, "exp": ahora + 300,
               "aud": "https://appleid.apple.com", "sub": cfg["service_id"]}
    try:
        return _pyjwt.encode(payload, cfg["private_key"], algorithm="ES256",
                             headers={"kid": cfg["key_id"]})
    except Exception:
        return None


def _apple_verificar_identity_token(cfg, identity_token):
    """Verifica firma y reclamos del identity_token con las llaves públicas de Apple."""
    try:
        import jwt as _pyjwt
    except Exception:
        return None
    try:
        with _urlreq.urlopen("https://appleid.apple.com/auth/keys", timeout=10) as resp:
            jwks = json.loads(resp.read().decode("utf-8"))
        sin_verificar = _pyjwt.decode(identity_token, options={"verify_signature": False})
        kid = _pyjwt.get_unverified_header(identity_token).get("kid")
        llave = next((k for k in jwks.get("keys", []) if k.get("kid") == kid), None)
        if not llave:
            return None
        clave = _pyjwt.algorithms.RSAAlgorithm.from_jwk(json.dumps(llave))
        datos = _pyjwt.decode(identity_token, clave, algorithms=["RS256"],
                              audience=cfg["service_id"], issuer="https://appleid.apple.com",
                              leeway=60)
        if str(datos.get("email_verified") or "").lower() not in ("true", "1", ""):
            return None
        return datos
    except Exception:
        return None


def _apple_intercambiar_codigo(cfg, code):
    """Intercambia el authorization code por tokens en Apple."""
    secreto = _apple_client_secret(cfg)
    if not secreto:
        return None
    try:
        cuerpo = _urlparse.urlencode({
            "grant_type": "authorization_code", "code": code,
            "redirect_uri": cfg["redirect"], "client_id": cfg["service_id"],
            "client_secret": secreto,
        }).encode("utf-8")
        req = _urlreq.Request("https://appleid.apple.com/auth/token", data=cuerpo, method="POST")
        with _urlreq.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception:
        return None


@app.get("/apple/callback")
@app.post("/apple/callback")
async def apple_callback(request: Request):
    """Retorno de Apple: verifica, crea un canje de un solo uso y vuelve a la app."""
    cfg = _apple_env()
    if request.method == "GET":
        params = dict(request.query_params)
    else:
        try:
            ctype = request.headers.get("content-type", "")
            if "application/json" in ctype:
                params = await request.json()
            else:
                forma = await request.form()
                params = dict(forma)
        except Exception:
            params = {}
    codigo_cli = str(params.get("state") or "").strip().upper()
    code = str(params.get("code") or "").strip()
    if not cfg["service_id"] or not codigo_cli or not code:
        return respuesta_error("Autorización de Apple incompleta.")
    tokens = _apple_intercambiar_codigo(cfg, code)
    if not tokens or not tokens.get("id_token"):
        return respuesta_error("Apple no devolvió credenciales válidas.")
    datos = _apple_verificar_identity_token(cfg, tokens["id_token"])
    if not datos or not datos.get("email"):
        return respuesta_error("No se pudo verificar tu cuenta de Apple.")
    correo = str(datos["email"]).lower().strip()
    canje = "APL-" + uuid.uuid4().hex[:12].upper()
    _CANJES_APPLE[canje] = {"empresa": codigo_cli, "correo": correo,
                            "expira": int(_time.time()) + 300}
    destino = "kaptaia://auth?canje=" + _urlparse.quote(canje, safe="") + "&empresa=" + _urlparse.quote(codigo_cli, safe="")
    # HTML que devuelve a la app vía deep link (el navegador no sabe mostrar JSON).
    html = ("<!doctype html><html><head><meta charset='utf-8'>"
            "<meta http-equiv='refresh' content='0;url=" + destino + "'>"
            "<title>Volviendo a Kapta IA…</title></head>"
            "<body style='font-family:sans-serif;text-align:center;padding-top:60px'>"
            "<p>Verificación exitosa. Volviendo a la aplicación…</p>"
            "<p><a href='" + destino + "'>Toca aquí si no regresa sola</a></p>"
            "<script>window.location.href='" + destino + "';</script>"
            "</body></html>")
    return HTMLResponse(html)


def action_login_apple_canjear(params):
    """Canjea el código de un solo uso del callback de Apple por una sesión."""
    canje = str(params.get("canje") or "").strip().upper()
    codigo_cli = _limpiar(params.get("empresa") or params.get("codigo") or params.get("idEmpresa"), 20).upper()
    datos = _CANJES_APPLE.pop(canje, None)
    if not datos or int(datos.get("expira") or 0) < int(_time.time()):
        return respuesta_error("Código de Apple vencido o inválido. Intenta de nuevo.")
    if codigo_cli and datos.get("empresa") != codigo_cli:
        return respuesta_error("El código no corresponde a este negocio.")
    return _login_social_por_correo(datos.get("empresa"), datos.get("correo"))


def action_registrar_empresa(params):
    datos = params.get("data") or params
    if isinstance(datos, str):
        try:
            datos = json.loads(datos)
        except json.JSONDecodeError:
            return respuesta_error("datos JSON inválidos.")

    nombre = str(datos.get("nombre") or "").strip()
    codigo = str(datos.get("codigo") or "").strip().upper()
    if not nombre:
        return respuesta_error("Debe indicar el nombre del negocio.")
    if not codigo:
        return respuesta_error("Debe indicar el código de acceso.")

    emp_existente = db.buscar_empresa(codigo)
    if emp_existente:
        # Recuperación: reactivar y conservar tablas/datos existentes (no recrear ni borrar)
        db.reactivar_empresa(emp_existente.get("codigo") or codigo)
        return respuesta_success({"idEmpresa": emp_existente.get("id"), "codigo": emp_existente.get("codigo") or codigo,
                                  "empresa": emp_existente.get("nombre"), "recuperada": True})

    nit = str(datos.get("nit") or "").strip()
    if db.buscar_empresa_por_nit(nit):
        return respuesta_error(f"El NIT {nit} ya está registrado para otra empresa.")

    id_empresa = "KIA-" + uuid.uuid4().hex[:8].upper()
    es_prueba = str(datos.get("estado") or "") in ("PRUEBA", "Prueba") \
        or str(datos.get("tiempo") or "") in ("15 días", "1 mes")
    estado_final = "PRUEBA" if es_prueba else (str(datos.get("estado") or "") or "Activo")
    fecha_hoy = fecha_actual()
    fecha_vencimiento = calcular_vencimiento(str(datos.get("tiempo") or "1 Mes"), fecha_hoy)

    db.registrar_empresa_db({
        "id": id_empresa, "nombre": nombre, "nit": nit,
        "codigo": codigo, "tipo": str(datos.get("tipo") or ""),
        "pais": str(datos.get("pais") or ""), "ciudad": str(datos.get("ciudad") or ""),
        "direccion": str(datos.get("direccion") or ""),
        "correo": str(datos.get("correo") or ""),
        "celular1": str(datos.get("celular1") or ""),
        "celular2": str(datos.get("celular2") or ""),
        "estado": estado_final, "plan": str(datos.get("plan") or "") or "Básico",
        "tiempo": str(datos.get("tiempo") or "") or "1 Mes",
        "fecha_creacion": fecha_hoy, "fecha_vencimiento": fecha_vencimiento,
        "observaciones": str(datos.get("observaciones") or ""),
        "logo_url": str(datos.get("logoUrl") or ""),
        "list_icon_url": str(datos.get("listIconUrl") or ""),
        "color_primario": str(datos.get("colorPrimario") or ""),
        "color_secundario": str(datos.get("colorSecundario") or ""),
        "color_terciario": str(datos.get("colorTerciario") or ""),
        "color_neutro": str(datos.get("colorNeutro") or ""),
        "tipo_fuente": str(datos.get("tipoFuente") or ""),
        "funciones": str(datos.get("funciones") or ""),
    })

    # Ingreso KAPTA: registrar el plan elegido en finanzas_kapta (no si es Permanente)
    tiempo_sel = str(datos.get("tiempo") or "").strip()
    if tiempo_sel.lower() != "permanente":
        monto_plan = str(datos.get("precio") or datos.get("monto") or "0")
        db.registrar_finanza_kapta(
            tipo="Ingreso",
            categoria=f"Plan {str(datos.get('plan') or 'Básico')}",
            concepto=f"Registro empresa {nombre} - Plan {str(datos.get('plan') or 'Básico')} {tiempo_sel}",
            monto=monto_plan,
            metodo_pago="", referencia="", usuario=str(datos.get("adminCorreo") or ""),
        )

    # Crear hoja: headers (fila 2) en tablas por negocio Y globales
    for nombre_tabla, cab in {**CABECERAS, **CABECERAS_GLOBALES}.items():
        db.guardar_fila(codigo, nombre_tabla.lower(), 2, list(cab))

    configs = [
        ["PLAN", str(datos.get("plan") or "") or "Básico", "Plan contratado", fecha_hoy, "Sistema", ""],
        ["PAIS", str(datos.get("pais") or ""), "País del negocio", fecha_hoy, "Sistema", ""],
        ["CIUDAD", str(datos.get("ciudad") or ""), "Ciudad del negocio", fecha_hoy, "Sistema", ""],
    ]
    for i, cfg in enumerate(configs):
        db.guardar_fila(codigo, "config_negocio", 3 + i, list(cfg))

    admin = {
        "nombre": str(datos.get("adminNombre") or ""),
        "correo": str(datos.get("adminCorreo") or "").lower(),
        "password": _hash_password(str(datos.get("adminPassword") or "")[:128]),
    }

    # Limpiar usuarios existentes (solo dejar 1: el admin del form)
    for (n, _d) in db.leer_tabla(codigo, "usuarios"):
        if n > 2:
            db.borrar_fila(codigo, "usuarios", n)

    fila_admin = ["USR-" + uuid.uuid4().hex[:8].upper(), admin["nombre"],
                  admin["correo"], admin["password"], "Administrador", "Activo",
                  fecha_hoy, "", "", "", ""]
    db.guardar_fila(codigo, "usuarios", 3, fila_admin)

    return respuesta_success({"idEmpresa": id_empresa, "codigo": codigo,
                              "empresa": nombre})


def action_actualizar_empresa(params):
    datos = params.get("data") or params
    if isinstance(datos, str):
        try:
            datos = json.loads(datos)
        except json.JSONDecodeError:
            return respuesta_error("datos JSON inválidos.")
    if not isinstance(datos, dict):
        return respuesta_error("datos inválidos.")

    codigo = str(datos.get("codigo") or "").strip().upper()
    if not codigo:
        return respuesta_error("Debe indicar el código de la empresa.")
    emp = db.buscar_empresa(codigo)
    if not emp:
        return respuesta_error("No existe la empresa: " + codigo)

    codigo_real = emp.get("codigo") or codigo
    campos = {}
    for cli, bd in (
        ("nombre", "nombre"),
        ("tipo", "tipo"),
        ("pais", "pais"),
        ("ciudad", "ciudad"),
        ("direccion", "direccion"),
        ("correo", "correo"),
        ("celular1", "celular1"),
        ("celular2", "celular2"),
        ("estado", "estado"),
        ("plan", "plan"),
        ("logoUrl", "logo_url"),
        ("listIconUrl", "list_icon_url"),
        ("colorPrimario", "color_primario"),
        ("colorSecundario", "color_secundario"),
        ("colorTerciario", "color_terciario"),
        ("colorNeutro", "color_neutro"),
        ("tipoFuente", "tipo_fuente"),
        ("funciones", "funciones"),
    ):
        if cli in datos:
            campos[bd] = str(datos[cli])
    if "tiempo" in datos:
        campos["tiempo"] = str(datos["tiempo"])

    db.actualizar_empresa_db(codigo_real, campos)
    return respuesta_success({"codigo": codigo, "actualizada": True})


def action_listar_funciones(params=None):
    funciones = db.listar_funciones_db()
    data = [{
        "nombre": f["nombre"],
        "descripcion": f["descripcion"],
        "rol": f["rol"],
        "planTier": f["plan_tier"],
        "tipoNegocio": f["tipo_negocio"],
        "modulo": f["modulo"],
    } for f in funciones]
    return respuesta_success(data)


def action_crear_funcion(params):
    nombre = str(params.get("nombre") or "").strip()
    if not nombre:
        return respuesta_error("Debe indicar el nombre de la función.")
    db.crear_funcion_db({
        "nombre": nombre,
        "descripcion": str(params.get("descripcion") or ""),
        "rol": str(params.get("rol") or ""),
        "plan_tier": str(params.get("planTier") or "Basico"),
        "tipo_negocio": str(params.get("tipoNegocio") or ""),
        "modulo": str(params.get("modulo") or ""),
        "creado_por": str(params.get("creadoPor") or ""),
        "fecha": fecha_actual(),
    })
    return respuesta_success({"nombre": nombre, "creada": True})


def action_eliminar_funcion(params):
    nombre = str(params.get("nombre") or "").strip()
    if not nombre:
        return respuesta_error("Debe indicar el nombre de la función.")
    db.eliminar_funcion_db(nombre)
    return respuesta_success({"nombre": nombre, "eliminada": True})


def calcular_vencimiento(tiempo, desde):
    try:
        fecha = datetime.datetime.strptime(desde, "%Y-%m-%d %H:%M")
        t = tiempo.lower()
        if "15" in t:
            fecha += datetime.timedelta(days=15)
        elif "año" in t:
            fecha += datetime.timedelta(days=365)
        elif "1 mes" == t:
            fecha += datetime.timedelta(days=30)
        elif "3" in t:
            fecha += datetime.timedelta(days=90)
        elif "6" in t:
            fecha += datetime.timedelta(days=180)
        else:
            fecha += datetime.timedelta(days=30)
        return fecha.strftime("%Y-%m-%d %H:%M")
    except ValueError:
        return ""


def action_obtener_todo(params):
    clave = str(params.get("sheetName") or params.get("idEmpresa") or "").strip()
    if not clave:
        return respuesta_error("No se recibió sheetName.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)
    resultado = {}
    for nombre in TABLAS:
        resultado[nombre.lower()] = leer_hoja_rows(empresa, nombre)
    return respuesta_success(resultado)


def action_escribir_fila(params):
    clave = str(params.get("sheetName") or params.get("idEmpresa") or "").strip()
    table_name = str(params.get("tableName") or params.get("targetTable") or "").strip()
    if not clave:
        return respuesta_error("No se recibió sheetName.")
    if not table_name:
        return respuesta_error("No se recibió tableName.")
    tabla_key = identificar_tabla(table_name)
    if not tabla_key:
        return respuesta_error("Tabla no reconocida: " + table_name)
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)

    if params.get("values") is not None and isinstance(params["values"], list):
        datos = params["values"][0]
    else:
        datos = params.get("data")
    if isinstance(datos, str):
        try:
            datos = json.loads(datos)
        except json.JSONDecodeError:
            return respuesta_error("data inválido.")
    if not datos or not isinstance(datos, list):
        return respuesta_error("No se recibieron datos (data).")

    datos = [str(x) if x is not None else "" for x in datos]

    # Nunca guardar contraseñas en texto plano: hashear al crear/actualizar usuarios
    if tabla_key == "USUARIOS" and len(datos) > 3:
        pwd = datos[3]
        if pwd and not pwd.startswith("pbkdf2$"):
            datos = list(datos)
            datos[3] = _hash_password(pwd[:128])

    if tabla_key == "INVENTARIO":
        prefijo = prefijo_inventario(empresa)
        ancho = 4
    else:
        prefijo = prefijo_id_tabla(table_name)
        ancho = 5
    id_era_vacio = str(datos[0] or "").strip() == ""
    fila = None
    # ponytail: el cliente re-envía el producto sin Id_Producto en cada sync; si ya
    # existe uno con el mismo Nom_Producto, reusa su fila en vez de crear otra (anti-duplicado).
    if id_era_vacio and tabla_key == "INVENTARIO":
        nombre_n = str(datos[2] or "").strip().lower()
        if nombre_n:
            for (n, d) in db.leer_tabla(empresa, "inventario"):
                if str(d[2] or "").strip().lower() == nombre_n:
                    datos = list(datos)
                    datos[0] = str(d[0] or "").strip()
                    fila = n
                    id_era_vacio = False
                    break
    # Usuarios: upsert por correo. Editar un usuario (p. ej. cambiar su contraseña)
    # actualiza su fila en vez de crear un duplicado, así el cambio es global
    # y multidispositivo.
    if tabla_key == "USUARIOS":
        correo_n = str(datos[2] if len(datos) > 2 else "").strip().lower()
        if correo_n:
            for (n, d) in db.leer_tabla(empresa, "usuarios"):
                if str(d[2] or "").strip().lower() == correo_n:
                    datos = list(datos)
                    if not str(datos[0] or "").strip():
                        datos[0] = str(d[0] or "").strip()
                    fila = n
                    id_era_vacio = False
                    break
    if fila is None:
        fila = db.siguiente_fila_libre(empresa, tabla_key.lower(), TABLAS[tabla_key]["FILA_INICIO"])
    if prefijo and id_era_vacio:
        datos = list(datos)
        datos[0] = db.siguiente_id(empresa, tabla_key.lower(), prefijo, ancho)

    # Venta directa: el cliente suele enviar Id_Producto vacio; se resuelve desde el
    # inventario por nombre (igual que en el pago de deudores) y se normalizan numeros
    # (sin decimales fantasma) y fecha (unico formato dd/mm/yyyy).
    if tabla_key == "VENTAS":
        datos = list(datos)
        if not str(datos[4] or "").strip():
            datos[4] = _resolver_id_producto(empresa, datos[5])
        datos[1] = _fecha_iso_a_latina(datos[1])
        for i in (6, 7, 8, 9, 10, 11, 12):
            datos[i] = _normalizar_numero(datos[i])

    # Tablas globales (USUARIOS, GASTOS...) usan CABECERAS_GLOBALES (sin Codigo_Empresa);
    # las por-negocio usan TABLAS. El corte debe respetar esa longitud o se pierden
    # columnas como Funciones en usuarios.
    if tabla_key in CABECERAS_GLOBALES:
        columnas = len(CABECERAS_GLOBALES[tabla_key])
    else:
        columnas = TABLAS[tabla_key]["COLUMNAS"]
    fila_valores = datos[:columnas] + [""] * max(0, columnas - len(datos))

    # ponytail: evita duplicados en VENTAS por re-envío de red; solo omite fila idéntica
    # (mismo Cliente/Producto/Cantidad/Total/Fecha/Hora). Techo: no dedupe ventas
    # legítimas distintas en el mismo minuto.
    if tabla_key == "VENTAS":
        cli_n = str(datos[3] or "").strip()
        prod_n = str(datos[5] or "").strip()
        cant_n = str(datos[6] or "").strip()
        tot_n = str(datos[12] or "").strip()
        fec_n = str(datos[1] or "").strip()
        hor_n = str(datos[2] or "").strip()
        for (n, d) in db.leer_tabla(empresa, "ventas"):
            if (str(d[3] or "").strip() == cli_n and str(d[5] or "").strip() == prod_n
                    and str(d[6] or "").strip() == cant_n and str(d[12] or "").strip() == tot_n
                    and str(d[1] or "").strip() == fec_n and str(d[2] or "").strip() == hor_n):
                return respuesta_success({"registrado": True, "duplicado": True, "fila": n})

    db.guardar_fila(empresa, tabla_key.lower(), fila, fila_valores)

    return respuesta_success({
        "id": fila - TABLAS[tabla_key]["FILA_INICIO"] + 1,
        "fila": fila, "tabla": table_name, "registrado": True,
    })


def action_pagar_deudor(params):
    clave = str(params.get("sheetName") or params.get("idEmpresa") or "").strip()
    cliente = str(params.get("clienteNombre") or params.get("clientName") or "").strip()
    producto = str(params.get("productoNombre") or params.get("productName") or "").strip()
    try:
        transferencia = float(params.get("transferAmount") or 0)
    except (TypeError, ValueError):
        transferencia = 0.0
    try:
        efectivo = float(params.get("cashAmount") or 0)
    except (TypeError, ValueError):
        efectivo = 0.0
    monto_pagado = transferencia + efectivo

    if not clave:
        return respuesta_error("No se recibió sheetName.")
    if not cliente:
        return respuesta_error("No se recibió el nombre del cliente.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)

    fila_deudor = None
    fila_datos = None
    for (n, d) in sorted(db.leer_tabla(empresa, "deudores"), key=lambda f: f[0]):
        nom_cliente = str(d[1] or "").strip()
        nom_producto = str(d[2] or "").strip()
        if nom_cliente == cliente and (producto == "" or nom_producto == producto):
            fila_deudor = n
            fila_datos = d
            break
    if fila_deudor is None:
        return respuesta_error("No se encontró el deudor: " + cliente)

    total_pendiente = _to_float(fila_datos[7])
    transf_previa = _to_float(fila_datos[5])
    efectivo_previo = _to_float(fila_datos[6])
    nueva_transferencia = transf_previa + transferencia
    nuevo_efectivo = efectivo_previo + efectivo
    nuevo_pendiente = max(total_pendiente - monto_pagado, 0)

    fila_datos[5] = nueva_transferencia
    fila_datos[6] = nuevo_efectivo
    fila_datos[7] = nuevo_pendiente
    db.guardar_fila(empresa, "deudores", fila_deudor, fila_datos)

    movido_a_ventas = False
    if nuevo_pendiente == 0:
        usuario = str(params.get("usuario") or params.get("userName")
                      or params.get("usuarioNombre") or "").strip()
        movido_a_ventas = mover_deudor_a_ventas(empresa, fila_deudor, fila_datos,
                                                nueva_transferencia, nuevo_efectivo, usuario)

    return respuesta_success({
        "cliente": cliente, "pagado": monto_pagado,
        "totalPendiente": nuevo_pendiente, "pagadoOk": True,
        "movidoAVentas": movido_a_ventas,
    })


def action_asignar_perdedor(params):
    """Escribe el nombre del perdedor (col 9) de un chico/orden deudor.
    En bolirrana una cuenta (cliente) acumula varios chicos; el perdedor se
    asigna después, a un chico concreto. Si se envía chico, solo se toca ese
    chico para no afectar rondas de otras bolirranas. Si hay varios chicos del mismo
    producto, se edita el primero que aún no tenga perdedor.
    ponytail: coincidencia por cliente+producto (el app no reenvía la fila);
    si un mismo chico necesitara edición fina, pasar tambien fecha para cotejar."""
    clave = str(params.get("sheetName") or params.get("idEmpresa") or "").strip()
    cliente = str(params.get("clienteNombre") or params.get("clientName") or "").strip()
    producto = str(params.get("productoNombre") or params.get("productName") or "").strip()
    perdedor = str(params.get("perdedor") or "").strip()
    try:
        chico = int(params.get("chico") or 0)
    except (TypeError, ValueError):
        chico = 0
    if not clave:
        return respuesta_error("No se recibió sheetName.")
    if not cliente:
        return respuesta_error("No se recibió el nombre del cliente.")
    if not perdedor:
        return respuesta_error("No se recibió el nombre del perdedor.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)

    for (n, d) in sorted(db.leer_tabla(empresa, "deudores"), key=lambda f: f[0]):
        nom_cliente = str(d[1] or "").strip()
        if nom_cliente.lower() != cliente.lower():
            continue
        if producto and str(d[2] or "").strip().lower() != producto.lower():
            continue
        if chico > 0 and str(d[10] if len(d) > 10 else "").strip() != str(chico):
            continue
        if str(d[9] or "").strip():
            continue
        d[9] = perdedor
        db.guardar_fila(empresa, "deudores", n, d)
        return respuesta_success({"cliente": cliente, "producto": producto, "perdedor": perdedor})
    return respuesta_error("No se encontró el chico sin perdedor para: " + cliente)


def action_dividir_chico(params):
    """Divide el total pendiente de un chico (col 10) de una bolirrana entre
    varias personas: crea (o recarga) una cuenta deudora por persona con su
    parte y borra las filas del chico de la cuenta bolirrana.
    partes = [nombre1, nombre2, ...] -> reparto equitativo del total del chico.
    ponytail: reparto equitativo; si se necesitara importe por persona, pasar
    partes como [{"nombre": x, "monto": y}]."""
    clave = str(params.get("sheetName") or params.get("idEmpresa") or "").strip()
    bolirrana = str(params.get("clienteNombre") or params.get("clientName") or "").strip()
    partes = params.get("partes") or []
    if isinstance(partes, str):
        try:
            partes = json.loads(partes)
        except (json.JSONDecodeError, TypeError):
            partes = []
    try:
        chico = int(params.get("chico") or 0)
    except (TypeError, ValueError):
        chico = 0
    nombres = [str(p.get("nombre") if isinstance(p, dict) else p or "").strip()
               for p in partes if p]
    nombres = [n for n in nombres if n]
    if not clave:
        return respuesta_error("No se recibió sheetName.")
    if not bolirrana:
        return respuesta_error("No se recibió el nombre de la bolirrana.")
    if chico < 0:
        return respuesta_error("No se recibió el número de chico.")
    if not nombres:
        return respuesta_error("No se recibieron personas para dividir.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)

    filas_chico = []
    for (n, d) in sorted(db.leer_tabla(empresa, "deudores"), key=lambda f: f[0]):
        if str(d[1] or "").strip().lower() != bolirrana.lower():
            continue
        chico_txt = str(d[10] if len(d) > 10 else "").strip()
        if chico == 0:
            # Legado sin número: solo filas sin chico asignado.
            if chico_txt not in ("", "0"):
                continue
        elif chico_txt != str(chico):
            continue
        filas_chico.append((n, d))
    if not filas_chico:
        return respuesta_error("No se encontró el chico " + str(chico) + " de " + bolirrana)

    total = sum(_to_float(d[7]) for (_, d) in filas_chico)
    share = (total / len(nombres)) if len(nombres) else 0
    fecha = str(filas_chico[0][1][0] or "").strip()
    # Detalle transparente: conserva cada producto del chico para que el deudor
    # sepa exactamente qué perdió. Solo se tocan las filas de esta bolirrana+chico.
    lineas = []
    for (_, d) in filas_chico:
        cant = int(_to_float(d[3]) or 1)
        nom = str(d[2] or "").strip() or "Producto"
        lineas.append(str(cant) + "x " + nom)
    detalle = " + ".join(lineas) or bolirrana
    es_unico = len(nombres) == 1
    for persona in nombres:
        if es_unico:
            producto = bolirrana + " • Chico " + str(chico) + " • Perdedor " + persona + " • " + detalle
            perdedor_val = persona
        else:
            producto = bolirrana + " • Chico " + str(chico) + " • Dividido " + str(len(nombres)) + " • " + detalle
            perdedor_val = "Dividido: " + ", ".join(nombres)
        fila_nueva = db.siguiente_fila_libre(empresa, "deudores", TABLAS["DEUDORES"]["FILA_INICIO"])
        datos = [fecha, persona, producto, "1",
                 "1", 0, 0, _normalizar_numero(share), "Bolirrana", perdedor_val, chico]
        db.guardar_fila(empresa, "deudores", fila_nueva,
                        [str(x) if x is not None else "" for x in datos])
    for (n, _) in filas_chico:
        db.borrar_fila(empresa, "deudores", n)
    return respuesta_success({
        "bolirrana": bolirrana, "chico": chico, "total": _normalizar_numero(total),
        "personas": nombres, "parte": _normalizar_numero(share),
    })


def _to_float(v):
    try:
        num = float(v)
        return num if num == num else 0.0
    except (TypeError, ValueError):
        return 0.0


def _normalizar_numero(v):
    """5.0 -> 5, 2500.5 -> 2500.5, 'abc'/vacio -> se deja como llego (texto o '')."""
    if v is None or str(v).strip() == "":
        return ""
    try:
        num = float(v)
    except (TypeError, ValueError):
        return str(v).strip()
    if num == int(num):
        return int(num)
    return round(num, 2)


def _fecha_iso_a_latina(texto):
    """'2026-08-30' -> '30/08/2026'; si ya viene dd/mm/yyyy (o no parsea) se deja igual."""
    if not texto:
        return texto
    t = str(texto).strip()
    try:
        return datetime.datetime.strptime(t[:10], "%Y-%m-%d").strftime("%d/%m/%Y")
    except ValueError:
        return t


def _resolver_id_producto(empresa, nombre_producto):
    nombre = (nombre_producto or "").strip().lower()
    if not nombre:
        return ""
    try:
        for (n, d) in db.leer_tabla(empresa, "inventario"):
            if str(d[2] or "").strip().lower() == nombre:
                return str(d[0] or "").strip()
    except Exception:
        pass
    return ""


def mover_deudor_a_ventas(empresa, fila_deudor, datos, nueva_transferencia, nuevo_efectivo, usuario=""):
    fila_venta = db.siguiente_fila_libre(empresa, "ventas", TABLAS["VENTAS"]["FILA_INICIO"])
    total = nueva_transferencia + nuevo_efectivo
    cantidad = _to_float(datos[3]) or 1
    precio_unitario = total / cantidad if cantidad else total

    fecha_hora = str(datos[0] or "").strip().split()
    fecha = _fecha_iso_a_latina(fecha_hora[0] if fecha_hora else "")
    hora = fecha_hora[1] if len(fecha_hora) > 1 else ""

    id_venta = db.siguiente_id(empresa, "ventas", "V-")
    id_producto = _resolver_id_producto(empresa, datos[2])
    fila_valores = [
        id_venta, fecha, hora, datos[1] or "", id_producto, datos[2] or "",
        _normalizar_numero(cantidad), _normalizar_numero(precio_unitario),
        _normalizar_numero(total), "", _normalizar_numero(nueva_transferencia),
        _normalizar_numero(nuevo_efectivo), _normalizar_numero(total), usuario,
        "Activo", "", "", "", "", "", "",
        str(datos[8] or "").strip() or "Normal",
    ]
    db.guardar_fila(empresa, "ventas", fila_venta, fila_valores)
    db.borrar_fila(empresa, "deudores", fila_deudor)
    return True


def action_eliminar_empresa(params):
    clave = str(params.get("empresaNombre") or params.get("sheetName")
                or params.get("idEmpresa") or "").strip()
    if not clave:
        return respuesta_error("No se recibió el nombre de la empresa.")
    emp = db.buscar_empresa(clave)
    if not emp:
        return respuesta_error("Empresa no encontrada en la Base Maestra.")
    db.marcar_empresa_eliminada(emp["id"], fecha_actual())
    return respuesta_success({"empresa": clave, "eliminada": True})


def action_eliminar_usuario(params):
    correo = str(params.get("userEmail") or params.get("correo") or "").lower().strip()
    if not correo:
        return respuesta_error("No se recibió el correo del usuario.")
    clave = ""
    # Preferir el código exacto de empresa; el nombre puede tener mayúsculas,
    # tildes o espacios que rompen la resolución por nombre.
    codigo_empresa = str(params.get("idEmpresa") or params.get("codigo")
                         or params.get("codigoEmpresa") or "").strip()
    empresa = resolver_hoja(codigo_empresa) if codigo_empresa else None
    if empresa is None:
        clave = str(params.get("sheetName") or params.get("empresaNombre")
                    or params.get("idEmpresa") or "").strip()
        if not clave:
            return respuesta_error("No existe la hoja: " + clave)
        empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + (codigo_empresa or clave))

    fila_usr = None
    for (n, d) in db.leer_tabla(empresa, "usuarios"):
        if str(d[2] or "").lower().strip() == correo:
            fila_usr = n
            break
    if fila_usr is None:
        return respuesta_error("Usuario no encontrado: " + correo)
    db.borrar_fila(empresa, "usuarios", fila_usr)
    return respuesta_success({"correo": correo, "eliminado": True})


def action_actualizar_contrasena(params):
    clave = str(params.get("sheetName") or params.get("empresaNombre")
                or params.get("idEmpresa") or "").strip()
    correo = str(params.get("userEmail") or params.get("correo") or "").lower().strip()
    clave_nueva = str(params.get("clave") or "").strip()
    if not clave:
        return respuesta_error("No existe la hoja: " + clave)
    if not correo or not clave_nueva:
        return respuesta_error("No se recibió correo o contraseña.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)
    db.actualizar_contrasena(empresa, correo, _hash_password(clave_nueva))
    return respuesta_success({"correo": correo, "actualizado": True})


def action_eliminar_producto(params):
    clave = str(params.get("sheetName") or params.get("idEmpresa")
                or params.get("empresa") or "").strip()
    if not clave:
        return respuesta_error("No se recibió la empresa.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)
    nombre = str(params.get("producto") or params.get("nombre")
                or params.get("idProducto") or "").strip()
    if not nombre:
        return respuesta_error("No se recibió el nombre del producto.")
    borrados = 0
    for (n, d) in db.leer_tabla(empresa, "inventario"):
        if str(d[2] or "").strip().lower() == nombre.lower():
            db.borrar_fila(empresa, "inventario", n)
            borrados += 1
    return respuesta_success({"empresa": empresa, "producto": nombre, "eliminados": borrados})


def action_reportes(params):
    id_empresa = str(params.get("idEmpresa") or "").strip()
    tipo = str(params.get("tipo") or "ventas").strip().lower()
    periodo = str(params.get("periodo") or "mes").strip().lower()

    if not id_empresa:
        return respuesta_error("No se recibió idEmpresa.")
    if tipo not in ("ventas",):
        return respuesta_error("Tipo de reporte no soportado: " + tipo)

    empresa = resolver_hoja(id_empresa)
    if not empresa:
        return respuesta_error("No existe la hoja: " + id_empresa)

    fechas = obtener_fechas_periodo(periodo)
    if not fechas:
        return respuesta_error("No se pudo determinar el período: " + periodo)

    ventas = leer_hoja_rows(empresa, "VENTAS")
    objetos = []
    for r in ventas:
        if not str(r[0] or "").strip().startswith("V-"):
            continue
        objetos.append({
            "fecha": r[1] or "", "hora": r[2] or "", "total": _to_float(r[12]),
            "transferencia": _to_float(r[10]), "efectivo": _to_float(r[11]),
            "metodoPago": "TRANSFERENCIA" if _to_float(r[10]) > 0 else "EFECTIVO",
            "cliente": r[3] or "", "producto": r[5] or "", "estado": r[14] or "",
        })

    desde = fechas["fechaInicio"]
    hasta = fechas["fechaFin"]
    filtradas = [v for v in objetos if desde <= _parse_fecha(v["fecha"], desde) <= hasta]

    total = sum(_to_float(v["total"]) for v in filtradas)
    generado = generar_reporte_ventas(filtradas, total)
    return JSONResponse({
        "status": "success", "action": "reportes", "idEmpresa": id_empresa,
        "tipo": tipo, "periodo": periodo, "datos": generado,
    })


def _parse_fecha(texto, default):
    try:
        return datetime.datetime.strptime(str(texto)[:16], "%Y-%m-%d %H:%M")
    except ValueError:
        return default


def obtener_fechas_periodo(periodo):
    ahora = datetime.datetime.now()
    if periodo in ("mes", "este_mes"):
        inicio = ahora.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        prox = (inicio + datetime.timedelta(days=32)).replace(day=1)
        fin = prox - datetime.timedelta(seconds=1)
    elif periodo in ("semana", "esta_semana"):
        inicio = (ahora - datetime.timedelta(days=ahora.weekday())).replace(
            hour=0, minute=0, second=0, microsecond=0)
        fin = inicio + datetime.timedelta(days=6, hours=23, minutes=59, seconds=59)
    elif periodo in ("hoy", "dia"):
        inicio = ahora.replace(hour=0, minute=0, second=0, microsecond=0)
        fin = inicio + datetime.timedelta(days=1) - datetime.timedelta(seconds=1)
    else:
        return None
    return {"fechaInicio": inicio, "fechaFin": fin}


def generar_reporte_ventas(registros, total):
    cantidad = len(registros)
    metodos = {}
    por_dia = {}
    mayor = 0
    menor = 0
    for i, v in enumerate(registros):
        t = _to_float(v["total"])
        if i == 0 or t > mayor:
            mayor = t
        if i == 0 or t < menor:
            menor = t
        m = v.get("metodoPago") or "SIN_ESPECIFICAR"
        metodos.setdefault(m, {"cantidad": 0, "total": 0})
        metodos[m]["cantidad"] += 1
        metodos[m]["total"] += t
        dia = str(_parse_fecha(v["fecha"], datetime.datetime(2000, 1, 1)).strftime("%Y-%m-%d"))
        por_dia.setdefault(dia, {"cantidad": 0, "total": 0})
        por_dia[dia]["cantidad"] += 1
        por_dia[dia]["total"] += t

    resumen_metodos = [
        {"metodo": m, "cantidad": d["cantidad"], "total": round(d["total"], 2),
         "porcentaje": round(d["total"] / total * 100, 2) if total else 0}
        for m, d in metodos.items()
    ]
    resumen_diario = [
        {"fecha": d, "cantidad": x["cantidad"], "total": round(x["total"], 2)}
        for d, x in sorted(por_dia.items())
    ]
    return {
        "resumen": {
            "cantidadVentas": cantidad, "totalVentas": round(total, 2),
            "ventaPromedio": round(total / cantidad, 2) if cantidad else 0,
            "ventaMayor": round(mayor, 2), "ventaMenor": round(menor, 2),
        },
        "metodosPago": resumen_metodos,
        "ventasPorDia": resumen_diario,
        "ventas": registros,
    }


def action_ping(params=None):
    return respuesta_success({"mensaje": "KAPTA IA API funcionando", "fecha": fecha_actual()})


DIAS_POR_TIEMPO = {
    "mensual": 30, "1 mes": 30, "mes": 30,
    "anual": 365, "ano": 365, "año": 365,
    "prueba 15 dias": 15, "prueba 15 días": 15,
}


def action_comprar_plan(params):
    codigo = str(params.get("codigo") or params.get("idEmpresa") or "").strip()
    plan = str(params.get("plan") or "").strip()
    tiempo = str(params.get("tiempo") or "Mensual").strip()
    if not codigo:
        return respuesta_error("Debe ingresar el código de la empresa.")
    if not plan:
        return respuesta_error("Debe ingresar el plan.")

    import unicodedata
    clave_tiempo = "".join(
        c for c in unicodedata.normalize("NFD", tiempo.lower().strip())
        if not unicodedata.combining(c)
    )
    dias = DIAS_POR_TIEMPO.get(clave_tiempo)
    if dias is None:
        return respuesta_error("Tiempo no válido. Use Mensual, Anual o Prueba 15 días.")

    fecha_venc = (datetime.date.today() + datetime.timedelta(days=dias)).isoformat()
    db.comprar_plan_db(
        codigo, plan, tiempo,
        monto=str(params.get("monto") or ""),
        fecha_vencimiento=fecha_venc,
        usuario=str(params.get("usuario") or ""),
    )
    return respuesta_success({
        "codigo": codigo, "plan": plan, "tiempo": tiempo,
        "fechaVencimiento": fecha_venc, "estado": "Activo",
    })


def action_registrar_finanza_kapta(params):
    tipo = str(params.get("tipo") or "Egreso").strip().capitalize()
    if tipo not in ("Ingreso", "Egreso"):
        return respuesta_error("Tipo debe ser Ingreso o Egreso.")
    concepto = str(params.get("concepto") or "").strip()
    monto = params.get("monto")
    if not concepto:
        return respuesta_error("Debe ingresar el concepto.")
    if monto in (None, ""):
        return respuesta_error("Debe ingresar el monto.")
    db.registrar_finanza_kapta(
        tipo=tipo,
        categoria=str(params.get("categoria") or ""),
        concepto=concepto,
        monto=monto,
        metodo_pago=str(params.get("metodoPago") or params.get("metodo_pago") or ""),
        referencia=str(params.get("referencia") or ""),
        usuario=str(params.get("usuario") or ""),
    )
    return respuesta_success({"mensaje": "Registro financiero guardado", "tipo": tipo})


def action_listar_finanzas_kapta(params=None):
    registros = db.listar_finanzas_kapta()
    ingresos = sum(float(r["monto"]) for r in registros
                   if r["tipo"] == "Ingreso" and _es_numero(r["monto"]))
    egresos = sum(float(r["monto"]) for r in registros
                  if r["tipo"] == "Egreso" and _es_numero(r["monto"]))
    return respuesta_success({
        "registros": registros,
        "totalIngresos": round(ingresos, 2),
        "totalEgresos": round(egresos, 2),
        "balance": round(ingresos - egresos, 2),
    })


def action_listar_todos_usuarios(params=None):
    """Superadmin: todos los usuarios de TODAS las empresas (tabla global)."""
    filas = db.leer_tabla_global_todos("usuarios")
    usuarios = []
    for f in filas:
        usuarios.append({
            "codigoEmpresa": f.get("Codigo_Empresa", ""),
            "idUsuario": f.get("Id_Usuario", ""),
            "nombre": f.get("Nombre", ""),
            "correo": f.get("Correo", ""),
            "contrasena": f.get("Contrasena", ""),
            "rol": f.get("Rol", ""),
            "estado": f.get("Estado", ""),
            "fechaCreacion": f.get("Fecha_Creacion", ""),
            "ultimoAcceso": f.get("Ultimo_Acceso", ""),
            "fechaCambioEstado": f.get("Fecha_Cambio_Estado", ""),
            "motivoCambio": f.get("Motivo_Cambio", ""),
            "cambiadoPor": f.get("Cambiado_Por", ""),
            "funciones": f.get("Funciones", ""),
        })
    return respuesta_success({"usuarios": usuarios, "total": len(usuarios)})


def _es_numero(v):
    try:
        float(str(v).replace(",", "").strip())
        return True
    except (ValueError, TypeError):
        return False


# ===================================================
# ROUTING
# ===================================================
def action_subir_foto(params):
    datos = str(params.get("datos") or "")
    if len(datos) < 100:
        return respuesta_error("Imagen no válida.")
    clave = str(params.get("idEmpresa") or params.get("sheetName") or params.get("empresa") or "").strip()
    empresa = resolver_hoja(clave) if clave else ""
    if not empresa:
        empresa = clave.upper()
    foto_id = db.guardar_foto(datos, empresa)
    return respuesta_success({"id": foto_id, "url": "/foto/" + foto_id})


def action_listar_fotos(params=None):
    """Superadmin: qué imágenes pertenecen a cada empresa (foto id cifrado -> empresa + fecha)."""
    empresa = str((params or {}).get("empresa") or "").strip().upper()
    fotos = db.listar_fotos(empresa)
    return respuesta_success({"fotos": fotos, "total": len(fotos)})


def action_registrar_soporte(params):
    datos = params.get("data") or params
    tipo = str(datos.get("tipo_solicitud") or datos.get("tipo") or "").strip()
    obs = str(datos.get("observaciones") or "").strip()
    solic = str(datos.get("solicitante") or datos.get("codigo") or "").strip().upper()
    if not tipo:
        return respuesta_error("Debe indicar el tipo de solicitud.")
    nuevo_id = db.registrar_soporte(tipo, obs, solic)
    return respuesta_success({"idSoporte": nuevo_id})


def action_listar_soportes(params=None):
    return respuesta_success({"data": db.listar_soportes()})


def action_obtener_clave_dinamica(params=None):
    p = params or {}
    empresa = str(p.get("empresa") or p.get("codigo") or p.get("codigo_empresa") or p.get("idEmpresa") or p.get("sheetName") or "").strip()
    if not empresa:
        return respuesta_error("Falta empresa/codigo para clave dinamica.")
    cod = resolver_hoja(empresa) or empresa
    row = db.obtener_clave_dinamica_db(cod)
    if row is None:
        return respuesta_error("No se pudo generar la clave.")
    import time
    ahora = int(time.time() * 1000)
    segundos = max(0, (int(row.get("expira") or 0) - ahora) // 1000)
    return respuesta_success({"codigo": row.get("codigo"), "expira": row.get("expira"), "segundosRestantes": segundos, "empresa": row.get("empresa")})


def action_validar_clave_dinamica(params=None):
    p = params or {}
    empresa = str(p.get("empresa") or p.get("codigo") or p.get("codigo_empresa") or p.get("idEmpresa") or p.get("sheetName") or "").strip()
    codigo = str(p.get("codigo") or p.get("clave") or p.get("code") or "").strip()
    if not empresa or not codigo:
        return respuesta_error("Falta empresa o codigo.")
    cod = resolver_hoja(empresa) or empresa
    res = db.validar_clave_dinamica_db(cod, codigo)
    if res.get("ok"):
        return respuesta_success({"valida": True, "nuevo_codigo": res.get("nuevo_codigo")})
    return respuesta_success({"valida": False, "motivo": res.get("motivo"), "nuevo_codigo": res.get("nuevo_codigo")})


def action_importar_inventario(params):
    """Carga masiva de inventario desde CSV/TXT.
    Columnas (7): Nombre, Categoria, Costo, Precio, PrecioMinimo(vacio=sin valor),
    StockActual, StockMinimo. El backend autogenera Id_Producto."""
    clave = str(params.get("sheetName") or params.get("idEmpresa") or params.get("empresa") or "").strip()
    if not clave:
        return respuesta_error("No se recibió la empresa.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)
    raw = params.get("csv")
    if not isinstance(raw, str):
        raw = params.get("contenido")
    if not isinstance(raw, str) or not raw.strip():
        return respuesta_error("El archivo está vacío.")
    lineas = [l.strip() for l in raw.replace("\r\n", "\n").split("\n") if l.strip()]
    if not lineas:
        return respuesta_error("El archivo no contiene filas.")
    insertados = 0
    errores = []
    hoy = fecha_actual()
    for i, linea in enumerate(lineas, start=1):
        partes = [p.strip() for p in linea.split(",")]
        if len(partes) < 7:
            errores.append(f"Fila {i}: se esperaban 7 columnas, tiene {len(partes)}")
            continue
        nombre = partes[0]
        if nombre.lower() in ("nombre", "nombre del producto", "producto"):
            continue  # cabecera
        datos = ["", "", partes[0], partes[1], partes[5], partes[2],
                 partes[3], partes[4], partes[6], "Activo", hoy, hoy]
        sub = dict(params)
        sub["tableName"] = "inventario"
        sub["data"] = datos
        r = action_escribir_fila(sub)
        body = json.loads(r.body)
        if body.get("status") == "success":
            insertados += 1
        else:
            errores.append(f"Fila {i} ({partes[0]}): {body.get('message', 'error')}")
    return respuesta_success({"insertados": insertados, "errores": errores, "total": len(lineas)})


def action_registrar_movimiento(params):
    """Kardex por negocio: Id_Movimiento, Fecha, Id_Producto, Nom_Producto,
    Tipo, Cantidad, Stock_Anterior, Stock_Nuevo, Usuario, Observacion."""
    clave = str(params.get("sheetName") or params.get("idEmpresa") or "").strip()
    if not clave:
        return respuesta_error("No se recibió sheetName.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)
    producto = str(params.get("producto") or params.get("nomProducto") or "").strip()
    if not producto:
        return respuesta_error("No se recibió el producto del movimiento.")
    fila = db.siguiente_fila_libre(empresa, "movimientos", TABLAS["MOVIMIENTOS"]["FILA_INICIO"])
    datos = [
        db.siguiente_id(empresa, "movimientos", "M-", 5),
        str(params.get("fecha") or fecha_actual()),
        str(params.get("idProducto") or ""),
        producto,
        str(params.get("tipo") or ""),
        _normalizar_numero(params.get("cantidad")),
        _normalizar_numero(params.get("stockAnterior")),
        _normalizar_numero(params.get("stockNuevo")),
        str(params.get("usuario") or ""),
        str(params.get("observacion") or ""),
    ]
    db.guardar_fila(empresa, "movimientos", fila, datos)
    return respuesta_success({"movimiento": datos[0], "producto": producto})


POST_ACTIONS = {
    "reportes": action_reportes,
    "login": action_login,
    "registrar_empresa": action_registrar_empresa,
    "actualizar_empresa": action_actualizar_empresa,
    "listar_funciones": action_listar_funciones,
    "crear_funcion": action_crear_funcion,
    "eliminar_funcion": action_eliminar_funcion,
    "read": action_read,
    "obtener_todo": action_obtener_todo,
    "pagar_deudor": action_pagar_deudor,
    "asignar_perdedor": action_asignar_perdedor,
    "dividir_chico": action_dividir_chico,
    "eliminar_empresa": action_eliminar_empresa,
    "actualizar_contrasena": action_actualizar_contrasena,
    "eliminar_usuario": action_eliminar_usuario,
    "eliminar_producto": action_eliminar_producto,
    "comprar_plan": action_comprar_plan,
    "registrar_finanza_kapta": action_registrar_finanza_kapta,
    "registrar_soporte": action_registrar_soporte,
    "subir_foto": action_subir_foto,
    "importar_inventario": action_importar_inventario,
    "dedup_inventario": action_dedup_inventario,
    "registrar_inventario": action_escribir_fila,
    "registrar_movimiento": action_registrar_movimiento,
    "login_google": action_login_google,
    "apple_auth_url": action_apple_auth_url,
    "login_apple_canjear": action_login_apple_canjear,
    "registrar_venta": action_escribir_fila,
    "registrar_deudor": action_escribir_fila,
    "registrar_gasto": action_escribir_fila,
    "crear_usuario": action_escribir_fila,
    "append": action_escribir_fila,
    "validar_clave_dinamica": action_validar_clave_dinamica,
    "validar_clave": action_validar_clave_dinamica,
    "obtener_clave_dinamica": action_obtener_clave_dinamica,
    "obtener_clave": action_obtener_clave_dinamica,
}


GET_ACTIONS = {
    "listar_empresas": action_listar_empresas,
    "listarempresas": action_listar_empresas,
    "getcompanies": action_listar_empresas,
    "get_companies": action_listar_empresas,
    "read": action_read,
    "ping": action_ping,
    "saludo": action_ping,
    "listar_finanzas_kapta": action_listar_finanzas_kapta,
    "listar_soportes": action_listar_soportes,
    "listar_todos_usuarios": action_listar_todos_usuarios,
    "listar_fotos": action_listar_fotos,
    "obtener_clave_dinamica": action_obtener_clave_dinamica,
    "obtener_clave": action_obtener_clave_dinamica,
    "get_clave": action_obtener_clave_dinamica,
}


@app.on_event("startup")
def _startup():
    db.init_db()
    asegurar_admin()


import base64 as _b64
from fastapi.responses import Response as _Response


@app.get("/foto/{foto_id}")
def servir_foto(foto_id: str):
    data = db.obtener_foto(foto_id)
    if not data:
        return JSONResponse({"status": "error", "message": "Foto no encontrada"}, status_code=404)
    try:
        raw = _b64.b64decode(data)
    except Exception:
        raw = str(data).encode()
    # ponytail: respetar transparencia de PNG (sin fondo) detectando la firma mágica
    media_type = "image/png" if raw[:8] == b"\x89PNG\r\n\x1a\n" else "image/jpeg"
    return _Response(content=raw, media_type=media_type)


@app.api_route("/exec", methods=["GET", "POST"])
@app.api_route("/", methods=["GET", "POST"])
async def endpoint(request: Request):
    if request.method == "GET":
        params = dict(request.query_params)
        action = str(params.get("action") or "").lower().strip()
        if action in GET_ACTIONS:
            try:
                return GET_ACTIONS[action](params)
            except Exception as e:
                return respuesta_error("Error interno: " + str(e))
        if not action:
            return respuesta_success({
                "status": "success", "api": "KAPTA IA",
                "mensaje": "API funcionando correctamente",
                "acciones": ["listar_empresas", "getCompanies", "read", "login",
                             "registrar_inventario", "registrar_venta",
                             "registrar_deudor", "registrar_gasto", "crear_usuario",
                             "pagar_deudor", "obtener_todo", "eliminar_empresa",
                             "eliminar_usuario", "reportes", "ping",
                             "asignar_perdedor", "dividir_chico",
                             "registrar_movimiento"],
            })
        return respuesta_error("Acción GET no válida: " + action)

    # POST
    try:
        body = await request.json()
    except Exception:
        return respuesta_error("El JSON recibido no es válido.")
    if not isinstance(body, dict):
        return respuesta_error("El JSON recibido no es válido.")

    action = str(body.get("action") or "").lower().strip()
    if action in POST_ACTIONS:
        try:
            return POST_ACTIONS[action](body)
        except Exception as e:
            return respuesta_error("Error interno en " + action + ": " + str(e))
    if action in GET_ACTIONS:
        try:
            return GET_ACTIONS[action](body)
        except Exception as e:
            return respuesta_error("Error interno en " + action + ": " + str(e))
    return respuesta_error("Acción no válida: " + action)