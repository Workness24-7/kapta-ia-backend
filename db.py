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
    "gastos": ["Id_Gasto", "Fecha", "Hora", "Categoria", "Concepto",
               "Descripcion", "Proveedor", "Monto", "Metodo_Pago",
               "Referencia", "Usuario", "Estado", "Fecha_Modificacion", "Modificado_Por"],
    "auditoria_gastos": ["Id_Evento", "Id_Empresa", "Id_Gasto", "Accion",
                         "Usuario", "Fecha_Hora", "Detalles", "Estado"],
    "usuarios": ["Id_Usuario", "Nombre", "Correo", "Contrasena", "Rol",
                 "Estado", "Fecha_Creacion", "Ultimo_Acceso",
                 "Fecha_Cambio_Estado", "Motivo_Cambio", "Cambiado_Por"],
    "config_negocio": ["Parametro", "Valor", "Descripcion", "Fecha_Actualizacion",
                       "Usuario", "Observaciones"],
    "estadisticas": ["Ventas_Hoy", "Ventas_Mes", "Ventas_Año", "Total_Ingresos",
                     "Total_Gastos", "Total_Deudores", "Productos", "Usuarios",
                     "Ultima_Venta", "Ultima_Actualizacion"],
    "ia": ["Fecha", "Tipo", "Pregunta", "Respuesta", "Usuario", "Tokens",
           "Modelo", "Tiempo", "Costo", "Estado"],
    "movimientos": ["Id_Movimiento", "Fecha", "Id_Producto", "Nom_Producto",
                    "Tipo", "Cantidad", "Stock_Anterior", "Stock_Nuevo",
                    "Usuario", "Observacion"],
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


def init_db():
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(SCHEMA_EMPRESAS)
            cur.execute(SCHEMA_FINANZAS_KAPTA)
            cur.execute("SELECT codigo FROM empresas WHERE codigo IS NOT NULL AND codigo <> ''")
            for (codigo,) in cur.fetchall():
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
    """Devuelve todas las filas (incluye headers en fila 2) como (fila, lista_valores)."""
    tbl = _tabname(empresa, tabla)
    cols = COLUMNS[tabla]
    col_sql = ", ".join(f'"{c}"' for c in cols)
    with _connect() as conn:
        with conn.cursor() as cur:
            _crear_tabla(cur, empresa, tabla)
            cur.execute(f'SELECT fila, {col_sql} FROM "{tbl}" ORDER BY fila')
            return [
                (row[0], [v if v is not None else "" for v in row[1:]])
                for row in cur.fetchall()
            ]


def guardar_fila(empresa, tabla, fila, data):
    tbl = _tabname(empresa, tabla)
    cols = COLUMNS[tabla]
    values = list(data)[:len(cols)]
    values += [""] * (len(cols) - len(values))
    col_sql = ", ".join(f'"{c}"' for c in cols)
    set_sql = ", ".join(f'"{c}"=EXCLUDED."{c}"' for c in cols)
    placeholders = ", ".join(["%s"] * (len(cols) + 1))
    with _connect() as conn:
        with conn.cursor() as cur:
            _crear_tabla(cur, empresa, tabla)
            cur.execute(
                f'INSERT INTO "{tbl}" (fila, {col_sql}) VALUES ({placeholders}) '
                f'ON CONFLICT (fila) DO UPDATE SET {set_sql}',
                [fila, *values],
            )
        conn.commit()


def borrar_fila(empresa, tabla, fila):
    tbl = _tabname(empresa, tabla)
    with _connect() as conn:
        with conn.cursor() as cur:
            _crear_tabla(cur, empresa, tabla)
            cur.execute(f'DELETE FROM "{tbl}" WHERE fila=%s', (fila,))
        conn.commit()


def siguiente_fila_libre(empresa, tabla, fila_inicio=3):
    """siguienteFilaLibre: primera fila >= fila_inicio sin datos."""
    tbl = _tabname(empresa, tabla)
    with _connect() as conn:
        with conn.cursor() as cur:
            _crear_tabla(cur, empresa, tabla)
            cur.execute(
                f'SELECT COALESCE(MAX(fila), %s) FROM "{tbl}" WHERE fila >= %s',
                (fila_inicio - 1, fila_inicio),
            )
            return int(cur.fetchone()[0]) + 1


def siguiente_id(empresa, tabla, prefijo):
    """siguienteIdEmpresa: max número con prefijo en la col 0 + 1."""
    tbl = _tabname(empresa, tabla)
    col0 = COLUMNS[tabla][0]
    with _connect() as conn:
        with conn.cursor() as cur:
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
    tbl = _tabname(empresa_codigo, "usuarios")
    cols = COLUMNS["usuarios"]
    col_correo = cols[2]
    col_ultimo = cols[7]
    with _connect() as conn:
        with conn.cursor() as cur:
            _crear_tabla(cur, empresa_codigo, "usuarios")
            cur.execute(
                f'UPDATE "{tbl}" SET "{col_ultimo}"=%s WHERE LOWER("{col_correo}")=LOWER(%s)',
                (fecha, correo),
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