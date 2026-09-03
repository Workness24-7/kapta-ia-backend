/* Kapta IA POS — PWA v1 (iPhone/Android/PC). Vanilla JS contra el backend Railway. */
const BASE = "https://kapta-ia-backend-production.up.railway.app/exec";
const $ = (id) => document.getElementById(id);
const fmt = (n) => "$" + Math.round(Number(n) || 0).toLocaleString("es-CO");

let SES = null;          // {code, negocio, correo, nombre, rol}
let TODO = null;         // obtener_todo
let CARRITO = {};        // idxInventario -> qty
let DEU_SEL = null;

function toast(m) {
  const t = $("toast");
  t.textContent = m; t.classList.add("ver");
  clearTimeout(t._h); t._h = setTimeout(() => t.classList.remove("ver"), 2600);
}

async function api(payload) {
  const r = await fetch(BASE, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return r.json();
}

function hoyISO() {
  const d = new Date();
  return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
}
function hoyLat() {
  const d = new Date();
  return String(d.getDate()).padStart(2, "0") + "/" + String(d.getMonth() + 1).padStart(2, "0") + "/" + d.getFullYear();
}
function horaHM() {
  const d = new Date();
  return String(d.getHours()).padStart(2, "0") + ":" + String(d.getMinutes()).padStart(2, "0");
}
// true si la fecha (yyyy-mm-dd o dd/mm/yyyy) es hoy
function esHoy(f) {
  if (!f) return false;
  f = String(f).trim().slice(0, 10);
  if (/^\d{4}-\d{2}-\d{2}$/.test(f)) return f === hoyISO();
  const p = f.split("/");
  if (p.length === 3) return f === hoyLat();
  return false;
}
function esMesActual(f) {
  if (!f) return false;
  const h = hoyLat().slice(3); // mm/yyyy
  f = String(f).trim().slice(0, 10);
  if (/^\d{4}-\d{2}-\d{2}$/.test(f)) return f.slice(5, 7) + "/" + f.slice(0, 4) === h;
  return f.slice(3) === h;
}
const num = (v) => { const n = parseFloat(v); return isNaN(n) ? 0 : n; };

// ---------- navegación ----------
function ver(id) {
  document.querySelectorAll(".pantalla").forEach((s) => s.classList.add("oculto"));
  $("p-" + id).classList.remove("oculto");
}
function tab(nombre) {
  document.querySelectorAll(".tab").forEach((t) => t.classList.add("oculto"));
  $("t-" + nombre).classList.remove("oculto");
  document.querySelectorAll("#dock button").forEach((b) => b.classList.toggle("on", b.dataset.tab === nombre));
}
document.querySelectorAll("#dock button").forEach((b) => b.addEventListener("click", () => tab(b.dataset.tab)));
document.querySelectorAll(".acc[data-go]").forEach((b) => b.addEventListener("click", () => tab(b.dataset.go)));

// ---------- sesión ----------
function guardarSesion() {
  const rec = $("in-recordar").checked;
  (rec ? localStorage : sessionStorage).setItem("kapta_pwa", JSON.stringify(SES));
}
function cargarSesion() {
  try {
    return JSON.parse(localStorage.getItem("kapta_pwa") || sessionStorage.getItem("kapta_pwa") || "null");
  } catch { return null; }
}

$("btn-codigo").addEventListener("click", async () => {
  const code = $("in-codigo").value.trim().toUpperCase();
  $("err-codigo").textContent = "";
  if (!code) { $("err-codigo").textContent = "Escribe el código de tu negocio"; return; }
  $("btn-codigo").disabled = true;
  try {
    const r = await fetch(BASE + "?action=listar_empresas").then((x) => x.json());
    const emp = ((r.data || {}).empresas || []).find((e) => (e.codigo || "").toUpperCase() === code);
    if (!emp) { $("err-codigo").textContent = "Negocio no encontrado"; return; }
    SES = { code, negocio: emp.nombre || code };
    $("login-nombre").textContent = SES.negocio;
    $("login-dominio").textContent = code.toLowerCase() + ".kaptaia.com";
    ver("login");
  } catch { $("err-codigo").textContent = "Sin conexión. Intenta de nuevo."; }
  $("btn-codigo").disabled = false;
});

$("btn-volver-negocio").addEventListener("click", () => ver("negocio"));

$("btn-login").addEventListener("click", async () => {
  const correo = $("in-correo").value.trim();
  const clave = $("in-clave").value;
  $("err-login").textContent = "";
  if (!correo || !clave) { $("err-login").textContent = "Completa correo y contraseña"; return; }
  $("btn-login").disabled = true;
  try {
    const r = await api({ action: "login", codigo: SES.code, correo, password: clave });
    if (r.status !== "success") { $("err-login").textContent = r.message || "Credenciales inválidas"; return; }
    const d = r.data || {};
    SES.correo = correo;
    SES.nombre = d.nombre || correo;
    SES.rol = d.rol || r.rol || "Empleado";
    guardarSesion();
    // Consentimiento legal (una vez por usuario+negocio)
    const ck = "kapta_consent_" + SES.code + "_" + correo.toLowerCase();
    if (!localStorage.getItem(ck)) {
      const ok = confirm("Bienvenido a Kapta IA.\n\nAl entrar aceptas la Política de Privacidad y los Términos de Uso: tus datos se usan solo para operar tu negocio, la IA solo ayuda en funciones específicas y nunca vendemos tu información.\n\n¿Aceptas y deseas continuar?");
      if (!ok) return;
      localStorage.setItem(ck, new Date().toISOString());
    }
    await entrar();
  } catch { $("err-login").textContent = "Sin conexión. Intenta de nuevo."; }
  $("btn-login").disabled = false;
});

async function entrar() {
  $("pos-negocio").textContent = SES.negocio;
  $("pos-usuario").textContent = SES.nombre + " • " + SES.rol;
  $("cuenta-info").innerHTML = "<b>" + SES.nombre + "</b><br><small>" + SES.correo + " • " + SES.rol + "</small>";
  ver("pos"); tab("inicio");
  await recargar();
}

$("btn-salir").addEventListener("click", () => {
  SES = null;
  localStorage.removeItem("kapta_pwa"); sessionStorage.removeItem("kapta_pwa");
  ver("negocio");
});

// ---------- datos ----------
async function recargar() {
  toast("Cargando...");
  try {
    const r = await api({ action: "obtener_todo", sheetName: SES.code });
    if (r.status !== "success") { toast("No se pudo cargar"); return; }
    TODO = r.data;
    pintarResumen(); pintarVenta(); pintarInventario(); pintarDeudores();
  } catch { toast("Sin conexión"); }
}
$("btn-recargar").addEventListener("click", recargar);

const invRows = () => (TODO.inventario || []).filter((x) => x[2] && x[2] !== "Nom_Producto");
const venRows = () => (TODO.ventas || []).filter((x) => x[5] && x[5] !== "Producto");
const deuRows = () => (TODO.deudores || []).filter((x) => x[1] && x[1] !== "Nom_Cliente");
const gasRows = () => (TODO.gastos || []).filter((x) => x[0] && String(x[0]).startsWith("G-"));

function pintarResumen() {
  const ventasHoy = venRows().filter((v) => esHoy(v[1])).reduce((a, v) => a + num(v[12]), 0);
  const gastosMes = gasRows().filter((g) => esMesActual(g[1])).reduce((a, g) => a + num(g[7]), 0);
  const deudores = agruparDeudores();
  const totalDeu = deudores.reduce((a, d) => a + d.pendiente, 0);
  const alertas = invRows().filter((p) => num(p[4]) <= num(p[8] || 0));
  $("resumen").innerHTML =
    kpi("Ventas hoy", fmt(ventasHoy)) + kpi("Gastos del mes", fmt(gastosMes)) +
    kpi("Deudores", deudores.length + " • " + fmt(totalDeu)) + kpi("Alertas stock", String(alertas.length));
  $("alertas").innerHTML = alertas.length
    ? alertas.map((p) => `<div class="card"><b>${p[2]}</b><br><small>Quedan ${p[4]} (alerta: ${p[8] || 0})</small></div>`).join("")
    : '<div class="card">¡Todo en orden! Stock suficiente.</div>';
}
const kpi = (t, v) => `<div class="kpi"><small>${t}</small><b>${v}</b></div>`;

// ---------- venta ----------
function pintarVenta() {
  const q = ($("venta-buscar").value || "").toLowerCase();
  const list = invRows().filter((p) => !q || p[2].toLowerCase().includes(q));
  $("venta-productos").innerHTML = list.length ? "" : '<div class="card">Sin productos.</div>';
  list.forEach((p) => {
    const idx = (TODO.inventario || []).indexOf(p);
    const div = document.createElement("div");
    div.className = "card fila-prod";
    div.innerHTML = `<div><b>${p[2]}</b><small>Stock: ${p[4]} • ${p[3] || ""}</small></div>
      <div class="cant"><span class="precio">${fmt(p[6])}</span><button>+</button></div>`;
    div.querySelector("button").addEventListener("click", () => {
      CARRITO[idx] = (CARRITO[idx] || 0) + 1;
      pintarCarrito();
    });
    $("venta-productos").appendChild(div);
  });
  pintarCarrito();
}
$("venta-buscar").addEventListener("input", pintarVenta);

function pintarCarrito() {
  const ids = Object.keys(CARRITO).filter((i) => CARRITO[i] > 0);
  $("venta-carrito").classList.toggle("oculto", !ids.length);
  if (!ids.length) return;
  let total = 0, html = "";
  ids.forEach((i) => {
    const p = TODO.inventario[i], q = CARRITO[i], sub = q * num(p[6]);
    total += sub;
    html += `<div class="card fila-prod"><div><b>${p[2]}</b><small>${q} x ${fmt(p[6])}</small></div>
      <div class="cant"><button data-i="${i}" data-d="-1">−</button><b>${q}</b><button data-i="${i}" data-d="1">+</button></div></div>`;
  });
  $("carrito-items").innerHTML = html;
  $("carrito-total").textContent = fmt(total);
  $("carrito-items").querySelectorAll("button").forEach((b) => b.addEventListener("click", () => {
    const i = b.dataset.i;
    CARRITO[i] = Math.max(0, (CARRITO[i] || 0) + Number(b.dataset.d));
    pintarCarrito();
  }));
}

$("btn-cobrar").addEventListener("click", async () => {
  const ids = Object.keys(CARRITO).filter((i) => CARRITO[i] > 0);
  if (!ids.length) return;
  const cliente = ($("venta-cliente").value.trim() || "Cliente Mostrador");
  const metodo = $("venta-metodo").value;
  const fiado = $("venta-fiado").checked;
  const esTransf = metodo === "Transferencia";
  $("btn-cobrar").disabled = true;
  try {
    for (const i of ids) {
      const p = TODO.inventario[Number(i)], q = CARRITO[i];
      const pu = num(p[6]), sub = q * pu;
      const stockAnt = num(p[4]), stockNvo = Math.max(0, stockAnt - q);
      if (fiado) {
        await api({ action: "registrar_deudor", tableName: "Deudores", data: [
          fechaHora(), cliente, p[2], q, "", 0, 0, sub, "Normal", "", 0] });
        await api({ action: "registrar_movimiento", sheetName: SES.code, fecha: hoyLat(),
          producto: p[2], tipo: "Salida", cantidad: q,
          stockAnterior: stockAnt, stockNuevo: stockNvo, usuario: SES.nombre, observacion: "Descuento por deudor" });
      } else {
        await api({ action: "registrar_venta", tableName: "Ventas", data: [
          "", hoyISO(), horaHM(), cliente, p[0], p[2], q, pu, sub, "",
          esTransf ? sub : 0, esTransf ? 0 : sub, sub, SES.nombre, "Activo",
          "", "", "", "", "", "", "Normal"] });
        await api({ action: "registrar_movimiento", sheetName: SES.code, fecha: hoyLat(),
          producto: p[2], tipo: "Salida", cantidad: q,
          stockAnterior: stockAnt, stockNuevo: stockNvo, usuario: SES.nombre, observacion: "Descuento por venta" });
      }
      // actualiza stock (la fila se reutiliza por nombre)
      const fila = [...p];
      fila[4] = stockNvo;
      await api({ action: "registrar_inventario", tableName: "Inventario", data: fila });
    }
    CARRITO = {}; $("venta-cliente").value = ""; $("venta-fiado").checked = false;
    toast(fiado ? "Fiado registrado" : "Venta registrada");
    await recargar(); tab("inicio");
  } catch { toast("Error de conexión"); }
  $("btn-cobrar").disabled = false;
});
const fechaHora = () => hoyLat() + " " + horaHM();

// ---------- inventario ----------
function pintarInventario() {
  const q = ($("inv-buscar").value || "").toLowerCase();
  const list = invRows().filter((p) => !q || p[2].toLowerCase().includes(q));
  $("inv-lista").innerHTML = list.length ? "" : '<div class="card">Sin productos.</div>';
  list.forEach((p) => {
    const idx = (TODO.inventario || []).indexOf(p);
    const div = document.createElement("div");
    div.className = "card fila-prod";
    div.innerHTML = `<div><b>${p[2]}</b><small>Stock: ${p[4]} • ${fmt(p[6])} c/u</small></div>
      <button class="btn-mini verde">+ Stock</button>`;
    div.querySelector("button").addEventListener("click", async () => {
      const c = prompt("¿Cuántas unidades ingresan de " + p[2] + "?", "10");
      const n = parseInt(c || "", 10);
      if (!n || n <= 0) return;
      const stockAnt = num(p[4]), stockNvo = stockAnt + n;
      const fila = [...p]; fila[4] = stockNvo;
      await api({ action: "registrar_inventario", tableName: "Inventario", data: fila });
      await api({ action: "registrar_movimiento", sheetName: SES.code, fecha: hoyLat(),
        producto: p[2], tipo: "Entrada", cantidad: n,
        stockAnterior: stockAnt, stockNuevo: stockNvo, usuario: SES.nombre, observacion: "Ingreso de stock" });
      toast("Stock actualizado");
      await recargar();
    });
    $("inv-lista").appendChild(div);
  });
}
$("inv-buscar").addEventListener("input", pintarInventario);

// ---------- deudores ----------
function agruparDeudores() {
  const map = {};
  deuRows().forEach((d) => {
    const n = (d[1] || "").trim();
    if (!map[n]) map[n] = { nombre: n, pendiente: 0, items: [] };
    const pend = Math.max(0, num(d[7]) - num(d[5]) - num(d[6]));
    map[n].pendiente += pend;
    map[n].items.push(d);
  });
  return Object.values(map).filter((d) => d.pendiente > 0.5);
}

function pintarDeudores() {
  const list = agruparDeudores();
  $("deu-detalle").classList.add("oculto");
  $("deu-lista").classList.remove("oculto");
  $("deu-lista").innerHTML = list.length ? "" : '<div class="card">No hay cuentas por cobrar.</div>';
  list.forEach((d) => {
    const div = document.createElement("div");
    div.className = "card fila-deu";
    div.innerHTML = `<div><b>${d.nombre}</b><small>${d.items.length} registro(s)</small></div><span class="deuda">${fmt(d.pendiente)}</span>`;
    div.addEventListener("click", () => verDeudor(d));
    $("deu-lista").appendChild(div);
  });
}

function verDeudor(d) {
  DEU_SEL = d;
  $("deu-lista").classList.add("oculto");
  const det = $("deu-detalle");
  det.classList.remove("oculto");
  det.innerHTML = `<button class="volver" id="deu-volver">← Deudores</button>
    <h2>${d.nombre} • ${fmt(d.pendiente)}</h2>` +
    d.items.map((it) => `<div class="card"><b>${it[2]}</b><br><small>${it[0] || ""} • Pendiente: ${fmt(Math.max(0, num(it[7]) - num(it[5]) - num(it[6])))}</small></div>`).join("") +
    `<input id="deu-monto" type="number" inputmode="numeric" placeholder="Monto a pagar" value="${Math.round(d.pendiente)}">
     <div class="fila"><select id="deu-metodo"><option value="Efectivo">Efectivo</option><option value="Transferencia">Transferencia</option></select></div>
     <button class="btn exito" id="deu-pagar">Registrar pago</button>`;
  $("deu-volver").addEventListener("click", pintarDeudores);
  $("deu-pagar").addEventListener("click", async () => {
    const monto = num($("deu-monto").value);
    if (monto <= 0) { toast("Monto inválido"); return; }
    const esTransf = $("deu-metodo").value === "Transferencia";
    $("deu-pagar").disabled = true;
    try {
      const r = await api({ action: "pagar_deudor", sheetName: SES.code, clienteNombre: d.nombre,
        transferAmount: esTransf ? monto : 0, cashAmount: esTransf ? 0 : monto, usuario: SES.nombre });
      toast(r.status === "success" ? "Pago registrado" : (r.message || "No se pudo registrar"));
      await recargar(); pintarDeudores();
    } catch { toast("Error de conexión"); }
    $("deu-pagar").disabled = false;
  });
}

// ---------- arranque ----------
(function init() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("sw.js").catch(() => {});
  }
  SES = cargarSesion();
  if (SES && SES.code) {
    $("login-nombre").textContent = SES.negocio || SES.code;
    entrar();
  } else {
    const c = localStorage.getItem("kapta_code");
    if (c) { $("in-codigo").value = c; }
    ver("negocio");
  }
  const ci = $("in-codigo");
  ci.addEventListener("change", () => localStorage.setItem("kapta_code", ci.value.trim()));
})();
