// Portal mínimo: todas las reglas y permisos los aplica la API. Acá solo se ocultan los botones que el rol no puede usar.
const API = "/api/v1";
const $ = id => document.getElementById(id);
let muestraActual = null;

function sesion() {
    return { token: sessionStorage.getItem("token"), usuario: sessionStorage.getItem("usuario"), rol: sessionStorage.getItem("rol") };
}

function mostrarMensaje(texto, tipo) {
    const m = $("mensaje");
    m.textContent = texto;
    m.className = tipo;
    m.hidden = false;
}

async function pedir(metodo, ruta, cuerpo) {
    const opciones = { method: metodo, headers: { Authorization: "Bearer " + sesion().token } };
    if (cuerpo) {
        opciones.headers["Content-Type"] = "application/json";
        opciones.body = JSON.stringify(cuerpo);
    }
    const res = await fetch(API + ruta, opciones);
    if (res.status === 401) {
        salir("La sesión expiró o el token no es válido. Ingresá de nuevo.");
        throw new Error("401");
    }
    return res;
}

async function leerError(res) {
    try {
        return (await res.json()).mensaje;
    } catch {
        return "Error " + res.status;
    }
}

function mostrarPantalla() {
    const { token, usuario, rol } = sesion();
    $("pantalla-login").hidden = !!token;
    $("pantalla-principal").hidden = !token;
    $("sesion").hidden = !token;
    if (!token) return;
    $("usuario-actual").textContent = usuario;
    $("rol-actual").textContent = rol;
    document.querySelectorAll("[data-roles]").forEach(el => el.hidden = !el.dataset.roles.split(" ").includes(rol));
    cargarMuestras();
}

function salir(motivo) {
    sessionStorage.clear();
    muestraActual = null;
    $("detalle").hidden = true;
    mostrarPantalla();
    if (motivo) mostrarMensaje(motivo, "error");
    else $("mensaje").hidden = true;
}

async function ingresar(e) {
    e.preventDefault();
    const res = await fetch(API + "/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ usuario: $("usuario").value, clave: $("clave").value })
    });
    if (!res.ok) {
        mostrarMensaje(await leerError(res), "error");
        return;
    }
    const datos = await res.json();
    sessionStorage.setItem("token", datos.token);
    sessionStorage.setItem("usuario", $("usuario").value);
    sessionStorage.setItem("rol", datos.rol);
    $("form-login").reset();
    $("mensaje").hidden = true;
    mostrarPantalla();
}

function celda(fila, texto) {
    const td = document.createElement("td");
    td.textContent = texto ?? "-";
    fila.appendChild(td);
    return td;
}

async function cargarMuestras() {
    const filtros = new URLSearchParams();
    [["gen", "filtro-gen"], ["categoria", "filtro-categoria"], ["desde", "filtro-desde"], ["hasta", "filtro-hasta"]]
        .forEach(([param, id]) => { if ($(id).value.trim()) filtros.set(param, $(id).value.trim()); });
    const inicio = performance.now();
    const res = await pedir("GET", "/samples?" + filtros);
    if (!res.ok) {
        mostrarMensaje(await leerError(res), "error");
        return;
    }
    const lista = await res.json();
    $("resultado-filtros").textContent = lista.length + " muestra(s) · " + Math.round(performance.now() - inicio) + " ms";
    const cuerpo = $("tabla-muestras").querySelector("tbody");
    cuerpo.replaceChildren();
    for (const m of lista) {
        const fila = document.createElement("tr");
        fila.dataset.codigo = m.codigo;
        celda(fila, m.codigo);
        celda(fila, m.idCorrida);
        celda(fila, m.estado);
        celda(fila, m.fechaRecepcion);
        const ver = document.createElement("button");
        ver.type = "button";
        ver.textContent = "Ver";
        ver.className = "btn-ver";
        ver.onclick = () => verMuestra(m.codigo);
        celda(fila, "").appendChild(ver);
        cuerpo.appendChild(fila);
    }
}

function mostrarDetalle(m) {
    muestraActual = m.codigo;
    $("detalle").hidden = false;
    $("det-codigo").textContent = m.codigo;
    $("det-estado").textContent = m.estado;
    $("det-q30").textContent = m.q30;
    $("det-contaminacion").textContent = m.contaminacion;
    $("det-firma").textContent = m.firmadaPor ?? "-";
    const cuerpo = $("tabla-variantes").querySelector("tbody");
    cuerpo.replaceChildren();
    for (const v of m.variantes) {
        const fila = document.createElement("tr");
        [v.gen, v.posicion, v.categoriaPropuesta, v.confianza, v.categoria,
            v.requiereRevision ? "Sí" : "No", v.exigeFirma ? "Sí" : "No"].forEach(t => celda(fila, t));
        cuerpo.appendChild(fila);
    }
}

async function verMuestra(codigo) {
    const res = await pedir("GET", "/samples/" + encodeURIComponent(codigo));
    if (!res.ok) {
        mostrarMensaje(await leerError(res), "error");
        return;
    }
    mostrarDetalle(await res.json());
}

async function sha256(texto) {
    const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(texto));
    return [...new Uint8Array(hash)].map(b => b.toString(16).padStart(2, "0")).join("");
}

async function registrar(e) {
    e.preventDefault();
    const contenido = $("reg-contenido").value;
    const res = await pedir("POST", "/samples", {
        codigo: $("reg-codigo").value,
        idCorrida: $("reg-corrida").value,
        checksum: await sha256(contenido),
        contenido: contenido,
        q30: Number($("reg-q30").value),
        contaminacion: Number($("reg-contaminacion").value),
        variantes: [{
            gen: $("reg-gen").value, posicion: Number($("reg-posicion").value), tipo: "SNV",
            categoriaPropuesta: $("reg-categoria").value, confianza: Number($("reg-confianza").value)
        }]
    });
    if (res.status === 201) {
        const m = await res.json();
        mostrarMensaje("Muestra " + m.codigo + " registrada: " + m.estado, "ok");
        $("form-registro").reset();
        mostrarDetalle(m);
    } else if (res.status === 409 || res.status === 422) {
        const cuerpo = await res.json();
        mostrarMensaje(cuerpo.estado ? "Corrida rechazada: " + cuerpo.estado : cuerpo.mensaje, "error");
    } else {
        mostrarMensaje(await leerError(res), "error");
    }
    cargarMuestras();
}

async function accion(metodo, ruta, textoOk) {
    const res = await pedir(metodo, "/samples/" + encodeURIComponent(muestraActual) + ruta);
    if (!res.ok) {
        mostrarMensaje(await leerError(res), "error");
        return;
    }
    const m = await res.json();
    mostrarMensaje(textoOk + ": " + m.estado, "ok");
    mostrarDetalle(m);
    cargarMuestras();
}

async function descargarInforme() {
    const codigo = muestraActual;
    const res = await pedir("GET", "/reports/" + encodeURIComponent(codigo) + "/pdf");
    if (!res.ok) {
        mostrarMensaje(await leerError(res), "error");
        return;
    }
    const enlace = document.createElement("a");
    enlace.href = URL.createObjectURL(await res.blob());
    enlace.download = "informe-" + codigo + ".pdf";
    enlace.click();
    URL.revokeObjectURL(enlace.href);
    mostrarMensaje("Informe de " + codigo + " descargado", "ok");
    verMuestra(codigo);
    cargarMuestras();
}

$("form-login").onsubmit = ingresar;
$("form-registro").onsubmit = registrar;
$("form-buscar").onsubmit = e => { e.preventDefault(); verMuestra($("buscar-codigo").value.trim()); };
$("form-filtros").onsubmit = e => { e.preventDefault(); cargarMuestras(); };
$("btn-limpiar").onclick = () => { $("form-filtros").reset(); cargarMuestras(); };
$("btn-salir").onclick = () => salir();
$("btn-procesar").onclick = () => accion("POST", "/process", "Muestra procesada");
$("btn-firmar").onclick = () => accion("POST", "/sign", "Diagnóstico firmado");
$("btn-informe").onclick = descargarInforme;
mostrarPantalla();
