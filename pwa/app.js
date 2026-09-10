/* Kapta IA POS — PWA v2 paridad Android. Vanilla JS contra backend Railway. */
const VERSION_PWA = "PWA-2026-09-10";
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
let VENTA_CAT = "Todos";
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
  if (sec._tabDeudores) tabs.push(["deudores", "Deudores"]);
  $("dock").innerHTML = "";
  tabs.forEach(([k, txt], i) => {
    const b = document.createElement("button");
    b.dataset.tab = k; if (!i) b.classList.add("on");
    b.title = txt;
    b.innerHTML = `<img src="img/pos/dock/${DOCK_ICONS[k]}?v=1" alt="${txt}">`;
    b.addEventListener("click", () => { if (k === "inicio") vrCerrar(); tab(k); });
    $("dock").appendChild(b);
  });
}
const DOCK_ICONS = { inicio: "Inicio.png", venta: "Venta.png", inventario: "Inventario.png", usuarios: "Admin.png", finanzas: "Finanzas.png", deudores: "Deudores.png" };
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
  if (AG_ABIERTO) agRender(); else pintarAlertas();
}));
// ---------- agregar stock (dock sobre alerta de stock, vistas lista/recuadro) ----------
let AG_VISTA = "lista", AG_ABIERTO = false, AG_SEL = {}, AG_Q = "";
const agAbierta = () => AG_ABIERTO;
function agTitulo() {
  const h = document.querySelector(".alerta-head .sec-t");
  if (h) h.textContent = AG_ABIERTO ? "Agregar Stock y Mercancia" : "Alerta de Stock";
}
function agAbrir() {
  AG_ABIERTO = true; AG_Q = "";
  agTitulo(); agRender();
}
function agCerrar() {
  if (!AG_ABIERTO) return;
  AG_ABIERTO = false; AG_SEL = {}; AG_Q = "";
  agTitulo(); pintarAlertas();
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
  const modos = vrModos();
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
    const pu = usaMin ? num(p[7]) : num(p[6]);
    const card = document.createElement("div");
    card.className = "vrcard";
    card.innerHTML = `${it.qty > 0 ? `<span class="vrbadge">${it.qty}</span>` : ""}`
      + `<span class="vrfoto">${p[12] ? `<img src="${esc(p[12])}" alt="" loading="lazy">` : ""}</span>`
      + `<b>${esc(p[2])}</b>`
      + `<span class="vrprecio-row"><span class="vrprecio${usaMin ? " min" : ""}">$ ${miles(pu)} c/u</span>`
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
  try {
    for (const i of ids) {
      const p = TODO.inventario[Number(i)], it = CARRITO[i], q = it.qty;
      const pu = precioEfectivo(p, it.min), sub = q * pu;
      const ant = num(p[4]), nvo = Math.max(0, ant - q);
      if (fiabl || esBol) {
        await api({ action: "registrar_deudor", tableName: "Deudores",
          data: [fechaHora(), cliente, p[2], q, it.min ? "SI" : "", 0, 0, sub, esBol ? `Bolirrana(${mesa})` : modo, "", chico] });
        await logMov(p, "Salida", q, ant, nvo, "Descuento por deudor");
      } else {
        await api({ action: "registrar_venta", tableName: "Ventas",
          data: ["", hoyISO(), horaHM(), cliente, p[0], p[2], q, pu, sub, "", 0, sub, sub, SES.nombre, "Activo", "", "", "", "", "", "", modo] });
        await logMov(p, "Salida", q, ant, nvo, "Descuento por venta");
      }
      await actualizarStock(p, nvo);
    }
    if (esBol) chicoSiguiente(mesa, chico);
    CARRITO = {}; $("vr-cliente").value = ""; $("vr-cliente-x").classList.add("oculto");
    toast("Registrado");
    await recargar();
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

// ---------- escala proporcional (901-1600px se ve tal cual el diseño 1600) ----------
const DIS_W = 1600;
function zFactor() {
  try { const z = parseFloat(($("p-pos").style || {}).zoom); return z > 0 && z < 5 ? z : 1; } catch { return 1; }
}
function ajusteEscala() {
  try {
    const p = $("p-pos");
    if (!p) return;
    const w = window.innerWidth || 0;
    p.style.zoom = (w > 900 && w < DIS_W) ? String(w / DIS_W) : "";
  } catch {}
}
window.addEventListener("resize", ajusteEscala);
window.addEventListener("orientationchange", ajusteEscala);
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
    const s = $("split-v"), k = zFactor();
    s._x = e.clientX; s._w = $("col-izq").getBoundingClientRect().width / k; s._tw = ti.getBoundingClientRect().width / k;
  }, (e) => {
    const s = $("split-v"), k = zFactor();
    const w = Math.min(Math.max(s._w + (e.clientX - s._x) / k, 300), s._tw - 300);
    ti.style.gridTemplateColumns = `minmax(0,${Math.round(w)}px) 10px minmax(0,1fr)`;
    laySet("col", Math.round(w));
  }, () => { laySet("col", null); ti.style.gridTemplateColumns = ""; });
  dragSplit($("split-hl"), (e) => {
    const s = $("split-hl"), k = zFactor();
    s._y = e.clientY; s._h = $("resumen").getBoundingClientRect().height / k;
    s._min = 180; s._max = Math.max(200, $("col-izq").getBoundingClientRect().height / k - 170);
  }, (e) => {
    const s = $("split-hl"), r = $("resumen"), k = zFactor();
    const h = Math.min(Math.max(s._h + (e.clientY - s._y) / k, s._min), s._max);
    r.style.flex = "0 0 " + Math.round(h) + "px"; r.style.overflow = "hidden";
    laySet("res", Math.round(h));
  }, () => { laySet("res", null); const r = $("resumen"); r.style.flex = ""; r.style.overflow = ""; });
  dragSplit($("split-ha"), (e) => {
    const s = $("split-ha"), k = zFactor();
    s._y = e.clientY; s._h = $("bloque-acciones").getBoundingClientRect().height / k;
    s._min = 120; s._max = Math.max(140, $("col-der").getBoundingClientRect().height / k - 220);
  }, (e) => {
    const s = $("split-ha"), b = $("bloque-acciones"), k = zFactor();
    const h = Math.min(Math.max(s._h + (e.clientY - s._y) / k, s._min), s._max);
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
    const w = Math.min(Math.max(gr._w + (e.clientX - gr._x) / zFactor(), 120), 300);
    $("vr-grid").style.setProperty("--vrw", Math.round(w) + "px");
    laySet("vrw", Math.round(w));
  }, () => { laySet("vrw", null); $("vr-grid").style.removeProperty("--vrw"); });
}
// ---------- permisos (mismo esquema JSON que Android) ----------
const FULL = () => ({ resumen: ["ventas", "gastos", "deudores", "clientes"], acciones: ["venta", "gasto", "agregar", "deudores"], alertas: true, ventasResumen: ["hoy", "semana", "mes"], ventasRanking: true, ventasVerMas: true, ventasVerInventario: true, finPdf: true, finFiltros: ["dia", "mes", "rango"], finVentas: true, finGastos: true, finRegistrar: true, invCarga: true, invMovimientos: true, invCrear: true, invEditar: true, invEliminar: true, invGuardar: true, invHacer: true, invLectura: false, _dockVentas: true, _dockFinanzas: true, _dockInventario: true, _tabDeudores: true });
const esCajeroLike = (rol) => /cajero|empleado|mesero|barman/i.test(rol || "") && !/admin|supervisor/i.test(rol || "");

function resolverSec() {
  const admin = /admin|supervisor/i.test(SES.rol || "");
  if (admin) { ME.admin = true; return FULL(); }
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
    return s;
  }
  if (obj && obj.dock) {
    const d = obj.dock, inv = d.Inventario === true && !esCajeroLike(SES.rol);
    return Object.assign(FULL(), {
      _dockVentas: d.Ventas === true, _dockFinanzas: d.Finanzas === true, _dockInventario: d.Inventario === true,
      _tabDeudores: true,
      invCarga: inv, invMovimientos: inv, invCrear: inv, invEditar: inv, invEliminar: inv, invGuardar: inv, invHacer: inv,
      invLectura: esCajeroLike(SES.rol),
    });
  }
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
    $("login-dominio").textContent = code.toLowerCase() + ".kaptaia.com";
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
const ventasMes = (ven) => fmt(ven.filter((v) => esMesActual(v[1])).reduce((a, v) => a + num(v[12]), 0));

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
  SES = { code: (emp.codigo || "").toUpperCase(), negocio: emp.nombre || emp.codigo, correo: emp.correo || SUPER.correo, nombre: "SuperAdmin", rol: "Administrador", super: true };
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
  activarSplits(); aplicarLayout(); ajusteEscala();
  try {
    const cv = document.getElementById("cuenta-version");
    if (cv) cv.textContent = VERSION_PWA;
  } catch { /* noop */ }
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
$("btn-salir").addEventListener("click", () => {
  if (SES && SES.super && SUPER) {
    SES = null; ME = null;
    cargarNegocios();
    return;
  }
  SES = null; ME = null;
  localStorage.removeItem("kapta_pwa"); sessionStorage.removeItem("kapta_pwa");
  aplicarIdentidad(null);
  ver("negocio");
});

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
    ME = { row: urow || null, funciones: urow ? (urow[11] || "") : "", sec: null };
    ME.sec = resolverSec();
    if (urow && urow[1]) SES.nombre = urow[1];
    if (urow && urow[4]) SES.rol = urow[4];
    $("pos-usuario").textContent = SES.nombre + " • " + SES.rol;
    pintarCuentaInfo();
    pintarResumen(); pintarVenta(); pintarInventario(); pintarDeudores(); pintarFinanzas(); pintarUsuarios();
    refrescarChrome();
    if (vrAbierta()) vrRender();
  } catch { toast("Sin conexión"); }
}
if ($("btn-recargar")) $("btn-recargar").addEventListener("click", recargar);

const invRows = () => (TODO.inventario || []).filter((x) => x[2] && x[2] !== "Nom_Producto");
const venRows = () => (TODO.ventas || []).filter((x) => x[5] && x[5] !== "Producto");
const deuRows = () => (TODO.deudores || []).filter((x) => x[1] && x[1] !== "Nom_Cliente");
const gasRows = () => (TODO.gastos || []).filter((x) => x[0] && String(x[0]).startsWith("G-"));
const movRows = () => (TODO.movimientos || []).filter((x) => x[0] && String(x[0]).startsWith("M-"));
const usuRows = () => (TODO.usuarios || []).filter((x) => x[2] && x[2] !== "Correo");
const kpi = (t, v) => `<div class="kpi"><small>${t}</small><b>${v}</b></div>`;

// ---------- inicio ----------
function pintarResumen() {
  const s = ME.sec;
  const totVentasHoy = venRows().filter((v) => esHoy(v[1])).reduce((a, v) => a + num(v[12]), 0);
  const totGastosMes = gasRows().filter((g) => esMesActual(g[1])).reduce((a, g) => a + num(g[7]), 0);
  const deud = agruparDeudores();
  const totDeuda = deud.reduce((a, x) => a + x.pendiente, 0);
  const cliAct = (() => {
    const set = new Set(deud.map((d) => d.nombre));
    venRows().filter((v) => esHoy(v[1])).forEach((v) => {
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
      if (k === "gasto") { agCerrar(); tab("finanzas"); setTimeout(() => abrirGasto(), 100); return; }
      agCerrar();
      tab(go);
    });
    $("acciones").appendChild(b);
  });
  $("bloque-alertas").classList.toggle("oculto", !s.alertas);
  if (s.alertas) {
    if (AG_ABIERTO) { agTitulo(); agRender(); }
    else { agTitulo(); pintarAlertas(); }
  } else {
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
      return `<div class="acard"><span class="athumb">${img}</span><span class="ainfo"><b>${esc(p[2])}</b><small>Quedan ${p[4]} und</small></span><span class="abadge ${n === 2 ? "abajo" : "amedio"}">${n === 2 ? "Stock Bajo" : "Stock Medio"}</span><button class="aplus" data-i="${idx}" title="Agregar stock"><img src="img/pos/alerta/agregar2.png?v=1" alt="+"></button></div>`;
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
  const modo = $("venta-modo").value;
  const esBol = modo === "Bolirrana";
  $("venta-mesa").classList.toggle("oculto", !esBol);
  const mesa = Number($("venta-mesa").value || 1);
  if (esBol) {
    $("venta-chico-info").classList.remove("oculto");
    $("venta-chico-info").textContent = `Bolirrana ${mesa} • próximo chico: ${chicoActivo(mesa)}`;
  } else $("venta-chico-info").classList.add("oculto");
  const q = ($("venta-buscar").value || "").toLowerCase();
  const cats = ["Todos", ...new Set(invRows().map((p) => (p[3] || "General").trim()).filter(Boolean))];
  $("venta-cats").innerHTML = "";
  cats.forEach((c) => {
    const b = document.createElement("button");
    b.textContent = c;
    b.classList.toggle("on", (VENTA_CAT || "Todos") === c);
    b.addEventListener("click", () => { VENTA_CAT = c; pintarVenta(); });
    $("venta-cats").appendChild(b);
  });
  const list = invRows().filter((p) =>
    (!q || p[2].toLowerCase().includes(q)) &&
    ((VENTA_CAT || "Todos") === "Todos" || (p[3] || "General").trim() === VENTA_CAT));
  $("venta-productos").innerHTML = list.length ? "" : '<div class="card">Sin productos.</div>';
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
  pintarCarrito();
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
    const pu = precioEfectivo(p, it.min), sub = it.qty * pu;
    total += sub;
    const tieneMin = num(p[7]) > 0;
    html += `<div class="card"><div class="fila-prod"><div><b>${esc(p[2])}</b><small>${it.qty} x ${fmt(pu)}${it.min ? " (mínimo)" : ""}</small></div>
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
  const esBol = $("venta-modo").value === "Bolirrana";
  const mesa = Number($("venta-mesa").value || 1);
  const chico = esBol ? chicoActivo(mesa) : 0;
  const cliente = esBol ? `Bolirrana ${mesa}` : ($("venta-cliente").value.trim() || "Cliente Mostrador");
  const metodo = $("venta-metodo").value;
  const fiado = $("venta-fiado").checked;
  const esTransf = metodo === "Transferencia";
  $("btn-cobrar").disabled = true;
  try {
    for (const i of ids) {
      const p = TODO.inventario[Number(i)], it = CARRITO[i], q = it.qty;
      const pu = precioEfectivo(p, it.min), sub = q * pu;
      const ant = num(p[4]), nvo = Math.max(0, ant - q);
      if (fiado || esBol) {
        await api({ action: "registrar_deudor", tableName: "Deudores",
          data: [fechaHora(), cliente, p[2], q, it.min ? "SI" : "", 0, 0, sub, esBol ? `Bolirrana(${mesa})` : "Normal", "", chico] });
        await logMov(p, "Salida", q, ant, nvo, "Descuento por deudor");
      } else {
        await api({ action: "registrar_venta", tableName: "Ventas",
          data: ["", hoyISO(), horaHM(), cliente, p[0], p[2], q, pu, sub, "", esTransf ? sub : 0, esTransf ? 0 : sub, sub, SES.nombre, "Activo", "", "", "", "", "", "", "Normal"] });
        await logMov(p, "Salida", q, ant, nvo, "Descuento por venta");
      }
      await actualizarStock(p, nvo);
    }
    if (esBol) chicoSiguiente(mesa, chico);
    CARRITO = {}; $("venta-cliente").value = ""; $("venta-fiado").checked = false;
    toast(esBol ? `Chico ${chico} de Bolirrana ${mesa} registrado` : fiado ? "Fiado registrado" : "Venta registrada");
    await recargar(); tab("inicio");
  } catch { toast("Error de conexión"); }
  $("btn-cobrar").disabled = false;
});

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
    inp.type = "file"; inp.accept = ".csv,.txt"; inp.style.display = "none";
    inp.addEventListener("change", async () => {
      if (!inp.files.length) return;
      const texto = await inp.files[0].text();
      const r = await api({ action: "importar_inventario", sheetName: SES.code, csv: texto });
      toast(r.status === "success" ? `Importados ${((r.data || {}).insertados || 0)}` : (r.message || "Error al importar"));
      await recargar();
    });
    lab.appendChild(inp); $("inv-botones").appendChild(lab);
  }
  if (s.invMovimientos) addBtn("Movimientos", () => { MOV_VER = !MOV_VER; pintarMovimientos(); });
  if (s.invCrear) addBtn("+ Producto", () => formProducto(null), true);
  const q = ($("inv-buscar").value || "").toLowerCase();
  const list = invRows().filter((p) => !q || p[2].toLowerCase().includes(q) || (p[3] || "").toLowerCase().includes(q));
  $("inv-lista").innerHTML = list.length ? "" : '<div class="card">Sin productos en inventario.</div>';
  list.forEach((p) => {
    const idx = (TODO.inventario || []).indexOf(p);
    const img = p[12] ? `<img class="thumb" src="${p[12]}" alt="" loading="lazy">` : "";
    const div = document.createElement("div");
    div.className = "card fila-prod";
    div.innerHTML = `${img}<div><b>${esc(p[2])}</b><small>${esc(p[3] || "")} • Stock: ${p[4]} • ${fmt(p[6])} c/u</small></div>
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

function verDeudorNormal(det, d) {
  det.innerHTML = `<button class="volver" id="deu-volver">← Deudores</button>
    <h2>${esc(d.nombre)} • ${fmt(d.pendiente)}</h2>` +
    d.items.map((it) => `<div class="card"><b>${esc(it[2])}</b><br><small>${esc(it[0] || "")} • Pendiente: ${fmt(Math.max(0, num(it[7]) - num(it[5]) - num(it[6])))}</small></div>`).join("") +
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
  const ventas = s.finVentas ? venRows().filter((v) => matchV(v[1])) : [];
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
}
$("fin-desde")?.addEventListener("change", () => {});
document.addEventListener("change", (e) => {
  if (e.target.id === "fin-desde") { FIN_DESDE = e.target.value.trim(); pintarFinanzas(); }
  if (e.target.id === "fin-hasta") { FIN_HASTA = e.target.value.trim(); pintarFinanzas(); }
});

function abrirGasto() {
  openModal(`<h2>Registrar Gasto</h2>
    <input id="g-concepto" placeholder="Concepto *">
    <div class="fila"><input id="g-monto" type="number" placeholder="Monto *">
    <select id="g-cat"><option>Operativo</option><option>Administrativo</option><option>Regalo</option><option>Otro</option></select></div>
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

function imprimirFinanzas() {
  const ventas = venRows().filter((v) => FIN_FILTRO === "Día" ? esHoy(v[1]) : FIN_FILTRO === "Mes" ? esMesActual(v[1]) : enRango(v[1], FIN_DESDE, FIN_HASTA));
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
const SPEC_SECCIONES = [
  { v: "Inicio", grupos: [
    { t: "Resumen general", tipo: "set", key: "resumen", items: [["ventas", "Ventas del día"], ["gastos", "Gastos del mes"], ["deudores", "Deudores"], ["clientes", "Clientes Activos"]] },
    { t: "Acciones rápidas", tipo: "set", key: "acciones", items: [["venta", "Venta"], ["gasto", "Gasto"], ["agregar", "Agregar"], ["deudores", "Deudores"]] },
    { t: "Alertas de stock", tipo: "bool", key: "alertas" } ] },
  { v: "Ventas", grupos: [
    { t: "Resumen financiero", tipo: "set", key: "ventasResumen", items: [["hoy", "Ventas Hoy"], ["semana", "Esta Semana"], ["mes", "Este Mes"]] },
    { t: "Ranking de productos", tipo: "bool", key: "ventasRanking" },
    { t: "Botón Ver más", tipo: "bool", key: "ventasVerMas" },
    { t: "Botón Ver Inventario Completo", tipo: "bool", key: "ventasVerInventario" } ] },
  { v: "Finanzas", grupos: [
    { t: "Botón Exportar a PDF", tipo: "bool", key: "finPdf" },
    { t: "Filtros", tipo: "set", key: "finFiltros", items: [["dia", "Día"], ["mes", "Mes"], ["rango", "Rango de fechas"]] },
    { t: "Ventas", tipo: "bool", key: "finVentas" },
    { t: "Gastos", tipo: "bool", key: "finGastos" },
    { t: "Botón Registrar Gasto", tipo: "bool", key: "finRegistrar" } ] },
  { v: "Inventario", grupos: [
    { t: "Botón Carga masiva", tipo: "bool", key: "invCarga" },
    { t: "Botón Movimientos", tipo: "bool", key: "invMovimientos" },
    { t: "Botón Crear producto", tipo: "bool", key: "invCrear" },
    { t: "Editar productos", tipo: "bool", key: "invEditar" },
    { t: "Eliminar productos", tipo: "bool", key: "invEliminar" },
    { t: "Botón Guardar inventario", tipo: "bool", key: "invGuardar" },
    { t: "Botón Hacer inventario", tipo: "bool", key: "invHacer" },
    { t: "Modo lectura (solo ver)", tipo: "bool", key: "invLectura" } ] },
];
const MODULOS = ["Reportes y Analytics", "Control de Turnos y Caja", "Facturación Electrónica DIAN", "Happy Hour & Promociones", "Venta por Mesa & Comandero", "División de Cuentas (Split)", "Agente IA Kapta Assistant"];

function pintarUsuarios() {
  if (!ME.admin) { $("t-usuarios").innerHTML = "<h2>Usuarios y Roles</h2><div class='card'>Solo administradores.</div>"; return; }
  const list = usuRows();
  $("usu-lista").innerHTML = list.length ? "" : '<div class="card">Sin usuarios registrados.</div>';
  list.forEach((u) => {
    const div = document.createElement("div");
    div.className = "card fila-deu";
    div.innerHTML = `<div><b>${esc(u[1])}</b><small>${esc(u[4] || "")} • ${esc(u[2] || "")}</small></div>
      <div class="cant"><button data-a="edit">✏️</button><button data-a="del">🗑️</button></div>`;
    div.querySelectorAll("button").forEach((b) => b.addEventListener("click", (ev) => {
      ev.stopPropagation();
      if (b.dataset.a === "edit") formUsuario(u);
      else if (confirm(`¿Eliminar a ${u[1]}?`)) borrarUsuario(u[2]);
    }));
    $("usu-lista").appendChild(div);
  });
}
$("btn-nuevo-usuario").addEventListener("click", () => formUsuario(null));

async function borrarUsuario(correo) {
  const r = await api({ action: "eliminar_usuario", sheetName: SES.code, userEmail: correo });
  toast(r.status === "success" ? "Usuario eliminado" : (r.message || "No se pudo eliminar"));
  await recargar();
}

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
function formUsuario(u) {
  USU_EDIT = u;
  let sec = FULL();
  let dock = { Inicio: true, Ventas: true, Finanzas: true, Inventario: true };
  let mods = Object.fromEntries(MODULOS.map((m) => [m, true]));
  let rol = u ? (u[4] || "Cajero") : "Cajero";
  if (u) {
    try {
      const o = JSON.parse(u[11] || "null");
      if (o && o.secciones) sec = Object.assign(FULL(), o.secciones);
      if (o && o.dock) dock = Object.assign(dock, o.dock);
      if (o && o.modulos) mods = Object.assign(mods, o.modulos);
    } catch { /* legado */ }
  } else {
    aplicarDefaultsRol(rol, sec, dock, mods);
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
    <h3>Funciones (vistas)</h3>
    ${["Inicio", "Ventas", "Finanzas", "Inventario"].map((v) => `
      <label class="check"><input type="checkbox" data-dock="${v}" ${dock[v] ? "checked" : ""}> <b>${v}</b></label>
      <div data-det="${v}" style="margin-left:14px"></div>`).join("")}
    <h3>Funciones avanzadas</h3>
    <div id="u-mods">${MODULOS.map((m) => `<label class="check"><input type="checkbox" data-mod="${esc(m)}" ${mods[m] ? "checked" : ""}> ${esc(m)}</label>`).join("")}</div>
    <p id="u-err" class="error"></p>
    <button class="btn exito" id="u-guardar">Guardar Usuario y Permisos</button>
    <button class="btn link" id="u-cancelar">Cancelar</button>`);

  const pintarDet = () => {
    ["Inicio", "Ventas", "Finanzas", "Inventario"].forEach((v) => {
      const box = document.querySelector(`[data-det="${v}"]`);
      const on = document.querySelector(`[data-dock="${v}"]`).checked;
      box.innerHTML = "";
      if (!on) return;
      const spec = SPEC_SECCIONES.find((s) => s.v === v);
      spec.grupos.forEach((g) => {
        const h = document.createElement("div");
        h.innerHTML = `<b style="font-size:12px">${g.t}</b>`;
        box.appendChild(h);
        if (g.tipo === "bool") {
          const lab = document.createElement("label");
          lab.className = "check";
          lab.innerHTML = `<input type="checkbox" data-sec="${g.key}" ${sec[g.key] ? "checked" : ""}> Activado`;
          box.appendChild(lab);
        } else {
          g.items.forEach(([k, t]) => {
            const lab = document.createElement("label");
            lab.className = "check";
            lab.innerHTML = `<input type="checkbox" data-sec="${g.key}" value="${k}" ${(sec[g.key] || []).includes(k) ? "checked" : ""}> ${t}`;
            box.appendChild(lab);
          });
        }
      });
    });
  };
  document.querySelectorAll("[data-dock]").forEach((c) => c.addEventListener("change", pintarDet));
  pintarDet();

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
    aplicarDefaultsRol(rol, sec, dock, mods);
    document.querySelectorAll("[data-dock]").forEach((c) => { c.checked = !!dock[c.dataset.dock]; });
    document.querySelectorAll("[data-mod]").forEach((c) => { c.checked = !!mods[c.dataset.mod]; });
    pintarDet();
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
    ["Inicio", "Ventas", "Finanzas", "Inventario"].forEach((v) => { dock[v] = document.querySelector(`[data-dock="${v}"]`).checked; });
    document.querySelectorAll("[data-mod]").forEach((c) => { mods[c.dataset.mod] = c.checked; });
    SPEC_SECCIONES.forEach((sp) => sp.grupos.forEach((g) => {
      const boxes = [...document.querySelectorAll(`[data-det] [data-sec="${g.key}"]`)];
      if (g.tipo === "bool") sec[g.key] = boxes.length ? boxes[0].checked : sec[g.key];
      else sec[g.key] = boxes.filter((b) => b.checked).map((b) => b.value);
    }));
    if (sec.invLectura) ["invCarga", "invMovimientos", "invCrear", "invEditar", "invEliminar", "invGuardar", "invHacer"].forEach((k) => (sec[k] = false));
    const ningunDock = !["Inicio", "Ventas", "Finanzas", "Inventario"].some((v) => dock[v]);
    if (ningunDock && !confirm("Este usuario no tendrá acceso a ninguna vista (todo desactivado). ¿Guardar de todos modos?")) return;
    const caps = new Set();
    if (dock.Ventas) ["ventas", "deudores", "clientes"].forEach((c) => caps.add(c));
    if (dock.Finanzas) ["gastos", "reporte"].forEach((c) => caps.add(c));
    if (dock.Inventario) caps.add("inventario");
    Object.entries(mods).forEach(([m, on]) => {
      if (!on) return;
      if (m === "Control de Turnos y Caja") caps.add("gastos");
      if (m === "Reportes y Analytics") caps.add("reporte");
      if (m === "Facturación Electrónica DIAN") caps.add("facturacion");
      if (["Venta por Mesa & Comandero", "División de Cuentas (Split)", "Happy Hour & Promociones"].includes(m)) caps.add("ventas");
    });
    const payload = { dock, functions: [], caps: [...caps], secciones: sec };
    const fecha = hoyLat();
    const r = await api({ action: "crear_usuario", tableName: "Usuarios",
      data: ["", nombre, correo, (!u || pin) ? pin : (u[3] || ""), rol, "Activo", u ? (u[6] || fecha) : fecha, fechaHora(), "", "", "", JSON.stringify(payload)] });
    if (r.status !== "success") { $("u-err").textContent = r.message || "No se pudo guardar"; return; }
    closeModal(); toast("Usuario guardado");
    await recargar();
  });
}

function aplicarDefaultsRol(rol, sec, dock, mods) {
  const F = FULL();
  const admin = /admin|supervisor/i.test(rol);
  const cajero = /cajero/i.test(rol);
  Object.assign(sec, admin ? F : Object.assign(F, {
    acciones: cajero ? ["venta", "gasto", "deudores"] : ["venta", "deudores"],
    finPdf: admin, finFiltros: admin ? F.finFiltros : [], finVentas: admin, finGastos: admin, finRegistrar: admin,
    invCarga: admin, invMovimientos: admin, invCrear: admin, invEditar: admin, invEliminar: admin,
    invGuardar: admin, invHacer: admin, invLectura: !admin,
  }));
  Object.assign(dock, admin ? { Inicio: true, Ventas: true, Finanzas: true, Inventario: true }
    : cajero ? { Inicio: true, Ventas: true, Finanzas: true, Inventario: false }
    : { Inicio: true, Ventas: true, Finanzas: false, Inventario: false });
  Object.keys(mods).forEach((m) => {
    mods[m] = admin || ["Control de Turnos y Caja", "Facturación Electrónica DIAN", "Happy Hour & Promociones", "Venta por Mesa & Comandero", "División de Cuentas (Split)"].includes(m) && (cajero || admin);
  });
}

// ---------- cuenta ----------
function pintarCuentaInfo() {
  $("cuenta-info").innerHTML = `<b>${esc(SES.nombre)}</b><br><small>${esc(SES.correo)} • ${esc(SES.rol)} • ${esc(SES.negocio)}</small>`;
}
function stopClave() { if (CLAVE_TIMER) { clearInterval(CLAVE_TIMER); CLAVE_TIMER = null; } }
function pintarCuenta() {
  pintarCuentaInfo();
  stopClave();
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
}
$("btn-tel").addEventListener("click", async () => {
  const tel = $("cuenta-tel").value.trim();
  if (!tel) { toast("Escribe el número"); return; }
  const r = await api({ action: "actualizar_empresa", codigo: SES.code, celular1: tel });
  toast(r.status === "success" ? "Teléfono actualizado" : (r.message || "No se pudo actualizar"));
});

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
(function init() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("sw.js").catch(() => {});
    // Fuerza la última versión del worker en cada arranque.
    navigator.serviceWorker.ready.then((r) => { try { r.update(); } catch { /* noop */ } }).catch(() => {});
  }
  pintarPaises();
  activarSplits(); aplicarLayout(); ajusteEscala();
  try {
    const vv = document.getElementById("app-version");
    if (vv) vv.textContent = VERSION_PWA;
  } catch { /* noop */ }
  const tel = $("cuenta-tel");
  SES = cargarSesion();
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
  $("in-codigo").addEventListener("change", () => localStorage.setItem("kapta_code", $("in-codigo").value.trim()));
  // precarga teléfono del negocio al abrir cuenta
  const obs = new MutationObserver(() => {
    if (!$("t-cuenta").classList.contains("oculto") && EMPRESA && !tel.value) tel.value = EMPRESA.celular1 || "";
  });
  obs.observe($("t-cuenta"), { attributes: true, attributeFilter: ["class"] });
})();
