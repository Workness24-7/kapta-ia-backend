# test_contrato.py — valida el contrato de API replicando el flujo de la app Android.
# Sin dependencias externas: usa un fake en memoria de la capa DB.
# Correr: python test_contrato.py
import json
import sys
import types

import db as db_real
import main as backend

# ===================================================
# FAKE DB EN MEMORIA (misma interfaz que db.py)
# ===================================================
STORE = {}  # {(empresa, tabla, fila): [jsonb list]}
EMPRESAS = {}


def fake_init_db():
    pass


def fake_listar_empresas_db():
    out = []
    for e in EMPRESAS.values():
        out.append(dict(e))
    return sorted(out, key=lambda e: e["fecha_creacion"])


def fake_buscar_empresa(clave):
    if not clave:
        return None
    emp = EMPRESAS.get(clave)
    if emp:
        return emp
    n = db_real._normalize(clave)
    for e in EMPRESAS.values():
        if db_real._normalize(e["codigo"]) == n or db_real._normalize(e["nombre"]) == n:
            return e
    return None


def fake_hoja_existe(codigo):
    return any(emp == codigo for (emp, _, _) in STORE)


def fake_leer_tabla(empresa, tabla):
    keys = [k for k in STORE if k[0] == empresa and k[1] == tabla]
    return [(k[2], STORE[k]) for k in sorted(keys)]


def fake_guardar_fila(empresa, tabla, fila, data):
    STORE[(empresa, tabla, fila)] = list(data)


def fake_borrar_fila(empresa, tabla, fila):
    STORE.pop((empresa, tabla, fila), None)


def fake_siguiente_fila_libre(empresa, tabla, fila_inicio=3):
    max_fila = max((k[2] for k in STORE if k[0] == empresa and k[1] == tabla), default=fila_inicio - 1)
    return max_fila + 1


def fake_siguiente_id(empresa, tabla, prefijo):
    maximo = 0
    for (emp, tab, _), data in STORE.items():
        if emp == empresa and tab == tabla:
            texto = str(data[0] or "").strip()
            if texto.startswith(prefijo):
                try:
                    maximo = max(maximo, int(texto[len(prefijo):]))
                except ValueError:
                    pass
    return f"{prefijo}{maximo + 1:05d}"


def fake_registrar_empresa_db(datos):
    fila = dict(datos)
    for key in ("nit", "tipo", "pais", "ciudad", "direccion", "correo",
                "celular1", "celular2", "observaciones"):
        fila.setdefault(key, "")
    fila.setdefault("estado", "Activo")
    fila.setdefault("plan", "Básico")
    fila.setdefault("tiempo", "1 Mes")
    fila.setdefault("fecha_creacion", "")
    fila.setdefault("fecha_vencimiento", "")
    fila.setdefault("ultimo_acceso", "")
    fila.setdefault("tipo_sistema", "CLIENTE")
    fila.setdefault("tipo_plataforma", "POS")
    EMPRESAS[datos["codigo"]] = fila


def fake_actualizar_estado_empresa(empresa_id, estado):
    for e in EMPRESAS.values():
        if e["id"] == empresa_id:
            e["estado"] = estado


def fake_actualizar_ultimo_acceso(codigo, correo, fecha):
    for (emp, tab, _), data in STORE.items():
        if emp == codigo and tab == "usuarios" and str(data[2] or "").lower() == correo:
            data[7] = fecha


FINANZAS = []


def fake_comprar_plan_db(codigo, plan, tiempo, monto, fecha_vencimiento, usuario=""):
    emp = EMPRESAS.get(codigo)
    if not emp:
        raise ValueError("No existe empresa con código " + codigo)
    emp["plan"] = plan
    emp["tiempo"] = tiempo
    emp["fecha_vencimiento"] = fecha_vencimiento
    emp["estado"] = "Activo"
    FINANZAS.append({"fecha": "", "tipo": "Ingreso", "categoria": f"Plan {plan}",
                     "concepto": f"Plan {plan} {tiempo} - {codigo}", "monto": str(monto),
                     "metodo_pago": "", "referencia": "", "usuario": usuario})


def fake_registrar_finanza_kapta(tipo, categoria, concepto, monto, metodo_pago="", referencia="", usuario=""):
    FINANZAS.append({"fecha": "", "tipo": tipo, "categoria": categoria, "concepto": concepto,
                     "monto": str(monto), "metodo_pago": metodo_pago, "referencia": referencia,
                     "usuario": usuario})


def fake_listar_finanzas_kapta():
    return list(FINANZAS)


def install_fake():
    db_real.init_db = fake_init_db
    db_real.listar_empresas_db = fake_listar_empresas_db
    db_real.buscar_empresa = fake_buscar_empresa
    db_real.hoja_existe = fake_hoja_existe
    db_real.leer_tabla = fake_leer_tabla
    db_real.guardar_fila = fake_guardar_fila
    db_real.borrar_fila = fake_borrar_fila
    db_real.siguiente_fila_libre = fake_siguiente_fila_libre
    db_real.siguiente_id = fake_siguiente_id
    db_real.registrar_empresa_db = fake_registrar_empresa_db
    db_real.actualizar_estado_empresa = fake_actualizar_estado_empresa
    db_real.actualizar_ultimo_acceso = fake_actualizar_ultimo_acceso
    db_real.comprar_plan_db = fake_comprar_plan_db
    db_real.registrar_finanza_kapta = fake_registrar_finanza_kapta
    db_real.listar_finanzas_kapta = fake_listar_finanzas_kapta


def json_body(resp):
    return json.loads(resp.body.decode())


# ===================================================
# CHECKS
# ===================================================
def check(name, cond, extra=""):
    if not cond:
        print(f"FAIL {name} {extra}")
        sys.exit(1)
    print(f"OK   {name}")


def main():
    install_fake()

    # 1. LISTAR EMPRESAS vacío
    r = json_body(backend.action_listar_empresas())
    check("listar_empresas vacío", r["status"] == "success" and r["data"]["empresas"] == [])

    # 2. REGISTRAR EMPRESA
    emp = {
        "nombre": "Negocio Test", "nombreComercial": "Test SA", "nit": "900000001",
        "codigo": "TEST01", "tipo": "Tienda", "pais": "Colombia", "ciudad": "Bogotá",
        "direccion": "Calle 1", "correo": "negocio@test.com", "celular1": "3000001",
        "celular2": "", "plan": "Básico", "tiempo": "1 mes", "estado": "Activo",
        "adminNombre": "Admin", "adminCorreo": "admin@test.com", "adminPassword": "1234",
        "registradoPor": "U-KAPTA-00001",
    }
    r = json_body(backend.action_registrar_empresa({"action": "registrar_empresa", **emp}))
    check("registrar_empresa", r["status"] == "success" and r["data"]["codigo"] == "TEST01",
          json.dumps(r))
    codigo_emp = r["data"]["codigo"]
    id_empresa = r["data"]["idEmpresa"]
    check("idEmpresa KIA-", id_empresa.startswith("KIA-"))

    # 3. LOGIN con admin creado
    r = json_body(backend.action_login({"action": "login", "codigo": "test01",
                                        "correo": "ADMIN@test.com", "password": "1234"}))
    check("login", r["status"] == "success" and r.get("login") is True and
          r.get("idEmpresa") == "TEST01" and r["data"]["rol"] == "Administrador",
          json.dumps(r))

    # 4. LOGIN con contraseña mala -> error
    r = json_body(backend.action_login({"action": "login", "codigo": "TEST01",
                                        "correo": "admin@test.com", "password": "mala"}))
    check("login password errónea", r["status"] == "error")

    # 5. ESCRIBIR INVENTARIO (IDS auto-generados Ky_00001)
    inv = ["", "770000001", "Producto A", "General", 10, 1000, 2000, 1500, 5, "Activo", "", ""]
    r = json_body(backend.action_escribir_fila({"action": "registrar_inventario",
                                                "sheetName": "TEST01", "tableName": "Inventario",
                                                "data": inv}))
    check("registrar_inventario", r["status"] == "success" and r["data"]["registrado"] is True,
          json.dumps(r))
    store_inv = STORE[("TEST01", "inventario", 3)]
    check("id inventario Ky_00001", store_inv[0] == "Ky_00001", str(store_inv))

    # 6. LEER inventario: rows[0] = encabezados, rows[1] = datos
    r = json_body(backend.action_read({"action": "read", "sheetName": "TEST01",
                                       "tableName": "Inventario"}))
    check("read inventario", r["status"] == "success" and len(r["data"]["rows"]) == 2
          and r["data"]["rows"][0] == backend.CABECERAS["INVENTARIO"]
          and r["data"]["rows"][1][0] == "Ky_00001", json.dumps(r["data"]))

    # 7. REGISTRAR DEUDOR (layout kithos 8 cols)
    deudor = ["03/03/2026 14:30", "Cliente A", "Producto A", 2, 1, 10000, 0, 20000]
    r = json_body(backend.action_escribir_fila({"action": "registrar_deudor",
                                                "sheetName": "TEST01", "tableName": "Deudores",
                                                "data": deudor}))
    check("registrar_deudor", r["status"] == "success", json.dumps(r))

    # 8. PAGAR DEUDOR parcial -> no mueve a ventas
    r = json_body(backend.action_pagar_deudor({"action": "pagar_deudor", "sheetName": "TEST01",
                                               "clienteNombre": "Cliente A", "productoNombre": "Producto A",
                                               "transferAmount": 5000, "cashAmount": 0}))
    check("pagar_deudor parcial", r["status"] == "success" and r["data"]["totalPendiente"] == 15000
          and r["data"]["movidoAVentas"] is False, json.dumps(r))

    # 9. PAGAR DEUDOR completo -> mueve a VENTAS con layout kithos (21 cols)
    r = json_body(backend.action_pagar_deudor({"action": "pagar_deudor", "sheetName": "TEST01",
                                               "clienteNombre": "Cliente A", "productoNombre": "Producto A",
                                               "transferAmount": 15000, "cashAmount": 0}))
    check("pagar_deudor salda", r["status"] == "success" and r["data"]["totalPendiente"] == 0
          and r["data"]["movidoAVentas"] is True, json.dumps(r))
    venta = STORE.get(("TEST01", "ventas", 3))
    check("venta layout 21 cols + id V-", venta is not None and len(venta) == 21
          and venta[0].startswith("V-") and venta[3] == "Cliente A" and venta[14] == "Activo"
          and venta[12] == venta[8] == 30000, str(venta))
    check("deudor saldado eliminado", ("TEST01", "deudores", 3) not in STORE)

    # 10. REGISTRAR VENTA directa con Id generado V-00002
    venta = ["", "03/03/2026", "15:00", "Cliente B", "", "Producto B", 1, 5000, 0, "",
             0, 5000, 5000, "", "Activo", "", "", "", "", "", ""]
    r = json_body(backend.action_escribir_fila({"action": "registrar_venta",
                                                "sheetName": "TEST01", "tableName": "Ventas",
                                                "data": venta}))
    check("registrar_venta", r["status"] == "success", json.dumps(r))
    venta2 = STORE[("TEST01", "ventas", 4)]
    check("id venta secuencial V-00002", venta2[0] == "V-00002", str(venta2))

    # 11. REPORTES (tipo ventas, periodo mes)
    r = json_body(backend.action_reportes({"action": "reportes", "idEmpresa": "TEST01",
                                           "tipo": "ventas", "periodo": "mes"}))
    check("reportes ventas", r["status"] == "success" and r["datos"]["resumen"]["cantidadVentas"] >= 2
          and r["datos"]["resumen"]["totalVentas"] == 35000, json.dumps(r))

    # 12. OBTENER_TODO: incluye inventario
    r = json_body(backend.action_obtener_todo({"action": "obtener_todo", "sheetName": "TEST01"}))
    check("obtener_todo", r["status"] == "success" and "inventario" in r["data"]
          and len(r["data"]["inventario"]) == 2, str(list(r["data"].keys())))

    # 13. LISTAR empresas con la nueva
    r = json_body(backend.action_listar_empresas())
    check("listar_empresas con empresa", r["status"] == "success"
          and len(r["data"]["empresas"]) == 1 and r["data"]["empresas"][0]["codigo"] == "TEST01")

    # 14. ELIMINAR USUARIO -> Suspendido
    r = json_body(backend.action_eliminar_usuario({"action": "eliminar_usuario",
                                                   "sheetName": "TEST01",
                                                   "userEmail": "admin@test.com"}))
    check("eliminar_usuario", r["status"] == "success" and r["data"]["eliminado"] is True)
    usr = STORE[("TEST01", "usuarios", 3)]
    check("usuario Suspendido", usr[5] == "Suspendido", str(usr))
    r = json_body(backend.action_login({"action": "login", "codigo": "TEST01",
                                        "correo": "admin@test.com", "password": "1234"}))
    check("login bloqueado tras suspender", r["status"] == "error")

    # 15. ELIMINAR EMPRESA -> Suspendido en base maestra
    r = json_body(backend.action_eliminar_empresa({"action": "eliminar_empresa",
                                                   "empresaNombre": "TEST01"}))
    check("eliminar_empresa", r["status"] == "success" and r["data"]["eliminada"] is True)
    check("empresa Suspendida", EMPRESAS["TEST01"]["estado"] == "Suspendido")

    # 16. COMPRAR PLAN mensual -> reactiva, vence ~30 días y registra ingreso
    r = json_body(backend.action_comprar_plan({"codigo": "TEST01", "plan": "MAX IA",
                                               "tiempo": "Mensual", "monto": "339900"}))
    check("comprar_plan mensual", r["status"] == "success" and r["data"]["estado"] == "Activo"
          and r["data"]["fechaVencimiento"] != "", json.dumps(r))
    emp01 = fake_buscar_empresa("TEST01")
    check("comprar_plan actualiza empresa", emp01["plan"] == "MAX IA"
          and emp01["tiempo"] == "Mensual"
          and emp01["fecha_vencimiento"] == r["data"]["fechaVencimiento"])

    # 17. COMPRAR PLAN anual -> vence ~365 días
    r = json_body(backend.action_comprar_plan({"codigo": "TEST01", "plan": "Premium",
                                               "tiempo": "Anual", "monto": "2499000"}))
    check("comprar_plan anual", r["status"] == "success")

    # 18. TIEMPO inválido rechazado
    r = json_body(backend.action_comprar_plan({"codigo": "TEST01", "plan": "MAX IA",
                                               "tiempo": "Semanal"}))
    check("comprar_plan tiempo inválido", r["status"] == "error")

    # 19. GASTO KAPTA + LISTAR finanzas con balance
    r = json_body(backend.action_registrar_finanza_kapta({"tipo": "Egreso",
                                                          "concepto": "Publicidad", "monto": "50000"}))
    check("registrar_finanza_kapta egreso", r["status"] == "success")
    r = json_body(backend.action_listar_finanzas_kapta())
    check("listar_finanzas_kapta balance", r["status"] == "success"
          and r["data"]["totalIngresos"] == 339900.0 + 2499000.0
          and r["data"]["totalEgresos"] == 50000.0
          and len(r["data"]["registros"]) == 3, json.dumps(r["data"]))

    # 20. LOGIN bloqueado por membresía vencida (aunque usuario esté bien)
    fake_buscar_empresa("TEST01")["fecha_vencimiento"] = "2020-01-01"
    r = json_body(backend.action_login({"action": "login", "codigo": "TEST01",
                                        "correo": "admin@test.com", "password": "1234"}))
    check("login bloquea membresía vencida", r["status"] == "error", json.dumps(r))

    print("TODOS LOS CHECKS PASARON")


if __name__ == "__main__":
    main()