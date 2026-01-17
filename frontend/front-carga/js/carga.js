// Configuración de la API
const API_URL = 'http://localhost:9002/api/carga';

// Manejar checkbox "Seleccionar todas"
function configurarCheckboxTodas() {
    const checkTodas = document.getElementById('check-todas');
    const fuentesCheckboxes = document.querySelectorAll('.fuente-checkbox');

    checkTodas.addEventListener('change', () => {
        fuentesCheckboxes.forEach(checkbox => {
            checkbox.checked = checkTodas.checked;
        });
    });

    fuentesCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', () => {
            const todasMarcadas = Array.from(fuentesCheckboxes).every(cb => cb.checked);
            checkTodas.checked = todasMarcadas;
        });
    });
}

// Limpiar selección
function limpiarSeleccion() {
    document.getElementById('check-todas').checked = false;
    document.querySelectorAll('.fuente-checkbox').forEach(checkbox => {
        checkbox.checked = false;
    });
    limpiarResultados();
}

// Limpiar resultados
function limpiarResultados() {
    const resultsContainer = document.getElementById('results-container');
    resultsContainer.innerHTML = '<p class="info-text">Selecciona fuentes y haz clic en "Cargar" para empezar</p>';
}

// Obtener fuentes seleccionadas
function obtenerFuentesSeleccionadas() {
    const fuentesCheckboxes = document.querySelectorAll('.fuente-checkbox:checked');
    return Array.from(fuentesCheckboxes).map(cb => cb.value);
}

// Cargar datos
async function cargarDatos() {
    const fuentes = obtenerFuentesSeleccionadas();

    if (fuentes.length === 0) {
        alert('Por favor, selecciona al menos una fuente');
        return;
    }

    mostrarCargando(true);

    try {
        const queryParams = new URLSearchParams();
        fuentes.forEach(fuente => queryParams.append('fuentes', fuente));

        const response = await fetch(`${API_URL}/cargar?${queryParams.toString()}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        const resultado = await response.json();
        mostrarResultadosCarga(resultado);

    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error al cargar datos: ' + error.message);
    } finally {
        mostrarCargando(false);
    }
}

// Borrar almacén de datos
async function borrarAlmacen() {
    if (!confirm('¿Estás seguro de que deseas borrar todos los datos del almacén? Esta acción no se puede deshacer.')) {
        return;
    }

    mostrarCargando(true);

    try {
        const response = await fetch(`${API_URL}/borrar`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Error al borrar el almacén');
        }

        const mensaje = await response.text();
        mostrarMensajeExito('Almacén de datos borrado correctamente');

    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error al borrar el almacén: ' + error.message);
    } finally {
        mostrarCargando(false);
    }
}

// Mostrar/ocultar indicador de carga
function mostrarCargando(mostrar) {
    const loading = document.getElementById('loading');
    const resultsContainer = document.getElementById('results-container');

    if (mostrar) {
        loading.style.display = 'flex';
        resultsContainer.style.display = 'none';
    } else {
        loading.style.display = 'none';
        resultsContainer.style.display = 'block';
    }
}

// Mostrar resultados de la carga
function mostrarResultadosCarga(resultado) {
    const resultsContainer = document.getElementById('results-container');

    let html = `
        <div class="stats-box">
            <h3>Resumen de carga</h3>
            <div class="stat-item">
                <span class="stat-label">Número de registros cargados correctamente:</span>
                <span class="stat-value success">${resultado.registrosCorrectos}</span>
            </div>
            <div class="stat-item">
                <span class="stat-label">Registros con errores y reparados:</span>
                <span class="stat-value warning">${resultado.registrosConErroresReparados}</span>
            </div>
            <div class="stat-item">
                <span class="stat-label">Registros rechazados:</span>
                <span class="stat-value error">${resultado.registrosRechazados}</span>
            </div>
        </div>
    `;

    // Mostrar errores reparados
    if (resultado.erroresReparados && resultado.erroresReparados.length > 0) {
        html += `
            <div class="error-list">
                <h4>Registros con errores y reparados:</h4>
                <div class="error-table">
                    <table>
                        <thead>
                            <tr>
                                <th>Fuente</th>
                                <th>Nombre</th>
                                <th>Localidad</th>
                                <th>Motivo del error</th>
                                <th>Operación</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${resultado.erroresReparados.map(error => `
                                <tr>
                                    <td>${error.fuente}</td>
                                    <td>${error.nombre}</td>
                                    <td>${error.localidad}</td>
                                    <td>${error.motivo}</td>
                                    <td>${error.operacion}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }

    // Mostrar errores rechazados
    if (resultado.erroresRechazados && resultado.erroresRechazados.length > 0) {
        html += `
            <div class="error-list">
                <h4>Registros con errores y rechazados:</h4>
                <div class="error-table">
                    <table>
                        <thead>
                            <tr>
                                <th>Fuente</th>
                                <th>Nombre</th>
                                <th>Localidad</th>
                                <th>Motivo del error</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${resultado.erroresRechazados.map(error => `
                                <tr>
                                    <td>${error.fuente}</td>
                                    <td>${error.nombre}</td>
                                    <td>${error.localidad}</td>
                                    <td>${error.motivo}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }

    resultsContainer.innerHTML = html;
}

// Mostrar mensaje de éxito
function mostrarMensajeExito(mensaje) {
    const resultsContainer = document.getElementById('results-container');
    resultsContainer.innerHTML = `<div class="success-message">${mensaje}</div>`;
}

// Mostrar error
function mostrarError(mensaje) {
    const resultsContainer = document.getElementById('results-container');
    resultsContainer.innerHTML = `<div class="error-message">${mensaje}</div>`;
}

// Event listeners
document.addEventListener('DOMContentLoaded', () => {
    configurarCheckboxTodas();

    document.getElementById('btn-cargar').addEventListener('click', cargarDatos);
    document.getElementById('btn-cancelar').addEventListener('click', limpiarSeleccion);
    document.getElementById('btn-borrar').addEventListener('click', borrarAlmacen);
});
