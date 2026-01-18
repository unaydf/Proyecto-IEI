// Configuración de la API
const API_URL = 'http://localhost:9001/api/busqueda';

// Inicializar el mapa
let map;
let markersLayer;

function initMap() {
    // Centro de España aproximadamente
    map = L.map('map').setView([40.4168, -3.7038], 6);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(map);

    markersLayer = L.layerGroup().addTo(map);
}

// Limpiar formulario
function limpiarFormulario() {
    document.getElementById('localidad').value = '';
    document.getElementById('codigo-postal').value = '';
    document.getElementById('provincia').value = '';
    document.getElementById('tipo').value = '';
    limpiarResultados();
    markersLayer.clearLayers();
}

// Limpiar resultados
function limpiarResultados() {
    const tbody = document.getElementById('results-tbody');
    tbody.innerHTML = '<tr><td colspan="7" class="no-results">Realiza una búsqueda para ver resultados</td></tr>';
}

// Buscar estaciones
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

    const url = `${API_URL}/estaciones?${params.toString()}`;

    try {
        const response = await fetch(url, {
            method: 'GET'
        });

        if (!response.ok) {
            throw new Error('Error en la búsqueda');
        }

        const estaciones = await response.json();
        mostrarResultados(estaciones);
        mostrarEnMapa(estaciones);

    } catch (error) {
        console.error('Error:', error);

        if (error instanceof TypeError) {
            mostrarError(
                'No se pudo conectar con el servidor',
                'El servicio de búsqueda no está disponible en este momento. Inténtalo más tarde.'
            );
        } else {
            mostrarError(
                'Error en la búsqueda',
                error.message || 'Se produjo un error inesperado'
            );
        }
    }
}

// Mostrar resultados en la tabla
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

        mostrarInfo(
            'Sin resultados',
            'No se han encontrado estaciones ITV que coincidan con la búsqueda realizada.'
        );

        return;
    }

    ocultarInfo();
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

// Mostrar estaciones en el mapa
function mostrarEnMapa(estaciones) {
    // Limpiar marcadores anteriores
    markersLayer.clearLayers();

    const bounds = [];

    estaciones.forEach(estacion => {
        if (estacion.latitud && estacion.longitud) {
            const marker = L.marker([estacion.latitud, estacion.longitud]).addTo(markersLayer);

            // Crear tooltip con información de la estación
            const tooltipContent = `
                <strong>${estacion.nombre}</strong><br>
                <strong>Tipo:</strong> ${estacion.tipo || 'N/A'}<br>
                <strong>Dirección:</strong> ${estacion.direccion || 'N/A'}<br>
                <strong>Localidad:</strong> ${estacion.localidadNombre || 'N/A'}<br>
                <strong>CP:</strong> ${estacion.codigoPostal || 'N/A'}<br>
                <strong>Provincia:</strong> ${estacion.provinciaNombre || 'N/A'}
            `;

            marker.bindTooltip(tooltipContent, {
                direction: 'top',
                offset: [0, -10]
            });

            bounds.push([estacion.latitud, estacion.longitud]);
        }
    });

    // Ajustar el mapa para mostrar todos los marcadores
    if (bounds.length > 0) {
        map.fitBounds(bounds, { padding: [50, 50] });
    }
}

function mostrarError(titulo, mensaje) {
    const errorBox = document.getElementById('error-box');
    document.getElementById('error-title').textContent = titulo;
    document.getElementById('error-message').textContent = mensaje;

    errorBox.classList.remove('hidden');

    // Ocultar automáticamente tras 6 segundos
    setTimeout(() => {
        errorBox.classList.add('hidden');
    }, 6000);
}

function ocultarError() {
    document.getElementById('error-box').classList.add('hidden');
}

function mostrarInfo(titulo, mensaje) {
    const infoBox = document.getElementById('info-box');
    document.getElementById('info-title').textContent = titulo;
    document.getElementById('info-message').textContent = mensaje;

    infoBox.classList.remove('hidden');

    setTimeout(() => {
        infoBox.classList.add('hidden');
    }, 5000);
}

function ocultarInfo() {
    document.getElementById('info-box').classList.add('hidden');
}

// Event listeners
document.addEventListener('DOMContentLoaded', () => {
    initMap();

    document.getElementById('btn-buscar').addEventListener('click', buscarEstaciones);
    document.getElementById('btn-cancelar').addEventListener('click', limpiarFormulario);

    // Permitir búsqueda con Enter
    document.querySelectorAll('.form-group input').forEach(input => {
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                buscarEstaciones();
            }
        });
    });
});
