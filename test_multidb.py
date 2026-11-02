"""Autocomprobación de portabilidad de db.py.

Corre TODO el esquema y el CRUD contra SQLite (sin servidor) y valida la
sintaxis que se generaría para MySQL. Uso: python test_multidb.py

No toca Postgres ni producción: DATABASE_URL se fuerza a un archivo temporal.
"""
import importlib
import os
import sys
import tempfile

EMPRESA = {
    "id": "E-TEST01", "nombre": "Negocio Prueba", "nit": "", "codigo": "TEST01",
    "tipo": "Tienda", "pais": "Colombia", "ciudad": "Cali",
    "direccion": "Calle 1", "correo": "a@b.co", "celular1": "300",
    "celular2": "", "estado": "Activo", "plan": "Básico", "tiempo": "1 Mes",
    "fecha_creacion": "2026-01-01 10:00", "fecha_vencimiento": "2027-01-01",
}


def probar_sqlite(db):
    assert db._dialecto() == "sqlite", db._dialecto()
    db.init_db()

    db.registrar_empresa_db(dict(EMPRESA))
    assert db.buscar_empresa("TEST01")["nombre"] == "Negocio Prueba"
    assert db.buscar_empresa_por_nit("") is False
    assert db.hoja_existe("TEST01")
    assert db.siguiente_fila_libre("TEST01", "inventario", 3) == 3

    db.guardar_fila("TEST01", "inventario", 3, [
        "Ky_00001", "", "Arroz", "Grano", "10", "1000", "1500", "1200",
        "2", "Activo", "2026-01-01", "2026-01-01",
    ])
    filas = db.leer_tabla("TEST01", "inventario")
    assert len(filas) == 1 and filas[0][1][2] == "Arroz", filas
    assert db.siguiente_id("TEST01", "inventario", "Ky_") == "Ky_00002"

    # Duplicado por nombre: dedup conserva una sola fila.
    db.guardar_fila("TEST01", "inventario", 4, [
        "Ky_00009", "", "arroz", "Grano", "5", "1000", "1500", "1200",
        "2", "Activo", "2026-01-01", "2026-01-01",
    ])
    assert db.dedup_inventario("TEST01") == (1, None)

    # Tabla global: la primera columna la pone/lee el backend.
    db.guardar_fila("TEST01", "usuarios", 3, [
        "USR-0001", "Ana", "ana@b.co", "hash", "Administrador", "Activo",
    ])
    usuarios = db.leer_tabla("TEST01", "usuarios")
    assert len(usuarios) == 1 and usuarios[0][1][1] == "Ana", usuarios
    db.actualizar_contrasena("TEST01", "ana@b.co", "hash2")
    db.actualizar_ultimo_acceso("TEST01", "ana@b.co", "2026-02-02")
    assert db.leer_tabla("TEST01", "usuarios")[0][1][3] == "hash2"
    assert db.leer_tabla_global_todos("usuarios")[0]["Nombre"] == "Ana"

    # Planes, finanzas y biblioteca de funciones.
    db.comprar_plan_db("TEST01", "Pro", "1 Mes", 50000, "2027-02-01")
    db.registrar_finanza_kapta("Egreso", "Hosting", "Railway", 1000)
    assert len(db.listar_finanzas_kapta()) == 2
    db.crear_funcion_db({"nombre": "Caja", "descripcion": "Cobrar"})
    db.crear_funcion_db({"nombre": "Caja", "descripcion": "Cobrar v2"})
    assert [f["descripcion"] for f in db.listar_funciones_db()] == ["Cobrar v2"]
    db.eliminar_funcion_db("Caja")
    assert db.listar_funciones_db() == []

    # Fotos (base64), soporte y clave dinámica.
    foto = db.guardar_foto("Zm90bw==", "TEST01")
    assert db.obtener_foto(foto) == "Zm90bw=="
    assert db.listar_fotos("TEST01")[0]["bytes"] == 8
    assert db.registrar_soporte("Falla", "detalle", "Ana") == "S_00001"
    assert len(db.listar_soportes()) == 1

    clave = db.obtener_clave_dinamica_db("TEST01")
    assert clave["codigo"] and db.validar_clave_dinamica_db("TEST01", clave["codigo"])["ok"]

    # Bloqueo de login (upsert portable).
    assert db.segundos_bloqueo_restantes("TEST01", "ana@b.co") == 0
    assert db.registrar_fallo_login("TEST01", "ana@b.co") == 0
    db.reset_fallos_login("TEST01", "ana@b.co")

    # Ciclo de vida de la empresa.
    db.marcar_empresa_eliminada("E-TEST01", "2026-03-03")
    assert db.buscar_empresa("TEST01")["estado"] == "ELIMINADO"
    db.reactivar_empresa("TEST01")
    assert db.buscar_empresa("TEST01")["estado"] == "Activo"
    db.actualizar_empresa_db("TEST01", {"ciudad": "Bogotá"})
    assert db.buscar_empresa("TEST01")["ciudad"] == "Bogotá"
    db.borrar_fila("TEST01", "inventario", 3)
    assert db.leer_tabla("TEST01", "inventario") == []


def probar_sintaxis_mysql(db):
    os.environ["DATABASE_URL"] = "mysql://u:p@localhost:3306/kapta"
    assert db._dialecto() == "mysql"

    ddl = db._ddl(db.SCHEMA_EMPRESAS)
    assert "TEXT DEFAULT" not in ddl, ddl
    assert "codigo VARCHAR(191) UNIQUE" in ddl, ddl
    assert "INT AUTO_INCREMENT PRIMARY KEY" in db._ddl(db.SCHEMA_FUNCIONES)
    assert "VARCHAR(191) PRIMARY KEY" in db._ddl(db.SCHEMA_CLAVES_DINAMICAS)

    up = db._sql(
        'INSERT INTO "t" (fila,"a") VALUES (%s,%s) '
        'ON CONFLICT ("a") DO UPDATE SET "a"=EXCLUDED."a"'
    )
    assert "ON DUPLICATE KEY UPDATE" in up and "ON CONFLICT" not in up, up
    assert "`a`=VALUES(`a`)" in up, up
    assert "NOW()::text" not in db._sql("UPDATE t SET x=NOW()::text")
    return db


def probar_sintaxis_sqlite(db):
    os.environ["DATABASE_URL"] = "sqlite:///x.db"
    assert db._sql("SELECT 1 WHERE a=%s AND b=%s") == "SELECT 1 WHERE a=? AND b=?"
    assert "CURRENT_TIMESTAMP" in db._sql("UPDATE t SET x=NOW()::text")
    assert "INTEGER PRIMARY KEY AUTOINCREMENT" in db._ddl(db.SCHEMA_FUNCIONES)


def main():
    tmp = tempfile.mkdtemp(prefix="kapta_multidb_")
    os.environ.pop("LIMPIAR_EMPRESAS", None)
    os.environ["DATABASE_URL"] = "sqlite:///" + os.path.join(tmp, "kapta.db").replace("\\", "/")
    sys.modules.pop("db", None)
    db = importlib.import_module("db")

    probar_sqlite(db)
    probar_sintaxis_mysql(db)
    probar_sintaxis_sqlite(db)
    print("OK  SQLite: init_db + CRUD completo")
    print("OK  MySQL: DDL y ON DUPLICATE KEY generados")
    print("OK  SQLite: marcadores y DDL generados")
    print("base de prueba:", os.path.join(tmp, "kapta.db"))


if __name__ == "__main__":
    main()
