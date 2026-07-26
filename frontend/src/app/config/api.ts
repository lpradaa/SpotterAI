/**
 * Base de las llamadas al backend.
 *
 * Vacia a proposito: todas las rutas salen relativas ("/api/...") y quien las
 * lleva al backend es un proxy —el de `ng serve` en desarrollo, nginx en el
 * contenedor—. Antes cada servicio escribia "http://localhost:8080" a mano, en
 * nueve sitios, lo que significaba que:
 *
 *   - el frontend compilado solo servia para una maquina concreta, porque la
 *     direccion queda fijada en el bundle;
 *   - todo el trafico era entre origenes distintos, y de ahi la configuracion
 *     de CORS que hay que mantener sincronizada con el despliegue.
 *
 * Con rutas relativas el navegador habla con su propio origen y las dos cosas
 * desaparecen. Si algun dia el backend vive en otro dominio, se cambia aqui.
 */
export const API_BASE = '';

/** Atajo para no repetir la concatenacion en cada servicio. */
export const api = (ruta: string) => `${API_BASE}${ruta}`;
