// ================= CONFIGURACIÓN =================
const API_URL = 'http://localhost:9001/api/busqueda';

let map;
let markersLayer;

// Almacén global
let todasLasEstaciones = [];
let markersPorId = new Map();

// ================= ESTILOS DE MARCADORES =================
const ESTILO_NORMAL = {
    radius: 6,
    color: '#3388ff',
    fillColor: '#3388ff',
    fillOpacity: 0.8
};

const ESTILO_RESALTADO = {
    radius: 8,
    color: '#dc9606',
    fillColor: '#f1c353',
    fillOpacity: 0.95
};

// ================= MAPA =================
function initMap() {
    map = L.map('map').setView([40.4168, -3.7038], 6);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(map);

    markersLayer = L.layerGroup().addTo(map);
}

// ================= CARGA INICIAL =================
async function cargarTodasLasEstaciones() {
    try {
        const response = await fetch(`${API_URL}/estaciones`);
        if (!response.ok) throw new Error();

        const estaciones = await response.json();
        todasLasEstaciones = estaciones;

        pintarTodasEnMapa(estaciones);
        mostrarResultados(estaciones);

    } catch (e) {
        mostrarError(
            'Error inicial',
            'No se pudieron cargar las estaciones ITV.'
        );
    }
}

// ================= MAPA: PINTADO INICIAL =================
function pintarTodasEnMapa(estaciones) {
    markersLayer.clearLayers();
    markersPorId.clear();

    const bounds = [];

    estaciones.forEach(estacion => {
        if (!estacion.latitud || !estacion.longitud) return;

        const marker = L.circleMarker(
            [estacion.latitud, estacion.longitud],
            ESTILO_NORMAL
        );

        marker.bindTooltip(`
            <strong>${estacion.nombre}</strong><br>
            <strong>Tipo:</strong> ${estacion.tipo || 'N/A'}<br>
            <strong>Dirección:</strong> ${estacion.direccion || 'N/A'}<br>
            <strong>Localidad:</strong> ${estacion.localidadNombre || 'N/A'}<br>
            <strong>CP:</strong> ${estacion.codigoPostal || 'N/A'}<br>
            <strong>Provincia:</strong> ${estacion.provinciaNombre || 'N/A'}
        `);

        marker.addTo(markersLayer);

        // IMPORTANTE: id único
        markersPorId.set(estacion.id, marker);

        bounds.push([estacion.latitud, estacion.longitud]);
    });

    if (bounds.length) {
        map.fitBounds(bounds, { padding: [50, 50] });
    }
}

// ================= BÚSQUEDA =================
async function buscarEstaciones() {
    ocultarError();
    ocultarInfo();

    const localidad = document.getElementById('localidad').value.trim();
    const codigoPostal = document.getElementById('codigo-postal').value.trim();
    const provincia = document.getElementById('provincia').value.trim();
    const tipo = document.getElementById('tipo').value;

    const params = new URLSearchParams();
    if (localidad) params.append('localidad', localidad);
    if (codigoPostal) params.append('codigoPostal', codigoPostal);
    if (provincia) params.append('provincia', provincia);
    if (tipo) params.append('tipo', tipo);

    const url = params.toString()
        ? `${API_URL}/estaciones?${params.toString()}`
        : `${API_URL}/estaciones`;

    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error();

        const estaciones = await response.json();

        mostrarResultados(estaciones);
        resaltarEstaciones(estaciones);

    } catch (e) {
        mostrarError(
            'Error en la búsqueda',
            'No fue posible obtener los datos solicitados.'
        );
    }
}

// ================= RESALTADO =================
function resaltarEstaciones(estacionesBusqueda) {
    // Restaurar todos a normal
    markersPorId.forEach(marker => {
        marker.setStyle(ESTILO_NORMAL);
    });

    // Resaltar resultados
    estacionesBusqueda.forEach(estacion => {
        const marker = markersPorId.get(estacion.id);
        if (marker) {
            marker.setStyle(ESTILO_RESALTADO);
            marker.bringToFront();
        }
    });

    if (estacionesBusqueda.length === 0) {
        mostrarInfo(
            'Sin resultados',
            'No se han encontrado estaciones con los criterios indicados.'
        );
    }
}

// ================= TABLA =================
function mostrarResultados(estaciones) {
    const tbody = document.getElementById('results-tbody');

    if (estaciones.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="no-results">
                    No se encontraron estaciones con los criterios indicados
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = '';

    estaciones.forEach(estacion => {
        const fila = document.createElement('tr');
        fila.innerHTML = `
            <td>${estacion.nombre || ''}</td>
            <td>${estacion.tipo || ''}</td>
            <td>${estacion.direccion || ''}</td>
            <td>${estacion.localidadNombre || ''}</td>
            <td>${estacion.codigoPostal || ''}</td>
            <td>${estacion.provinciaNombre || ''}</td>
            <td>${estacion.descripcion || ''}</td>
        `;
        tbody.appendChild(fila);
    });
}

// ================= FORMULARIO =================
function limpiarFormulario() {
    document.getElementById('localidad').value = '';
    document.getElementById('codigo-postal').value = '';
    document.getElementById('provincia').value = '';
    document.getElementById('tipo').value = '';

    mostrarResultados(todasLasEstaciones);
    resaltarEstaciones([]);
}

// ================= MENSAJES =================
function mostrarError(titulo, mensaje) {
    const errorBox = document.getElementById('error-box');
    document.getElementById('error-title').textContent = titulo;
    document.getElementById('error-message').textContent = mensaje;

    errorBox.classList.remove('hidden');
    setTimeout(() => errorBox.classList.add('hidden'), 6000);
}

function ocultarError() {
    document.getElementById('error-box').classList.add('hidden');
}

function mostrarInfo(titulo, mensaje) {
    const infoBox = document.getElementById('info-box');
    document.getElementById('info-title').textContent = titulo;
    document.getElementById('info-message').textContent = mensaje;

    infoBox.classList.remove('hidden');
    setTimeout(() => infoBox.classList.add('hidden'), 5000);
}

function ocultarInfo() {
    document.getElementById('info-box').classList.add('hidden');
}

// ================= EVENTOS =================
document.addEventListener('DOMContentLoaded', () => {
    initMap();
    cargarTodasLasEstaciones();

    document.getElementById('btn-buscar')
        .addEventListener('click', buscarEstaciones);

    document.getElementById('btn-cancelar')
        .addEventListener('click', limpiarFormulario);

    document.querySelectorAll('.form-group input')
        .forEach(input => {
            input.addEventListener('keypress', e => {
                if (e.key === 'Enter') {
                    buscarEstaciones();
                }
            });
        });
});