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
    const localidad = document.getElementById('localidad').value.trim();
    const codigoPostal = document.getElementById('codigo-postal').value.trim();
    const provincia = document.getElementById('provincia').value.trim();
    const tipo = document.getElementById('tipo').value;

    // Crear objeto de búsqueda
    const busqueda = {
        localidad: localidad || null,
        codigoPostal: codigoPostal || null,
        provincia: provincia || null,
        tipo: tipo || null
    };

    try {
        const response = await fetch(`${API_URL}/estaciones`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(busqueda)
        });

        if (!response.ok) {
            throw new Error('Error en la búsqueda');
        }

        const estaciones = await response.json();
        mostrarResultados(estaciones);
        mostrarEnMapa(estaciones);

    } catch (error) {
        console.error('Error:', error);
        alert('Error al buscar estaciones: ' + error.message);
    }
}

// Mostrar resultados en la tabla
function mostrarResultados(estaciones) {
    const tbody = document.getElementById('results-tbody');

    if (estaciones.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="no-results">No se encontraron resultados</td></tr>';
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
