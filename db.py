# db.py — conexión Postgres (mismo patrón que Bar-Invenario)
import os
import json
from urllib.parse import urlparse

SCHEMA = """
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

CREATE TABLE IF NOT EXISTS filas (
    id SERIAL PRIMARY KEY,
    empresa TEXT NOT NULL,
    tabla TEXT NOT NULL,
    fila INT NOT NULL,
    data JSONB NOT NULL DEFAULT '[]',
    UNIQUE (empresa, tabla, fila)
);

CREATE INDEX IF NOT EXISTS idx_filas_empresa_tabla ON filas (empresa, tabla);
"""


def _connect():
    import psycopg2
    from psycopg2.extras import RealDictCursor
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


def init_db():
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(SCHEMA)
        conn.commit()


def _normalize(s):
    import unicodedata
    return "".join(
        c for c in unicodedata.normalize("NFD", str(s or "").lower())
        if not unicodedata.combining(c)
    ).replace("ñ", "n")


def listar_empresas_db():
    with _connect() as conn:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute("SELECT * FROM empresas ORDER BY fecha_creacion")
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


def hoja_existe(empresa_codigo):
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT 1 FROM filas WHERE empresa=%s LIMIT 1", (empresa_codigo,))
            return cur.fetchone() is not None


def leer_tabla(empresa, tabla):
    """Devuelve todas las filas (incluye headers en fila 2)."""
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT fila, data FROM filas WHERE empresa=%s AND tabla=%s ORDER BY fila",
                (empresa, tabla),
            )
            return [(fila, data) for fila, data in cur.fetchall()]


def guardar_fila(empresa, tabla, fila, data):
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO filas (empresa, tabla, fila, data) VALUES (%s,%s,%s,%s::jsonb) "
                "ON CONFLICT (empresa, tabla, fila) DO UPDATE SET data=EXCLUDED.data",
                (empresa, tabla, fila, json.dumps(data)),
            )
        conn.commit()


def borrar_fila(empresa, tabla, fila):
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute("DELETE FROM filas WHERE empresa=%s AND tabla=%s AND fila=%s",
                        (empresa, tabla, fila))
        conn.commit()


def siguiente_fila_libre(empresa, tabla, fila_inicio=3):
    """siguienteFilaLibre: primera fila >= fila_inicio sin datos."""
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT COALESCE(MAX(fila), %s) FROM filas "
                "WHERE empresa=%s AND tabla=%s AND fila >= %s",
                (fila_inicio - 1, empresa, tabla, fila_inicio),
            )
            return int(cur.fetchone()[0]) + 1


def siguiente_id(empresa, tabla, prefijo):
    """siguienteIdEmpresa: max número con prefijo en la col 0 + 1."""
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT data->>0 FROM filas WHERE empresa=%s AND tabla=%s",
                (empresa, tabla),
            )
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
        conn.commit()


def actualizar_estado_empresa(empresa_id, estado):
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute("UPDATE empresas SET estado=%s WHERE id=%s", (estado, empresa_id))
        conn.commit()


def actualizar_ultimo_acceso(empresa_codigo, correo, fecha):
    with _connect() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT fila FROM filas WHERE empresa=%s AND tabla='usuarios' AND data->>2 = %s",
                (empresa_codigo, correo),
            )
            fila = cur.fetchone()
            if fila:
                cur.execute(
                    """UPDATE filas SET data = jsonb_set(data, '{7}', to_jsonb(%s))
                       WHERE empresa=%s AND tabla='usuarios' AND fila=%s""",
                    (fecha, empresa_codigo, fila[0]),
                )
        conn.commit()