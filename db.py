# db.py — Postgres: una tabla real por negocio y tipo (ej. test01_inventario)
# Railway es la única fuente de datos. Sin dependencia de Google Sheets.
import os
import re
import datetime
import unicodedata
from urllib.parse import urlparse

import psycopg2
from psycopg2.extras import RealDictCursor

# Espejo de CABECERAS de main.py (claves en minúsculas)
# Tablas POR NEGOCIO: datos sensibles a cruces, una tabla física por empresa.
CABECERAS = {
    "inventario": ["Id_Producto", "Codigo_Barras", "Nom_Producto", "Categoria",
                   "Cantidad", "Costo", "Precio_Venta", "Precio_Minimo",
                   "Alerta_Stock", "Estado", "Fecha_Creacion", "Ultima_Modificacion"],
    "ventas": ["Id_Venta", "Fecha", "Hora", "Cliente", "Id_Producto", "Producto",
               "Cantidad", "Precio_Unitario", "Subtotal", "Descuento",
               "Transferencia", "Efectivo", "Total", "Usuario", "Estado",
               "Fecha_Modificacion", "Hora_Modificacion", "Modificado_Por",
               "Fecha_Anulacion", "Hora_Anulacion", "Anulado_Por"],
    "deudores": ["Fecha_Registro", "Nom_Cliente", "Producto", "Cantidad",
                 "Minimo", "Transferencia", "Efectivo", "Total_Pendiente"],
    "movimientos": ["Id_Movimiento", "Fecha", "Id_Producto", "Nom_Producto",
                    "Tipo", "Cantidad", "Stock_Anterior", "Stock_Nuevo",
                    "Usuario", "Observacion"],
}

# Tablas GLOBALES: una sola tabla física; la primera columna es el código de empresa.
# A los tenants se les sirven las filas SIN la columna codigo (índices intactos).
CABECERAS_GLOBALES = {
    "usuarios": ["Codigo_Empresa", "Id_Usuario", "Nombre", "Correo", "Contrasena",
                 "Rol", "Estado", "Fecha_Creacion", "Ultimo_Acceso",
                 "Fecha_Cambio_Estado", "Motivo_Cambio", "Cambiado_Por"],
    "gastos": ["Codigo_Empresa", "Id_Gasto", "Fecha", "Hora", "Categoria",
               "Concepto", "Descripcion", "Proveedor", "Monto", "Metodo_Pago",
               "Referencia", "Usuario", "Estado", "Fecha_Modificacion",
               "Modificado_Por"],
    "config_negocio": ["Codigo_Empresa", "Parametro", "Valor", "Descripcion",
                       "Fecha_Actualizacion", "Usuario", "Observaciones"],
    "auditoria_gastos": ["Codigo_Empresa", "Id_Evento", "Id_Empresa", "Id_Gasto",
                         "Accion", "Usuario", "Fecha_Hora", "Detalles", "Estado"],
}

# Encabezados antiguos de las tablas por-negocio que se migraron a globales
# (sin Codigo_Empresa) más las tablas eliminadas. Solo para la migración.
LEGACY_CABECERAS = {
    "usuarios": ["Id_Usuario", "Nombre", "Correo", "Contraseña", "Rol",
                 "Estado", "Fecha_Creacion", "Ultimo_Acceso",
                 "Fecha_Cambio_Estado", "Motivo_Cambio", "Cambiado_Por"],
    "gastos": ["Id_Gasto", "Fecha", "Hora", "Categoria", "Concepto",
               "Descripcion", "Proveedor", "Monto", "Metodo_Pago",
               "Referencia", "Usuario", "Estado", "Fecha_Modificacion",
               "Modificado_Por"],
    "auditoria_gastos": ["Id_Evento", "Id_Empresa", "Id_Gasto", "Accion",
                         "Usuario", "Fecha_Hora", "Detalles", "Estado"],
    "config_negocio": ["Parametro", "Valor", "Descripcion", "Fecha_Actualizacion",
                       "Usuario", "Observaciones"],
    "estadisticas": None,
    "ia": None,
}

SCHEMA_EMPRESAS = """
CREATE TABLE IF NOT EXISTS empresas (
    id VARCHAR(32) PRIMARY KEY,
    nombre TEXT NOT NULL DEFAULT '',
    nit TEXT DEFAULT '',
    codigo TEXT UNIQUE,
    tipo TEXT DEFAULT '',
    pais TEXT DEFAULT '',
    ciudad TEXT DEFAULT '',
    direccion TEXT DEFAULT '',
    correo TEXT DEFAULT '',
    celular1 TEXT DEFAULT '',
    celular2 TEXT DEFAULT '',
    estado TEXT DEFAULT 'Activo',
    plan TEXT DEFAULT 'Básico',
    tiempo TEXT DEFAULT '1 Mes',
    fecha_creacion TEXT DEFAULT '',
    ultimo_acceso TEXT DEFAULT '',
    fecha_vencimiento TEXT DEFAULT '',
    observaciones TEXT DEFAULT '',
    tipo_sistema TEXT DEFAULT 'CLIENTE',
    tipo_plataforma TEXT DEFAULT 'POS'
);
"""


SCHEMA_FINANZAS_KAPTA = """
CREATE TABLE IF NOT EXISTS finanzas_kapta (
    id SERIAL PRIMARY KEY,
    fecha TEXT DEFAULT '',
    tipo TEXT DEFAULT '',
    categoria TEXT DEFAULT '',
    concepto TEXT DEFAULT '',
    monto TEXT DEFAULT '',
    metodo_pago TEXT DEFAULT '',
    referencia TEXT DEFAULT '',
    usuario TEXT DEFAULT ''
);
"""


def _normalize(s):
    return "".join(
        c for c in unicodedata.normalize("NFD", str(s or "").lower())
        if not unicodedata.combining(c)
    ).replace("ñ", "n")


def _connect():
    url = os.getenv('DATABASE_URL')
    if not url:
        raise RuntimeError('DATABASE_URL no está configurada')
    result = urlparse(url)
    conn = psycopg2.connect(
        dbname=result.path.lstrip('/'),
        user=result.username,
        password=result.password,
        host=result.hostname,
        port=result.port,
    )
    conn.autocommit = False
    return conn


def _col(nombre):
    """Nombre de columna saneado a minúsculas sin acentos."""
    s = _normalize(nombre)
    s = re.sub(r"[^a-z0-9]+", "_", s).strip("_")
    return s or "col"


COLUMNS = {k: [_col(h) for h in v] for k, v in CABECERAS.items()}
COLUMNS_GLOBALES = {k: [_col(h) for h in v] for k, v in CABECERAS_GLOBALES.items()}


def _slug(s):
    """Prefijo de negocio a partir del código de acceso."""
    s = _normalize(s)
    s = re.sub(r"[^a-z0-9]+", "_", s).strip("_")
    return s or "empresa"


def _tabname(codigo, tabla):
    """Nombre físico de la tabla: {codigo}_{tipo} (ej. test01_inventario)."""
    return f"{_slug(codigo)}_{_slug(tabla)}"


def _crear_tabla(cur, codigo, tabla):
    tbl = _tabname(codigo, tabla)
    cols = COLUMNS[tabla]
    defs = ", ".join(f'"{c}" TEXT DEFAULT \'\'' for c in cols)
    cur.execute(f'CREATE TABLE IF NOT EXISTS "{tbl}" (fila INTEGER PRIMARY KEY, {defs})')
    return tbl


def _crear_tabla_global(cur, tabla):
    """Global: PK compuesta (codigo_empresa, fila); primera col = codigo_empresa."""
    cols = COLUMNS_GLOBALES[tabla]
    defs = ", ".join(f'"{c}" TEXT DEFAULT \'\'' for c in cols)
    cur.execute(
        f'CREATE TABLE IF NOT EXISTS "{tabla}" '
        f'(fila INTEGER NOT NULL, {defs}, PRIMARY KEY ("{cols[0]}", fila))'
    )
    return tabla


def _migrar_legacy(cur, codigo):
    """Mueve {codigo}_usuarios/gastos/config/auditoria a las globales y borra
    las tablas por-negocio obsoletas ({codigo}_estadisticas, {codigo}_ia)."""
    slug = _slug(codigo)
    for legacy, glo in (("usuarios", "usuarios"), ("gastos", "gastos"),
                        ("config_negocio", "config_negocio"),
                        ("auditoria_gastos", "auditoria_gastos")):
        vieja = f"{slug}_{legacy}"
        cur.execute("SELECT 1 FROM information_schema.tables WHERE table_name=%s", (vieja,))
        if cur.fetchone() is None:
            continue
        cols_viejas = [_col(h) for h in LEGACY_CABECERAS[legacy]]
        cols_nuevas = COLUMNS_GLOBALES[glo]
        sel = ", ".join(f't."{c}"' for c in cols_viejas)
        ins_cols = ", ".join(f'"{c}"' for c in cols_nuevas)
        cur.execute(
            f'INSERT INTO "{glo}" (fila, {ins_cols}) '
            f'SELECT t.fila, %s, {sel} FROM "{vieja}" t WHERE NOT EXISTS '
            f'(SELECT 1 FROM "{glo}" g WHERE g."{cols_nuevas[0]}"=%s AND g.fila=t.fila)',
            (codigo, codigo),
        )
        # ponytail: se renombra en vez de DROP para poder revertir si algo sale
        # mal; borrar las *_backup_2026_08 una vez verificado en producción.
        cur.execute(f'ALTER TABLE "{vieja}" RENAME TO "{vieja}_backup_2026_08"')
    for obsoleta in ("estadisticas", "ia"):
        cur.execute(f'DROP TABLE IF EXISTS "{slug}_{obsoleta}"')


def init_db():
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(SCHEMA_EMPRESAS)
            cur.execute(SCHEMA_FINANZAS_KAPTA)
            for tabla_global in CABECERAS_GLOBALES:
                _crear_tabla_global(cur, tabla_global)
            cur.execute("SELECT codigo FROM empresas WHERE codigo IS NOT NULL AND codigo <> ''")
            codigos = [c for (c,) in cur.fetchall()]
            for codigo in codigos:
                _migrar_legacy(cur, codigo)
                for tabla in CABECERAS:
                    _crear_tabla(cur, codigo, tabla)
        conn.commit()


def _slug_en_uso(slug):
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT codigo FROM empresas")
            return [c for (c,) in cur.fetchall() if _slug(c) == slug]


def listar_empresas_db():
    with _connect() as conn:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute("SELECT * FROM empresas ORDER BY fecha_creacion, codigo")
            return [dict(r) for r in cur.fetchall()]


def buscar_empresa(clave):
    """buscarHojaEmpresa: match por codigo, nombre o id (insensible a acentos)."""
    if not clave:
        return None
    empresas = listar_empresas_db()
    obj = _normalize(clave)
    for e in empresas:
        if str(e["codigo"] or "").strip() == clave:
            return e
    for e in empresas:
        if (
            obj and (
                _normalize(e["codigo"]) == obj or
                _normalize(e["nombre"]) == obj or
                _normalize(e["id"]) == obj
            )
        ):
            return e
    return None


def hoja_existe(codigo):
    tbl = _tabname(codigo, "inventario")
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT 1 FROM information_schema.tables WHERE table_name=%s", (tbl,))
            return cur.fetchone() is not None


def leer_tabla(empresa, tabla):
    """Devuelve todas las filas (incluye headers en fila 2) como (fila, lista_valores).
    En tablas globales filtra por codigo de empresa y NO incluye esa columna."""
    key = _slug(tabla)
    es_global = key in CABECERAS_GLOBALES
    cols = COLUMNS_GLOBALES[key] if es_global else COLUMNS[key]
    tbl = key if es_global else _tabname(empresa, tabla)
    col_sql = ", ".join(f'"{c}"' for c in (cols[1:] if es_global else cols))
    with _connect() as conn:
        with conn.cursor() as cur:
            if es_global:
                _crear_tabla_global(cur, key)
                cur.execute(
                    f'SELECT fila, {col_sql} FROM "{tbl}" WHERE "{cols[0]}"=%s ORDER BY fila',
                    (empresa,),
                )
            else:
                _crear_tabla(cur, empresa, tabla)
                cur.execute(f'SELECT fila, {col_sql} FROM "{tbl}" ORDER BY fila')
            return [
                (row[0], [v if v is not None else "" for v in row[1:]])
                for row in cur.fetchall()
            ]


def guardar_fila(empresa, tabla, fila, data):
    key = _slug(tabla)
    es_global = key in CABECERAS_GLOBALES
    cols = COLUMNS_GLOBALES[key] if es_global else COLUMNS[key]
    tbl = key if es_global else _tabname(empresa, tabla)
    values = list(data)[:len(cols) - (1 if es_global else 0)]
    if es_global:
        values = [empresa] + values
    values += [""] * (len(cols) - len(values))
    col_sql = ", ".join(f'"{c}"' for c in cols)
    set_sql = ", ".join(
        f'"{c}"=EXCLUDED."{c}"' for c in cols[(2 if es_global else 1):]
    )
    conflict_cols = f'"{cols[0]}", fila' if es_global else "fila"
    placeholders = ", ".join(["%s"] * (len(cols) + 1))
    with _connect() as conn:
        with conn.cursor() as cur:
            if es_global:
                _crear_tabla_global(cur, key)
            else:
                _crear_tabla(cur, empresa, tabla)
            cur.execute(
                f'INSERT INTO "{tbl}" (fila, {col_sql}) VALUES ({placeholders}) '
                f'ON CONFLICT ({conflict_cols}) DO UPDATE SET {set_sql}',
                [fila, *values],
            )
        conn.commit()


def borrar_fila(empresa, tabla, fila):
    key = _slug(tabla)
    es_global = key in CABECERAS_GLOBALES
    tbl = key if es_global else _tabname(empresa, tabla)
    with _connect() as conn:
        with conn.cursor() as cur:
            if es_global:
                _crear_tabla_global(cur, key)
                cur.execute(
                    f'DELETE FROM "{tbl}" WHERE "{COLUMNS_GLOBALES[key][0]}"=%s AND fila=%s',
                    (empresa, fila),
                )
            else:
                _crear_tabla(cur, empresa, tabla)
                cur.execute(f'DELETE FROM "{tbl}" WHERE fila=%s', (fila,))
        conn.commit()


def siguiente_fila_libre(empresa, tabla, fila_inicio=3):
    """siguienteFilaLibre: primera fila >= fila_inicio sin datos."""
    key = _slug(tabla)
    es_global = key in CABECERAS_GLOBALES
    tbl = key if es_global else _tabname(empresa, tabla)
    with _connect() as conn:
        with conn.cursor() as cur:
            if es_global:
                _crear_tabla_global(cur, key)
                cur.execute(
                    f'SELECT COALESCE(MAX(fila), %s) FROM "{tbl}" '
                    f'WHERE fila >= %s AND "{COLUMNS_GLOBALES[key][0]}"=%s',
                    (fila_inicio - 1, fila_inicio, empresa),
                )
            else:
                _crear_tabla(cur, empresa, tabla)
                cur.execute(
                    f'SELECT COALESCE(MAX(fila), %s) FROM "{tbl}" WHERE fila >= %s',
                    (fila_inicio - 1, fila_inicio),
                )
            return int(cur.fetchone()[0]) + 1


def siguiente_id(empresa, tabla, prefijo):
    """siguienteIdEmpresa: max número con prefijo en la col 0 + 1."""
    key = _slug(tabla)
    es_global = key in CABECERAS_GLOBALES
    cols = COLUMNS_GLOBALES[key] if es_global else COLUMNS[key]
    col0 = cols[1] if es_global else cols[0]
    tbl = key if es_global else _tabname(empresa, tabla)
    with _connect() as conn:
        with conn.cursor() as cur:
            if es_global:
                _crear_tabla_global(cur, key)
                cur.execute(
                    f'SELECT "{col0}" FROM "{tbl}" WHERE "{cols[0]}"=%s', (empresa,)
                )
            else:
                _crear_tabla(cur, empresa, tabla)
                cur.execute(f'SELECT "{col0}" FROM "{tbl}"')
            maximo = 0
            for (valor,) in cur.fetchall():
                texto = str(valor or "").strip()
                if texto.startswith(prefijo):
                    try:
                        maximo = max(maximo, int(texto[len(prefijo):]))
                    except ValueError:
                        pass
    return f"{prefijo}{maximo + 1:05d}"


def leer_tabla_global_todos(tabla):
    """Superadmin: TODAS las filas reales (fila>=3) de una tabla global.
    Retorna lista de dicts {encabezado_original: valor} con Codigo_Empresa incluido."""
    key = _slug(tabla)
    headers = CABECERAS_GLOBALES[key]
    cols = COLUMNS_GLOBALES[key]
    col_sql = ", ".join(f'"{c}"' for c in cols)
    with _connect() as conn:
        with conn.cursor() as cur:
            _crear_tabla_global(cur, key)
            cur.execute(
                f'SELECT {col_sql} FROM "{key}" WHERE fila >= 3 '
                f'ORDER BY "{cols[0]}", fila'
            )
            return [dict(zip(headers, row)) for row in cur.fetchall()]


def registrar_empresa_db(datos):
    slug = _slug(datos["codigo"])
    otros = [c for c in _slug_en_uso(slug) if c != datos["codigo"]]
    if otros:
        raise ValueError(f"El código {datos['codigo']} colisiona en tablas con {otros[0]}")
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """INSERT INTO empresas
                   (id, nombre, nit, codigo, tipo, pais, ciudad, direccion, correo,
                    celular1, celular2, estado, plan, tiempo, fecha_creacion,
                    ultimo_acceso, fecha_vencimiento, observaciones, tipo_sistema, tipo_plataforma)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
                (
                    datos["id"], datos["nombre"], datos.get("nit", ""),
                    datos["codigo"], datos.get("tipo", ""), datos.get("pais", ""),
                    datos.get("ciudad", ""), datos.get("direccion", ""),
                    datos.get("correo", ""), datos.get("celular1", ""),
                    datos.get("celular2", ""), datos["estado"], datos.get("plan", "Básico"),
                    datos.get("tiempo", "1 Mes"), datos["fecha_creacion"],
                    "", datos["fecha_vencimiento"], "", "CLIENTE", "POS",
                ),
            )
            for tabla in CABECERAS:
                _crear_tabla(cur, datos["codigo"], tabla)
        conn.commit()


def actualizar_estado_empresa(empresa_id, estado):
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute("UPDATE empresas SET estado=%s WHERE id=%s", (estado, empresa_id))
        conn.commit()


def actualizar_ultimo_acceso(empresa_codigo, correo, fecha):
    """Tabla global de usuarios: un UPDATE indexado por codigo+correo."""
    cols = COLUMNS_GLOBALES["usuarios"]
    col_correo = cols[3]   # Correo
    col_ultimo = cols[8]   # Ultimo_Acceso
    with _connect() as conn:
        with conn.cursor() as cur:
            _crear_tabla_global(cur, "usuarios")
            cur.execute(
                f'UPDATE "usuarios" SET "{col_ultimo}"=%s '
                f'WHERE "{cols[0]}"=%s AND LOWER("{col_correo}")=LOWER(%s)',
                (fecha, empresa_codigo, correo),
            )
        conn.commit()


def comprar_plan_db(codigo, plan, tiempo, monto, fecha_vencimiento, usuario=""):
    """Compra/renovación de plan: actualiza la empresa y registra el ingreso."""
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """UPDATE empresas
                   SET plan=%s, tiempo=%s, fecha_vencimiento=%s, estado='Activo'
                   WHERE codigo=%s""",
                (plan, tiempo, fecha_vencimiento, codigo),
            )
            if cur.rowcount == 0:
                raise ValueError("No existe empresa con código " + codigo)
            cur.execute(
                """INSERT INTO finanzas_kapta (fecha, tipo, categoria, concepto, monto, metodo_pago, referencia, usuario)
                   VALUES (%s,'Ingreso',%s,%s,%s,%s,%s,%s)""",
                (
                    datetime.date.today().isoformat(), f"Plan {plan}",
                    f"Plan {plan} {tiempo} - {codigo}", str(monto),
                    "", "", usuario,
                ),
            )
        conn.commit()


def registrar_finanza_kapta(tipo, categoria, concepto, monto, metodo_pago="", referencia="", usuario=""):
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """INSERT INTO finanzas_kapta (fecha, tipo, categoria, concepto, monto, metodo_pago, referencia, usuario)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s)""",
                (datetime.date.today().isoformat(), tipo, categoria, concepto,
                 str(monto), metodo_pago, referencia, usuario),
            )
        conn.commit()


def listar_finanzas_kapta():
    with _connect() as conn:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute("SELECT * FROM finanzas_kapta ORDER BY id DESC")
            return [dict(r) for r in cur.fetchall()]