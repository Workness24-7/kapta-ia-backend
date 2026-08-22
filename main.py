# main.py — KAPTA IA Backend FastAPI (contrato idéntico a api.gs)
# Reemplaza Google Apps Script. La app Android solo cambia APPS_SCRIPT_WEB_APP_URL.
import datetime
import json
import uuid

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

import db

app = FastAPI(title="KAPTA IA API", docs_url=None, redoc_url=None)

VERSION_API = "KAPTA-1.0.0"

# ===================================================
# TABLAS INTERNAS DE CADA EMPRESA (ESPEJO DE config.gs)
# Por negocio: ventas, inventario, deudores, movimientos.
# Globales (una sola tabla física con Código_Empresa al inicio):
# gastos, auditoria_gastos, usuarios, config_negocio.
# Eliminadas: estadisticas, ia.
# ===================================================
TABLAS = {
    "INVENTARIO": {"INICIO": 1, "FILA_INICIO": 3, "COLUMNAS": 12},
    "VENTAS": {"INICIO": 14, "FILA_INICIO": 3, "COLUMNAS": 21},
    "DEUDORES": {"INICIO": 36, "FILA_INICIO": 3, "COLUMNAS": 8},
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
               "Fecha_Anulacion", "Hora_Anulacion", "Anulado_Por"],
    "DEUDORES": ["Fecha_Registro", "Nom_Cliente", "Producto", "Cantidad",
                 "Minimo", "Transferencia", "Efectivo", "Total_Pendiente"],
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
                 "Fecha_Cambio_Estado", "Motivo_Cambio", "Cambiado_Por"],
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
    empresas = db.listar_empresas_db()
    try:
        db.purgar_empresas_eliminadas()
    except Exception:
        pass  # la purga diferida nunca debe romper el listado
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


def action_login(params):
    codigo = str(params.get("codigo") or "").strip().upper()
    correo = str(params.get("correo") or params.get("Correo_Admin") or "").lower().strip()
    password = str(params.get("password") or params.get("Contraseña_Admin")
                   or params.get("Contrasena_Admin") or "")
    if not codigo:
        return respuesta_error("Debe ingresar el código de acceso.")
    if not correo:
        return respuesta_error("Debe ingresar el correo.")
    if not password:
        return respuesta_error("Debe ingresar la contraseña.")

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

    usuario = None
    for (n, d) in db.leer_tabla(empresa, "usuarios"):
        if str(d[2] or "").lower().strip() == correo:
            usuario = d
            break
    if not usuario:
        return respuesta_error("Usuario no encontrado.")
    if str(usuario[5] or "") in ("Suspendido", "Bloqueado"):
        return respuesta_error("Usuario " + str(usuario[5]).lower() + ".")
    if str(usuario[3] or "") != password:
        return respuesta_error("Contraseña incorrecta.")

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

    if db.buscar_empresa(codigo):
        return respuesta_error("El código de acceso ya existe.")

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
    })

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
        "password": str(datos.get("adminPassword") or ""),
    }
    fila_admin = ["USR-" + uuid.uuid4().hex[:8].upper(), admin["nombre"],
                  admin["correo"], admin["password"], "Administrador", "Activo",
                  fecha_hoy, "", "", "", ""]
    db.guardar_fila(codigo, "usuarios", 3, fila_admin)

    return respuesta_success({"idEmpresa": id_empresa, "codigo": codigo,
                              "empresa": nombre})


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

    prefijo = prefijo_id_tabla(table_name)
    if prefijo and str(datos[0] or "").strip() == "":
        datos = list(datos)
        datos[0] = db.siguiente_id(empresa, tabla_key.lower(), prefijo)

    fila = db.siguiente_fila_libre(empresa, tabla_key.lower(), TABLAS[tabla_key]["FILA_INICIO"])
    columnas = TABLAS[tabla_key]["COLUMNAS"]
    fila_valores = datos[:columnas] + [""] * max(0, columnas - len(datos))
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
        movido_a_ventas = mover_deudor_a_ventas(empresa, fila_deudor, fila_datos,
                                                nueva_transferencia, nuevo_efectivo)

    return respuesta_success({
        "cliente": cliente, "pagado": monto_pagado,
        "totalPendiente": nuevo_pendiente, "pagadoOk": True,
        "movidoAVentas": movido_a_ventas,
    })


def _to_float(v):
    try:
        num = float(v)
        return num if num == num else 0.0
    except (TypeError, ValueError):
        return 0.0


def mover_deudor_a_ventas(empresa, fila_deudor, datos, nueva_transferencia, nuevo_efectivo):
    fila_venta = db.siguiente_fila_libre(empresa, "ventas", TABLAS["VENTAS"]["FILA_INICIO"])
    total = nueva_transferencia + nuevo_efectivo
    cantidad = _to_float(datos[3]) or 1
    precio_unitario = total / cantidad if cantidad else total

    fecha_hora = str(datos[0] or "").strip().split()
    fecha = fecha_hora[0] if fecha_hora else ""
    hora = fecha_hora[1] if len(fecha_hora) > 1 else ""

    id_venta = db.siguiente_id(empresa, "ventas", "V-")
    fila_valores = [
        id_venta, fecha, hora, datos[1] or "", "", datos[2] or "",
        cantidad, precio_unitario, total, "", nueva_transferencia,
        nuevo_efectivo, total, "", "Activo", "", "", "", "", "", "",
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
    clave = str(params.get("sheetName") or params.get("empresaNombre")
                or params.get("idEmpresa") or "").strip()
    correo = str(params.get("userEmail") or params.get("correo") or "").lower().strip()
    if not clave:
        return respuesta_error("No existe la hoja: " + clave)
    if not correo:
        return respuesta_error("No se recibió el correo del usuario.")
    empresa = resolver_hoja(clave)
    if not empresa:
        return respuesta_error("No existe la hoja: " + clave)

    fila_usr = None
    for (n, d) in db.leer_tabla(empresa, "usuarios"):
        if str(d[2] or "").lower().strip() == correo:
            fila_usr = n
            d[5] = "Suspendido"
            db.guardar_fila(empresa, "usuarios", n, d)
            break
    if fila_usr is None:
        return respuesta_error("Usuario no encontrado: " + correo)
    return respuesta_success({"correo": correo, "eliminado": True})


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
POST_ACTIONS = {
    "reportes": action_reportes,
    "login": action_login,
    "registrar_empresa": action_registrar_empresa,
    "read": action_read,
    "obtener_todo": action_obtener_todo,
    "pagar_deudor": action_pagar_deudor,
    "eliminar_empresa": action_eliminar_empresa,
    "eliminar_usuario": action_eliminar_usuario,
    "comprar_plan": action_comprar_plan,
    "registrar_finanza_kapta": action_registrar_finanza_kapta,
    "registrar_inventario": action_escribir_fila,
    "registrar_venta": action_escribir_fila,
    "registrar_deudor": action_escribir_fila,
    "registrar_gasto": action_escribir_fila,
    "crear_usuario": action_escribir_fila,
    "append": action_escribir_fila,
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
    "listar_todos_usuarios": action_listar_todos_usuarios,
}


@app.on_event("startup")
def _startup():
    db.init_db()


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
                             "eliminar_usuario", "reportes", "ping"],
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