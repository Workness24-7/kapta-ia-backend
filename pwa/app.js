/* Kapta IA POS — PWA v2 paridad Android. Vanilla JS contra backend Railway. */
const VERSION_PWA = "PWA-2026-09-22";
const BASE = "https://kapta-ia-backend-production.up.railway.app/exec";
const $ = (id) => document.getElementById(id);
const fmt = (n) => "$" + Math.round(Number(n) || 0).toLocaleString("es-CO");
const fmtM = (n) => "$ " + Math.round(Number(n) || 0).toLocaleString("es-CO");
const num = (v) => { const n = parseFloat(v); return isNaN(n) ? 0 : n; };
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));

let SES = null, TODO = null, EMPRESA = null;
let NOTIF_N = 0;
let ME = null;               // {row, sec, admin}
let SUPER = null;            // {correo} sesión maestra
let NEGOCIOS = [];
let VENTA_CAT = "Todos", INV_CAT = "Todos";
let CARRITO = {};            // idx -> {qty, min}
let DEU_SEL = null, CHICO_SEL = null;
let FIN_FILTRO = "Mes", FIN_DESDE = "", FIN_HASTA = "";
let MOV_FILTRO = "Mes", MOV_DESDE = "", MOV_HASTA = "";
let MOV_VER = false, USU_EDIT = null;
let CLAVE_TIMER = null;

function toast(m) {
  const t = $("toast");
  t.textContent = m; t.classList.add("ver");
  clearTimeout(t._h); t._h = setTimeout(() => t.classList.remove("ver"), 2600);
}
async function api(payload) {
  const r = await fetch(BASE, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
  return r.json();
}
function hoyISO() { const d = new Date(), p = (x) => String(x).padStart(2, "0"); return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`; }
function hoyLat() { const d = new Date(), p = (x) => String(x).padStart(2, "0"); return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()}`; }
function horaHM() { const d = new Date(), p = (x) => String(x).padStart(2, "0"); return `${p(d.getHours())}:${p(d.getMinutes())}`; }
function fechaHora() { return hoyLat() + " " + horaHM(); }
function esHoy(f) {
  f = String(f || "").trim().slice(0, 10);
  if (/^\d{4}-\d{2}-\d{2}$/.test(f)) return f === hoyISO();
  return f === hoyLat();
}
function enRango(f, desde, hasta) {
  const n = normFecha(f); if (!n) return false;
  if (desde && n < desde.split("/").reverse().join("")) return false;
  if (hasta && n > hasta.split("/").reverse().join("")) return false;
  return true;
}
function normFecha(f) {
  f = String(f || "").trim().slice(0, 10);
  if (/^\d{4}-\d{2}-\d{2}$/.test(f)) return f.replace(/-/g, "");
  const p = f.split("/");
  if (p.length === 3) return p[2] + p[1] + p[0];
  return "";
}
function esMesActual(f) {
  const n = normFecha(f);
  return n ? n.slice(0, 6) === hoyISO().replace(/-/g, "").slice(0, 6) : false;
}
function horaDe(fecha) { const p = String(fecha || "").split(" "); return p.length > 1 ? p[1] : ""; }

// ---------- navegación ----------
function ver(id) {
  document.querySelectorAll(".pantalla").forEach((s) => s.classList.add("oculto"));
  $("p-" + id).classList.remove("oculto");
  // El selector de país debe funcionar siempre que se muestre este login
  // (al volver de otras pantallas pintarPaises ya no corría y el botón moría).
  if (id === "negocio") pintarPaises();
}

// Países del selector (banderas locales). Por defecto: Colombia.
const PAISES = [["Colombia", "colombia"], ["México", "mexico"], ["Perú", "peru"], ["Chile", "chile"], ["Argentina", "argentina"], ["Ecuador", "ecuador"]];
let PAIS_SEL = "Colombia";
let PAISES_OK = false;
function pintarPaises() {
  const box = $("pais-lista");
  if (!box || PAISES_OK) return;
  PAISES_OK = true;
  box.innerHTML = "";
  const elegir = ([nombre, img]) => {
    PAIS_SEL = nombre;
    $("pais-sel").innerHTML = `<img id="pais-flag" src="img/flags/${img}.png?v=10" alt=""><span id="pais-nombre">${nombre}</span>`;
    box.classList.add("oculto");
  };
  PAISES.forEach(([nombre, img]) => {
    const b = document.createElement("button");
    b.type = "button";
    b.innerHTML = `<img src="img/flags/${img}.png?v=10" alt=""><span>${nombre}</span>`;
    b.addEventListener("click", () => elegir([nombre, img]));
    box.appendChild(b);
  });
  elegir(PAISES[0]);
  $("pais-btn").addEventListener("click", () => box.classList.toggle("oculto"));
}
$("btn-empresa-ayuda").addEventListener("click", () => {
  toast("Pide tu identificador al administrador de tu negocio o al soporte Kapta IA.");
});
function tab(nombre) {
  if (MODO_AISLADO && nombre !== MODO_AISLADO.vista) {
    location.hash = `#/${MODO_AISLADO.code}/${nombreVista(MODO_AISLADO.vista)}/${MODO_AISLADO.token}`;
    return;
  }
  stopClave();
  document.querySelectorAll(".tab").forEach((t) => t.classList.add("oculto"));
  $("t-" + nombre).classList.remove("oculto");
  document.querySelectorAll("#dock button").forEach((b) => b.classList.toggle("on", b.dataset.tab === nombre));
  if (nombre === "cuenta") pintarCuenta();
}
function armarDock(sec) {
  const tabs = [["inicio", "Inicio"]];
  if (sec._dockVentas) tabs.push(["venta", "Venta"]);
  if (sec._dockInventario) tabs.push(["inventario", "Inventario"]);
  if (ME.admin) tabs.push(["usuarios", "Usuarios"]);
  if (sec._dockFinanzas) tabs.push(["finanzas", "Finanzas"]);
  if (ME.admin || sec._dockFinanzas) tabs.push(["dashboard", "Panel"]);
  if (sec._tabDeudores) tabs.push(["deudores", "Deudores"]);
  $("dock").innerHTML = "";
  tabs.forEach(([k, txt], i) => {
    const b = document.createElement("button");
    b.dataset.tab = k; if (!i) b.classList.add("on");
    b.title = txt;
    b.innerHTML = DOCK_ICONS[k] ? `<img src="img/pos/dock/${DOCK_ICONS[k]}?v=1" alt="${txt}">` : `<span class="dock-emoji">📊</span>`;
    b.addEventListener("click", () => { if (k === "inicio") vrCerrar(); tab(k); });
    $("dock").appendChild(b);
  });
}
const DOCK_ICONS = { inicio: "Inicio.png", venta: "Venta.png", inventario: "Inventario.png", usuarios: "Admin.png", finanzas: "Finanzas.png", deudores: "Deudores.png", dashboard: "" };
if ($("btn-yo")) $("btn-yo").addEventListener("click", () => tab("cuenta"));
if ($("pos-avatar")) $("pos-avatar").addEventListener("click", () => tab("cuenta"));
if ($("btn-soporte")) $("btn-soporte").addEventListener("click", () => $("btn-ayuda").click());
if ($("btn-notif")) $("btn-notif").addEventListener("click", () => {
  tab("inicio");
  toast(NOTIF_N > 0 ? `${NOTIF_N} producto(s) con stock bajo` : "Sin alertas de stock");
});
if ($("dock-search")) $("dock-search").addEventListener("click", () => {
  if (!$("t-venta").classList.contains("oculto") && $("venta-buscar")) { $("venta-buscar").focus(); return; }
  if (!$("t-inventario").classList.contains("oculto") && $("inv-buscar")) { $("inv-buscar").focus(); return; }
  toast("Busca desde Venta o Inventario");
});
if ($("vista-dock")) $("vista-dock").querySelectorAll("button").forEach((b) => b.addEventListener("click", () => {
  AG_VISTA = b.dataset.v === "recuadro" ? "recuadro" : "lista";
  $("vista-dock").querySelectorAll("button").forEach((x) => x.classList.toggle("on", x === b));
  if (AG_ABIERTO) agRender(); else if (!DEU_ABIERTO && !GTO_ABIERTO) pintarAlertas();
}));
// ---------- agregar stock (dock sobre alerta de stock, vistas lista/recuadro) ----------
let AG_VISTA = "lista", AG_ABIERTO = false, AG_SEL = {}, AG_Q = "";
let DEU_ABIERTO = false, DEU_VISTA = "lista", DEU_MET = "Efectivo";
let GTO_ABIERTO = false, GTO_TIPO = "Administrativo", GTO_FOTO = "";
const agAbierta = () => AG_ABIERTO;
function dockDerRender() {
  const h = document.querySelector(".alerta-head .sec-t");
  if (AG_ABIERTO) { if (h) h.textContent = "Agregar Stock y Mercancia"; agRender(); }
  else if (DEU_ABIERTO) { if (h) h.textContent = "Deudores"; deuRender(); }
  else if (GTO_ABIERTO) { if (h) h.textContent = "Gasto"; gtoRender(); }
  else { if (h) h.textContent = "Alerta de Stock"; pintarAlertas(); }
}
function agAbrir() {
  AG_ABIERTO = true; AG_Q = "";
  DEU_ABIERTO = false; GTO_ABIERTO = false; GTO_FOTO = "";
  dockDerRender();
}
function agCerrar() {
  if (!AG_ABIERTO) return;
  AG_ABIERTO = false; AG_SEL = {}; AG_Q = "";
  dockDerRender();
}
function agToggle() { AG_ABIERTO ? agCerrar() : agAbrir(); }
function agIdx(p) { return (TODO.inventario || []).indexOf(p); }
function agRender() {
  if (!TODO) return;
  const box = $("alertas");
  const sel = Object.keys(AG_SEL).map(Number).filter((i) => TODO.inventario[i]).map((i) => [i, TODO.inventario[i]]);
  const q = (AG_Q || "").toLowerCase().trim();
  const sug = q ? invRows().filter((p) => (p[2] || "").toLowerCase().includes(q) && !AG_SEL.hasOwnProperty(agIdx(p))).slice(0, 6) : [];
  let h = `<div class="ag-busca-box"><input id="ag-buscar" class="ag-buscar" placeholder="Buscar producto" autocomplete="off" value="${esc(AG_Q || "")}">`;
  h += `<div id="ag-sug" class="ag-sug flot${sug.length ? "" : " oculto"}">${sug.map((p) => `<button data-i="${agIdx(p)}">${esc(p[2])}</button>`).join("")}</div></div>`;
  if (!sel.length) h += `<div class="card">Busca y selecciona productos para agregar stock.</div>`;
  else if (AG_VISTA === "recuadro") {
    h += `<div class="ag-grid">${sel.map(([i, p]) => `<div class="ag-card"><span class="ag-foto">${p[12] ? `<img src="${esc(p[12])}" alt="" loading="lazy">` : ""}</span><b class="ag-nom">${esc(p[2])}</b><span class="ag-row"><input class="ag-qty" data-i="${i}" inputmode="numeric" value="${AG_SEL[i] || 1}"><button class="ag-add" data-i="${i}">Agregar</button></span></div>`).join("")}</div>`;
  } else {
    h += `<div class="ag-lista">${sel.map(([i, p]) => `<div class="ag-lrow"><span class="ag-foto sm">${p[12] ? `<img src="${esc(p[12])}" alt="" loading="lazy">` : ""}</span><b class="ag-nom lg">${esc(p[2])}</b><button class="ag-menmas" data-i="${i}" data-d="-1" title="Quitar uno"><img src="img/pos/alerta/menos_stock.png?v=1" alt="−"></button><input class="ag-qty" data-i="${i}" inputmode="numeric" value="${AG_SEL[i] || 1}"><button class="ag-menmas" data-i="${i}" data-d="1" title="Agregar uno"><img src="img/pos/alerta/mas_stock.png?v=1" alt="+"></button></div>`).join("")}</div>`;
  }
  if (sel.length) h += `<button id="ag-commit" class="ag-commit">Agregar Stock</button>`;
  box.innerHTML = h;
  const inp = $("ag-buscar");
  if (inp) {
    inp.addEventListener("input", () => {
      const s = inp.selectionStart, e = inp.selectionEnd;
      AG_Q = inp.value; agRender();
      const r = $("ag-buscar");
      if (r) { r.focus(); try { r.setSelectionRange(s, e); } catch {} }
    });
    inp.addEventListener("focus", () => { const s = $("ag-sug"); if (s && s.children.length) s.classList.remove("oculto"); });
    inp.addEventListener("blur", () => setTimeout(() => { const s = $("ag-sug"); if (s) s.classList.add("oculto"); }, 150));
    // ponytail: sin dropdown-keyboard; clic o Enter agrega la primera coincidencia
    inp.addEventListener("keydown", (e) => { if (e.key === "Enter" && sug.length) { const i = agIdx(sug[0]); AG_SEL[i] = 1; AG_Q = ""; agRender(); } });
  }
  box.querySelectorAll("#ag-sug button").forEach((b) => b.addEventListener("click", () => { AG_SEL[b.dataset.i] = AG_SEL[b.dataset.i] || 1; AG_Q = ""; agRender(); }));
  box.querySelectorAll(".ag-qty").forEach((c) => c.addEventListener("input", () => {
    c.value = c.value.replace(/\D/g, "").slice(0, 5);
    const v = parseInt(c.value || "0", 10);
    AG_SEL[c.dataset.i] = isNaN(v) ? 0 : v;
  }));
  box.querySelectorAll(".ag-menmas").forEach((b) => b.addEventListener("click", () => {
    AG_SEL[b.dataset.i] = Math.max(0, (AG_SEL[b.dataset.i] || 0) + Number(b.dataset.d));
    agRender();
  }));
  box.querySelectorAll(".ag-add").forEach((b) => b.addEventListener("click", () => {
    AG_SEL[b.dataset.i] = (AG_SEL[b.dataset.i] || 0) + 1;
    agRender();
  }));
  const cm = $("ag-commit");
  if (cm) cm.addEventListener("click", agCommit);
}
async function agCommit() {
  const ids = Object.keys(AG_SEL).filter((i) => AG_SEL[i] > 0 && TODO.inventario[Number(i)]);
  if (!ids.length) { toast("Escribe una cantidad mayor a 0"); return; }
  try {
    for (const i of ids) {
      const p = TODO.inventario[Number(i)], n = AG_SEL[i];
      const ant = num(p[4]), nvo = ant + n;
      await actualizarStock(p, nvo);
      await logMov(p, "Entrada", n, ant, nvo, "Ingreso de stock");
    }
    AG_SEL = {}; AG_Q = "";
    toast("Stock actualizado");
    await recargar();
    if (AG_ABIERTO) agRender();
  } catch { toast("Error de conexión"); }
}
// ---------- dock deudores (lista → resumen → historial + pago/abono) ----------
function deuToggle() { DEU_ABIERTO ? deuCerrar() : deuAbrir(); }
function deuAbrir() {
  DEU_ABIERTO = true; DEU_SEL = ""; DEU_VISTA = "lista";
  AG_ABIERTO = false; AG_SEL = {}; GTO_ABIERTO = false; GTO_FOTO = "";
  dockDerRender();
}
function deuCerrar() {
  if (!DEU_ABIERTO) return;
  DEU_ABIERTO = false; DEU_SEL = ""; DEU_VISTA = "lista";
  dockDerRender();
}
function deuPieHTML(d) {
  return `<div class="fila" style="justify-content:space-between;margin-top:8px"><b>Total</b><b>${fmt(d.pendiente)}</b></div>`
    + `<div class="seg" id="deu-met">${["Efectivo", "Transferencia"].map((m) => `<button data-m="${m}" class="${DEU_MET === m ? "on" : ""}">${m}</button>`).join("")}</div>`
    + `<div class="fila"><button class="dbtn-pago" id="deu-pago">Pago</button><button class="dbtn-abono" id="deu-abono">Abono</button></div>`;
}
function deuWirePie(d) {
  const met = $("deu-met");
  if (met) met.querySelectorAll("button").forEach((b) => b.addEventListener("click", () => { DEU_MET = b.dataset.m; deuRender(); }));
  if ($("deu-pago")) $("deu-pago").addEventListener("click", () => pagarDeudorDock(d, false));
  if ($("deu-abono")) $("deu-abono").addEventListener("click", () => pagarDeudorDock(d, true));
}
async function pagarDeudorDock(d, esAbono) {
  let monto = d.pendiente;
  if (esAbono) {
    monto = num(prompt(`Abono para ${d.nombre} (pendiente ${fmt(d.pendiente)}):`, ""));
    if (monto <= 0) return;
    if (monto > d.pendiente + 0.5) { toast("El abono no puede superar la deuda"); return; }
  } else if (!confirm(`Registrar pago total de ${fmt(d.pendiente)} de ${d.nombre}?`)) return;
  const esT = DEU_MET === "Transferencia";
  try {
    const r = await api({ action: "pagar_deudor", sheetName: SES.code, clienteNombre: d.nombre,
      transferAmount: esT ? monto : 0, cashAmount: esT ? 0 : monto, usuario: SES.nombre });
    toast(r.status === "success" ? "Pago registrado" : (r.message || "No se pudo registrar"));
    await recargar();
    if (DEU_ABIERTO) deuRender();
  } catch { toast("Error de conexión"); }
}
function resumenDeudor(d) {
  const grupos = {};
  d.items.forEach((it) => {
    const bol = /^bolirrana/i.test(it[8] || "");
    const k = bol ? "§BOL" : ("P:" + (it[2] || "").toLowerCase());
    if (!grupos[k]) grupos[k] = { bol, prod: it[2] || "", cant: 0, sub: 0, partes: [] };
    const g = grupos[k];
    g.cant += num(it[3]); g.sub += num(it[7]);
    if (bol) g.partes.push(`x${num(it[3])} ${it[2]}`);
  });
  return Object.values(grupos).map((g) => g.bol
    ? { txt: "Bolirrana", desc: g.partes.join(" + "), sub: g.sub }
    : { txt: `x${g.cant} ${g.prod}`, desc: "", sub: g.sub });
}
function deuRender() {
  const box = $("alertas");
  if (!box || !TODO) return;
  const deud = agruparDeudores();
  const d = deud.find((x) => x.nombre === DEU_SEL);
  if (DEU_VISTA !== "lista" && !d) DEU_VISTA = "lista";
  if (DEU_VISTA === "lista" || !d) {
    DEU_VISTA = "lista";
    box.innerHTML = deud.length ? deud.map((x) => {
      const f = String((x.items[0] && x.items[0][0]) || "").slice(0, 10);
      return `<div class="acard deu-it" data-n="${esc(x.nombre)}"><span class="ainfo"><b>${esc(x.nombre)}</b><small>Fecha: ${esc(f)}</small></span><span class="monto">${fmtM(x.pendiente)}</span></div>`;
    }).join("") : '<div class="card">Sin deudores pendientes.</div>';
    box.querySelectorAll(".deu-it").forEach((el) => el.addEventListener("click", () => { DEU_SEL = el.dataset.n; DEU_VISTA = "resumen"; deuRender(); }));
    return;
  }
  if (DEU_VISTA === "resumen") {
    box.innerHTML = `<div class="card"><div class="fila" style="justify-content:space-between"><b>${esc(d.nombre)}</b><button class="hbtn" id="deu-hist" title="Historial">🕐</button></div>`
      + `<small class="muted">Resumen de productos</small>`
      + resumenDeudor(d).map((l) => `<div class="fila" style="justify-content:space-between;align-items:flex-start"><span><small>${esc(l.txt)}</small>${l.desc ? `<br><small class="muted">${esc(l.desc)}</small>` : ""}</span><b>${fmt(l.sub)}</b></div>`).join("")
      + deuPieHTML(d)
      + `<button class="btn link" id="deu-volver">← Deudores</button></div>`;
    $("deu-hist").addEventListener("click", () => { DEU_VISTA = "historial"; deuRender(); });
    $("deu-volver").addEventListener("click", () => { DEU_VISTA = "lista"; deuRender(); });
    deuWirePie(d);
    return;
  }
  box.innerHTML = `<div class="card"><div class="fila" style="justify-content:space-between"><b>${esc(d.nombre)}</b><button class="hbtn rx" id="deu-x" title="Volver">✕</button></div>`
    + `<small class="muted">Historial</small>`
    + d.items.map((it) => {
      const cant = num(it[3]), sub = num(it[7]), pu = cant ? Math.round(sub / cant) : 0;
      const ch = parseInt(it[10] || "0", 10) || 0;
      const t1 = [(it[8] && it[8] !== "Normal" ? it[8] : ""), ch ? "Chico " + ch : ""].filter(Boolean).join(" - ") || it[2];
      return `<div class="acard" style="margin:8px 0"><span class="ainfo"><b>${esc(t1)}</b><small>x${cant} ${esc(it[2])}</small><br><small>${esc(horaDe(it[0] || ""))} - $ ${miles(pu)} C/U${it[4] === "SI" ? " - Minimo" : ""}</small></span><b>${fmt(sub)}</b></div>`;
    }).join("")
    + deuPieHTML(d) + `</div>`;
  $("deu-x").addEventListener("click", () => { DEU_VISTA = "resumen"; deuRender(); });
  deuWirePie(d);
}
// ---------- dock gasto (form con tipo, concepto, foto y registro) ----------
function gtoToggle() { GTO_ABIERTO ? gtoCerrar() : gtoAbrir(); }
function gtoAbrir() {
  GTO_ABIERTO = true; GTO_FOTO = "";
  AG_ABIERTO = false; AG_SEL = {}; DEU_ABIERTO = false;
  dockDerRender();
}
function gtoCerrar() {
  if (!GTO_ABIERTO) return;
  GTO_ABIERTO = false; GTO_FOTO = "";
  dockDerRender();
}
function gtoRender() {
  const box = $("alertas");
  if (!box) return;
  box.innerHTML = `<div style="font-weight:700;margin:2px 2px 8px">Tipo de Gasto</div>`
    + `<div class="vr-modos" id="gto-tipos">${["Administrativo", "Recurrente"].map((t) => `<button data-t="${t}" class="${GTO_TIPO === t ? "on" : ""}">${t}</button>`).join("")}</div>`
    + `<div class="gto-fila"><input id="gto-concepto" class="ag-buscar" placeholder="Concepto" autocomplete="off"><input id="gto-valor" class="ag-buscar" placeholder="Valor" inputmode="numeric"></div>`
    + `<textarea id="gto-desc" class="ag-area" placeholder="Descripcion"></textarea>`
    + `<label class="gto-foto" title="Foto del comprobante"><input id="gto-file" type="file" accept="image/*" class="oculto"><span id="gto-mas">+</span><img id="gto-prev" class="oculto" alt=""></label>`
    + `<button id="gto-commit" class="ag-commit">Registrar Gasto</button>`;
  box.querySelectorAll("#gto-tipos button").forEach((b) => b.addEventListener("click", () => { GTO_TIPO = b.dataset.t; gtoRender(); }));
  const fv = $("gto-valor");
  if (fv) fv.addEventListener("input", () => { fv.value = fv.value.replace(/\D/g, "").slice(0, 9); });
  $("gto-file").addEventListener("change", () => {
    const f = $("gto-file").files[0];
    if (!f) return;
    if (f.size > 2 * 1024 * 1024) { toast("Imagen muy pesada (máx 2MB)"); return; }
    const rd = new FileReader();
    rd.onload = () => { GTO_FOTO = String(rd.result || ""); $("gto-prev").src = GTO_FOTO; $("gto-prev").classList.remove("oculto"); $("gto-mas").classList.add("oculto"); };
    rd.readAsDataURL(f);
  });
  $("gto-commit").addEventListener("click", async () => {
    const concepto = ($("gto-concepto").value || "").trim(), monto = num($("gto-valor").value);
    const desc = ($("gto-desc").value || "").trim();
    if (!concepto || monto <= 0) { toast("Concepto y valor son obligatorios"); return; }
    try {
      let foto = "";
      if (GTO_FOTO) {
        const up = await api({ action: "subir_foto", datos: GTO_FOTO, idEmpresa: SES.code });
        if (up.status === "success" && up.data && up.data.url) {
          try { foto = new URL(up.data.url, BASE).href; } catch { foto = up.data.url; }
        }
      }
      const r = await api({ action: "registrar_gasto", tableName: "Gastos",
        data: ["", hoyLat(), horaHM(), GTO_TIPO, concepto, desc, "", monto, "Efectivo", foto, SES.nombre, "Activo", "", ""] });
      if (r.status !== "success") { toast(r.message || "No se pudo guardar"); return; }
      GTO_FOTO = "";
      toast("Gasto registrado");
      await recargar();
      if (GTO_ABIERTO) gtoRender();
    } catch { toast("Error de conexión"); }
  });
}
// ---------- premium: periodos, promos, turnos, costos ----------
let DASH_F = "Mes";
function rangoPeriodo(f, back) {
  const now = new Date();
  let ini, fin;
  back = back || 0;
  if (f === "Día") { ini = new Date(now); fin = new Date(now); }
  else if (f === "Semana") { const dw = (now.getDay() + 6) % 7; ini = new Date(now.getFullYear(), now.getMonth(), now.getDate() - dw - 7 * back); fin = new Date(ini); fin.setDate(fin.getDate() + 6); }
  else if (f === "Año") { ini = new Date(now.getFullYear() - back, 0, 1); fin = new Date(now.getFullYear() - back, 11, 31); }
  else { ini = new Date(now.getFullYear(), now.getMonth() - back, 1); fin = new Date(now.getFullYear(), now.getMonth() - back + 1, 0); }
  const c = (d) => `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`;
  return { d: c(ini), h: c(fin) };
}
const enPeriodo = (f, r) => { const n = normFecha(f); return n ? n >= r.d && n <= r.h : false; };
function cfgHappy() {
  try {
    const rows = (TODO && TODO.config_negocio) || [];
    const r = rows.find((x) => String(x[0] || "").toUpperCase() === "HAPPY_HOUR");
    const a = JSON.parse((r && r[1]) || "[]");
    return Array.isArray(a) ? a : [];
  } catch { return []; }
}
function cfgValor(k, fb) {
  try {
    const rows = (TODO && TODO.config_negocio) || [];
    const r = rows.find((x) => String(x[0] || "").toUpperCase() === String(k).toUpperCase());
    return r ? String(r[1] || "") : fb;
  } catch { return fb; }
}
function happyPara(prod, cuando) {
  if (!tieneFuncion("BAR3")) return null;
  const list = cfgHappy();
  if (!list.length) return null;
  const d = cuando || new Date();
  const dia = (d.getDay() + 6) % 7;
  const hm = d.getHours() * 60 + d.getMinutes();
  const aMin = (t) => { const p = String(t || "").split(":"); return (parseInt(p[0], 10) || 0) * 60 + (parseInt(p[1], 10) || 0); };
  let best = null;
  list.forEach((h, i) => {
    if (!h || h.producto !== prod) return;
    if (Array.isArray(h.dias) && h.dias.length && !h.dias.includes(dia)) return;
    if (hm < aMin(h.desde) || hm > aMin(h.hasta)) return;
    if (h.tipo === "2x1") best = Object.assign({ idx: i }, h);
    else if (!best && num(h.valor) > 0) best = Object.assign({ idx: i }, h);
  });
  return best;
}
// línea con promo y/o mínimo: {pu, sub, desc, promo}
function lineaConPromo(p, q, usarMin) {
  const pn = num(p[6]), mn = num(p[7]);
  if (usarMin && mn > 0) return { pu: mn, sub: mn * q, desc: (pn - mn) * q, promo: "" };
  const promo = happyPara(p[2], new Date());
  if (promo && promo.tipo === "2x1") {
    const paga = Math.ceil(q / 2);
    return { pu: pn, sub: paga * pn, desc: (q - paga) * pn, promo: "HH" + promo.idx };
  }
  if (promo && num(promo.valor) > 0) {
    const pu = Math.max(0, Math.round(pn * (1 - num(promo.valor) / 100)));
    return { pu, sub: pu * q, desc: (pn - pu) * q, promo: "HH" + promo.idx };
  }
  return { pu: pn, sub: pn * q, desc: 0, promo: "" };
}
function costoDe(prod) {
  const p = (TODO.inventario || []).find((x) => (x[2] || "") === prod);
  return p ? num(p[5]) : 0;
}
function catDe(prod) {
  const p = (TODO.inventario || []).find((x) => (x[2] || "") === prod);
  return p ? (((p[3] || "General").trim()) || "General") : "General";
}
// ---------- venta rápida (dock sobre la sección 1, comparte CARRITO) ----------
let VR_MODO = "Normal", VR_Q = "";
const VR_MODOS_DEF = ["Normal", "Bolirrana", "Dados"];
function vrAbierta() { const v = $("venta-rapida"); return !!(v && !v.classList.contains("oculto")); }
function vrModos() {
  try {
    const arr = JSON.parse(localStorage.getItem("kapta_modos_" + SES.code) || "null");
    if (Array.isArray(arr) && arr.length) return arr;
  } catch {}
  return [...VR_MODOS_DEF];
}
function vrGuardarModos(m) { try { localStorage.setItem("kapta_modos_" + SES.code, JSON.stringify(m)); } catch {} }
function vrAbrir() {
  VR_Q = ""; if ($("vr-buscar")) $("vr-buscar").value = "";
  CARRITO = {};
  if ($("vr-cliente") && !$("vr-cliente").value) $("vr-cliente-x").classList.add("oculto");
  $("col-izq").classList.add("oculto");
  $("venta-rapida").classList.remove("oculto");
  vrRender();
}
function vrCerrar() {
  if (!vrAbierta()) return;
  $("venta-rapida").classList.add("oculto");
  $("col-izq").classList.remove("oculto");
}
function vrRender() {
  if (!vrAbierta() || !TODO) return;
  const modos = vrModos().filter((m) => (m !== "Bolirrana" || tieneFuncion("BAR1")) && (m !== "Dados" || tieneFuncion("BAR2")));
  if (!modos.length) return;
  if (!modos.includes(VR_MODO)) VR_MODO = modos[0];
  const mb = $("vr-modos");
  mb.innerHTML = "";
  modos.forEach((m) => {
    const b = document.createElement("button");
    b.type = "button"; b.textContent = m; b.classList.toggle("on", m === VR_MODO);
    b.addEventListener("click", () => { VR_MODO = m; vrRender(); });
    mb.appendChild(b);
  });
  const more = document.createElement("button");
  more.type = "button"; more.textContent = "+"; more.className = "vr-mas"; more.title = "Agregar modo";
  more.addEventListener("click", () => {
    const nom = ((prompt("Nombre del nuevo modo:", "") || "").trim());
    if (!nom) return;
    const arr = vrModos();
    if (!arr.includes(nom)) { arr.push(nom); vrGuardarModos(arr); }
    VR_MODO = nom; vrRender();
  });
  mb.appendChild(more);
  const q = (VR_Q || "").toLowerCase().trim();
  const enCarro = (idx) => CARRITO[idx] && CARRITO[idx].qty > 0;
  const sug = q ? invRows().filter((p) => (p[2] || "").toLowerCase().includes(q) && !enCarro((TODO.inventario || []).indexOf(p))).slice(0, 6) : [];
  const sb = $("vr-sug");
  if (sb) {
    sb.classList.toggle("oculto", !(sug.length && document.activeElement === $("vr-buscar")));
    sb.innerHTML = sug.map((p) => `<button data-i="${(TODO.inventario || []).indexOf(p)}">${esc(p[2])}</button>`).join("");
    sb.querySelectorAll("button").forEach((b) => b.addEventListener("click", () => {
      CARRITO[b.dataset.i] = { qty: 1, min: false };
      VR_Q = ""; if ($("vr-buscar")) $("vr-buscar").value = "";
      vrRender();
    }));
  }
  const cliInp = $("vr-cliente"), cb = $("vr-cli-sug");
  if (cb && cliInp) {
    const cq = (cliInp.value || "").trim().toLowerCase();
    const todos = clientesConocidos();
    const exact = cq && todos.some((n) => n.toLowerCase() === cq);
    const m = todos.filter((n) => !cq || (n.toLowerCase().includes(cq) && n.toLowerCase() !== cq)).slice(0, 6);
    cb.classList.toggle("oculto", !(document.activeElement === cliInp && (m.length || (cq && !exact))));
    cb.innerHTML = m.map((n) => `<button data-n="${esc(n)}">${esc(n)}</button>`).join("")
      + ((cq && !exact) ? `<button class="usar" data-n="${esc(cliInp.value.trim())}">＋ Usar "${esc(cliInp.value.trim())}"</button>` : "");
    cb.querySelectorAll("button").forEach((b) => b.addEventListener("click", () => {
      cliInp.value = b.dataset.n;
      $("vr-cliente-x").classList.remove("oculto");
      cliInp.blur(); vrRender();
    }));
  }
  const ids = Object.keys(CARRITO).filter((i) => enCarro(i) && TODO.inventario[Number(i)]);
  const g = $("vr-grid");
  g.innerHTML = ids.length ? "" : '<div class="card">Busca productos para agregar a la venta.</div>';
  ids.forEach((i) => {
    const p = TODO.inventario[Number(i)];
    const idx = Number(i);
    const it = CARRITO[idx] || { qty: 0, min: false };
    const tieneMin = num(p[7]) > 0;
    const usaMin = it.min && tieneMin;
    const L = lineaConPromo(p, it.qty || 0, it.min), pu = L.pu;
    const card = document.createElement("div");
    card.className = "vrcard";
    card.innerHTML = `${it.qty > 0 ? `<span class="vrbadge">${it.qty}</span>` : ""}`
      + `<span class="vrfoto">${p[12] ? `<img src="${esc(p[12])}" alt="" loading="lazy">` : ""}</span>`
      + `<b>${esc(p[2])}</b>`
      + `<span class="vrprecio-row"><span class="vrprecio${(usaMin || L.promo) ? " min" : ""}">$ ${miles(pu)} c/u${L.promo ? " • HH" : ""}</span>`
      + (tieneMin ? `<button class="vrswitch${usaMin ? " on" : ""}" title="Precio mínimo"></button>` : "") + `</span>`
      + `<span class="vrgrip" title="Arrastra para cambiar el tamaño de las tarjetas"></span>`;
    const bump = (d) => {
      CARRITO[idx] = CARRITO[idx] || { qty: 0, min: false };
      CARRITO[idx].qty = Math.max(0, CARRITO[idx].qty + d);
      vrRender();
    };
    const badge = card.querySelector(".vrbadge");
    if (badge) badge.addEventListener("click", (ev) => { ev.stopPropagation(); bump(1); });
    card.querySelector(".vrfoto").addEventListener("click", () => bump(-1));
    const sw = card.querySelector(".vrswitch");
    if (sw) sw.addEventListener("click", () => {
      CARRITO[idx] = CARRITO[idx] || { qty: 0, min: false };
      CARRITO[idx].min = !CARRITO[idx].min;
      vrRender();
    });
    g.appendChild(card);
  });
  g.querySelectorAll(".vrgrip").forEach(conectarVrGrip);
  const tot = Object.keys(CARRITO).reduce((a, i) => {
    const p = TODO.inventario[Number(i)], it = CARRITO[i];
    if (!p || !it || !it.qty) return a;
    return a + it.qty * precioEfectivo(p, it.min);
  }, 0);
  $("vr-total").textContent = fmtM(tot);
}
const miles = (n) => Math.round(Number(n) || 0).toLocaleString("es-CO");
async function vrCobrar(fiabl) {
  const ids = Object.keys(CARRITO).filter((i) => CARRITO[i].qty > 0);
  if (!ids.length) { toast("Agrega productos primero"); return; }
  const modo = VR_MODO || "Normal";
  const esBol = modo === "Bolirrana";
  const mesa = 1;
  const chico = esBol ? chicoActivo(mesa) : 0;
  let cliente = ($("vr-cliente").value || "").trim();
  if (!cliente) cliente = esBol ? `Bolirrana ${mesa}` : "Cliente Mostrador";
  const tipoTxt = esBol ? `Bolirrana(${mesa})` : modo;
  const folio = (!fiabl && !esBol) ? nuevoFolio() : "";
  try {
    const det = [];
    for (const i of ids) {
      const p = TODO.inventario[Number(i)], it = CARRITO[i], q = it.qty;
      const L = lineaConPromo(p, q, it.min), pu = L.pu, sub = L.sub;
      const ant = num(p[4]), nvo = Math.max(0, ant - q);
      if (fiabl || esBol) {
        await api({ action: "registrar_deudor", tableName: "Deudores",
          data: [fechaHora(), cliente, p[2], q, it.min ? "SI" : (L.promo ? "HH" : ""), 0, 0, sub, tipoTxt, "", chico] });
        await logMov(p, "Salida", q, ant, nvo, "Descuento por deudor");
      } else {
        await api({ action: "registrar_venta", tableName: "Ventas",
          data: [folio, hoyISO(), horaHM(), cliente, p[0], p[2], q, pu, sub, Math.round(L.desc), 0, sub, sub, SES.nombre, "Activo", "", "", L.promo, "", "", "", tipoTxt] });
        await logMov(p, "Salida", q, ant, nvo, "Descuento por venta");
      }
      await actualizarStock(p, nvo);
      det.push({ prod: p[2], cant: q, pu, sub });
    }
    if (esBol) chicoSiguiente(mesa, chico);
    CARRITO = {}; $("vr-cliente").value = ""; $("vr-cliente-x").classList.add("oculto");
    await recargar();
    const fac = folio ? await emitirFactura(det, { folio, cliente, modo: tipoTxt, medio: "Efectivo" }) : null;
    if (fac) verFacturaDIAN(fac);
    else if (folio) verComprobante({ cod: folio, fecha: hoyLat(), hora: horaHM(), cliente, modo: tipoTxt, usuario: SES.nombre, estado: "Activo", items: det, total: det.reduce((a, d) => a + d.sub, 0) });
    else toast("Registrado");
    if (vrAbierta()) vrRender();
  } catch { toast("Error de conexión"); }
}
if ($("vr-buscar")) $("vr-buscar").addEventListener("input", () => { VR_Q = $("vr-buscar").value; vrRender(); });
if ($("vr-buscar")) $("vr-buscar").addEventListener("focus", () => vrRender());
if ($("vr-buscar")) $("vr-buscar").addEventListener("blur", () => setTimeout(() => { const b = $("vr-sug"); if (b) b.classList.add("oculto"); }, 150));
if ($("vr-buscar")) $("vr-buscar").addEventListener("keydown", (e) => {
  if (e.key !== "Enter" || !TODO) return;
  const q = ($("vr-buscar").value || "").toLowerCase().trim();
  if (!q) return;
  const p = invRows().find((x) => (x[2] || "").toLowerCase().includes(q) && !(CARRITO[(TODO.inventario || []).indexOf(x)] || {}).qty);
  if (!p) return;
  CARRITO[(TODO.inventario || []).indexOf(p)] = { qty: 1, min: false };
  VR_Q = ""; $("vr-buscar").value = "";
  vrRender();
});
if ($("vr-cliente")) $("vr-cliente").addEventListener("input", () => {
  $("vr-cliente-x").classList.toggle("oculto", !$("vr-cliente").value.trim());
  vrRender();
});
if ($("vr-cliente")) $("vr-cliente").addEventListener("focus", () => vrRender());
if ($("vr-cliente")) $("vr-cliente").addEventListener("blur", () => setTimeout(() => { const b = $("vr-cli-sug"); if (b) b.classList.add("oculto"); }, 150));
if ($("vr-cliente-x")) $("vr-cliente-x").addEventListener("click", () => {
  $("vr-cliente").value = ""; $("vr-cliente-x").classList.add("oculto"); $("vr-cliente").focus();
});
if ($("vr-paga")) $("vr-paga").addEventListener("click", () => vrCobrar(false));
if ($("vr-debe")) $("vr-debe").addEventListener("click", () => vrCobrar(true));

// ---------- divisores arrastrables del inicio + tamaño tarjetas VR ----------
function layKey(k) {
  let code = "pub";
  try { code = (SES && SES.code) || "pub"; } catch { code = "pub"; }
  return "kapta_lay_" + code + "_" + k;
}
function layGet(k) { try { const v = localStorage.getItem(layKey(k)); return v == null || v === "" ? null : Number(v); } catch { return null; } }
function laySet(k, v) { try { if (v == null) localStorage.removeItem(layKey(k)); else localStorage.setItem(layKey(k), String(v)); } catch {} }
function aplicarLayout() {
  try {
    const ti = $("t-inicio");
    const l = layGet("col");
    if (l && ti) ti.style.gridTemplateColumns = `minmax(0,${l}px) 10px minmax(0,1fr)`;
    const rh = layGet("res"), res = $("resumen");
    if (rh && res) { res.style.flex = "0 0 " + rh + "px"; res.style.overflow = "hidden"; }
    const ah = layGet("acc"), acc = $("bloque-acciones");
    if (ah && acc) { acc.style.flex = "0 0 " + ah + "px"; acc.style.overflow = "hidden"; }
    const vw = layGet("vrw"), g = $("vr-grid");
    if (vw && g) g.style.setProperty("--vrw", vw + "px");
  } catch {}
}
function dragSplit(el, onStart, onMove, onReset) {
  if (!el || el._on) return;
  el._on = true;
  el.addEventListener("dblclick", (e) => { e.preventDefault(); onReset(); });
  el.addEventListener("pointerdown", (e) => {
    if (e.pointerType === "mouse" && e.button !== 0) return;
    e.preventDefault();
    el.classList.add("on");
    try { el.setPointerCapture(e.pointerId); } catch {}
    onStart(e);
    const mv = (ev) => onMove(ev);
    const up = () => {
      el.classList.remove("on");
      el.removeEventListener("pointermove", mv);
      el.removeEventListener("pointerup", up);
      el.removeEventListener("pointercancel", up);
    };
    el.addEventListener("pointermove", mv);
    el.addEventListener("pointerup", up);
    el.addEventListener("pointercancel", up);
  });
}
function activarSplits() {
  const ti = $("t-inicio");
  if (!ti) return;
  dragSplit($("split-v"), (e) => {
    const s = $("split-v");
    s._x = e.clientX; s._w = $("col-izq").getBoundingClientRect().width; s._tw = ti.getBoundingClientRect().width;
  }, (e) => {
    const s = $("split-v");
    const w = Math.min(Math.max(s._w + e.clientX - s._x, 300), s._tw - 300);
    ti.style.gridTemplateColumns = `minmax(0,${Math.round(w)}px) 10px minmax(0,1fr)`;
    laySet("col", Math.round(w));
  }, () => { laySet("col", null); ti.style.gridTemplateColumns = ""; });
  dragSplit($("split-hl"), (e) => {
    const s = $("split-hl");
    s._y = e.clientY; s._h = $("resumen").getBoundingClientRect().height;
    s._min = 180; s._max = Math.max(200, $("col-izq").getBoundingClientRect().height - 170);
  }, (e) => {
    const s = $("split-hl"), r = $("resumen");
    const h = Math.min(Math.max(s._h + e.clientY - s._y, s._min), s._max);
    r.style.flex = "0 0 " + Math.round(h) + "px"; r.style.overflow = "hidden";
    laySet("res", Math.round(h));
  }, () => { laySet("res", null); const r = $("resumen"); r.style.flex = ""; r.style.overflow = ""; });
  dragSplit($("split-ha"), (e) => {
    const s = $("split-ha");
    s._y = e.clientY; s._h = $("bloque-acciones").getBoundingClientRect().height;
    s._min = 120; s._max = Math.max(140, $("col-der").getBoundingClientRect().height - 220);
  }, (e) => {
    const s = $("split-ha"), b = $("bloque-acciones");
    const h = Math.min(Math.max(s._h + e.clientY - s._y, s._min), s._max);
    b.style.flex = "0 0 " + Math.round(h) + "px"; b.style.overflow = "hidden";
    laySet("acc", Math.round(h));
  }, () => { laySet("acc", null); const b = $("bloque-acciones"); b.style.flex = ""; b.style.overflow = ""; });
}
function conectarVrGrip(gr) {
  if (!gr || !$("vr-grid")) return;
  dragSplit(gr, (e) => {
    gr._x = e.clientX;
    const cur = getComputedStyle($("vr-grid")).getPropertyValue("--vrw");
    gr._w = parseInt(cur, 10) || 170;
  }, (e) => {
    const w = Math.min(Math.max(gr._w + (e.clientX - gr._x), 120), 300);
    $("vr-grid").style.setProperty("--vrw", Math.round(w) + "px");
    laySet("vrw", Math.round(w));
  }, () => { laySet("vrw", null); $("vr-grid").style.removeProperty("--vrw"); });
}
// ---------- permisos (mismo esquema JSON que Android) ----------
const FULL = () => ({ resumen: ["ventas", "gastos", "deudores", "clientes"], acciones: ["venta", "gasto", "agregar", "deudores"], alertas: true, ventasResumen: ["hoy", "semana", "mes"], ventasRanking: true, ventasVerMas: true, ventasVerInventario: true, finPdf: true, finFiltros: ["dia", "mes", "rango"], finVentas: true, finGastos: true, finRegistrar: true, invCarga: true, invMovimientos: true, invCrear: true, invEditar: true, invEliminar: true, invGuardar: true, invHacer: true, invLectura: false, _dockVentas: true, _dockFinanzas: true, _dockInventario: true, _tabDeudores: true });
const esCajeroLike = (rol) => /cajero|empleado|mesero|barman/i.test(rol || "") && !/admin|supervisor/i.test(rol || "");

function resolverSec() {
  const admin = /admin|supervisor/i.test(SES.rol || "");
  if (admin) { ME.admin = true; ME.codes = new Set(FUNC_CATALOG.map((f) => f.c)); return FULL(); }
  ME.admin = false;
  const raw = ME.funciones || "";
  let obj = null;
  try { if (raw.trim().startsWith("{")) obj = JSON.parse(raw); } catch { obj = null; }
  if (obj && obj.secciones) {
    const s = Object.assign(FULL(), obj.secciones);
    const dock = obj.dock || {};
    s._dockVentas = dock.Ventas === true; s._dockFinanzas = dock.Finanzas === true; s._dockInventario = dock.Inventario === true;
    s._tabDeudores = (obj.caps || []).includes("deudores") || s._dockVentas;
    if (s.invLectura) { ["invCarga", "invMovimientos", "invCrear", "invEditar", "invEliminar", "invGuardar", "invHacer"].forEach((k) => (s[k] = false)); }
    ME.codes = new Set(["BAR1", "BAR2", "BAR3", "BAR4"]);
    if (s.invEditar) ME.codes.add("BAR5");
    return s;
  }
  if (obj && obj.dock) {
    const d = obj.dock, inv = d.Inventario === true && !esCajeroLike(SES.rol);
    ME.codes = new Set(["BAR1", "BAR2", "BAR3", "BAR4"]);
    if (inv) ME.codes.add("BAR5");
    return Object.assign(FULL(), {
      _dockVentas: d.Ventas === true, _dockFinanzas: d.Finanzas === true, _dockInventario: d.Inventario === true,
      _tabDeudores: true,
      invCarga: inv, invMovimientos: inv, invCrear: inv, invEditar: inv, invEliminar: inv, invGuardar: inv, invHacer: inv,
      invLectura: esCajeroLike(SES.rol),
    });
  }
  // Compacto "1 - 5 - BAR1": funciones por número.
  const codes = String(raw).split("-").map((x) => x.trim().toUpperCase()).filter(Boolean);
  if (codes.length && codes.some((c) => /^\d+$/.test(c) || /^BAR\d+$/.test(c))) {
    ME.codes = new Set(codes);
    return secFromCodes(ME.codes);
  }
  ME.codes = new Set(["BAR1", "BAR2", "BAR3", "BAR4"]);
  // Legado: reproduce la vista anterior por rol.
  const mesero = /mesero|barman/i.test(SES.rol || "");
  return Object.assign(FULL(), {
    acciones: mesero ? ["venta", "deudores"] : ["venta", "gasto", "deudores"],
    _dockVentas: !mesero, _dockFinanzas: false, _dockInventario: true, _tabDeudores: true,
    ventasResumen: mesero ? [] : ["hoy", "semana", "mes"],
    ventasRanking: !mesero, ventasVerMas: !mesero, ventasVerInventario: !mesero,
    finPdf: false, finFiltros: [], finVentas: false, finGastos: false, finRegistrar: false,
    invCarga: false, invMovimientos: false, invCrear: false, invEditar: false, invEliminar: false,
    invGuardar: false, invHacer: false, invLectura: true,
  });
}

// ---------- sesión ----------
function guardarSesion() {
  ($( "in-recordar").checked ? localStorage : sessionStorage).setItem("kapta_pwa", JSON.stringify(SES));
}
function cargarSesion() {
  try { return JSON.parse(localStorage.getItem("kapta_pwa") || sessionStorage.getItem("kapta_pwa") || "null"); }
  catch { return null; }
}

// Identidad visual del negocio (colores + logo), como en Android.
function aplicarIdentidad(emp) {
  try {
    const root = document.documentElement;
    const prim = (emp && emp.colorPrimario) || "#4F46E5";
    root.style.setProperty("--prim", prim);
    const meta = document.getElementById("meta-theme");
    if (meta) meta.setAttribute("content", prim);
    const logo = (emp && (emp.listIconUrl || emp.logoUrl)) || "";
    const ll = $("login-logo"), le = $("login-emoji"), pl = $("pos-logo");
    const fb = $("pos-logo-fb");
    if (logo && ll && le && pl) {
      ll.src = logo; ll.classList.remove("oculto"); le.classList.add("oculto");
      pl.src = logo; pl.classList.remove("oculto");
      if (fb) fb.classList.add("oculto");
    } else if (ll && le && pl) {
      ll.classList.add("oculto"); le.classList.remove("oculto");
      pl.classList.add("oculto");
      if (fb) { fb.textContent = ((emp && emp.nombre) || "K").trim().charAt(0).toUpperCase(); fb.classList.remove("oculto"); }
    }
    refrescarChrome();
  } catch { /* HTML en caché de versión anterior: no bloquea el ingreso */ }
}

function planDesdeTODO() {
  try {
    const rows = (TODO && TODO.config_negocio) || [];
    const r = rows.find((x) => String(x[0] || "").toUpperCase() === "PLAN");
    return (r && r[1]) || "";
  } catch { return ""; }
}
function pintarPlanBar(emp) {
  const bar = $("pos-planbar");
  if (!bar) return;
  const plan = ((emp && emp.plan) || planDesdeTODO() || "").trim();
  const estado = ((emp && emp.estado) || "").trim().toUpperCase();
  let g;
  if (/PRUEBA/.test(estado)) g = "linear-gradient(135deg, #5ce1e6, #0012ff)";
  else if (/MAX/i.test(plan)) g = "linear-gradient(135deg, #8c52ff, #ff7a00)";
  else if (/PREMIUM/i.test(plan)) g = "linear-gradient(135deg, #5ce1e6, #8c52ff)";
  else g = "linear-gradient(135deg, #ffffff, #5ce1e6)";
  bar.style.background = g;
}
function refrescarChrome() {
  try {
    const av = $("avatar-letra");
    if (av) av.textContent = ((SES && SES.nombre) || (EMPRESA && EMPRESA.nombre) || "?").trim().charAt(0).toUpperCase() || "?";
  } catch {}
  try { pintarPlanBar(EMPRESA); } catch {}
}
$("btn-codigo").addEventListener("click", async () => {
  const code = $("in-codigo").value.trim().toUpperCase();
  $("err-codigo").textContent = "";
  if (!code) { $("err-codigo").textContent = "Escribe el código de tu negocio"; return; }
  if (code === "APTADMIN") { ver("superlogin"); return; }
  $("btn-codigo").disabled = true;
  try {
    const r = await fetch(BASE + "?action=resolver_empresa&codigo=" + encodeURIComponent(code)).then((x) => x.json());
    const emp = (r.status === "success" && r.data && r.data.empresa) || null;
    if (!emp) { $("err-codigo").textContent = "Negocio no encontrado"; return; }
    SES = { code, negocio: emp.nombre || code };
    EMPRESA = emp;
    aplicarIdentidad(emp);
    $("login-nombre").textContent = SES.negocio;
    $("login-dominio").textContent = code.toLowerCase() + ".kaptaia.app";
    localStorage.setItem("kapta_code", code);
    ver("login");
  } catch { $("err-codigo").textContent = "Sin conexión. Intenta de nuevo."; }
  $("btn-codigo").disabled = false;
});
$("btn-volver-negocio").addEventListener("click", () => ver("negocio"));

// ---------- superadmin ----------
$("btn-super-volver").addEventListener("click", () => ver("negocio"));
$("btn-superlogin").addEventListener("click", async () => {
  const correo = $("in-super-correo").value.trim();
  const clave = $("in-super-clave").value;
  $("err-super").textContent = "";
  if (!correo || !clave) { $("err-super").textContent = "Completa correo y contraseña"; return; }
  $("btn-superlogin").disabled = true;
  try {
    const r = await api({ action: "login_superadmin", correo, password: clave });
    if (r.status !== "success") { $("err-super").textContent = r.message || "Credenciales inválidas"; return; }
    SUPER = { correo, token: ((r.data || {}).token || "") };
    sessionStorage.setItem("kapta_super", correo);
    sessionStorage.setItem("kapta_super_tok", SUPER.token);
    await cargarNegocios();
  } catch { $("err-super").textContent = "Sin conexión. Intenta de nuevo."; }
  $("btn-superlogin").disabled = false;
});

async function cargarNegocios() {
  try {
    const tok = (SUPER && SUPER.token) ? "&token=" + encodeURIComponent(SUPER.token) : "";
    const r = await fetch(BASE + "?action=listar_empresas" + tok).then((x) => x.json());
    NEGOCIOS = ((r.data || {}).empresas || []);
  } catch { NEGOCIOS = []; }
  pintarNegocios();
  ver("negocios");
}

function badgeEstado(e) {
  const suspended = /suspend|eliminado/i.test(e.estado || "");
  return `<span class="badge ${suspended ? "susp" : "activo"}">${esc(e.estado || "Activo")}</span>`;
}

function pintarNegocios() {
  const box = $("neg-lista");
  box.innerHTML = NEGOCIOS.length ? "" : '<div class="card">Sin negocios.</div>';
  NEGOCIOS.forEach((e) => {
    const logo = e.listIconUrl || e.logoUrl || "";
    const div = document.createElement("div");
    div.className = "card";
    div.innerHTML = `<div class="fila-deu">
      <div style="display:flex;gap:10px;align-items:center">${logo ? `<img class="logo-neg" src="${logo}" alt="">` : `<span style="font-size:28px">🏪</span>`}
      <div><b>${esc(e.nombre || e.codigo)}</b><br><small>${esc(e.codigo || "")} • ${esc(e.ciudad || "")}</small><br>
      <span class="badge plan">${esc(e.plan || "")}</span>${badgeEstado(e)}</div></div>
      <div class="cant"><button class="btn-mini" data-a="entrar">Entrar</button></div></div>
      <div class="fila-btns"><button class="btn-mini" data-a="stats">📊 Ver datos</button>
      <button class="btn-mini" data-a="susp">${/suspend|eliminado/i.test(e.estado || "") ? "Reactivar" : "Suspender"}</button>
      <button class="btn-mini" data-a="del">🗑️ Eliminar</button></div>
      <div data-d="stats"></div>`;
    div.querySelectorAll("button").forEach((b) => b.addEventListener("click", async (ev) => {
      ev.stopPropagation();
      const a = b.dataset.a;
      if (a === "entrar") entrarComoAdmin(e);
      else if (a === "stats") verStatsNegocio(e, div.querySelector('[data-d="stats"]'));
      else if (a === "susp") {
        const nuevo = /suspend|eliminado/i.test(e.estado || "") ? "Activo" : "Suspendido";
        if (!confirm(`¿${nuevo === "Activo" ? "Reactivar" : "Suspender"} ${e.nombre}?`)) return;
        const r = await api({ action: "actualizar_empresa", codigo: e.codigo, estado: nuevo });
        toast(r.status === "success" ? "Negocio actualizado" : (r.message || "No se pudo actualizar"));
        await cargarNegocios();
      } else if (a === "del") {
        if (!confirm(`¿Eliminar ${e.nombre}? Se borrará en 2 días.`)) return;
        const r = await api({ action: "eliminar_empresa", empresaNombre: e.nombre || e.codigo });
        toast(r.status === "success" ? "Negocio eliminado" : (r.message || "No se pudo eliminar"));
        await cargarNegocios();
      }
    }));
    box.appendChild(div);
  });
}

async function verStatsNegocio(e, box) {
  if (box.innerHTML) { box.innerHTML = ""; return; }
  box.innerHTML = "<small>Cargando...</small>";
  try {
    const r = await api({ action: "obtener_todo", sheetName: e.codigo });
    if (r.status !== "success") { box.innerHTML = "<small>No se pudo cargar.</small>"; return; }
    const d = r.data || {};
    const inv = (d.inventario || []).filter((x) => x[2] && x[2] !== "Nom_Producto");
    const ven = (d.ventas || []).filter((x) => x[5] && x[5] !== "Producto");
    const deu = (d.deudores || []).filter((x) => x[1] && x[1] !== "Nom_Cliente");
    const usu = (d.usuarios || []).filter((x) => x[2] && x[2] !== "Correo");
    const pend = deu.reduce((a, x) => a + Math.max(0, num(x[7]) - num(x[5]) - num(x[6])), 0);
    box.innerHTML = `<small>📦 ${inv.length} productos • 💰 ${ventasMes(ven)} en ventas del mes • 👤 ${fmt(pend)} por cobrar • 👥 ${usu.length} usuarios • 📞 ${esc(e.celular1 || "—")} • Vence: ${esc(e.fechaVencimiento || "—")}</small>`;
  } catch { box.innerHTML = "<small>Sin conexión.</small>"; }
}
const ventasMes = (ven) => fmt(ven.filter((v) => esMesActual(v[1]) && String(v[14] || "").toLowerCase() !== "anulado").reduce((a, v) => a + num(v[12]), 0));

$("btn-nuevo-negocio").addEventListener("click", () => {
  openModal(`<h2>Nuevo Negocio</h2>
    <input id="n-nombre" placeholder="Nombre *">
    <div class="fila"><input id="n-codigo" placeholder="Código *" autocapitalize="none"><input id="n-ciudad" placeholder="Ciudad"></div>
    <div class="fila"><input id="n-tel" placeholder="Teléfono" inputmode="tel"><input id="n-nit" placeholder="NIT" inputmode="numeric"></div>
    <input id="n-email" placeholder="Correo admin *">
    <input id="n-pass" type="password" placeholder="Contraseña admin *">
    <div class="fila"><select id="n-plan"><option>Básico</option><option>Premium</option><option>MAX IA</option></select>
    <select id="n-tiempo"><option>1 Mes</option><option>3 Meses</option><option>6 Meses</option><option>1 Año</option><option>Permanente</option></select></div>
    <select id="n-tipo"><option>Bar</option><option>Restaurante</option><option>Café</option><option>Licorería</option><option>Tienda</option><option>Otro</option></select>
    <p id="n-err" class="error"></p>
    <button class="btn exito" id="n-guardar">Crear Negocio</button>
    <button class="btn link" id="n-cancelar">Cancelar</button>`);
  $("n-cancelar").addEventListener("click", closeModal);
  $("n-guardar").addEventListener("click", async () => {
    const nombre = $("n-nombre").value.trim(), codigo = $("n-codigo").value.trim().toUpperCase();
    const email = $("n-email").value.trim(), pass = $("n-pass").value;
    if (!nombre || !codigo) { $("n-err").textContent = "Nombre y código son obligatorios"; return; }
    if (!EMAIL_RE.test(email)) { $("n-err").textContent = "Correo admin inválido"; return; }
    if (!pass) { $("n-err").textContent = "Contraseña admin obligatoria"; return; }
    const r = await api({ action: "registrar_empresa", nombre, codigo,
      ciudad: $("n-ciudad").value.trim(), celular1: $("n-tel").value.trim(), nit: $("n-nit").value.trim(),
      correo: email, adminNombre: "Administrador", adminCorreo: email, adminPassword: pass,
      plan: $("n-plan").value, tiempo: $("n-tiempo").value, tipo: $("n-tipo").value, pais: "Colombia" });
    if (r.status !== "success") { $("n-err").textContent = r.message || "No se pudo crear"; return; }
    closeModal(); toast("Negocio creado: " + codigo);
    await cargarNegocios();
  });
});

$("btn-ver-soportes").addEventListener("click", async () => {
  const box = $("sop-lista");
  if (!box.classList.contains("oculto")) { box.classList.add("oculto"); box.innerHTML = ""; return; }
  box.classList.remove("oculto");
  box.innerHTML = "<small>Cargando...</small>";
  try {
    const r = await api({ action: "listar_soportes" });
    const list = ((r.data || {}).data || r.data?.soportes || r.data || []);
    const arr = Array.isArray(list) ? list : [];
    box.innerHTML = "<h3>Solicitudes de soporte</h3>" + (arr.length ? "" : "<div class='card'>Sin solicitudes.</div>");
    arr.forEach((s) => {
      const div = document.createElement("div");
      div.className = "card";
      div.innerHTML = `<b>${esc(s.tipo_solicitud || s.tipo || "Soporte")}</b><br><small>${esc(s.solicitante || "")} • ${esc(s.fecha_solicitud || s.fecha || "")}</small><br>${esc(s.observaciones || s.mensaje || "")}`;
      box.appendChild(div);
    });
  } catch { box.innerHTML = "<small>Sin conexión.</small>"; }
});

async function entrarComoAdmin(emp) {
  SES = { code: (emp.codigo || "").toUpperCase(), negocio: emp.nombre || emp.codigo, correo: emp.correo || SUPER.correo, nombre: "SuperAdmin", rol: "Administrador", uid: "SUPERADMIN", super: true };
  EMPRESA = emp;
  aplicarIdentidad(emp);
  const ck = "kapta_consent_" + SES.code + "_superadmin";
  if (!localStorage.getItem(ck)) {
    const ok = confirm("Entras como SuperAdmin a " + SES.negocio + ".\n\nAl entrar aceptas la Política de Privacidad y los Términos de Uso.\n\n¿Aceptas y deseas continuar?");
    if (!ok) return;
    localStorage.setItem(ck, new Date().toISOString());
  }
  await entrar();
}
$("btn-neg-salir").addEventListener("click", () => {
  SUPER = null; SES = null; ME = null;
  sessionStorage.removeItem("kapta_super");
  sessionStorage.removeItem("kapta_super_tok");
  aplicarIdentidad(null);
  ver("negocio");
});

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
    SES.uid = d.idUsuario || correo;
    SES.rol = d.rol || r.rol || "Empleado";
    guardarSesion();
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
  ver("pos");
  try {
    await recargar();
  } catch {
    toast("Sin conexión: revisa tu internet");
    ME = ME || { row: null, funciones: "", sec: null };
    if (!ME.sec) ME.sec = resolverSec();
  }
  try {
    armarDock(ME.sec);
  } catch {
    armarDock(FULL());
  }
  $("btn-regalo").style.display = ME.admin ? "" : "none";
  tab("inicio");
  activarSplits(); aplicarLayout();
  rellenarFunciones().catch(() => {});
  try {
    const cv = document.getElementById("cuenta-version");
    if (cv) cv.textContent = VERSION_PWA;
  } catch { /* noop */ }
}
// Escribe una vez las funciones numeradas si la columna viene vacía o legada.
async function rellenarFunciones() {
  if (!ME || !ME.row) return;
  const raw = String(ME.row[22] || "");
  if (/\d|BAR/i.test(raw)) return;
  const codes = ME.admin ? FUNC_CATALOG.map((f) => f.c) : [...(ME.codes || [])];
  if (!codes.length) return;
  const sec = ME.admin ? FULL() : secFromCodes(new Set(codes));
  const dc = dockCapsFromCodes(new Set(codes));
  const row = ME.row;
  const data = [row[0] || "", row[1] || "", row[2] || "", row[3] || "", row[4] || "Cajero", row[5] || "Activo",
    row[6] || "", row[7] || "", row[8] || "", row[9] || "0", row[10] || "", row[11] || "",
    row[12] || "0", row[13] || "0", row[14] || "", row[15] || "", row[16] || "",
    row[17] || hoyLat(), row[18] || "", fmtFechaHora(new Date()), "Numeración de funciones", SES.uid || SES.correo,
    JSON.stringify({ compact: codes.join(" - "), dock: dc.dock, functions: [], caps: dc.caps, secciones: sec })];
  const r = await api({ action: "crear_usuario", tableName: "Usuarios", data });
  if (r.status === "success") {
    ME.row = data;
    ME.funciones = data[22];
  }
}
$("btn-ayuda").addEventListener("click", () => {
  openModal(`<h2>Solicitar Soporte</h2>
    <select id="s-tipo"><option>Error en la app</option><option>Duda de uso</option><option>Planes y pagos</option><option>Otro</option></select>
    <input id="s-msg" placeholder="Cuéntanos qué pasa">
    <p id="s-err" class="error"></p>
    <button class="btn exito" id="s-enviar">Enviar</button>
    <button class="btn link" id="s-cancelar">Cancelar</button>`);
  $("s-cancelar").addEventListener("click", closeModal);
  $("s-enviar").addEventListener("click", async () => {
    const msg = $("s-msg").value.trim();
    if (!msg) { $("s-err").textContent = "Escribe tu mensaje"; return; }
    const r = await api({ action: "registrar_soporte", tipo_solicitud: $("s-tipo").value, observaciones: msg + ` (${SES.nombre})`, solicitante: SES.code });
    if (r.status !== "success") { $("s-err").textContent = r.message || "No se pudo enviar"; return; }
    closeModal(); toast("Solicitud enviada. Te contactaremos pronto.");
  });
});

$("btn-nuevo-deudor").addEventListener("click", () => {
  openModal(`<h2>Nuevo Deudor</h2>
    <input id="nd-cliente" placeholder="Cliente *">
    <input id="nd-prod" placeholder="Producto / concepto *">
    <div class="fila"><input id="nd-cant" type="number" value="1" inputmode="numeric"><input id="nd-precio" type="number" placeholder="Precio c/u *"></div>
    <p id="nd-err" class="error"></p>
    <button class="btn exito" id="nd-guardar">Registrar</button>
    <button class="btn link" id="nd-cancelar">Cancelar</button>`);
  $("nd-cancelar").addEventListener("click", closeModal);
  $("nd-guardar").addEventListener("click", async () => {
    const cliente = $("nd-cliente").value.trim(), prod = $("nd-prod").value.trim();
    const cant = Math.max(1, parseInt($("nd-cant").value || "1", 10)), pu = num($("nd-precio").value);
    if (!cliente || !prod || pu <= 0) { $("nd-err").textContent = "Completa cliente, producto y precio"; return; }
    const r = await api({ action: "registrar_deudor", tableName: "Deudores",
      data: [fechaHora(), cliente, prod, cant, "", 0, 0, cant * pu, "Normal", "", 0] });
    if (r.status !== "success") { $("nd-err").textContent = r.message || "No se pudo registrar"; return; }
    closeModal(); toast("Deudor registrado");
    await recargar(); pintarDeudores();
  });
});
function salir() {
  if (SES && SES.super && SUPER) {
    SES = null; ME = null;
    cargarNegocios();
    return;
  }
  const ais = MODO_AISLADO;
  SES = null; ME = null; MI_TOKEN = null; MODO_AISLADO = null;
  try { document.getElementById("p-pos").classList.remove("aislado"); } catch {}
  localStorage.removeItem("kapta_pwa"); sessionStorage.removeItem("kapta_pwa");
  aplicarIdentidad(null);
  if (ais) location.hash = `#/${ais.code}/Login`;
  else ver("negocio");
}
$("btn-salir").addEventListener("click", salir);
if ($("btn-salir-ais")) $("btn-salir-ais").addEventListener("click", salir);

// ---------- datos ----------
async function recargar() {
  toast("Cargando...");
  try {
    const [t, e] = await Promise.all([
      api({ action: "obtener_todo", sheetName: SES.code }),
      fetch(BASE + "?action=resolver_empresa&codigo=" + encodeURIComponent(SES.code)).then((x) => x.json()).catch(() => null),
    ]);
    if (t.status !== "success") { toast("No se pudo cargar"); return; }
    TODO = t.data;
    if (e && e.status === "success") {
      const emp = (e.data || {}).empresa || null;
      if (emp && (emp.codigo || "").toUpperCase() === SES.code) EMPRESA = emp;
    }
    const urow = (TODO.usuarios || []).find((u) => (u[2] || "").toLowerCase() === (SES.correo || "").toLowerCase());
    ME = { row: urow || null, funciones: urow ? (urow[22] || "") : "", sec: null };
    ME.sec = resolverSec();
    if (urow && urow[1]) SES.nombre = urow[1];
    if (urow && urow[4]) SES.rol = urow[4];
    $("pos-usuario").textContent = SES.nombre + " • " + SES.rol;
    pintarCuentaInfo();
    pintarResumen(); pintarVenta(); pintarInventario(); pintarDeudores(); pintarFinanzas(); pintarUsuarios(); pintarDashboard();
    await cargarFicha();
    refrescarChrome();
    if (vrAbierta()) vrRender();
  } catch { toast("Sin conexión"); }
}
if ($("btn-recargar")) $("btn-recargar").addEventListener("click", recargar);

const invRows = () => (TODO.inventario || []).filter((x) => x[2] && x[2] !== "Nom_Producto");
const venRows = () => (TODO.ventas || []).filter((x) => x[5] && x[5] !== "Producto");
const venVivas = () => venRows().filter((v) => String(v[14] || "").toLowerCase() !== "anulado");
const deuRows = () => (TODO.deudores || []).filter((x) => x[1] && x[1] !== "Nom_Cliente");
const gasRows = () => (TODO.gastos || []).filter((x) => x[0] && String(x[0]).startsWith("G-"));
const movRows = () => (TODO.movimientos || []).filter((x) => x[0] && String(x[0]).startsWith("M-"));
const usuRows = () => (TODO.usuarios || []).filter((x) => x[2] && x[2] !== "Correo").map((x) => x.concat(Array(Math.max(0, 23 - x.length)).fill("")));
const kpi = (t, v) => `<div class="kpi"><small>${t}</small><b>${v}</b></div>`;

// ---------- inicio ----------
function pintarResumen() {
  const s = ME.sec;
  const totVentasHoy = venVivas().filter((v) => esHoy(v[1])).reduce((a, v) => a + num(v[12]), 0);
  const totGastosMes = gasRows().filter((g) => esMesActual(g[1])).reduce((a, g) => a + num(g[7]), 0);
  const deud = agruparDeudores();
  const totDeuda = deud.reduce((a, x) => a + x.pendiente, 0);
  const cliAct = (() => {
    const set = new Set(deud.map((d) => d.nombre));
    venVivas().filter((v) => esHoy(v[1])).forEach((v) => {
      const c = (v[3] || "").trim();
      if (c && !/^cliente mostrador$/i.test(c)) set.add(c);
    });
    return set.size;
  })();
  const miles = (n) => Math.round(n).toLocaleString("es-CO");
  const cards = [
    ["ventas", "k-verde", "Ventas.png", "Ventas del dia", `<small>$</small>${miles(totVentasHoy)}`, "En tiempo Real (Clic)", "finanzas"],
    ["gastos", "k-rojo", "Gastos.png", "Gastos del mes", `<small>$</small>${miles(totGastosMes)}`, "Total Acumulado", ""],
    ["deudores", "k-amarillo", "Deudores.png", "Deudores", `${deud.length} <span class="pers">Personas</span>`, `Total: $ ${miles(totDeuda)} (Clic)`, "deudores"],
    ["clientes", "k-cian", "cliente_Activos.png", "Clientes Activos", `${cliAct} <span class="pers">Personas</span>`, "En el establecimiento", ""],
  ].filter(([k]) => s.resumen.includes(k));
  const box = $("resumen");
  box.innerHTML = cards.length ? cards.map(([, cls, icon, titulo, numHtml, sub, go]) =>
    `<div class="kcard ${cls}"${go ? ` data-ir="${go}"` : ""}><span class="kico"><img src="img/pos/resumen/${icon}?v=1" alt=""></span><h4>${titulo}</h4><div class="knum">${numHtml}</div><div class="ksub">${sub}</div></div>`
  ).join("") : '<div class="card">Sin tarjetas activas.</div>';
  box.querySelectorAll("[data-ir]").forEach((d) => d.addEventListener("click", () => tab(d.dataset.ir)));
  const accs = [["venta", "acc-venta", "Venta.png", "Venta", "venta"], ["gasto", "acc-gasto", "Gasto.png", "Gasto", "finanzas"], ["agregar", "acc-agregar", "Agregar.png", "Agregar", "inventario"], ["deudores", "acc-deudores", "Deudores.png", "Deudores", "deudores"]]
    .filter(([k]) => s.acciones.includes(k));
  $("bloque-acciones").classList.toggle("oculto", !accs.length);
  $("acciones").innerHTML = "";
  accs.forEach(([k, cls, icon, txt, go]) => {
    const b = document.createElement("button");
    b.className = "accb " + cls; b.innerHTML = `<img src="img/pos/acciones/${icon}?v=1" alt=""><span>${txt}</span>`;
    b.addEventListener("click", () => {
      if (k === "venta") { vrAbierta() ? vrCerrar() : vrAbrir(); return; }
      if (k === "agregar") { agToggle(); return; }
      if (k === "deudores") { deuToggle(); return; }
      if (k === "gasto") { gtoToggle(); return; }
      agCerrar(); deuCerrar(); gtoCerrar();
      tab(go);
    });
    $("acciones").appendChild(b);
  });
  $("bloque-alertas").classList.toggle("oculto", !s.alertas);
  if (s.alertas) dockDerRender();
  else {
    NOTIF_N = 0;
    const dot = $("notif-dot");
    if (dot) dot.style.display = "none";
  }
  pintarPerfil();
}
function pintarAlertas() {
  const items = invRows().map((p) => [p, alertaDe(p)]).filter(([, n]) => n > 0);
  NOTIF_N = items.length;
  const dot = $("notif-dot");
  if (dot) dot.style.display = NOTIF_N ? "" : "none";
  const abox = $("alertas");
  if (!items.length) {
    abox.innerHTML = '<div class="card">¡Todo en orden! Stock suficiente.</div>';
  } else {
    abox.innerHTML = items.map(([p]) => {
        const idx = (TODO.inventario || []).indexOf(p);
        const n = alertaDe(p);
        const img = p[12] ? `<img src="${esc(p[12])}" alt="" loading="lazy">` : "";
        const nulo = num(p[4]) === 0;
        return `<div class="acard"><span class="athumb">${img}</span><span class="ainfo"><b>${esc(p[2])}</b><small>Quedan ${p[4]} und</small></span><span class="abadge ${nulo ? "anulo" : n === 2 ? "abajo" : "amedio"}">${nulo ? "Stock Nulo" : n === 2 ? "Stock Bajo" : "Stock Medio"}</span><button class="aplus" data-i="${idx}" title="Agregar stock"><img src="img/pos/alerta/agregar2.png?v=1" alt="+"></button></div>`;
    }).join("");
    abox.querySelectorAll(".aplus").forEach((b) => b.addEventListener("click", async () => {
      const p = TODO.inventario[Number(b.dataset.i)];
      if (!p) return;
      const c = prompt("¿Cuántas unidades ingresan de " + p[2] + "?", "10");
      const n = parseInt(c || "", 10);
      if (!n || n <= 0) return;
      const ant = num(p[4]), nvo = ant + n;
      await actualizarStock(p, nvo);
      await logMov(p, "Entrada", n, ant, nvo, "Ingreso de stock");
      toast("Stock actualizado"); await recargar();
    }));
  }
}
// 0 sin alerta · 1 stock medio (<= 1.5x mínimo) · 2 stock bajo (<= mínimo)
function alertaDe(p) {
  const stock = num(p[4]), min = num(p[8] || 0);
  if (stock <= min) return 2;
  if (min > 0 && stock <= min * 1.5) return 1;
  return 0;
}
async function pintarPerfil() {
  const box = $("perfil");
  if (!box) return;
  const nombre = (SES && SES.nombre) || "", rol = (SES && SES.rol) || "", correo = (SES && SES.correo) || "";
  const tel = (EMPRESA && EMPRESA.celular1) || "";
  const ini = (nombre || "?").trim().charAt(0).toUpperCase();
  box.innerHTML = `<div class="perfil-card"><span class="perfil-foto">${esc(ini)}</span><span class="perfil-datos"><b>${esc(nombre)}</b><i>${esc(rol)}</i><small>${esc(correo)}</small>${tel ? `<small>${esc(tel)}</small>` : ""}</span><span class="perfil-clave"><i id="perfil-clave-txt">••••••</i></span><button class="perfil-menu" id="perfil-puntos" title="Mi cuenta"><img src="img/pos/trespuntos.png?v=1" alt=""></button></div>`;
  $("perfil-puntos").addEventListener("click", () => tab("cuenta"));
  try {
    const r = await api({ action: "obtener_clave_dinamica", empresa: SES.code, codigo: SES.code });
    const cod = ((r.data || {}).codigo || "").trim();
    if (cod && $("perfil-clave-txt")) $("perfil-clave-txt").textContent = cod;
  } catch {}
}

// ---------- venta ----------
function chicoActivo(mesa) {
  try {
    const j = JSON.parse(localStorage.getItem(`kapta_chico_${SES.code}_${mesa}`) || "null");
    if (!j || Date.now() - j.ts > 20 * 60 * 1000) return 1;
    return j.n;
  } catch { return 1; }
}
function chicoSiguiente(mesa, actual) {
  localStorage.setItem(`kapta_chico_${SES.code}_${mesa}`, JSON.stringify({ n: actual + 1, ts: Date.now() }));
}
function pintarVenta() {
  const selM = $("venta-modo");
  [...selM.options].forEach((o) => { if ((o.value === "Bolirrana" && !tieneFuncion("BAR1")) || (o.value === "Dados" && !tieneFuncion("BAR2"))) o.remove(); });
  if (![...selM.options].some((o) => o.value === selM.value)) selM.value = "Normal";
  const modo = $("venta-modo").value;
  const esBol = modo === "Bolirrana";
  $("venta-mesa").classList.toggle("oculto", !esBol);
  const mesa = Number($("venta-mesa").value || 1);
  if (esBol) {
    $("venta-chico-info").classList.remove("oculto");
    $("venta-chico-info").textContent = `Bolirrana ${mesa} • próximo chico: ${chicoActivo(mesa)}`;
  } else $("venta-chico-info").classList.add("oculto");
  const q = ($("venta-buscar").value || "").toLowerCase().trim();
  const sinFiltro = !q && (VENTA_CAT || "Todos") === "Todos";
  const cats = ["Todos", ...new Set(invRows().map((p) => (p[3] || "General").trim()).filter(Boolean))];
  $("venta-cats").innerHTML = "";
  cats.forEach((c) => {
    const b = document.createElement("button");
    b.textContent = c;
    b.classList.toggle("on", (VENTA_CAT || "Todos") === c);
    b.addEventListener("click", () => { VENTA_CAT = c; pintarVenta(); });
    $("venta-cats").appendChild(b);
  });
  const list = sinFiltro ? [] : invRows().filter((p) =>
    (!q || p[2].toLowerCase().includes(q)) &&
    ((VENTA_CAT || "Todos") === "Todos" || (p[3] || "General").trim() === VENTA_CAT));
  $("venta-productos").innerHTML = list.length ? "" : `<div class="card">${sinFiltro ? "Busca o filtra por categoría para agregar productos." : "Sin productos."}</div>`;
  list.forEach((p) => {
    const idx = (TODO.inventario || []).indexOf(p);
    const img = p[12] ? `<img class="thumb" src="${p[12]}" alt="" loading="lazy">` : "";
    const div = document.createElement("div");
    div.className = "card fila-prod";
    div.innerHTML = `${img}<div><b>${esc(p[2])}</b><small>Stock: ${p[4]} • ${esc(p[3] || "")}</small></div>
      <div class="cant"><span class="precio">${fmt(p[6])}</span><button>+</button></div>`;
    div.querySelector("button").addEventListener("click", () => {
      CARRITO[idx] = CARRITO[idx] || { qty: 0, min: false };
      CARRITO[idx].qty += 1;
      pintarCarrito();
    });
    $("venta-productos").appendChild(div);
  });
  pintarCarrito(); pintarHistorial();
}
$("venta-buscar").addEventListener("input", pintarVenta);
$("venta-modo").addEventListener("change", pintarVenta);
$("venta-mesa").addEventListener("change", pintarVenta);

function precioEfectivo(p, usarMin) {
  return usarMin && num(p[7]) > 0 ? num(p[7]) : num(p[6]);
}
function pintarCarrito() {
  const ids = Object.keys(CARRITO).filter((i) => CARRITO[i].qty > 0);
  $("venta-carrito").classList.toggle("oculto", !ids.length);
  if (!ids.length) return;
  let total = 0, html = "";
  ids.forEach((i) => {
    const p = TODO.inventario[Number(i)], it = CARRITO[i];
    const L = lineaConPromo(p, it.qty, it.min), pu = L.pu, sub = L.sub;
    total += sub;
    const tieneMin = num(p[7]) > 0;
    html += `<div class="card"><div class="fila-prod"><div><b>${esc(p[2])}</b><small>${it.qty} x ${fmt(pu)}${L.promo ? " (HH)" : it.min ? " (mínimo)" : ""}</small></div>
      <div class="cant"><button data-i="${i}" data-d="-1">−</button><b>${it.qty}</b><button data-i="${i}" data-d="1">+</button></div></div>
      ${tieneMin ? `<button class="btn-mini" data-m="${i}">${it.min ? "Quitar mínimo" : "Precio mínimo"}</button>` : ""}</div>`;
  });
  $("carrito-items").innerHTML = html;
  $("carrito-total").textContent = fmt(total);
  $("carrito-items").querySelectorAll("button[data-d]").forEach((b) => b.addEventListener("click", () => {
    const it = CARRITO[b.dataset.i];
    it.qty = Math.max(0, it.qty + Number(b.dataset.d));
    pintarCarrito();
  }));
  $("carrito-items").querySelectorAll("button[data-m]").forEach((b) => b.addEventListener("click", () => {
    CARRITO[b.dataset.m].min = !CARRITO[b.dataset.m].min;
    pintarCarrito();
  }));
}
$("btn-regalo").style.display = "none";

async function actualizarStock(p, nuevo) {
  const fila = [...p]; fila[4] = nuevo;
  return api({ action: "registrar_inventario", tableName: "Inventario", data: fila });
}
async function logMov(p, tipo, cant, ant, nvo, obs) {
  return api({ action: "registrar_movimiento", sheetName: SES.code, fecha: hoyLat(), producto: p[2], tipo, cantidad: cant, stockAnterior: ant, stockNuevo: nvo, usuario: SES.nombre, observacion: obs });
}

$("btn-cobrar").addEventListener("click", async () => {
  const ids = Object.keys(CARRITO).filter((i) => CARRITO[i].qty > 0);
  if (!ids.length) return;
  const modo = $("venta-modo").value || "Normal";
  const esBol = modo === "Bolirrana";
  const mesa = Number($("venta-mesa").value || 1);
  const chico = esBol ? chicoActivo(mesa) : 0;
  const cliente = esBol ? `Bolirrana ${mesa}` : ($("venta-cliente").value.trim() || "Cliente Mostrador");
  const metodo = $("venta-metodo").value;
  const fiado = $("venta-fiado").checked;
  const esTransf = metodo === "Transferencia";
  const tipoTxt = esBol ? `Bolirrana(${mesa})` : modo;
  const folio = (!fiado && !esBol) ? nuevoFolio() : "";
  $("btn-cobrar").disabled = true;
  try {
    const det = [];
    for (const i of ids) {
      const p = TODO.inventario[Number(i)], it = CARRITO[i], q = it.qty;
      const L = lineaConPromo(p, q, it.min), pu = L.pu, sub = L.sub;
      const ant = num(p[4]), nvo = Math.max(0, ant - q);
      if (fiado || esBol) {
        await api({ action: "registrar_deudor", tableName: "Deudores",
          data: [fechaHora(), cliente, p[2], q, it.min ? "SI" : (L.promo ? "HH" : ""), 0, 0, sub, tipoTxt, "", chico] });
        await logMov(p, "Salida", q, ant, nvo, "Descuento por deudor");
      } else {
        await api({ action: "registrar_venta", tableName: "Ventas",
          data: [folio, hoyISO(), horaHM(), cliente, p[0], p[2], q, pu, sub, Math.round(L.desc), esTransf ? sub : 0, esTransf ? 0 : sub, sub, SES.nombre, "Activo", "", "", L.promo, "", "", "", tipoTxt] });
        await logMov(p, "Salida", q, ant, nvo, "Descuento por venta");
      }
      await actualizarStock(p, nvo);
      det.push({ prod: p[2], cant: q, pu, sub });
    }
    if (esBol) chicoSiguiente(mesa, chico);
    CARRITO = {}; $("venta-cliente").value = ""; $("venta-fiado").checked = false;
    await recargar();
    const fac = folio ? await emitirFactura(det, { folio, cliente, modo: tipoTxt, medio: esTransf ? "Transferencia" : "Efectivo" }) : null;
    if (fac) verFacturaDIAN(fac);
    else if (folio) verComprobante({ cod: folio, fecha: hoyLat(), hora: horaHM(), cliente, modo: tipoTxt, usuario: SES.nombre, estado: "Activo", items: det, total: det.reduce((a, d) => a + d.sub, 0) });
    else { toast(esBol ? `Chico ${chico} de Bolirrana ${mesa} registrado` : fiado ? "Fiado registrado" : "Venta registrada"); tab("inicio"); }
  } catch { toast("Error de conexión"); }
  $("btn-cobrar").disabled = false;
});

// ---------- ventas: folio, historial, comprobante y anulación ----------
let VENH_F = "Día", VENH_D = "", VENH_H = "";
function nuevoFolio() {
  const d = new Date(), p = (x) => String(x).padStart(2, "0");
  const az = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let s = "";
  for (let i = 0; i < 3; i++) s += az[Math.floor(Math.random() * az.length)];
  return `F-${String(d.getFullYear()).slice(2)}${p(d.getMonth() + 1)}${p(d.getDate())}-${p(d.getHours())}${p(d.getMinutes())}-${s}`;
}
function lunesISO() { const d = new Date(), p = (x) => String(x).padStart(2, "0"); const l = new Date(d.getFullYear(), d.getMonth(), d.getDate() - ((d.getDay() + 6) % 7)); return `${l.getFullYear()}-${p(l.getMonth() + 1)}-${p(l.getDate())}`; }
const esSemanaActual = (f) => { const n = normFecha(f); return n ? n >= lunesISO().replace(/-/g, "") : false; };
const qrURL = (txt) => "https://api.qrserver.com/v1/create-qr-code/?size=140x140&margin=6&data=" + encodeURIComponent(txt);
function qrPayload(g) {
  return JSON.stringify({ f: g.cod, n: SES.negocio, c: g.cliente, h: g.fecha + " " + g.hora, t: Math.round(g.total), d: g.items.map((it) => [it.prod, it.cant, Math.round(it.sub)]) });
}
function comprobanteHTML(g) {
  return `<h2>Comprobante de venta</h2>`
    + `<div class="card" style="text-align:center"><b>${esc(SES.negocio)}</b><br><small>${esc(g.fecha)} • ${esc(g.hora)} • ${esc(g.modo)}</small><br>`
    + `<div class="monto">${esc(g.cod)}</div>`
    + (g.estado === "Anulado" ? `<br><span class="badge susp">ANULADA</span>` : "") + `</div>`
    + `<div class="card"><b>Cliente / Mesa:</b> ${esc(g.cliente)}<br><small>Atendido por ${esc(g.usuario)}</small>`
    + `<table class="tabla"><tr><th>Pedido</th><th>Cant.</th><th>Subtotal</th></tr>`
    + g.items.map((it) => `<tr><td>${esc(it.prod)}</td><td>${it.cant}</td><td>${fmt(it.sub)}</td></tr>`).join("")
    + `</table><div class="monto">Total: ${fmt(g.total)}</div></div>`
    + `<div class="card" style="text-align:center"><img src="${qrURL(qrPayload(g))}" alt="QR ${esc(g.cod)}" width="140" height="140" loading="lazy" onerror="this.outerHTML='<b>${esc(g.cod)}</b>'"><br><small>Referencia: ${esc(g.cod)}</small></div>`
    + `<button class="btn exito" id="comp-print">🖨️ Imprimir</button>`
    + `<button class="btn link" id="comp-cerrar">Cerrar</button>`;
}
function comprobantePrint(g) {
  return `<p><b>${esc(SES.negocio)}</b><br>${esc(g.fecha)} ${esc(g.hora)} • ${esc(g.modo)}<br>Cliente / Mesa: <b>${esc(g.cliente)}</b> • Atiende: ${esc(g.usuario)}</p>`
    + `<p><b>Factura: ${esc(g.cod)}</b>${g.estado === "Anulado" ? " (ANULADA)" : ""}</p>`
    + `<table class="tabla"><tr><th>Pedido</th><th>Cant.</th><th>Subtotal</th></tr>`
    + g.items.map((it) => `<tr><td>${esc(it.prod)}</td><td>${it.cant}</td><td>${fmt(it.sub)}</td></tr>`).join("")
    + `</table><p><b>Total: ${fmt(g.total)}</b></p>`
    + `<p><img src="${qrURL(qrPayload(g))}" width="140" height="140"><br><small>Referencia: ${esc(g.cod)}</small></p>`;
}
function verComprobante(g) {
  openModal(comprobanteHTML(g));
  $("comp-cerrar").addEventListener("click", closeModal);
  $("comp-print").addEventListener("click", () => imprimir("Comprobante " + g.cod, comprobantePrint(g)));
}
function gruposVentas() {
  const mapa = {};
  venRows().forEach((v) => {
    const id = String(v[0] || "");
    const folio = /^F-/i.test(id);
    const key = folio ? id.toUpperCase() : [v[1], v[2], v[3]].join("|");
    if (!mapa[key]) mapa[key] = { cod: folio ? id.toUpperCase() : "S/F " + (v[1] || ""), fecha: v[1] || "", hora: v[2] || "", cliente: v[3] || "", modo: v[21] || "Normal", usuario: v[13] || "", estado: "Activo", items: [], total: 0, folio, ids: [] };
    const g = mapa[key];
    if (id) g.ids.push(id);
    g.items.push({ prod: v[5] || "", cant: num(v[6]), pu: num(v[7]), sub: num(v[8]) || num(v[12]) });
    g.total += num(v[12]);
    if (String(v[14] || "").toLowerCase() === "anulado") g.estado = "Anulado";
    if (!g.hora && v[2]) g.hora = v[2];
  });
  const k = (g) => (normFecha(g.fecha) || "") + (g.hora || "");
  return Object.values(mapa).sort((a, b) => k(b).localeCompare(k(a)));
}
function pintarHistorial() {
  const box = $("venh-lista");
  if (!box) return;
  const fr = $("venh-facrow");
  if (fr) fr.classList.toggle("oculto", !tieneFuncion("BAR4"));
  const F = $("venh-filtros");
  F.innerHTML = ["Día", "Semana", "Mes", "Rango"].map((t) => `<button data-f="${t}" class="${VENH_F === t ? "on" : ""}">${t}</button>`).join("");
  F.querySelectorAll("button").forEach((b) => b.addEventListener("click", () => { VENH_F = b.dataset.f; pintarHistorial(); }));
  $("venh-rango").classList.toggle("oculto", VENH_F !== "Rango");
  const match = (f) => VENH_F === "Día" ? esHoy(f) : VENH_F === "Semana" ? esSemanaActual(f) : VENH_F === "Mes" ? esMesActual(f) : enRango(f, VENH_D, VENH_H);
  const list = gruposVentas().filter((g) => match(g.fecha));
  box.innerHTML = list.length ? "" : '<div class="card">Sin ventas en el periodo.</div>';
  list.forEach((g) => {
    const div = document.createElement("div");
    div.className = "card fila-deu";
    div.innerHTML = `<div><b>${esc(g.cod)}</b> ${g.estado === "Anulado" ? '<span class="badge susp">Anulada</span>' : ""}<br><small>${esc(g.fecha)} ${esc(g.hora)} • ${esc(g.cliente)} • ${g.items.length} item(s)</small></div>`
      + `<div class="cant"><span class="monto">${fmt(g.total)}</span>${g.ids.length && g.estado !== "Anulado" ? '<button class="btn-mini" data-a="pdf">PDF</button><button class="btn-mini" data-a="anular">Anular</button>' : ""}</div>`;
    div.addEventListener("click", (e) => {
      const b = e.target.closest("button[data-a]");
      if (b && b.dataset.a === "anular") { e.stopPropagation(); anularVenta(g); return; }
      if (b && b.dataset.a === "pdf") {
        e.stopPropagation();
        facturaDeGrupo(g).then((f) => { imprimir("Factura " + f.cod, facturaHTML(f)); dibujarBarras(); }).catch(() => toast("No se pudo generar"));
        return;
      }
      verDocumento(g);
    });
    box.appendChild(div);
  });
}
async function anularVenta(g, pre) {
  const ids = (g.ids || []).filter(Boolean);
  if (!ids.length) { toast("Esta venta no tiene folio anulable"); return false; }
  if (!pre && !confirm(`¿Anular la venta ${g.cod} por ${fmt(g.total)}? Se devolverá el stock.`)) return false;
  if (!ME.admin) {
    const clave = (prompt("Clave dinámica del administrador (6 dígitos):", "") || "").trim();
    if (!clave) return false;
    try {
      const v = await api({ action: "validar_clave_dinamica", empresa: SES.code, codigo: clave, clave });
      if (!(v.status === "success" && ((v.data || {}).valida === true))) { toast(v.message || "Clave inválida"); return false; }
    } catch { toast("Error de conexión"); return false; }
  }
  try {
    for (const id of ids) {
      const r = await api({ action: "anular_venta", sheetName: SES.code, idVenta: id, usuario: SES.nombre });
      if (r.status !== "success") { toast(r.message || "No se pudo anular"); return false; }
    }
    for (const it of g.items) {
      const p = (TODO.inventario || []).find((x) => (x[2] || "") === it.prod);
      if (!p || !it.cant) continue;
      const ant = num(p[4]), nvo = ant + it.cant;
      await logMov(p, "Entrada", it.cant, ant, nvo, "Anulación " + g.cod);
      await actualizarStock(p, nvo);
    }
    toast("Venta anulada");
    await recargar();
    return true;
  } catch { toast("Error de conexión"); return false; }
}

// Regalo de la casa (requiere clave dinámica del admin).
async function regaloCasa() {
  const ids = Object.keys(CARRITO).filter((i) => CARRITO[i].qty > 0);
  if (!ids.length) { toast("Agrega productos primero"); return; }
  const clave = prompt("Clave dinámica del administrador (6 dígitos):", "");
  if (!clave) return;
  try {
    const v = await api({ action: "validar_clave_dinamica", empresa: SES.code, codigo: clave.trim(), clave: clave.trim() });
    const ok = v.status === "success" && ((v.data || {}).valida === true);
    if (!ok) { toast(v.message || "Clave inválida"); return; }
    let total = 0;
    const detalle = [];
    for (const i of ids) {
      const p = TODO.inventario[Number(i)], q = CARRITO[i].qty;
      const pu = precioEfectivo(p, CARRITO[i].min), sub = q * pu;
      total += sub; detalle.push(`${p[2]} x${q}`);
      const ant = num(p[4]), nvo = Math.max(0, ant - q);
      await logMov(p, "Salida", q, ant, nvo, "Descuento por venta");
      await actualizarStock(p, nvo);
    }
    await api({ action: "registrar_gasto", tableName: "Gastos",
      data: ["", hoyLat(), horaHM(), "Regalo", "Regalo de la casa: " + detalle.join(", "), "", "", total, "", "", SES.nombre, "Activo", "", ""] });
    CARRITO = {};
    toast("Regalo registrado: " + fmt(total));
    await recargar(); tab("inicio");
  } catch { toast("Error de conexión"); }
}

// ---------- inventario ----------
function pintarInventario() {
  const s = ME.sec;
  $("inv-botones").innerHTML = "";
  const addBtn = (txt, fn, verde) => {
    const b = document.createElement("button");
    b.className = "btn-mini" + (verde ? " verde" : "");
    b.textContent = txt; b.addEventListener("click", fn);
    $("inv-botones").appendChild(b);
  };
  if (s.invCarga) {
    const lab = document.createElement("label");
    lab.className = "btn-mini"; lab.textContent = "Carga Masiva";
    const inp = document.createElement("input");
    inp.type = "file"; inp.accept = ".csv,.txt,.xlsx,.xls"; inp.style.display = "none";
    inp.addEventListener("change", async () => {
      if (!inp.files.length) return;
      try {
        const f = inp.files[0];
        const texto = /\.xlsx?$/i.test(f.name) ? await xlsxACsv(f) : await f.text();
        const r = await api({ action: "importar_inventario", sheetName: SES.code, csv: texto });
        toast(r.status === "success" ? `Importados ${((r.data || {}).insertados || 0)}` : (r.message || "Error al importar"));
        await recargar();
      } catch { toast("No se pudo leer el Excel (revisa tu internet)"); }
    });
    lab.appendChild(inp); $("inv-botones").appendChild(lab);
  }
  if (s.invMovimientos) addBtn("Movimientos", () => { MOV_VER = !MOV_VER; pintarMovimientos(); });
  if (s.invCrear) addBtn("+ Producto", () => formProducto(null), true);
  if (s.invEditar && tieneFuncion("BAR5")) addBtn("Merma", () => formMerma());
  if (s.invEditar && tieneFuncion("BAR5")) addBtn("Conteo", () => formConteo());
  let cbox = $("inv-cats");
  if (!cbox) { cbox = document.createElement("div"); cbox.id = "inv-cats"; cbox.className = "chips"; $("inv-botones").after(cbox); }
  const cats = ["Todos", ...new Set(invRows().map((p) => (p[3] || "General").trim()).filter(Boolean))];
  if (!cats.includes(INV_CAT)) INV_CAT = "Todos";
  cbox.innerHTML = "";
  cats.forEach((c) => {
    const b = document.createElement("button");
    b.textContent = c;
    b.classList.toggle("on", INV_CAT === c);
    b.addEventListener("click", () => { INV_CAT = c; pintarInventario(); });
    cbox.appendChild(b);
  });
  const q = ($("inv-buscar").value || "").toLowerCase().trim().replace(/í/g, "i");
  const soloMin = q === "minimo";
  const list = invRows().filter((p) =>
    (INV_CAT === "Todos" || (p[3] || "General").trim() === INV_CAT) &&
    (soloMin ? num(p[4]) <= num(p[8] || 0) : (!q || p[2].toLowerCase().includes(q) || (p[3] || "").toLowerCase().includes(q))));
  $("inv-lista").innerHTML = list.length ? "" : '<div class="card">Sin productos en inventario.</div>';
  list.forEach((p) => {
    const idx = (TODO.inventario || []).indexOf(p);
    const img = p[12] ? `<img class="thumb" src="${p[12]}" alt="" loading="lazy">` : "";
    const div = document.createElement("div");
    div.className = "card fila-prod";
    div.innerHTML = `${img}<div><b>${esc(p[2])}</b><small>${esc(p[3] || "")} • Stock: ${p[4]} (mín ${p[8] || 0}) • ${fmt(p[6])} c/u</small></div>
      <div class="cant"><button data-a="stock">+ Stock</button>${s.invEditar ? '<button data-a="edit">✏️</button>' : ""}${s.invEliminar ? '<button data-a="del">🗑️</button>' : ""}</div>`;
    div.querySelectorAll("button").forEach((b) => b.addEventListener("click", async () => {
      if (b.dataset.a === "stock") {
        const c = prompt("¿Cuántas unidades ingresan de " + p[2] + "?", "10");
        const n = parseInt(c || "", 10);
        if (!n || n <= 0) return;
        const ant = num(p[4]), nvo = ant + n;
        await actualizarStock(p, nvo);
        await logMov(p, "Entrada", n, ant, nvo, "Ingreso de stock");
        toast("Stock actualizado"); await recargar();
      } else if (b.dataset.a === "edit") formProducto(idx);
      else if (b.dataset.a === "del" && confirm("¿Eliminar " + p[2] + " del inventario?")) {
        const r = await api({ action: "eliminar_producto", sheetName: SES.code, producto: p[2] });
        toast(r.status === "success" ? "Producto eliminado" : (r.message || "No se pudo eliminar"));
        await recargar();
      }
    }));
    $("inv-lista").appendChild(div);
  });
  if (!MOV_VER) { $("inv-movimientos").classList.add("oculto"); }
  else pintarMovimientos();
}
$("inv-buscar").addEventListener("input", pintarInventario);

function formProducto(idx) {
  const p = idx == null ? null : TODO.inventario[idx];
  openModal(`
    <h2>${p ? "Editar producto" : "Nuevo producto"}</h2>
    <input id="f-nombre" placeholder="Nombre *" value="${esc(p ? p[2] : "")}">
    <input id="f-cat" placeholder="Categoría" value="${esc(p ? p[3] : "")}">
    <div class="fila"><input id="f-precio" type="number" placeholder="Precio venta *" value="${p ? p[6] : ""}">
    <input id="f-costo" type="number" placeholder="Costo" value="${p ? p[5] : ""}"></div>
    <div class="fila"><input id="f-min" type="number" placeholder="Precio mínimo" value="${p ? p[7] : ""}">
    <input id="f-alerta" type="number" placeholder="Alerta stock" value="${p ? p[8] : ""}"></div>
    <div class="fila"><input id="f-stock" type="number" placeholder="Stock" value="${p ? p[4] : ""}">
    <input id="f-bar" placeholder="Código barras" value="${esc(p ? p[1] : "")}"></div>
    <p id="f-err" class="error"></p>
    <button class="btn exito" id="f-guardar">Guardar</button>
    <button class="btn link" id="f-cancelar">Cancelar</button>`);
  $("f-cancelar").addEventListener("click", closeModal);
  $("f-guardar").addEventListener("click", async () => {
    const nombre = $("f-nombre").value.trim();
    const precio = num($("f-precio").value);
    if (!nombre || precio <= 0) { $("f-err").textContent = "Nombre y precio son obligatorios"; return; }
    const fila = [p ? p[0] : "", $("f-bar").value.trim(), nombre, $("f-cat").value.trim() || "General",
      num($("f-stock").value), num($("f-costo").value), precio, num($("f-min").value),
      num($("f-alerta").value) || 5, "Activo", p ? p[10] : hoyLat(), hoyLat(), p ? p[12] : ""];
    if (p && p[2] !== nombre) {
      await api({ action: "eliminar_producto", sheetName: SES.code, producto: p[2] });
      fila[0] = "";
    }
    const r = await api({ action: "registrar_inventario", tableName: "Inventario", data: fila });
    if (r.status !== "success") { $("f-err").textContent = r.message || "No se pudo guardar"; return; }
    closeModal(); toast("Producto guardado");
    await recargar();
  });
}

// ---------- movimientos ----------
async function xlsxACsv(file) {
  if (!window.XLSX) await new Promise((res, rej) => {
    const s = document.createElement("script");
    s.src = "https://cdn.sheetjs.com/xlsx-0.20.3/package/dist/xlsx.full.min.js";
    s.onload = res; s.onerror = rej;
    document.head.appendChild(s);
  });
  const buf = await file.arrayBuffer();
  const wb = window.XLSX.read(buf, { type: "array" });
  return window.XLSX.utils.sheet_to_csv(wb.Sheets[wb.SheetNames[0]]);
}
function formMerma() {
  openModal(`<h2>Registrar pérdida</h2>
    <input id="m-prod" list="m-sug" placeholder="Producto *" autocomplete="off">
    <datalist id="m-sug">${invRows().map((p) => `<option value="${esc(p[2])}">`).join("")}</datalist>
    <div class="fila"><select id="m-tipo"><option>Dañado</option><option>Vencido</option><option>Ajuste</option><option>Merma</option><option>Consumo interno</option><option>Salida sin venta</option></select>
    <input id="m-cant" type="number" placeholder="Cant. *" inputmode="numeric"></div>
    <p id="m-err" class="error"></p>
    <button class="btn exito" id="m-guardar">Registrar</button>
    <button class="btn link" id="m-cancelar">Cancelar</button>`);
  $("m-cancelar").addEventListener("click", closeModal);
  $("m-guardar").addEventListener("click", async () => {
    const p = invRows().find((x) => (x[2] || "") === $("m-prod").value.trim());
    const c = Math.max(1, parseInt($("m-cant").value || "0", 10));
    if (!p) { $("m-err").textContent = "Elige un producto válido"; return; }
    const tipo = $("m-tipo").value;
    const ant = num(p[4]), nvo = Math.max(0, ant - c);
    await logMov(p, "Salida", c, ant, nvo, tipo + " (premium)");
    await actualizarStock(p, nvo);
    closeModal(); toast(tipo + " registrado");
    await recargar();
  });
}
function formConteo() {
  const rows = invRows();
  openModal(`<h2>Conteo físico</h2><small class="muted">Escribe lo contado. Vacío = sin cambio.</small>`
    + `<div style="max-height:40vh;overflow-y:auto;margin:8px 0">` + rows.map((p) => `<div class="fila"><small style="flex:2">${esc(p[2])} <b>(sist: ${p[4]})</b></small><input data-c="${esc(p[2])}" type="number" inputmode="numeric" placeholder="Físico" style="flex:1"></div>`).join("") + `</div>`
    + `<button class="btn exito" id="c-guardar">Aplicar conteo</button><button class="btn link" id="c-cancelar">Cancelar</button>`);
  $("c-cancelar").addEventListener("click", closeModal);
  $("c-guardar").addEventListener("click", async () => {
    let difs = 0, perdida = 0;
    for (const inp of document.querySelectorAll("[data-c]")) {
      if (String(inp.value).trim() === "") continue;
      const p = invRows().find((x) => (x[2] || "") === inp.dataset.c);
      if (!p) continue;
      const fis = Math.max(0, parseInt(inp.value || "0", 10)), ant = num(p[4]), dif = fis - ant;
      if (!dif) continue;
      difs++;
      if (dif < 0) perdida += -dif * costoDe(p[2]);
      await logMov(p, dif > 0 ? "Entrada" : "Salida", Math.abs(dif), ant, fis, "Conteo: diferencia " + (dif > 0 ? "+" : "") + dif);
      await actualizarStock(p, fis);
    }
    closeModal();
    toast(difs ? `Conteo aplicado. Faltantes: ${fmt(perdida)}` : "Sin diferencias");
    await recargar();
  });
}
function pintarMovimientos() {
  const box = $("inv-movimientos");
  box.classList.remove("oculto");
  const hoy = hoyLat();
  const match = (f) => {
    if (MOV_FILTRO === "Día") return String(f || "").slice(0, 10) === hoy;
    if (MOV_FILTRO === "Mes") return esMesActual(f);
    return enRango(f, MOV_DESDE, MOV_HASTA);
  };
  const rows = movRows().filter((m) => match(m[1]));
  box.innerHTML = `<h3>Movimientos de Inventario</h3>
    <div class="seg">${["Día", "Mes", "Rango"].map((f) => `<button data-f="${f}" class="${MOV_FILTRO === f ? "on" : ""}">${f}</button>`).join("")}</div>
    ${MOV_FILTRO === "Rango" ? `<div class="fila"><input id="mov-desde" placeholder="dd/mm/aaaa" value="${MOV_DESDE}"><input id="mov-hasta" placeholder="dd/mm/aaaa" value="${MOV_HASTA}"></div>` : ""}
    <button class="btn-mini" id="mov-print">🖨️ Imprimir / PDF</button>
    ${rows.length ? `<div class="card"><table class="tabla">
      <tr><th>Fecha</th><th>Tipo</th><th>Cant.</th><th>Producto</th></tr>
      ${rows.map((m) => `<tr><td>${esc(m[1])}</td><td>${esc(m[4])}</td><td>${m[4] === "Entrada" ? "+" : "−"}${m[5]}</td><td>${esc(m[3])}</td></tr>`).join("")}
    </table></div>` : '<div class="card">Sin movimientos en el periodo.</div>'}`;
  box.querySelectorAll("[data-f]").forEach((b) => b.addEventListener("click", () => { MOV_FILTRO = b.dataset.f; pintarMovimientos(); }));
  const d = $("mov-desde"), h = $("mov-hasta");
  if (d) d.addEventListener("change", () => { MOV_DESDE = d.value.trim(); pintarMovimientos(); });
  if (h) h.addEventListener("change", () => { MOV_HASTA = h.value.trim(); pintarMovimientos(); });
  $("mov-print").addEventListener("click", () => imprimir("Movimientos de Inventario",
    `<table class="tabla"><tr><th>Fecha</th><th>Tipo</th><th>Cant.</th><th>Producto</th><th>Usuario</th></tr>` +
    rows.map((m) => `<tr><td>${esc(m[1])}</td><td>${esc(m[4])}</td><td>${m[5]}</td><td>${esc(m[3])}</td><td>${esc(m[8] || "")}</td></tr>`).join("") + "</table>"));
}

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
const esBolirrana = (d) => /^bolirrana/i.test(d.nombre) || d.items.some((it) => /^bolirrana/i.test(it[8] || ""));
function clientesConocidos() {
  const mapa = new Map();
  const add = (n) => {
    n = (n || "").trim();
    if (!n || /^cliente mostrador$/i.test(n)) return;
    const k = n.toLowerCase();
    if (!mapa.has(k)) mapa.set(k, n);
  };
  try { deuRows().forEach((d) => add(d[1])); } catch {}
  try { venRows().forEach((v) => add(v[3])); } catch {}
  return [...mapa.values()].sort((a, b) => a.localeCompare(b, "es"));
}

function pintarDeudores() {
  const list = agruparDeudores();
  $("deu-detalle").classList.add("oculto");
  $("deu-lista").classList.remove("oculto");
  $("deu-lista").innerHTML = list.length ? "" : '<div class="card">No hay cuentas por cobrar.</div>';
  list.forEach((d) => {
    const div = document.createElement("div");
    div.className = "card fila-deu";
    div.innerHTML = `<div><b>${esc(d.nombre)}</b><small>${d.items.length} registro(s)</small></div><span class="deuda">${fmt(d.pendiente)}</span>`;
    div.addEventListener("click", () => verDeudor(d));
    $("deu-lista").appendChild(div);
  });
}

function origenDe(item) {
  const prod = item[2] || "";
  const i = prod.indexOf(" • Chico ");
  if ((item[8] || "").toLowerCase() === "bolirrana" && i > 0) {
    return { origen: prod.slice(0, i).trim(), detalle: prod.slice(i + 9).split(" • ").slice(-1)[0] };
  }
  return { origen: "", detalle: "" };
}

function verDeudor(d) {
  DEU_SEL = d; CHICO_SEL = null;
  $("deu-lista").classList.add("oculto");
  const det = $("deu-detalle");
  det.classList.remove("oculto");
  if (esBolirrana(d)) verDeudorBolirrana(det, d);
  else verDeudorNormal(det, d);
}

function historialDeudorHTML(items) {
  const rows = items.map((it) => {
    const total = num(it[7]), abon = num(it[5]) + num(it[6]), pend = Math.max(0, total - abon);
    const est = pend <= 0.5 ? ["Pagado", "activo"] : abon > 0.5 ? ["Abono parcial", "plan"] : ["Sin abono", "susp"];
    return { fecha: it[0] || "", prod: it[2] || "", total, abon, pend, est };
  });
  const tT = rows.reduce((a, r) => a + r.total, 0), tA = rows.reduce((a, r) => a + r.abon, 0);
  return `<h3>Historial de abonos y pagos</h3><div class="card"><small>Total: ${fmt(tT)} • Abonado: ${fmt(tA)} • Pendiente: ${fmt(Math.max(0, tT - tA))}</small></div>`
    + (rows.length ? rows.map((r) => `<div class="card"><b>${esc(r.prod)}</b> <span class="badge ${r.est[1]}">${r.est[0]}</span><br><small>${esc(r.fecha)} • Total ${fmt(r.total)} • Abonado ${fmt(r.abon)} • Pendiente ${fmt(r.pend)}</small></div>`).join("") : '<div class="card">Sin registros.</div>');
}

function verDeudorNormal(det, d) {
  det.innerHTML = `<button class="volver" id="deu-volver">← Deudores</button>
    <h2>${esc(d.nombre)} • ${fmt(d.pendiente)}</h2>` +
    d.items.map((it) => `<div class="card"><b>${esc(it[2])}</b><br><small>${esc(it[0] || "")} • Pendiente: ${fmt(Math.max(0, num(it[7]) - num(it[5]) - num(it[6])))}</small></div>`).join("") +
    historialDeudorHTML(d.items) +
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

function verDeudorBolirrana(det, d) {
  const porChico = {};
  d.items.forEach((it) => {
    const ch = parseInt(it[10] || "0", 10) || 0;
    (porChico[ch] = porChico[ch] || []).push(it);
  });
  const chicos = Object.keys(porChico).map(Number).sort((a, b) => a - b);
  det.innerHTML = `<button class="volver" id="deu-volver">← Deudores</button>
    <h2>${esc(d.nombre)} • ${fmt(d.pendiente)}</h2>
    <h3>Chicos / Rondas</h3>
    <div id="bol-chicos">` + (chicos.length ? "" : '<div class="card">Sin chicos pendientes.</div>') + `</div>
    <div id="bol-detalle"></div>`;
  $("deu-volver").addEventListener("click", pintarDeudores);
  const box = $("bol-chicos");
  chicos.forEach((ch) => {
    const items = porChico[ch];
    const sub = items.reduce((a, it) => a + Math.max(0, num(it[7]) - num(it[5]) - num(it[6])), 0);
    const perd = [...new Set(items.map((it) => (it[9] || "").trim()).filter(Boolean))];
    const titulo = ch === 0 ? "Pendiente sin número" : "Chico " + ch;
    const div = document.createElement("div");
    div.className = "card fila-deu";
    div.innerHTML = `<div><b>${titulo}</b>${perd.length ? `<small>Perdedor: ${esc(perd.join(", "))} (por trasladar)</small>` : ""}</div><span class="deuda">${fmt(sub)}</span>`;
    div.addEventListener("click", () => verChico(d, ch, perd));
    box.appendChild(div);
  });
}

function verChico(d, ch, perdMarcado) {
  CHICO_SEL = ch;
  const items = d.items.filter((it) => (parseInt(it[10] || "0", 10) || 0) === ch);
  const sub = items.reduce((a, it) => a + Math.max(0, num(it[7]) - num(it[5]) - num(it[6])), 0);
  const det = $("bol-detalle");
  const existentes = [...new Set(agruparDeudores().map((x) => x.nombre))];
  det.innerHTML = `<h3>${ch === 0 ? "Pendiente sin número" : "Chico " + ch} • ${fmt(sub)}</h3>` +
    items.map((it) => `<div class="card"><b>${esc(it[2])}</b><br><small>${esc(it[0] || "")}</small></div>`).join("") +
    historialDeudorHTML(items) +
    `<input id="bol-persona" list="bol-sug" placeholder="Escribe quién pierde (ej. ruben 5)" value="${esc(perdMarcado.length === 1 ? perdMarcado[0] : "")}">
     <datalist id="bol-sug">${existentes.map((n) => `<option value="${esc(n)}">`).join("")}</datalist>
     <button class="btn primario" id="bol-dividir">${"Asignar perdedor"}</button>
     <button class="btn link" id="bol-atras">← Volver a chicos</button>`;
  $("bol-atras").addEventListener("click", () => verDeudor(d));
  $("bol-dividir").addEventListener("click", async () => {
    const nombre = $("bol-persona").value.trim();
    if (!nombre) { toast("Escribe la persona"); return; }
    $("bol-dividir").disabled = true;
    try {
      const r = await api({ action: "dividir_chico", sheetName: SES.code, clienteNombre: d.nombre, chico: ch, partes: [nombre] });
      toast(r.status === "success" ? `Chico asignado a ${nombre}` : (r.message || "No se pudo asignar"));
      await recargar(); pintarDeudores();
    } catch { toast("Error de conexión"); }
    $("bol-dividir").disabled = false;
  });
}

// ---------- finanzas ----------
function pintarFinanzas() {
  const s = ME.sec;
  $("fin-pdf-btn").innerHTML = s.finPdf ? '<button class="btn primario" id="fin-print">📄 Exportar a PDF</button>' : "";
  if (s.finPdf) $("fin-print").addEventListener("click", imprimirFinanzas);
  const filtros = [["dia", "Día"], ["mes", "Mes"], ["rango", "Rango"]].filter(([k]) => s.finFiltros.includes(k));
  $("fin-filtros").innerHTML = filtros.map(([k, t]) => `<button data-f="${t}" class="${FIN_FILTRO === t ? "on" : ""}">${t}</button>`).join("");
  $("fin-filtros").querySelectorAll("button").forEach((b) => b.addEventListener("click", () => {
    FIN_FILTRO = b.dataset.f; pintarFinanzas();
  }));
  if (!filtros.some(([, t]) => t === FIN_FILTRO) && filtros.length) FIN_FILTRO = filtros[0][1];
  $("fin-rango").classList.toggle("oculto", FIN_FILTRO !== "Rango de fechas");
  const matchV = (f) => FIN_FILTRO === "Día" ? esHoy(f) : FIN_FILTRO === "Mes" ? esMesActual(f) : enRango(f, FIN_DESDE, FIN_HASTA);
  const ventas = s.finVentas ? venVivas().filter((v) => matchV(v[1])) : [];
  const gastos = s.finGastos ? gasRows().filter((g) => matchV(g[1])) : [];
  const totV = ventas.reduce((a, v) => a + num(v[12]), 0);
  const totG = gastos.reduce((a, g) => a + num(g[7]), 0);
  $("fin-kpis").innerHTML = kpi("Ventas Total", fmt(totV)) + kpi("Gastos Total", fmt(totG));
  $("fin-utilidad").innerHTML = `<div class="card utilidad"><small>Utilidad Neta (Neto)</small><b>${fmt(totV - totG)}</b></div>`;
  $("fin-ventas").innerHTML = s.finVentas ? `<h3>1. Ventas</h3>` + (ventas.length
    ? ventas.map((v) => `<div class="card"><b>${esc(v[5])}</b><br><small>${esc(v[1])} • ${v[6]} x ${fmt(v[7])} • Total: ${fmt(v[12])}</small></div>`).join("")
    : '<div class="card">Sin ventas en el periodo.</div>') : "";
  $("fin-gastos").innerHTML = s.finGastos ? `<h3>2. Gastos</h3>` +
    (s.finRegistrar ? '<button class="btn-mini verde" id="fin-nuevo-gasto">+ Registrar Gasto</button>' : "") +
    (gastos.length ? gastos.map((g) => `<div class="card"><b>${esc(g[4])}</b><br><small>${esc(g[1])} • ${fmt(g[7])}</small></div>`).join("")
    : '<div class="card">Sin gastos en el periodo.</div>') : "";
  if (s.finGastos && s.finRegistrar) $("fin-nuevo-gasto").addEventListener("click", abrirGasto);
  pintarFacturas();
}
$("fin-desde")?.addEventListener("change", () => {});
document.addEventListener("change", (e) => {
  if (e.target.id === "fin-desde") { FIN_DESDE = e.target.value.trim(); pintarFinanzas(); }
  if (e.target.id === "fin-hasta") { FIN_HASTA = e.target.value.trim(); pintarFinanzas(); }
  if (e.target.id === "venh-desde") { VENH_D = e.target.value.trim(); pintarHistorial(); }
  if (e.target.id === "venh-hasta") { VENH_H = e.target.value.trim(); pintarHistorial(); }
});

function abrirGasto() {
  openModal(`<h2>Registrar Gasto</h2>
    <input id="g-concepto" placeholder="Concepto *">
    <div class="fila"><input id="g-monto" type="number" placeholder="Monto *">
    <select id="g-cat"><option>Operativo</option><option>Administrativo</option><option>Recurrente</option><option>Regalo</option><option>Otro</option></select></div>
    <select id="g-metodo"><option value="Efectivo">Efectivo</option><option value="Transferencia">Transferencia</option></select>
    <p id="g-err" class="error"></p>
    <button class="btn exito" id="g-guardar">Guardar Gasto</button>
    <button class="btn link" id="g-cancelar">Cancelar</button>`);
  $("g-cancelar").addEventListener("click", closeModal);
  $("g-guardar").addEventListener("click", async () => {
    const concepto = $("g-concepto").value.trim(), monto = num($("g-monto").value);
    if (!concepto || monto <= 0) { $("g-err").textContent = "Concepto y monto son obligatorios"; return; }
    const r = await api({ action: "registrar_gasto", tableName: "Gastos",
      data: ["", hoyLat(), horaHM(), $("g-cat").value, concepto, "", "", monto, $("g-metodo").value, "", SES.nombre, "Activo", "", ""] });
    if (r.status !== "success") { $("g-err").textContent = r.message || "No se pudo guardar"; return; }
    closeModal(); toast("Gasto registrado");
    await recargar(); tab("finanzas");
  });
}

// ---------- dashboard premium ----------
function pintarDashboard() {
  const F = $("dash-filtros");
  if (!F) return;
  F.innerHTML = ["Día", "Semana", "Mes", "Año"].map((t) => `<button data-f="${t}" class="${DASH_F === t ? "on" : ""}">${t}</button>`).join("");
  F.querySelectorAll("button").forEach((b) => b.addEventListener("click", () => { DASH_F = b.dataset.f; pintarDashboard(); }));
  const r = rangoPeriodo(DASH_F, 0), rp = rangoPeriodo(DASH_F, 1);
  const V = venVivas().filter((v) => enPeriodo(v[1], r));
  const Vp = venVivas().filter((v) => enPeriodo(v[1], rp));
  const G = gasRows().filter((g) => enPeriodo(g[1], r));
  const Gp = gasRows().filter((g) => enPeriodo(g[1], rp));
  const totV = (a) => a.reduce((x, v) => x + num(v[12]), 0);
  const totG = (a) => a.reduce((x, g) => x + num(g[7]), 0);
  const tV = totV(V), tVp = totV(Vp), tG = totG(G), tGp = totG(Gp);
  const gs = gruposVentas().filter((g) => enPeriodo(g.fecha, r));
  const nV = gs.length, nVp = gruposVentas().filter((g) => enPeriodo(g.fecha, rp)).length;
  const ticket = nV ? tV / nV : 0, ticketp = nVp ? tVp / nVp : 0;
  const costo = V.reduce((a, v) => a + num(v[6]) * costoDe(v[5]), 0);
  const rent = tV - costo - tG;
  const pc = (a, b) => b > 0.5 ? Math.round((a - b) / b * 100) : (a > 0.5 ? 100 : 0);
  const fl = (p) => `<small>${p > 0 ? "▲ +" : p < 0 ? "▼ " : "• "}${p}% vs ant.</small>`;
  const porProd = {}, porCat = {};
  V.forEach((v) => {
    const p = v[5] || "";
    porProd[p] = porProd[p] || { cant: 0, total: 0 };
    porProd[p].cant += num(v[6]); porProd[p].total += num(v[12]);
    const c = catDe(p);
    porCat[c] = porCat[c] || { cant: 0, total: 0 };
    porCat[c].cant += num(v[6]); porCat[c].total += num(v[12]);
  });
  const top = (o) => Object.entries(o).sort((a, b) => b[1].total - a[1].total)[0];
  const tP = top(porProd), tC = top(porCat);
  const gAdm = G.filter((g) => String(g[3] || "").toLowerCase() !== "recurrente").reduce((a, g) => a + num(g[7]), 0);
  $("dash-kpis").innerHTML =
    kpi(`Ventas ${DASH_F} ${fl(pc(tV, tVp))}`, fmt(tV)) +
    kpi(`Gastos ${DASH_F} ${fl(pc(tG, tGp))}`, fmt(tG)) +
    kpi(`Ticket promedio ${fl(pc(ticket, ticketp))}`, fmt(ticket)) +
    kpi(`N° ventas ${fl(pc(nV, nVp))}`, nV);
  $("dash-rent").innerHTML = `<div class="card utilidad"><small>Rentabilidad estimada (ventas − costo − gastos)</small><b>${fmt(rent)}</b><br><small>Costo productos: ${fmt(costo)}</small></div>`;
  $("dash-tops").innerHTML = `<h3>Lo más vendido</h3>`
    + (tP ? `<div class="card"><b>${esc(tP[0])}</b><br><small>${tP[1].cant} und • ${fmt(tP[1].total)}</small></div>` : '<div class="card">Sin ventas en el periodo.</div>')
    + (tC ? `<div class="card"><b>Categoría: ${esc(tC[0])}</b><br><small>${tC[1].cant} und • ${fmt(tC[1].total)}</small></div>` : "")
    + `<h3>Gastos</h3><div class="card"><small>Administrativos: ${fmt(gAdm)} • Recurrentes: ${fmt(tG - gAdm)}</small><br><small>Ingresos vs gastos: ${fmt(tV)} vs ${fmt(tG)}</small></div>`;
  pintarDashHappy(V);
  pintarDashAlertas();
}
function pintarDashHappy(V) {
  const box = $("dash-happy");
  if (!box) return;
  if (!tieneFuncion("BAR3")) { box.innerHTML = ""; return; }
  const VHH = V.filter((v) => String(v[17] || "").toUpperCase().startsWith("HH"));
  const tHH = VHH.reduce((a, v) => a + num(v[12]), 0);
  const cantHH = VHH.reduce((a, v) => a + num(v[6]), 0);
  const cliHH = new Set(VHH.map((v) => (v[3] || "").trim()).filter((c) => c && !/^cliente mostrador$/i.test(c))).size;
  const cfg = cfgHappy();
  const diasN = ["L", "M", "X", "J", "V", "S", "D"];
  let h = `<h3>Happy Hours</h3>`;
  h += cfg.length ? cfg.map((x, i) => `<div class="card"><b>${esc(x.producto)}</b> <span class="badge plan">${x.tipo === "2x1" ? "2x1" : "-" + num(x.valor) + "%"}</span><br><small>${(x.dias || []).map((d) => diasN[d]).join(" ") || "Todos los días"} • ${esc(x.desde || "")}–${esc(x.hasta || "")}</small>${ME.admin ? `<br><button class="btn-mini" data-hh="${i}">Eliminar</button>` : ""}</div>`).join("") : '<div class="card">Sin promos activas.</div>';
  h += `<div class="card"><small>Happy Hour generó: ${fmt(tHH)} • ${cantHH} productos • ${cliHH} clientes</small></div>`;
  if (ME.admin) h += `<div class="card"><b>Nueva promo</b><select id="hh-prod">${invRows().map((p) => `<option>${esc(p[2])}</option>`).join("")}</select>`
    + `<div class="fila"><select id="hh-tipo"><option value="desc">% descuento</option><option value="2x1">2x1</option></select><input id="hh-valor" type="number" placeholder="%"></div>`
    + `<div class="fila"><input id="hh-desde" placeholder="17:00"><input id="hh-hasta" placeholder="19:00"></div>`
    + `<div id="hh-dias">${diasN.map((d, i) => `<label class="check"><input type="checkbox" data-d="${i}"${i < 5 ? " checked" : ""}> ${d}</label>`).join("")}</div>`
    + `<button class="btn-mini verde" id="hh-guardar">Guardar promo</button></div>`;
  box.innerHTML = h;
  box.querySelectorAll("[data-hh]").forEach((b) => b.addEventListener("click", async () => {
    const arr = cfgHappy();
    arr.splice(Number(b.dataset.hh), 1);
    const r = await api({ action: "guardar_config", sheetName: SES.code, parametro: "HAPPY_HOUR", valor: JSON.stringify(arr), descripcion: "Promos happy hour" });
    toast(r.status === "success" ? "Promo eliminada" : (r.message || "No se pudo eliminar"));
    await recargar();
  }));
  const hg = $("hh-guardar");
  if (hg) hg.addEventListener("click", async () => {
    const dias = [...box.querySelectorAll("#hh-dias input")].filter((c) => c.checked).map((c) => Number(c.dataset.d));
    const arr = cfgHappy();
    arr.push({ producto: $("hh-prod").value, tipo: $("hh-tipo").value, valor: num($("hh-valor").value), dias, desde: ($("hh-desde").value || "").trim() || "00:00", hasta: ($("hh-hasta").value || "").trim() || "23:59" });
    const r = await api({ action: "guardar_config", sheetName: SES.code, parametro: "HAPPY_HOUR", valor: JSON.stringify(arr), descripcion: "Promos happy hour" });
    if (r.status !== "success") { toast(r.message || "No se pudo guardar"); return; }
    toast("Promo guardada"); await recargar();
  });
}
function pintarDashAlertas() {
  const box = $("dash-alertas");
  if (!box) return;
  const out = [];
  invRows().filter((p) => num(p[8] || 0) > 0 && num(p[4]) <= num(p[8]) * 0.5).forEach((p) => out.push(["Stock crítico", `${p[2]}: quedan ${p[4]} und`]));
  const lim = new Date(); lim.setDate(lim.getDate() - 14);
  const p2 = (x) => String(x).padStart(2, "0");
  const limS = `${lim.getFullYear()}${p2(lim.getMonth() + 1)}${p2(lim.getDate())}`;
  const conVenta = new Set(venVivas().filter((v) => (normFecha(v[1]) || "") >= limS).map((v) => v[5]));
  invRows().filter((p) => num(p[4]) > 0 && !conVenta.has(p[2])).slice(0, 8).forEach((p) => out.push(["Sin movimiento", `${p[2]}: 14+ días sin venderse`]));
  const rM = rangoPeriodo("Mes", 0), rA = rangoPeriodo("Mes", 1);
  const tM = venVivas().filter((v) => enPeriodo(v[1], rM)).reduce((a, v) => a + num(v[12]), 0);
  const tA = venVivas().filter((v) => enPeriodo(v[1], rA)).reduce((a, v) => a + num(v[12]), 0);
  if (tA > 0.5 && tM < tA * 0.5) out.push(["Caída de ventas", `Este mes ${fmt(tM)} vs ${fmt(tA)} anterior`]);
  const plazo = parseInt(cfgValor("PLAZO_DIAS", "7"), 10) || 7;
  const limD = new Date(); limD.setDate(limD.getDate() - plazo);
  const limDS = `${limD.getFullYear()}${p2(limD.getMonth() + 1)}${p2(limD.getDate())}`;
  agruparDeudores().forEach((d) => {
    const vieja = d.items.some((it) => { const n = normFecha(String(it[0] || "").slice(0, 10)); return n && n < limDS; });
    if (vieja) out.push(["Deudor atrasado", `${d.nombre}: ${fmt(d.pendiente)} a +${plazo} días`]);
  });
  const tGM = gasRows().filter((g) => enPeriodo(g[1], rM)).reduce((a, g) => a + num(g[7]), 0);
  const avgG = [1, 2, 3].map((b) => gasRows().filter((g) => enPeriodo(g[1], rangoPeriodo("Mes", b))).reduce((a, g) => a + num(g[7]), 0)).reduce((a, x) => a + x, 0) / 3;
  if (avgG > 0.5 && tGM > avgG * 1.3) out.push(["Gasto sobre promedio", `Este mes ${fmt(tGM)} vs promedio ${fmt(avgG)}`]);
  movRows().filter((m) => String(m[9] || "").startsWith("Conteo: diferencia -")).slice(-5).forEach((m) => out.push(["Diferencia de inventario", `${m[3]}: ${m[9].replace("Conteo: diferencia ", "")}`]));
  const hoyT = venVivas().filter((v) => esHoy(v[1])).reduce((a, v) => a + num(v[12]), 0);
  const diaMes = new Date().getDate();
  if (tM > 0.5 && hoyT < (tM / diaMes) * 0.5) out.push(["Ventas bajo promedio", `Hoy ${fmt(hoyT)} vs promedio diario ${fmt(tM / diaMes)}`]);
  let h = `<h3>Alertas inteligentes</h3>`;
  if (ME.admin) h += `<div class="card"><small>Días de plazo deudor: </small><div class="fila"><input id="plazo-dias" type="number" value="${plazo}"><button class="btn-mini" id="plazo-guardar">Guardar</button></div></div>`;
  h += out.length ? out.map(([t, d]) => `<div class="card"><b>${esc(t)}</b><br><small>${esc(d)}</small></div>`).join("") : '<div class="card">Todo bajo control.</div>';
  box.innerHTML = h;
  const pg = $("plazo-guardar");
  if (pg) pg.addEventListener("click", async () => {
    const r = await api({ action: "guardar_config", sheetName: SES.code, parametro: "PLAZO_DIAS", valor: String(Math.max(1, parseInt($("plazo-dias").value || "7", 10))), descripcion: "Plazo deudor en días" });
    toast(r.status === "success" ? "Plazo guardado" : (r.message || "No se pudo guardar"));
    await recargar();
  });
}

// ---------- factura electrónica DIAN ----------
const CODE39 = { "0": "101001101101", "1": "110100001011", "2": "101100001011", "3": "110110000101", "4": "101001101011", "5": "110100110101", "6": "101100110101", "7": "101001011011", "8": "110100101101", "9": "101100101101", "A": "110101000011", "B": "101101000011", "C": "110110100001", "D": "101011000011", "E": "110101100001", "F": "101101100001", "G": "101010011011", "H": "110101001101", "I": "101101001101", "J": "101011001101", "K": "110101010001", "L": "101101010001", "M": "110110101000", "N": "101010110001", "O": "110101011000", "P": "101101011000", "Q": "101010111000", "R": "110101011100", "S": "101101011100", "T": "101011011100", "U": "110010101011", "V": "100110101011", "W": "110011010101", "X": "100101101011", "Y": "110010110101", "Z": "100110110101", "-": "100101011011", ".": "110010101101", " ": "100110101101", "*": "100101101101", "$": "100100100101", "/": "100100101001", "+": "100101001001", "%": "101001001001" };
function dibujarBarras() {
  document.querySelectorAll("canvas.bc39").forEach((cv) => {
    const txt = "*" + String(cv.dataset.code || "").toUpperCase() + "*";
    let patron = "";
    for (const ch of txt) patron += (CODE39[ch] || CODE39[" "]) + "0";
    const w = cv.width, h = cv.height, bw = w / patron.length;
    const ctx = cv.getContext("2d");
    if (!ctx) return;
    ctx.fillStyle = "#fff"; ctx.fillRect(0, 0, w, h);
    ctx.fillStyle = "#000";
    for (let i = 0; i < patron.length; i++) if (patron[i] === "1") ctx.fillRect(Math.floor(i * bw), 0, Math.ceil(bw), h);
  });
}
async function generarCUFE(txt) {
  try {
    if (window.crypto && crypto.subtle) {
      const d = await crypto.subtle.digest("SHA-384", new TextEncoder().encode(txt));
      return [...new Uint8Array(d)].map((b) => b.toString(16).padStart(2, "0")).join("");
    }
  } catch {}
  let h1 = 0x811c9dc5, h2 = 0x01000193, h3 = 0xdeadbeef, h4 = 0x41c6ce57;
  const s = String(txt);
  let out = "";
  for (let r = 0; r < 4; r++) {
    for (let i = 0; i < s.length; i++) { h1 = (h1 ^ (s.charCodeAt(i) + r)) * 16777619 >>> 0; h2 = (h2 + (h1 ^ (i + r * 7))) * 31 >>> 0; h3 = (h3 ^ h2) * 16777619 >>> 0; h4 = (h4 + h3 + i) * 131 >>> 0; }
    out += [h1, h2, h3, h4].map((x) => (x >>> 0).toString(16).padStart(8, "0")).join("");
  }
  return (out + out).slice(0, 96);
}
function fechaYMDHora() {
  const d = new Date(), p = (x) => String(x).padStart(2, "0");
  const f = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
  let h = d.getHours();
  const ap = h >= 12 ? "PM" : "AM";
  h = h % 12 || 12;
  return { fecha: f, hora: `${p(h)}:${p(d.getMinutes())}:${p(d.getSeconds())} ${ap}` };
}
function cliKey() { return "kapta_cli_" + ((SES && SES.code) || "pub"); }
function cliLeer() { try { return JSON.parse(localStorage.getItem(cliKey()) || "{}"); } catch { return {}; } }
function cliGuardar(o) { try { localStorage.setItem(cliKey(), JSON.stringify(o)); } catch {} }
async function emitirFactura(det, info) {
  if (!tieneFuncion("BAR4")) return null;
  try {
    const ts = fechaYMDHora();
    const mem = cliLeer()[info.cliente] || {};
    const mostrador = /^(cliente mostrador|bolirrana)/i.test(info.cliente);
    const total = det.reduce((a, d) => a + d.sub, 0);
    const fv = fvDe(info.folio);
    const f = {
      cod: fv, folio: info.folio, fecha: ts.fecha, hora: ts.hora,
      cliente: mostrador ? "Consumidor Fiscal" : info.cliente,
      nit: mem.nit || (mostrador ? "999999999" : ""), tel: mem.tel || "",
      items: det.map((d) => ({ prod: d.prod, cant: d.cant, pu: d.pu, sub: d.sub })),
      total, modo: info.modo, medio: info.medio || "Efectivo", usuario: SES.nombre,
      emision: ts.fecha + " " + ts.hora, nc: null, cufe: "",
    };
    f.cufe = await cufeDe(info.folio, f);
    return f;
  } catch { return null; }
}
async function crearNC(g) {
  if (g.estado !== "Anulado") {
    if (!confirm(`Crear nota crédito y anular la venta ${g.cod}? Se devolverá el stock.`)) return;
    if (!(await anularVenta(g, true))) return;
  } else if (!confirm(`Crear nota crédito de ${g.cod}?`)) return;
  const m = facMapLeer();
  m[g.cod] = Object.assign(m[g.cod] || { fv: fvDe(g.cod) }, { nc: { fecha: hoyLat(), hora: horaHM(), usuario: SES.nombre } });
  facMapGuardar(m);
  toast("Nota crédito creada"); pintarFacturas();
}
function facQR(f) {
  return ["FACTURA " + f.cod, "REF " + f.folio, f.fecha + " " + f.hora,
    "CLIENTE " + f.cliente + " CC " + (f.nit || ""), "TOTAL " + Math.round(f.total),
    "CUFE " + f.cufe].join("\n") + "\n" + f.items.map((it) => `${it.cant}x ${it.prod} ${Math.round(it.sub)}`).join("\n");
}
function facturaHTML(f) {
  const e = EMPRESA_FULL || {};
  const base = Math.round(f.total / 1.19 * 100) / 100, iva = Math.round((f.total - base) * 100) / 100;
  return `<div class="fac">`
    + `<div class="fac-c"><b>${esc(e.nombre || SES.negocio)}</b><br><small>Nit: ${esc(e.nit || "")}</small><br><small>${esc(e.direccion || "")}</small><br><small>Tels: ${esc(e.celular1 || "")}</small></div>`
    + `<div class="fac-c"><b>FACTURA ELECTRÓNICA DE VENTA</b><br><b>No. ${esc(f.cod)}</b>${f.nc ? ' • <span class="badge susp">NOTA CRÉDITO</span>' : ""}</div>`
    + `<canvas class="bc39" data-code="${esc(f.folio)}" width="300" height="56"></canvas>`
    + `<div class="fac-fila"><b>Fecha:</b> ${esc(f.fecha)}&nbsp;&nbsp;Hora:${esc(f.hora)}</div>`
    + `<div class="fac-fila"><b>Cliente:</b> ${esc(f.cliente)}<br>C.C. ${esc(f.nit || "999999999")}<br>Tel: ${esc(f.tel || "")}</div>`
    + `<div class="fac-fila"><b>Forma de Pago:</b> Contado<br><b>Medio de Pago:</b> ${esc(f.medio || "Efectivo")}<br><b>Vendedor:</b> Barra</div>`
    + `<table class="tabla"><tr><th>Cant</th><th>Detalle</th><th>Iva</th><th>Total</th></tr>`
    + f.items.map((it) => `<tr><td>${it.cant}</td><td>${esc(it.prod)}<br><small>Precio Unitario: ${fmt(it.pu)}</small></td><td>19</td><td>${fmt(it.sub)}</td></tr>`).join("")
    + `</table><div class="fac-tot">Total: ${fmtM(f.total)}</div><small>Cantidad ítems: ${f.items.length}</small>`
    + `<div class="fac-c"><b>DETALLES DE IMPUESTOS</b><br><small>IVA 19% — Base: ${fmt(base)} — Impuesto: ${fmt(iva)}</small></div>`
    + `<div class="fac-c"><small>${esc(cfgValor("EMP_CALIDAD", "Responsable de IVA"))}<br>RANGO ${esc(cfgValor("DIAN_RANGO", ""))}<br>AUTORIZACIÓN NUMERACIÓN DE FACTURACIÓN<br>No. ${esc(cfgValor("DIAN_AUT", ""))} VENCE ${esc(cfgValor("DIAN_VIG", ""))}</small></div>`
    + `<div class="fac-c"><small>Fabricante: KAPTA CORP.<br>Software KAPTA Contable 1.0<br>www.kaptacontable.com<br>Nit: 1012319596</small></div>`
    + `<img src="${qrURL(facQR(f))}" width="150" height="150" loading="lazy" onerror="this.outerHTML='<b>${esc(f.cod)}</b>'">`
    + `<div class="fac-c"><b>CUFE:</b><br><small class="fac-cufe">${esc(f.cufe)}</small></div>`
    + `<div class="fac-fila"><small>Emisión: ${esc(f.emision || (f.fecha + " " + f.hora))}<br>Expedición: ${esc(f.emision || (f.fecha + " " + f.hora))}</small></div>`
    + `</div>`;
}
function verFacturaDIAN(f) {
  openModal(`<h2>Factura ${esc(f.cod)}</h2>` + facturaHTML(f)
    + `<div class="fila"><input id="fd-nit" placeholder="NIT / C.C." value="${esc(f.nit || "")}"><input id="fd-tel" placeholder="Tel" value="${esc(f.tel || "")}"></div>`
    + `<button class="btn exito" id="fd-guardar">Guardar cliente</button>`
    + `<button class="btn exito" id="fd-print">🖨️ Imprimir / PDF</button>`
    + `<button class="btn link" id="fd-cerrar">Cerrar</button>`);
  dibujarBarras();
  $("fd-cerrar").addEventListener("click", closeModal);
  $("fd-print").addEventListener("click", () => { imprimir("Factura " + f.cod, facturaHTML(f)); dibujarBarras(); });
  $("fd-guardar").addEventListener("click", () => {
    const nit = ($("fd-nit").value || "").trim(), tel = ($("fd-tel").value || "").trim();
    const m = facMapLeer();
    m[f.folio] = Object.assign(m[f.folio] || { fv: f.cod }, { nit, tel });
    facMapGuardar(m);
    const mem = cliLeer(); mem[f.cliente] = { nit, tel }; cliGuardar(mem);
    f.nit = nit; f.tel = tel;
    toast("Cliente guardado"); verFacturaDIAN(f);
  });
}
function facMapLeer() { try { const o = JSON.parse(localStorage.getItem(facKey()) || "{}"); return o && typeof o === "object" && !Array.isArray(o) ? o : {}; } catch { return {}; } }
function facMapGuardar(o) { try { localStorage.setItem(facKey(), JSON.stringify(o)); } catch {} }
function migrarFacturas() {
  try {
    const raw = JSON.parse(localStorage.getItem(facKey()) || "null");
    if (Array.isArray(raw)) {
      const m = {};
      raw.forEach((f) => { if (f && f.folio) m[f.folio] = { fv: f.cod, cufe: f.cufe || "", nc: f.nc || null, nit: f.nit || "", tel: f.tel || "" }; });
      facMapGuardar(m);
    }
  } catch {}
}
function fvDe(folio) {
  const m = facMapLeer();
  if (!m[folio]) {
    let mx = 0;
    Object.values(m).forEach((x) => { const mt = /^FV-(\d+)$/.exec(x.fv || ""); if (mt) mx = Math.max(mx, parseInt(mt[1], 10)); });
    m[folio] = { fv: "FV-" + String(mx + 1).padStart(4, "0"), cufe: "", nc: null, nit: "", tel: "" };
    facMapGuardar(m);
  }
  return m[folio].fv;
}
async function cufeDe(folio, f) {
  const m = facMapLeer();
  if (m[folio] && m[folio].cufe) return m[folio].cufe;
  const cufe = await generarCUFE([f.cod, folio, f.fecha, f.hora, f.cliente, Math.round(f.total), SES.code].join("|"));
  const m2 = facMapLeer();
  m2[folio] = Object.assign(m2[folio] || { fv: f.cod }, { cufe });
  facMapGuardar(m2);
  return cufe;
}
function latAYMD(f, h) {
  let Y = f, H = h;
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(String(f || ""));
  if (m) Y = `${m[3]}-${m[2]}-${m[1]}`;
  const t = /^(\d{1,2}):(\d{2})/.exec(String(h || ""));
  if (t) { let hh = +t[1]; const ap = hh >= 12 ? "PM" : "AM"; hh = hh % 12 || 12; H = `${String(hh).padStart(2, "0")}:${t[2]}:00 ${ap}`; }
  return { fecha: Y, hora: H };
}
async function facturaDeGrupo(g) {
  const m = facMapLeer()[g.cod] || {};
  const mem = cliLeer()[g.cliente] || {};
  const mostrador = /^(cliente mostrador|bolirrana)/i.test(g.cliente);
  const yh = latAYMD(g.fecha, g.hora);
  const f = {
    cod: m.fv || fvDe(g.cod), folio: g.cod, fecha: yh.fecha, hora: yh.hora,
    cliente: mostrador ? "Consumidor Fiscal" : g.cliente,
    nit: m.nit || mem.nit || (mostrador ? "999999999" : ""), tel: m.tel || mem.tel || "",
    items: g.items, total: g.total, modo: g.modo, medio: g.medio || "Efectivo", usuario: g.usuario,
    emision: yh.fecha + " " + yh.hora, nc: m.nc || null, cufe: "",
  };
  f.cufe = await cufeDe(g.cod, f);
  return f;
}
async function verDocumento(g) {
  try {
    verFacturaDIAN(await facturaDeGrupo(g));
  } catch { verComprobante(g); }
}
async function exportarFacturas() {
  const list = gruposVentas();
  if (!list.length) { toast("Sin ventas"); return; }
  toast("Generando facturas...");
  const parts = [];
  for (const g of list) {
    try { parts.push(facturaHTML(await facturaDeGrupo(g)) + `<div class="salto-pag"></div>`); } catch {}
  }
  if (!parts.length) { toast("No se pudo generar"); return; }
  $("print-area").innerHTML = `<h2>${esc(SES.negocio)} — Facturas electrónicas</h2>` + parts.join("");
  dibujarBarras();
  window.print();
}
function plantKey() { return "kapta_plant_" + ((SES && SES.code) || "pub"); }
async function plantillaBytes() {
  try {
    const b64 = localStorage.getItem(plantKey());
    if (b64) {
      const bin = atob(b64.split(",")[1] || b64);
      const u8 = new Uint8Array(bin.length);
      for (let i = 0; i < bin.length; i++) u8[i] = bin.charCodeAt(i);
      return u8;
    }
  } catch {}
  return new Uint8Array(await (await fetch("plantilla_factura.xlsx")).arrayBuffer());
}
async function descargarPlantilla() {
  try {
    const u8 = await plantillaBytes();
    const a = document.createElement("a");
    a.href = URL.createObjectURL(new Blob([u8], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }));
    a.download = "plantilla_factura.xlsx";
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 2000);
  } catch { toast("No se pudo descargar"); }
}
function verFactura(f) { verFacturaDIAN(f); }

// ---------- facturación local premium ----------
function pintarFacturas() {
  const box = $("fac-lista");
  if (!box) return;
  const q = (($("fac-buscar") || {}).value || "").toLowerCase();
  const arr = gruposVentas().filter((g) => !q || g.cod.toLowerCase().includes(q) || (g.cliente || "").toLowerCase().includes(q));
  box.innerHTML = arr.length ? "" : '<div class="card">Sin ventas para facturar.</div>';
  arr.slice().reverse().forEach((g) => {
    const m = facMapLeer()[g.cod] || {};
    const fv = m.fv || fvDe(g.cod);
    const div = document.createElement("div");
    div.className = "card fila-deu";
    div.innerHTML = `<div><b>${esc(fv)}</b> ${m.nc ? '<span class="badge susp">NC</span>' : g.estado === "Anulado" ? '<span class="badge susp">Anulada</span>' : ""}<br><small>${esc(g.fecha)} ${esc(g.hora)} • ${esc(g.cliente)} • ${g.items.length} item(s)</small></div><div class="cant"><span class="monto">${fmt(g.total)}</span><button data-a="ver">Ver</button>${m.nc || g.estado === "Anulado" ? "" : '<button data-a="nc">NC</button>'}</div>`;
    div.querySelectorAll("button").forEach((b) => b.addEventListener("click", (e) => {
      e.stopPropagation();
      if (b.dataset.a === "ver") verDocumento(g);
      else crearNC(g);
    }));
    box.appendChild(div);
  });
}
if ($("fac-buscar")) $("fac-buscar").addEventListener("input", pintarFacturas);
if ($("venh-exp")) $("venh-exp").addEventListener("click", exportarFacturas);
if ($("venh-plant")) $("venh-plant").addEventListener("click", descargarPlantilla);
if ($("venh-plantup-btn")) $("venh-plantup-btn").addEventListener("click", () => $("venh-plantup").click());
if ($("venh-plantup")) $("venh-plantup").addEventListener("change", () => {
  const f = ($("venh-plantup").files || [])[0];
  if (!f) return;
  const rd = new FileReader();
  rd.onload = () => { try { localStorage.setItem(plantKey(), String(rd.result || "")); toast("Plantilla actualizada"); } catch { toast("Plantilla muy pesada"); } };
  rd.readAsDataURL(f);
});
if ($("fac-exp")) $("fac-exp").addEventListener("click", () => {
  const list = gruposVentas();
  if (!list.length) { toast("Sin facturas para exportar"); return; }
  const m = facMapLeer();
  const csv = "Factura,Folio,Fecha,Hora,Cliente,NIT,Total,Modo,NC\n" + list.map((g) => {
    const e = m[g.cod] || {};
    return [e.fv || "", g.cod, g.fecha, g.hora, `"${(g.cliente || "").replace(/"/g, "")}"`, e.nit || "", Math.round(g.total), g.modo, e.nc ? "SI" : ""].join(",");
  }).join("\n");
  const a = document.createElement("a");
  a.href = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
  a.download = "facturas_" + SES.code + ".csv";
  a.click();
  setTimeout(() => URL.revokeObjectURL(a.href), 2000);
});

function imprimirFinanzas() {
  const ventas = venVivas().filter((v) => FIN_FILTRO === "Día" ? esHoy(v[1]) : FIN_FILTRO === "Mes" ? esMesActual(v[1]) : enRango(v[1], FIN_DESDE, FIN_HASTA));
  const gastos = gasRows().filter((g) => FIN_FILTRO === "Día" ? esHoy(g[1]) : FIN_FILTRO === "Mes" ? esMesActual(g[1]) : enRango(g[1], FIN_DESDE, FIN_HASTA));
  imprimir(`Estado Financiero — ${SES.negocio} (${FIN_FILTRO})`,
    `<h3>Ventas (${ventas.length})</h3><table class="tabla"><tr><th>Fecha</th><th>Producto</th><th>Cant.</th><th>Total</th></tr>` +
    ventas.map((v) => `<tr><td>${esc(v[1])}</td><td>${esc(v[5])}</td><td>${v[6]}</td><td>${fmt(v[12])}</td></tr>`).join("") +
    `</table><h3>Gastos (${gastos.length})</h3><table class="tabla"><tr><th>Fecha</th><th>Concepto</th><th>Monto</th></tr>` +
    gastos.map((g) => `<tr><td>${esc(g[1])}</td><td>${esc(g[4])}</td><td>${fmt(g[7])}</td></tr>`).join("") + "</table>");
}
function imprimir(titulo, html) {
  $("print-area").innerHTML = `<h2>${esc(SES.negocio)} — ${titulo}</h2><p>${hoyLat()} ${horaHM()}</p>` + html;
  window.print();
}

// ---------- usuarios ----------
const ROLES_BASE = ["Administrador", "Cajero", "Mesero", "Barman", "Supervisor"];
// Catálogo de funciones por número (plan Básico 1-63 + exclusivas BAR).
// a = fragmento de permiso que otorga: r resumen, a acciones, al alertas,
// dv/di/df docks, td tab deudores, fr resumen financiero, rk ranking, vm ver más,
// vi ver inventario, fp pdf, ff filtros, fv ventas, fg gastos, frg registrar,
// ic carga, im movimientos, icr crear, ie editar, idel eliminar, ig guardar, ih hacer.
const FUNC_CATALOG = [
  { c: "1", n: "Seleccionar país", g: "Acceso", a: {} },
  { c: "2", n: "Identificador del negocio", g: "Acceso", a: {} },
  { c: "3", n: "Continuar", g: "Acceso", a: {} },
  { c: "4", n: "Ayuda empresa", g: "Acceso", a: {} },
  { c: "5", n: "Ingresar al POS", g: "Acceso", a: {} },
  { c: "6", n: "Recuérdame", g: "Acceso", a: {} },
  { c: "7", n: "Cambiar empresa", g: "Acceso", a: {} },
  { c: "8", n: "Cerrar sesión", g: "Acceso", a: {} },
  { c: "9", n: "YO / Mi cuenta", g: "Acceso", a: {} },
  { c: "10", n: "Campana de alertas", g: "Inicio", a: { al: 1 } },
  { c: "11", n: "Dock por rol", g: "Inicio", a: { dv: 1, di: 1, df: 1, td: 1 } },
  { c: "12", n: "Lupa de búsqueda", g: "Inicio", a: {} },
  { c: "13", n: "Ventas del día", g: "Inicio", a: { r: ["ventas"] } },
  { c: "14", n: "Gastos del mes", g: "Inicio", a: { r: ["gastos"] } },
  { c: "15", n: "Deudores (tarjeta)", g: "Inicio", a: { r: ["deudores"] } },
  { c: "16", n: "Clientes activos", g: "Inicio", a: { r: ["clientes"] } },
  { c: "17", n: "Redimensionar secciones", g: "Inicio", a: {} },
  { c: "18", n: "Cambiar modo (VR)", g: "Venta Rápida", a: { a: ["venta"], dv: 1 } },
  { c: "19", n: "Crear modo (VR)", g: "Venta Rápida", a: { a: ["venta"], dv: 1 } },
  { c: "20", n: "Buscar y agregar producto", g: "Venta Rápida", a: { a: ["venta"], dv: 1 } },
  { c: "21", n: "Sumar/restar cantidad", g: "Venta Rápida", a: { a: ["venta"], dv: 1 } },
  { c: "22", n: "Cliente (VR)", g: "Venta Rápida", a: { a: ["venta"], dv: 1 } },
  { c: "23", n: "Precio mínimo (VR)", g: "Venta Rápida", a: { a: ["venta"], dv: 1 } },
  { c: "24", n: "Redimensionar tarjetas", g: "Venta Rápida", a: { a: ["venta"], dv: 1 } },
  { c: "25", n: "Paga", g: "Venta Rápida", a: { a: ["venta"], dv: 1 } },
  { c: "26", n: "Debe", g: "Venta Rápida", a: { a: ["venta"], dv: 1 } },
  { c: "27", n: "Buscar (Agregar)", g: "Agregar", a: { a: ["agregar"] } },
  { c: "28", n: "Vista lista/recuadro", g: "Agregar", a: { a: ["agregar"] } },
  { c: "29", n: "Cantidad (Agregar)", g: "Agregar", a: { a: ["agregar"] } },
  { c: "30", n: "Agregar Stock masivo", g: "Agregar", a: { a: ["agregar"] } },
  { c: "31", n: "Listar deudores", g: "Deudores", a: { a: ["deudores"], td: 1 } },
  { c: "32", n: "Resumen por deudor", g: "Deudores", a: { a: ["deudores"], td: 1 } },
  { c: "33", n: "Historial por pedido", g: "Deudores", a: { a: ["deudores"], td: 1 } },
  { c: "34", n: "Pago total", g: "Deudores", a: { a: ["deudores"], td: 1 } },
  { c: "35", n: "Abono parcial", g: "Deudores", a: { a: ["deudores"], td: 1 } },
  { c: "36", n: "Efectivo/Transferencia", g: "Deudores", a: { a: ["deudores"], td: 1 } },
  { c: "37", n: "Tipo de gasto", g: "Gasto", a: { a: ["gasto"] } },
  { c: "38", n: "Concepto/Valor/Descripción", g: "Gasto", a: { a: ["gasto"] } },
  { c: "39", n: "Foto comprobante", g: "Gasto", a: { a: ["gasto"] } },
  { c: "40", n: "Registrar gasto", g: "Gasto", a: { a: ["gasto"] } },
  { c: "41", n: "Ver Bajo/Medio/Nulo", g: "Alertas", a: { al: 1 } },
  { c: "42", n: "Surtir (+)", g: "Alertas", a: { al: 1 } },
  { c: "43", n: "Perfil + clave", g: "Perfil", a: {} },
  { c: "44", n: "Tipo + mesa/chico", g: "Venta", a: { dv: 1 } },
  { c: "45", n: "Buscar/categorías", g: "Venta", a: { dv: 1 } },
  { c: "46", n: "Carrito", g: "Venta", a: { dv: 1 } },
  { c: "47", n: "Cobrar + QR", g: "Venta", a: { dv: 1 } },
  { c: "48", n: "Regalo de la casa", g: "Venta", a: {} },
  { c: "49", n: "Historial + anular", g: "Venta", a: { dv: 1 } },
  { c: "50", n: "Buscar/categorías (inv)", g: "Inventario", a: { di: 1 } },
  { c: "51", n: "Crear/editar/eliminar", g: "Inventario", a: { icr: 1, ie: 1, idel: 1 } },
  { c: "52", n: "Surtir stock", g: "Inventario", a: { ie: 1 } },
  { c: "53", n: "Movimientos", g: "Inventario", a: { im: 1 } },
  { c: "54", n: "Carga masiva", g: "Inventario", a: { ic: 1 } },
  { c: "55", n: "Nuevo deudor", g: "Deudores", a: { td: 1 } },
  { c: "56", n: "Detalle + abonos", g: "Deudores", a: { td: 1 } },
  { c: "57", n: "Registrar pago", g: "Deudores", a: { td: 1 } },
  { c: "58", n: "Chicos + perdedor", g: "Deudores", a: { td: 1 } },
  { c: "59", n: "Exportar PDF", g: "Finanzas", a: { fp: 1 } },
  { c: "60", n: "Filtros + KPIs", g: "Finanzas", a: { ff: ["dia", "mes", "rango"], fv: 1, fg: 1 } },
  { c: "61", n: "Registrar gasto (fin)", g: "Finanzas", a: { frg: 1 } },
  { c: "62", n: "Usuarios y roles", g: "Usuarios", a: {} },
  { c: "63", n: "Cuenta", g: "Cuenta", a: {} },
  { c: "BAR1", n: "Venta Bolirrana", g: "BAR", a: {} },
  { c: "BAR2", n: "Venta Dados", g: "BAR", a: {} },
  { c: "BAR3", n: "Happy Hours", g: "BAR", a: {} },
  { c: "BAR4", n: "Facturación", g: "BAR", a: {} },
  { c: "BAR5", n: "Merma/conteo", g: "BAR", a: {} },
];
const FUNC_MAP = Object.fromEntries(FUNC_CATALOG.map((f) => [f.c, f]));
const FUNC_GRUPOS = [...new Set(FUNC_CATALOG.map((f) => f.g))];
function secFromCodes(codes) {
  const s = { resumen: [], acciones: [], alertas: false, ventasResumen: [], ventasRanking: false, ventasVerMas: false, ventasVerInventario: false, finPdf: false, finFiltros: [], finVentas: false, finGastos: false, finRegistrar: false, invCarga: false, invMovimientos: false, invCrear: false, invEditar: false, invEliminar: false, invGuardar: false, invHacer: false, invLectura: true, _dockVentas: false, _dockFinanzas: false, _dockInventario: false, _tabDeudores: false };
  const union = (k, v) => v.forEach((x) => { if (!s[k].includes(x)) s[k].push(x); });
  codes.forEach((c) => {
    const e = FUNC_MAP[String(c).toUpperCase()];
    if (!e || !e.a) return;
    const a = e.a;
    if (a.r) union("resumen", a.r);
    if (a.a) union("acciones", a.a);
    if (a.ff) union("finFiltros", a.ff);
    if (a.al) s.alertas = true;
    if (a.dv) s._dockVentas = true;
    if (a.di) s._dockInventario = true;
    if (a.df) s._dockFinanzas = true;
    if (a.td) s._tabDeudores = true;
    const BOOLS = { rk: "ventasRanking", vm: "ventasVerMas", vi: "ventasVerInventario", fp: "finPdf", fv: "finVentas", fg: "finGastos", frg: "finRegistrar", ic: "invCarga", im: "invMovimientos", icr: "invCrear", ie: "invEditar", idel: "invEliminar", ig: "invGuardar", ih: "invHacer" };
    Object.keys(BOOLS).forEach((k) => { if (a[k]) s[BOOLS[k]] = true; });
  });
  if (!(s.invCarga || s.invMovimientos || s.invCrear || s.invEditar || s.invEliminar || s.invGuardar || s.invHacer)) s.invLectura = true;
  else s.invLectura = false;
  return s;
}
function tieneFuncion(cod) {
  if (!ME) return true;
  if (ME.admin) return true;
  return !!(ME.codes && ME.codes.has(String(cod).toUpperCase()));
}
const MODULOS = ["Reportes y Analytics", "Control de Turnos y Caja", "Facturación Electrónica DIAN", "Happy Hour & Promociones", "Venta por Mesa & Comandero", "División de Cuentas (Split)", "Agente IA Kapta Assistant"];

function pintarUsuarios() {
  if (!ME.admin) { $("t-usuarios").innerHTML = "<h2>Usuarios y Roles</h2><div class='card'>Solo administradores.</div>"; return; }
  const list = usuRows();
  $("usu-lista").innerHTML = list.length ? "" : '<div class="card">Sin usuarios registrados.</div>';
  list.forEach((u) => {
    const div = document.createElement("div");
    div.className = "card fila-deu";
    const act = String(u[5] || "Activo").toLowerCase() === "activo";
    div.innerHTML = `<div><b>${esc(u[1])}</b> <span class="badge ${act ? "activo" : "susp"}">${act ? "Activo" : "Inactivo"}</span><small>${esc(u[4] || "")} • ${esc(u[2] || "")}</small></div>
      <div class="cant"><button data-a="estado" title="${act ? "Desactivar" : "Activar"}">${act ? "⏸️" : "▶️"}</button><button data-a="edit">✏️</button><button data-a="del">🗑️</button></div>`;
    div.querySelectorAll("button").forEach((b) => b.addEventListener("click", (ev) => {
      ev.stopPropagation();
      if (b.dataset.a === "edit") formUsuario(u);
      else if (b.dataset.a === "estado") cambiarEstadoUsuario(u);
      else if (confirm(`¿Eliminar a ${u[1]}?`)) borrarUsuario(u[2]);
    }));
    $("usu-lista").appendChild(div);
  });
  pintarEquipo();
}
$("btn-nuevo-usuario").addEventListener("click", () => formUsuario(null));

async function borrarUsuario(correo) {
  const r = await api({ action: "eliminar_usuario", sheetName: SES.code, userEmail: correo });
  toast(r.status === "success" ? "Usuario eliminado" : (r.message || "No se pudo eliminar"));
  await recargar();
}

async function cambiarEstadoUsuario(u) {
  const act = String(u[5] || "Activo").toLowerCase() === "activo";
  if (act && (u[2] || "").toLowerCase() === (SES.correo || "").toLowerCase()) { toast("No puedes desactivar tu propia cuenta"); return; }
  const nuevo = act ? "Inactivo" : "Activo";
  if (!confirm(`¿${act ? "Desactivar" : "Activar"} a ${u[1]}?${act ? " No podrá ingresar." : ""}`)) return;
  const motivo = ((prompt(`Motivo del cambio a ${u[1]}:`, "") || "").trim());
  if (!motivo) { toast("Escribe el motivo del cambio"); return; }
  const r = await api({ action: "crear_usuario", tableName: "Usuarios",
    data: [u[0] || "", u[1] || "", u[2] || "", u[3] || "", u[4] || "Cajero", nuevo,
      u[6] || "", u[7] || "", u[8] || "", u[9] || "0", u[10] || "", u[11] || "",
      u[12] || "0", u[13] || "0", u[14] || "", u[15] || "", u[16] || "",
      u[17] || hoyLat(), u[18] || "", fmtFechaHora(new Date()), motivo, SES.uid || SES.correo, u[22] || ""] });
  toast(r.status === "success" ? `Usuario ${nuevo.toLowerCase()}` : (r.message || "No se pudo actualizar"));
  await recargar();
}

function fmtFechaHora(d) {
  const ME_ = ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"];
  const p = (x) => String(x).padStart(2, "0");
  return `${p(d.getDate())} ${ME_[d.getMonth()]} ${String(d.getFullYear()).slice(2)} - ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}
function codesFromSec(sec) {
  const out = [];
  FUNC_CATALOG.forEach((f) => {
    const a = f.a;
    if (!a || !Object.keys(a).length) { out.push(f.c); return; }
    const okR = !a.r || a.r.every((x) => (sec.resumen || []).includes(x));
    const okA = !a.a || a.a.every((x) => (sec.acciones || []).includes(x));
    const okF = !a.ff || a.ff.every((x) => (sec.finFiltros || []).includes(x));
    const M = { al: "alertas", dv: "_dockVentas", di: "_dockInventario", df: "_dockFinanzas", td: "_tabDeudores", rk: "ventasRanking", vm: "ventasVerMas", vi: "ventasVerInventario", fp: "finPdf", fv: "finVentas", fg: "finGastos", frg: "finRegistrar", ic: "invCarga", im: "invMovimientos", icr: "invCrear", ie: "invEditar", idel: "invEliminar", ig: "invGuardar", ih: "invHacer" };
    const okB = Object.keys(M).every((k) => !a[k] || sec[M[k]]);
    if (okR && okA && okF && okB) out.push(f.c);
  });
  return out;
}
function dockCapsFromCodes(codes) {
  const has = (c) => codes.has(c);
  const num = [...codes].some((c) => /^\d+$/.test(c));
  const ventas = num && ["18", "19", "20", "21", "22", "23", "24", "25", "26", "44", "45", "46", "47", "49"].some(has);
  const deu = ["31", "32", "33", "34", "35", "36", "55", "56", "57", "58"].some(has);
  const gasto = ["37", "38", "39", "40"].some(has);
  const inv = ["50", "51", "52", "53", "54"].some(has);
  const fin = ["59", "60", "61"].some(has);
  const dock = { Inicio: true, Ventas: ventas, Finanzas: fin, Inventario: inv };
  const caps = new Set();
  if (ventas) ["ventas", "deudores", "clientes"].forEach((c) => caps.add(c));
  if (deu) caps.add("deudores");
  if (fin) ["gastos", "reporte"].forEach((c) => caps.add(c));
  if (gasto) caps.add("gastos");
  if (inv) caps.add("inventario");
  if (has("BAR4")) caps.add("facturacion");
  return { dock, caps: [...caps] };
}
function presetCods(rol) {
  const all = FUNC_CATALOG.map((f) => f.c);
  if (/admin|supervisor/i.test(rol || "")) return all;
  if (/cajero/i.test(rol || "")) return all.filter((c) => !["48", "51", "52", "53", "54", "59", "62", "BAR5"].includes(c));
  return all.filter((c) => !["27", "28", "29", "30", "37", "38", "39", "40", "48", "51", "52", "53", "54", "59", "60", "61", "62", "BAR5"].includes(c));
}
const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
function formUsuario(u) {
  USU_EDIT = u;
  let dock = { Inicio: true, Ventas: true, Finanzas: true, Inventario: true };
  let mods = Object.fromEntries(MODULOS.map((m) => [m, true]));
  let rol = u ? (u[4] || "Cajero") : "Cajero";
  let codes = new Set();
  if (u) {
    const raw = u[22] || "";
    if (raw.trim().startsWith("{")) {
      try {
        const o = JSON.parse(raw);
        if (o && o.compact) String(o.compact).split("-").map((x) => x.trim().toUpperCase()).filter((x) => /^\d+$/.test(x) || /^BAR\d+$/.test(x)).forEach((c) => codes.add(c));
        else if (o && o.secciones) codes = new Set(codesFromSec(Object.assign(FULL(), o.secciones)));
        if (o && o.dock) dock = Object.assign(dock, o.dock);
        if (o && o.modulos) mods = Object.assign(mods, o.modulos);
      } catch { codes = new Set(presetCods(rol)); }
    } else {
      String(raw).split("-").map((x) => x.trim().toUpperCase()).filter((x) => /^\d+$/.test(x) || /^BAR\d+$/.test(x)).forEach((c) => codes.add(c));
    }
    if (!codes.size) codes = new Set(presetCods(rol));
  } else {
    codes = new Set(presetCods(rol));
  }
  const roles = [...ROLES_BASE];
  openModal(`<h2>${u ? "Editar usuario" : "Crear usuario"}</h2>
    <input id="u-nombre" placeholder="Nombre del empleado *" value="${esc(u ? u[1] : "")}">
    <input id="u-correo" placeholder="Correo *" value="${esc(u ? u[2] : "")}">
    <p id="u-err-correo" class="error"></p>
    <input id="u-pin" type="password" placeholder="${u ? "Nueva contraseña (vacío = no cambiar)" : "Contraseña *"}">
    <div id="u-reqs" class="muted" style="font-size:11px"></div>
    <input id="u-pin2" type="password" placeholder="Verificación de contraseña *">
    <p id="u-err-pin" class="error"></p>
    <div class="fila"><select id="u-rol">${roles.map((r) => `<option ${r === rol ? "selected" : ""}>${r}</option>`).join("")}</select>
    <button class="btn-mini" id="u-mas-rol">+ Rol</button></div>
    <h3>Funciones por número (separadas por -)</h3>
    <div id="u-cods">${FUNC_GRUPOS.map((g) => `<b style="font-size:12px">${esc(g)}</b>` + FUNC_CATALOG.filter((f) => f.g === g).map((f) => `<label class="check"><input type="checkbox" data-cod="${f.c}"${codes.has(f.c) ? " checked" : ""}> ${f.c} · ${esc(f.n)}</label>`).join("")).join("")}</div>
    <h3>Funciones avanzadas</h3>
    <div id="u-mods">${MODULOS.map((m) => `<label class="check"><input type="checkbox" data-mod="${esc(m)}" ${mods[m] ? "checked" : ""}> ${esc(m)}</label>`).join("")}</div>
    <p id="u-err" class="error"></p>
    <button class="btn exito" id="u-guardar">Guardar Usuario y Permisos</button>
    <button class="btn link" id="u-cancelar">Cancelar</button>`);

  const pintarCods = () => {
    document.querySelectorAll("#u-cods [data-cod]").forEach((c) => { c.checked = codes.has(c.dataset.cod); });
  };
  pintarCods();
  document.querySelectorAll("#u-cods [data-cod]").forEach((c) => c.addEventListener("change", () => {
    c.checked ? codes.add(c.dataset.cod) : codes.delete(c.dataset.cod);
  }));

  const checarPin = () => {
    const p = $("u-pin").value, p2 = $("u-pin2").value;
    const reqs = [["Mayúscula", /[A-Z]/], ["minúscula", /[a-z]/], ["número", /[0-9]/], ["especial", /[^A-Za-z0-9]/]]
      .map(([t, re]) => `${re.test(p) ? "✅" : "⬜"} ${t}`).join(" • ");
    $("u-reqs").innerHTML = p ? reqs : "";
    $("u-err-pin").textContent = p2 && p !== p2 ? "Las contraseñas no coinciden" : "";
  };
  $("u-pin").addEventListener("input", checarPin);
  $("u-pin2").addEventListener("input", checarPin);

  $("u-rol").addEventListener("change", (e) => {
    rol = e.target.value;
    codes = new Set(presetCods(rol));
    aplicarDefaultsRol(rol, dock, mods);
    document.querySelectorAll("[data-mod]").forEach((c) => { c.checked = !!mods[c.dataset.mod]; });
    pintarCods();
  });
  $("u-mas-rol").addEventListener("click", () => {
    const n = prompt("Nombre del rol personalizado:", "");
    if (n && n.trim()) {
      const sel = $("u-rol"), op = document.createElement("option");
      op.textContent = n.trim(); sel.appendChild(op); sel.value = n.trim(); rol = n.trim();
    }
  });
  $("u-cancelar").addEventListener("click", closeModal);
  $("u-guardar").addEventListener("click", async () => {
    const nombre = $("u-nombre").value.trim(), correo = $("u-correo").value.trim();
    const pin = $("u-pin").value, pin2 = $("u-pin2").value;
    if (!nombre) { $("u-err").textContent = "El nombre es obligatorio"; return; }
    if (!EMAIL_RE.test(correo)) { $("u-err").textContent = "El correo no es válido"; return; }
    const dup = usuRows().some((x) => (x[2] || "").toLowerCase() === correo.toLowerCase() && (!u || (x[2] || "").toLowerCase() !== (u[2] || "").toLowerCase()));
    if (dup) { $("u-err").textContent = "Este correo ya está registrado en otro usuario"; return; }
    if (!u || pin) {
      if (!(/[A-Z]/.test(pin) && /[a-z]/.test(pin) && /[0-9]/.test(pin) && /[^A-Za-z0-9]/.test(pin))) {
        $("u-err").textContent = "La contraseña debe tener mayúscula, minúscula, número y carácter especial"; return;
      }
      if (pin !== pin2) { $("u-err").textContent = "Las contraseñas no coinciden"; return; }
    }
    ["Inicio", "Ventas", "Finanzas", "Inventario"].forEach((v) => { dock[v] = true; });
    document.querySelectorAll("[data-mod]").forEach((c) => { mods[c.dataset.mod] = c.checked; });
    const sec = secFromCodes(codes);
    if (sec.invLectura) ["invCarga", "invMovimientos", "invCrear", "invEditar", "invEliminar", "invGuardar", "invHacer"].forEach((k) => (sec[k] = false));
    if (!codes.size && !confirm("Este usuario no tendrá ninguna función. ¿Guardar de todos modos?")) return;
    const dc = dockCapsFromCodes(codes);
    Object.assign(dock, dc.dock);
    const payload = { compact: [...codes].join(" - "), dock, functions: [], caps: dc.caps, modulos: mods, secciones: sec };
    const motivo = u ? ((prompt(`Motivo del cambio a ${nombre}:`, "") || "").trim()) : "Creación de usuario";
    if (u && !motivo) { toast("Escribe el motivo del cambio"); return; }
    const ahora = fmtFechaHora(new Date());
    const por = SES.uid || SES.correo;
    const r = await api({ action: "crear_usuario", tableName: "Usuarios",
      data: [u ? (u[0] || "") : "", nombre, correo, (!u || pin) ? pin : (u[3] || ""), rol, u ? (u[5] || "Activo") : "Activo",
        u ? (u[6] || "") : "", u ? (u[7] || "") : "", u ? (u[8] || "") : "", u ? (u[9] || "0") : "0",
        u ? (u[10] || "") : "", u ? (u[11] || "") : "", u ? (u[12] || "0") : "0", u ? (u[13] || "0") : "0",
        u ? (u[14] || "") : "", u ? (u[15] || "") : "", u ? (u[16] || "") : "",
        u ? (u[17] || hoyLat()) : hoyLat(), u ? (u[18] || "") : "",
        ahora, motivo, por, JSON.stringify(payload)] });
    if (r.status !== "success") { $("u-err").textContent = r.message || "No se pudo guardar"; return; }
    closeModal(); toast("Usuario guardado");
    await recargar();
  });
}

function aplicarDefaultsRol(rol, dock, mods) {
  const admin = /admin|supervisor/i.test(rol);
  Object.assign(dock, { Inicio: true, Ventas: true, Finanzas: admin, Inventario: true });
  Object.keys(mods).forEach((m) => {
    mods[m] = admin || ["Control de Turnos y Caja", "Facturación Electrónica DIAN", "Happy Hour & Promociones", "Venta por Mesa & Comandero", "División de Cuentas (Split)"].includes(m);
  });
}

// ---------- jornada y rendimiento del equipo (columnas del usuario) ----------
function miUsuario() {
  const c = (SES.correo || "").toLowerCase();
  return usuRows().find((u) => (u[2] || "").toLowerCase() === c) || null;
}
function fhParse(s) {
  const m = /^(\d{2}) ([A-Za-z]{3}) (\d{2}) - (\d{2}):(\d{2})(?::(\d{2}))?/.exec(String(s || ""));
  if (!m) return null;
  const ME_ = ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"];
  const mi = ME_.indexOf(m[2][0].toUpperCase() + m[2].slice(1).toLowerCase());
  if (mi < 0) return null;
  return new Date(2000 + Number(m[3]), mi, Number(m[1]), Number(m[4]), Number(m[5]), Number(m[6] || 0));
}
function fhHoy(s) {
  const d = fhParse(s);
  if (!d) return false;
  const n = new Date();
  return d.getFullYear() === n.getFullYear() && d.getMonth() === n.getMonth() && d.getDate() === n.getDate();
}
function fhHora(s) {
  const p = String(s || "").split(" - ");
  return p.length > 1 ? p[1].slice(0, 5) : "—";
}
function refNum(s, yyMon) {
  const m = String(s || "").match(new RegExp(yyMon.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + "\\s*\\(([^)]*)\\)"));
  return m ? (parseFloat(String(m[1]).replace(",", "")) || 0) : 0;
}
function refHoy(s) {
  const d = new Date(), ME_ = ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"];
  return refNum(s, String(d.getFullYear()).slice(2) + ME_[d.getMonth()]);
}
function sumaRefs(s) {
  let t = 0;
  String(s || "").replace(/\(([^)]*)\)/g, (_, v) => { t += parseFloat(String(v).replace(",", "")) || 0; return ""; });
  return t;
}
function pintarJornadaMia() {
  const box = $("jornada-mia");
  if (!box || !SES) return;
  const u = miUsuario();
  if (!u) { box.innerHTML = ""; return; }
  const dentro = fhHoy(u[6]) && (!fhHoy(u[7]) || fhParse(u[7]) < fhParse(u[6]));
  box.innerHTML = `<b>🕐 Mi jornada</b> <span class="badge ${dentro ? "activo" : "plan"}">${dentro ? "En turno" : "Fuera"}</span><br>`
    + `<small>Mes: ${refHoy(u[8]).toFixed(1)} h • Total: ${num(u[9]).toFixed(1)} h</small><br>`
    + `<small>Entrada ${fhHora(u[6])} • Salida ${fhHora(u[7])}</small><br>`
    + `<div class="fila"><button class="btn-mini${dentro ? "" : " verde"}" id="jor-in">Entrada</button><button class="btn-mini${dentro ? " verde" : ""}" id="jor-out">Salida</button></div>`;
  const marcar = async (tipo) => {
    const r = await api({ action: "registrar_jornada", sheetName: SES.code, tipo, usuario: SES.nombre });
    toast(r.status === "success" ? tipo + " registrada" : (r.message || "No se pudo registrar"));
    await recargar();
    if (!$("t-cuenta").classList.contains("oculto")) pintarCuenta();
  };
  $("jor-in").addEventListener("click", () => marcar("Entrada"));
  $("jor-out").addEventListener("click", () => marcar("Salida"));
}
function pintarEquipo() {
  const box = $("usu-equipo");
  if (!box || !ME.admin) { if (box) box.innerHTML = ""; return; }
  let h = `<h3>📈 Rendimiento del equipo</h3><button class="btn-mini" id="equipo-sync">Sincronizar estadísticas</button>`;
  usuRows().forEach((u) => {
    const dentro = fhHoy(u[6]) && (!fhHoy(u[7]) || fhParse(u[7]) < fhParse(u[6]));
    h += `<div class="card"><b>${esc(u[1])}</b> ${dentro ? '<span class="badge activo">En turno</span>' : ""} <span class="badge ${String(u[5] || "") === "Activo" ? "activo" : "susp"}">${esc(u[5] || "")}</span><br>`
      + `<small>Ventas mes: ${refHoy(u[10])} • Valor mes: ${fmt(refHoy(u[11]))} • Promedio: ${fmt(num(u[12]))}</small><br>`
      + `<small>Horas mes: ${refHoy(u[8]).toFixed(1)} • $/hora: ${fmt(num(u[13]))} • Productos: ${sumaRefs(u[14])}</small><br>`
      + `<small>Anulaciones mes: ${refHoy(u[15])} • Descuentos mes: ${fmt(refHoy(u[16]))}</small></div>`;
  });
  box.innerHTML = h || '<div class="card">Sin equipo.</div>';
  const sb = $("equipo-sync");
  if (sb) sb.addEventListener("click", async () => {
    for (const u of usuRows()) {
      await api({ action: "sincronizar_usuario", sheetName: SES.code, correo: u[2] || "" });
    }
    toast("Estadísticas sincronizadas");
    await recargar();
  });
}

// ---------- cuenta ----------
let EMPRESA_FULL = null;
async function cargarFicha() {
  try {
    const r = await api({ action: "ficha_negocio", sheetName: SES.code });
    if (r.status === "success" && r.data) EMPRESA_FULL = r.data.empresa || null;
  } catch {}
}
function pintarCuentaInfo() {
  $("cuenta-info").innerHTML = `<b>${esc(SES.nombre)}</b><br><small>${esc(SES.correo)} • ${esc(SES.rol)} • ${esc(SES.negocio)}</small>`;
}
function stopClave() { if (CLAVE_TIMER) { clearInterval(CLAVE_TIMER); CLAVE_TIMER = null; } }
function pintarCuenta() {
  pintarCuentaInfo();
  stopClave();
  pintarJornadaMia();
  pintarEnlaces();
  const box = $("cuenta-clave");
  if (!ME.admin) { box.innerHTML = ""; return; }
  box.innerHTML = `<div class="card"><b>🔑 Clave dinámica del admin</b><div class="monto" id="clave-valor">···</div>
    <small class="muted">Se renueva cada 60 segundos. Compártela solo en persona.</small><br>
    <button class="btn-mini" id="clave-actualizar">Actualizar</button></div>`;
  const cargar = async () => {
    try {
      const r = await api({ action: "obtener_clave_dinamica", empresa: SES.code, codigo: SES.code });
      const cod = ((r.data || {}).codigo || "").trim();
      if (cod) $("clave-valor").textContent = cod;
    } catch { /* reintenta en el ciclo */ }
  };
  $("clave-actualizar").addEventListener("click", cargar);
  cargar();
  CLAVE_TIMER = setInterval(() => { if (!$("t-cuenta").classList.contains("oculto")) cargar(); }, 30000);
  pintarDatosFact();
}
function pintarDatosFact() {
  const card = $("cuenta-fact");
  if (!card) return;
  card.classList.toggle("oculto", !ME.admin);
  if (!ME.admin) return;
  const e = EMPRESA_FULL || {};
  $("cf-nit").value = e.nit || "";
  $("cf-ciudad").value = e.ciudad || "";
  $("cf-dir").value = e.direccion || "";
  $("cf-tel2").value = e.celular2 || "";
  $("cf-calidad").value = cfgValor("EMP_CALIDAD", "Responsable de IVA");
  $("cf-aut").value = cfgValor("DIAN_AUT", "");
  $("cf-rango").value = cfgValor("DIAN_RANGO", "");
  $("cf-vig").value = cfgValor("DIAN_VIG", "");
}
if ($("btn-cf")) $("btn-cf").addEventListener("click", async () => {
  try {
    const r = await api({ action: "actualizar_empresa", codigo: SES.code, nit: $("cf-nit").value.trim(), direccion: $("cf-dir").value.trim(), ciudad: $("cf-ciudad").value.trim(), celular2: $("cf-tel2").value.trim() });
    if (r.status !== "success") { toast(r.message || "No se pudo guardar"); return; }
    for (const [k, id] of [["EMP_CALIDAD", "cf-calidad"], ["DIAN_AUT", "cf-aut"], ["DIAN_RANGO", "cf-rango"], ["DIAN_VIG", "cf-vig"]])
      await api({ action: "guardar_config", sheetName: SES.code, parametro: k, valor: $(id).value.trim() });
    toast("Datos guardados");
    await recargar();
  } catch { toast("Error de conexión"); }
});
$("btn-tel").addEventListener("click", async () => {
  const tel = $("cuenta-tel").value.trim();
  if (!tel) { toast("Escribe el número"); return; }
  const r = await api({ action: "actualizar_empresa", codigo: SES.code, celular1: tel });
  toast(r.status === "success" ? "Teléfono actualizado" : (r.message || "No se pudo actualizar"));
});

async function pintarEnlaces() {
  const box = $("enlaces-lista");
  if (!box || !SES || !ME || !ME.sec) return;
  const vistas = [["inicio", "Inicio"]];
  if (ME.sec._dockVentas) vistas.push(["venta", "Venta"]);
  if (ME.sec._dockInventario) vistas.push(["inventario", "Inventario"]);
  if (ME.admin) vistas.push(["usuarios", "Usuarios"]);
  if (ME.sec._dockFinanzas) vistas.push(["finanzas", "Finanzas"]);
  if (ME.admin || ME.sec._dockFinanzas) vistas.push(["dashboard", "Panel"]);
  if (ME.sec._tabDeudores) vistas.push(["deudores", "Deudores"]);
  if (!MI_TOKEN) {
    box.innerHTML = `<small class="muted">Generando...</small>`;
    try {
      const args = { action: "crear_enlace", sheetName: SES.code, correo: SES.correo };
      if (SES.super && SUPER) args.super = SUPER.token;
      const r = await api(args);
      if (r.status === "success" && r.data) MI_TOKEN = r.data.token;
    } catch {}
  }
  if (!MI_TOKEN) { box.innerHTML = `<small>No se pudo generar.</small>`; return; }
  const base = location.origin + location.pathname;
  box.innerHTML = vistas.map(([v, t]) => {
    const link = `${base}#/${SES.code}/${{ dashboard: "Panel" }[v] || t}/${MI_TOKEN}`;
    return `<div class="fila" style="justify-content:space-between"><small><b>${t}</b><br><span class="muted" style="word-break:break-all">${esc(link)}</span></small><button class="btn-mini" data-l="${encodeURIComponent(link)}">Copiar</button></div>`;
  }).join("");
  box.querySelectorAll("button").forEach((b) => b.addEventListener("click", async () => {
    const link = decodeURIComponent(b.dataset.l);
    try { await navigator.clipboard.writeText(link); toast("Enlace copiado"); }
    catch { prompt("Copia tu enlace:", link); }
  }));
}
// ---------- modal / print ----------
function openModal(html) {
  $("modal-card").innerHTML = html;
  $("modal").classList.remove("oculto");
}
function closeModal() { $("modal").classList.add("oculto"); $("modal-card").innerHTML = ""; }
$("modal").addEventListener("click", (e) => { if (e.target.id === "modal") closeModal(); });
function imprimir(titulo, html) {
  $("print-area").innerHTML = `<h2>${esc(SES.negocio)} — ${titulo}</h2><p>${hoyLat()} ${horaHM()}</p>` + html;
  window.print();
}

// ---------- arranque ----------
function rutaInicial() {
  try {
    const hs = String(location.hash || "").replace(/^#\/?/, "");
    const hp = hs.split("/").filter(Boolean);
    if (hp.length >= 3 && /^[a-z0-9-]+$/i.test(hp[0]) && !/^login$/i.test(hp[0]) && !/^aptadmin$/i.test(hp[0]))
      return { modo: "vista", code: hp[0].toUpperCase(), vista: normVista(hp[1]), token: hp.slice(2).join("/") };
    if (hp.length && /^aptadmin$/i.test(hp[0])) return { modo: "super" };
    if (hp.length && /^[a-z0-9-]+$/i.test(hp[0]) && !/^login$/i.test(hp[0])) return { modo: "negocio", code: hp[0].toUpperCase() };
    if (hp.length && /^login$/i.test(hp[0])) return { modo: "redireccion" };
    const h = String(location.hostname || "").toLowerCase();
    if (h === "aptadmin.kaptaia.app") return { modo: "super" };
    const m = h.match(/^([a-z0-9-]+)\.kaptaia\.app$/);
    if (m && m[1] !== "www" && m[1] !== "kapta") return { modo: "negocio", code: m[1].toUpperCase() };
  } catch {}
  return { modo: "redireccion" };
}
function normVista(v) {
  v = String(v || "").toLowerCase();
  if (v === "panel") return "dashboard";
  if (["inicio", "venta", "inventario", "usuarios", "finanzas", "dashboard", "deudores"].includes(v)) return v;
  return v;
}
function nombreVista(v) {
  return { dashboard: "Panel", inicio: "Inicio", venta: "Venta", inventario: "Inventario", usuarios: "Usuarios", finanzas: "Finanzas", deudores: "Deudores" }[v] || "Inicio";
}
let MODO_AISLADO = null;
let MI_TOKEN = null;
async function probarVista(R, vista) {
  try {
    const r = await api({ action: "validar_acceso", token: R.token, vista });
    if (r.status !== "success") return false;
    const u = r.data.usuario;
    SES = { code: R.code, negocio: R.code, correo: u.correo, nombre: u.nombre, rol: u.rol, uid: u.id };
    try {
      const e = await fetch(BASE + "?action=resolver_empresa&codigo=" + encodeURIComponent(R.code)).then((x) => x.json());
      const emp = (e.status === "success" && e.data && e.data.empresa) || null;
      if (emp) { SES.negocio = emp.nombre || R.code; EMPRESA = emp; aplicarIdentidad(emp); }
    } catch {}
    MODO_AISLADO = { code: R.code, vista, token: R.token };
    MI_TOKEN = R.token;
    $("pos-negocio").textContent = SES.negocio;
    $("pos-usuario").textContent = SES.nombre + " • " + SES.rol;
    ver("pos");
    try { await recargar(); } catch {
      toast("Sin conexión: revisa tu internet");
      ME = ME || { row: null, funciones: "", sec: null, codes: new Set() };
      if (!ME.sec) ME.sec = resolverSec();
    }
    document.getElementById("p-pos").classList.add("aislado");
    await cargarFicha();
    tab(vista);
    return true;
  } catch { return false; }
}
async function entrarAislado(R) {
  if (await probarVista(R, R.vista)) return;
  if (R.vista !== "inicio" && await probarVista(R, "inicio")) {
    location.hash = `#/${R.code}/Inicio/${R.token}`;
    return;
  }
  location.hash = `#/${R.code}/Login`;
}
async function entrarDirecto(code) {
  try {
    const r = await fetch(BASE + "?action=resolver_empresa&codigo=" + encodeURIComponent(code)).then((x) => x.json());
    const emp = (r.status === "success" && r.data && r.data.empresa) || null;
    if (!emp) { ver("negocio"); return; }
    SES = SES && SES.code === code ? SES : { code, negocio: emp.nombre || code };
    EMPRESA = emp;
    aplicarIdentidad(emp);
    $("login-nombre").textContent = SES.negocio;
    $("login-dominio").textContent = code.toLowerCase() + ".kaptaia.app";
    localStorage.setItem("kapta_code", code);
    if (SES.correo) { entrar(); return; }
    ver("login");
  } catch { ver("negocio"); }
}
(function init() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("sw.js").catch(() => {});
    // Fuerza la última versión del worker en cada arranque.
    navigator.serviceWorker.ready.then((r) => { try { r.update(); } catch { /* noop */ } }).catch(() => {});
  }
  pintarPaises();
  activarSplits(); aplicarLayout();
  try {
    const vv = document.getElementById("app-version");
    if (vv) vv.textContent = VERSION_PWA;
  } catch { /* noop */ }
  const tel = $("cuenta-tel");
  SES = cargarSesion();
  MI_TOKEN = null;
  const RUTA = rutaInicial();
  if (RUTA.modo === "vista") { entrarAislado(RUTA); }
  else if (RUTA.modo === "super") { ver("superlogin"); }
  else if (RUTA.modo === "negocio") {
    if (SES && SES.code && SES.code !== RUTA.code) { SES = null; localStorage.removeItem("kapta_pwa"); sessionStorage.removeItem("kapta_pwa"); }
    entrarDirecto(RUTA.code);
  }
  else {
  const sup = sessionStorage.getItem("kapta_super");
  if (sup) {
    const tok = sessionStorage.getItem("kapta_super_tok") || "";
    if (!tok) { ver("superlogin"); return; }
    SUPER = { correo: sup, token: tok };
    cargarNegocios();
  } else if (SES && SES.code) {
    $("login-nombre").textContent = SES.negocio || SES.code;
    fetch(BASE + "?action=resolver_empresa&codigo=" + encodeURIComponent(SES.code)).then((x) => x.json()).then((r) => {
      const emp = (r.status === "success" && r.data && r.data.empresa) || null;
      if (emp && (emp.codigo || "").toUpperCase() === SES.code) { EMPRESA = emp; aplicarIdentidad(emp); }
    }).catch(() => {}).finally(() => entrar());
  } else {
    const c = localStorage.getItem("kapta_code");
    if (c) $("in-codigo").value = c;
    pintarPaises();
    ver("negocio");
  }
  }
  $("in-codigo").addEventListener("change", () => localStorage.setItem("kapta_code", $("in-codigo").value.trim()));
  window.addEventListener("hashchange", () => location.reload());
  // precarga teléfono del negocio al abrir cuenta
  const obs = new MutationObserver(() => {
    if (!$("t-cuenta").classList.contains("oculto") && EMPRESA && !tel.value) tel.value = EMPRESA.celular1 || "";
  });
  obs.observe($("t-cuenta"), { attributes: true, attributeFilter: ["class"] });
})();
