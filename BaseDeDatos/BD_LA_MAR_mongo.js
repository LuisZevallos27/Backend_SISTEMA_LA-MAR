// =====================================================================
// PROYECTO FINAL BASE DE DATOS II - GRUPO 02 - LA MAR CEVICHERIA
// Parte NO relacional (MongoDB): encuestas de satisfaccion y resenas de platos
//
// Se ejecuta con mongosh (ver instrucciones al final del archivo).
// ATENCION: este script ELIMINA y vuelve a crear las colecciones
// "encuestas" y "resenas" de la base LaMarFeedback.
//
// Relacion con SQL Server (LaMarDB): MongoDB NO tiene llaves foraneas.
// Los documentos guardan los IDs de SQL Server como referencia
// (idPedido, idPlato, idEmpleado) y copian los nombres, para que la
// opinion siga siendo legible aunque el plato o el empleado cambien.
// La regla "solo se encuesta un pedido Cerrado" la valida el backend
// consultando SQL Server antes de insertar.
//
// Los datos de ejemplo usan idPedido, idPlato e idEmpleado REALES de
// BD_LA_MAR.sql y BD_LA_MAR_datos.sql (solo pedidos en estado Cerrado) y
// sus fechas son relativas a la hora de Lima, igual que en el script SQL.
// =====================================================================

db = db.getSiblingDB('LaMarFeedback');

// ---------------------------------------------------------------------
// 1. COLECCIONES (con validacion de esquema)
// ---------------------------------------------------------------------
db.encuestas.drop();
db.resenas.drop();

// ENCUESTA: una por pedido. Mide la visita completa.
db.createCollection('encuestas', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['idPedido', 'idEmpleado', 'fecha', 'calificaciones', 'recomendaria'],
      properties: {
        idPedido:     { bsonType: 'number', minimum: 1, description: 'Pedido.id_pedido en SQL Server' },
        numeroMesa:   { bsonType: 'number', minimum: 1 },
        idEmpleado:   { bsonType: 'number', minimum: 1, description: 'Empleado.id_empleado en SQL Server' },
        mozo:         { bsonType: 'string', maxLength: 121 },
        fecha:        { bsonType: 'date' },
        calificaciones: {
          bsonType: 'object',
          required: ['comida', 'servicio', 'ambiente', 'tiempoEspera'],
          properties: {
            comida:       { bsonType: 'number', minimum: 1, maximum: 5 },
            servicio:     { bsonType: 'number', minimum: 1, maximum: 5 },
            ambiente:     { bsonType: 'number', minimum: 1, maximum: 5 },
            tiempoEspera: { bsonType: 'number', minimum: 1, maximum: 5 }
          }
        },
        recomendaria: { bsonType: 'bool' },
        comentario:   { bsonType: 'string', maxLength: 500 },
        etiquetas:    { bsonType: 'array', maxItems: 10, items: { bsonType: 'string', maxLength: 30 } }
      }
    }
  },
  validationLevel: 'strict',
  validationAction: 'error'
});

// RESENA: una por plato y pedido. Mide cada plato por separado.
db.createCollection('resenas', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['idPedido', 'idPlato', 'nombrePlato', 'estrellas', 'fecha'],
      properties: {
        idPedido:    { bsonType: 'number', minimum: 1, description: 'Pedido.id_pedido en SQL Server' },
        idPlato:     { bsonType: 'number', minimum: 1, description: 'Plato.id_plato en SQL Server' },
        nombrePlato: { bsonType: 'string', maxLength: 80 },
        categoria:   { bsonType: 'string', maxLength: 40 },
        estrellas:   { bsonType: 'number', minimum: 1, maximum: 5 },
        comentario:  { bsonType: 'string', maxLength: 500 },
        fecha:       { bsonType: 'date' }
      }
    }
  },
  validationLevel: 'strict',
  validationAction: 'error'
});

// ---------------------------------------------------------------------
// 2. INDICES
// ---------------------------------------------------------------------
// Una sola encuesta por pedido
db.encuestas.createIndex({ idPedido: 1 }, { unique: true, name: 'UQ_encuesta_pedido' });
// Ultimas encuestas / filtros por periodo
db.encuestas.createIndex({ fecha: -1 }, { name: 'IX_encuesta_fecha' });
// Ranking y promedios por mozo
db.encuestas.createIndex({ idEmpleado: 1, fecha: -1 }, { name: 'IX_encuesta_empleado' });

// Un plato solo se resena una vez por pedido
db.resenas.createIndex({ idPedido: 1, idPlato: 1 }, { unique: true, name: 'UQ_resena_pedido_plato' });
// Promedio y ultimas resenas de un plato (carta)
db.resenas.createIndex({ idPlato: 1, fecha: -1 }, { name: 'IX_resena_plato' });

// ---------------------------------------------------------------------
// 3. DATOS DE EJEMPLO
// ---------------------------------------------------------------------
// Hora de Lima (UTC-5, sin horario de verano): medianoche de hoy en Lima.
// hace(n) = n minutos antes de esa medianoche (igual que DATEADD(MINUTE, -n, @hoy)).
const LIMA_MS = 5 * 60 * 60 * 1000;
const MEDIANOCHE_LIMA = Math.floor((Date.now() - LIMA_MS) / 86400000) * 86400000 + LIMA_MS;
const hace = (minutos) => new Date(MEDIANOCHE_LIMA - minutos * 60000);

// Encuestas: 70 pedidos cerrados de los ultimos 30 dias
db.encuestas.insertMany([
    { idPedido: 4, numeroMesa: 15, idEmpleado: 1, mozo: "Carlos Ramos Quispe", fecha: hace(42395), calificaciones: { comida: 2, servicio: 5, ambiente: 3, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["poco-fresco", "atencion-amable"] },
    { idPedido: 8, numeroMesa: 3, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(42309), calificaciones: { comida: 4, servicio: 4, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "ambiente-agradable"] },
    { idPedido: 15, numeroMesa: 3, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(40912), calificaciones: { comida: 5, servicio: 2, ambiente: 4, tiempoEspera: 4 }, recomendaria: true, comentario: "Todo normal, sin quejas pero sin nada especial.", etiquetas: ["buen-sabor", "atencion-lenta", "rapido", "ambiente-agradable"] },
    { idPedido: 31, numeroMesa: 18, idEmpleado: 8, mozo: "Diego Torres Villanueva", fecha: hace(38081), calificaciones: { comida: 3, servicio: 4, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, comentario: "La comida muy buena, el tiempo de espera algo largo.", etiquetas: ["atencion-amable", "ambiente-agradable"] },
    { idPedido: 46, numeroMesa: 10, idEmpleado: 1, mozo: "Carlos Ramos Quispe", fecha: hace(36428), calificaciones: { comida: 4, servicio: 5, ambiente: 4, tiempoEspera: 5 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 48, numeroMesa: 15, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(36201), calificaciones: { comida: 5, servicio: 3, ambiente: 5, tiempoEspera: 5 }, recomendaria: true, comentario: "Todo estuvo excelente, volveremos pronto.", etiquetas: ["fresco", "rapido", "ambiente-agradable"] },
    { idPedido: 52, numeroMesa: 8, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(35129), calificaciones: { comida: 5, servicio: 4, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, comentario: "La comida muy buena, el tiempo de espera algo largo.", etiquetas: ["fresco", "atencion-amable", "ambiente-agradable"] },
    { idPedido: 53, numeroMesa: 7, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(35125), calificaciones: { comida: 4, servicio: 2, ambiente: 4, tiempoEspera: 2 }, recomendaria: false, comentario: "La comida muy buena, el tiempo de espera algo largo.", etiquetas: ["buen-sabor", "atencion-lenta", "demora", "ambiente-agradable"] },
    { idPedido: 57, numeroMesa: 7, idEmpleado: 8, mozo: "Diego Torres Villanueva", fecha: hace(35056), calificaciones: { comida: 4, servicio: 2, ambiente: 3, tiempoEspera: 3 }, recomendaria: false, etiquetas: ["buen-sabor", "atencion-lenta"] },
    { idPedido: 59, numeroMesa: 6, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(35022), calificaciones: { comida: 5, servicio: 4, ambiente: 5, tiempoEspera: 3 }, recomendaria: true, comentario: "Buena atención y los platos salieron rápido.", etiquetas: ["buen-sabor", "atencion-amable", "ambiente-agradable"] },
    { idPedido: 63, numeroMesa: 7, idEmpleado: 6, mozo: "Anghelo Lopez Castilla", fecha: hace(34688), calificaciones: { comida: 4, servicio: 3, ambiente: 4, tiempoEspera: 4 }, recomendaria: true, comentario: "Buena comida, pero hubo que esperar un poco.", etiquetas: ["fresco", "rapido", "ambiente-agradable"] },
    { idPedido: 66, numeroMesa: 6, idEmpleado: 9, mozo: "Valeria Castillo Núñez", fecha: hace(33728), calificaciones: { comida: 5, servicio: 3, ambiente: 5, tiempoEspera: 3 }, recomendaria: true, comentario: "Los platos ricos, el servicio puede mejorar.", etiquetas: ["buen-sabor", "ambiente-agradable"] },
    { idPedido: 67, numeroMesa: 15, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(33621), calificaciones: { comida: 2, servicio: 5, ambiente: 3, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["poco-fresco", "atencion-amable", "rapido"] },
    { idPedido: 70, numeroMesa: 12, idEmpleado: 6, mozo: "Anghelo Lopez Castilla", fecha: hace(33543), calificaciones: { comida: 5, servicio: 5, ambiente: 3, tiempoEspera: 4 }, recomendaria: true, comentario: "Excelente sazón, las porciones son generosas.", etiquetas: ["fresco", "atencion-amable", "rapido"] },
    { idPedido: 74, numeroMesa: 17, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(33278), calificaciones: { comida: 4, servicio: 4, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["fresco", "atencion-amable", "ambiente-agradable"] },
    { idPedido: 80, numeroMesa: 3, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(32184), calificaciones: { comida: 3, servicio: 4, ambiente: 5, tiempoEspera: 5 }, recomendaria: true, comentario: "Una de las mejores cevicherías que he probado.", etiquetas: ["atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 118, numeroMesa: 6, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(27585), calificaciones: { comida: 4, servicio: 5, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, comentario: "Todo normal, sin quejas pero sin nada especial.", etiquetas: ["buen-sabor", "atencion-amable", "ambiente-agradable"] },
    { idPedido: 125, numeroMesa: 9, idEmpleado: 1, mozo: "Carlos Ramos Quispe", fecha: hace(27386), calificaciones: { comida: 5, servicio: 4, ambiente: 4, tiempoEspera: 4 }, recomendaria: true, comentario: "Nos atendieron de maravilla, todo muy rico.", etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 128, numeroMesa: 3, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(26374), calificaciones: { comida: 4, servicio: 4, ambiente: 3, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable"] },
    { idPedido: 133, numeroMesa: 7, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(25982), calificaciones: { comida: 5, servicio: 2, ambiente: 4, tiempoEspera: 5 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-lenta", "rapido", "ambiente-agradable"] },
    { idPedido: 139, numeroMesa: 16, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(24975), calificaciones: { comida: 2, servicio: 3, ambiente: 4, tiempoEspera: 3 }, recomendaria: false, comentario: "Estuvo bien, aunque el local estaba algo ruidoso.", etiquetas: ["poco-fresco", "ambiente-agradable"] },
    { idPedido: 143, numeroMesa: 1, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(23655), calificaciones: { comida: 3, servicio: 5, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["atencion-amable", "ambiente-agradable"] },
    { idPedido: 160, numeroMesa: 8, idEmpleado: 6, mozo: "Anghelo Lopez Castilla", fecha: hace(22076), calificaciones: { comida: 3, servicio: 4, ambiente: 4, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 162, numeroMesa: 13, idEmpleado: 8, mozo: "Diego Torres Villanueva", fecha: hace(21781), calificaciones: { comida: 5, servicio: 5, ambiente: 5, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 171, numeroMesa: 2, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(20735), calificaciones: { comida: 4, servicio: 3, ambiente: 3, tiempoEspera: 3 }, recomendaria: true, comentario: "Buena comida, pero hubo que esperar un poco.", etiquetas: ["buen-sabor"] },
    { idPedido: 175, numeroMesa: 3, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(20667), calificaciones: { comida: 4, servicio: 4, ambiente: 4, tiempoEspera: 4 }, recomendaria: true, comentario: "Buena comida, pero hubo que esperar un poco.", etiquetas: ["fresco", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 178, numeroMesa: 2, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(20331), calificaciones: { comida: 4, servicio: 4, ambiente: 5, tiempoEspera: 5 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 189, numeroMesa: 3, idEmpleado: 9, mozo: "Valeria Castillo Núñez", fecha: hace(19264), calificaciones: { comida: 4, servicio: 5, ambiente: 3, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable"] },
    { idPedido: 193, numeroMesa: 13, idEmpleado: 4, mozo: "Moises Vilchez Aliaga", fecha: hace(18863), calificaciones: { comida: 3, servicio: 3, ambiente: 3, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["rapido"] },
    { idPedido: 197, numeroMesa: 6, idEmpleado: 6, mozo: "Anghelo Lopez Castilla", fecha: hace(17500), calificaciones: { comida: 5, servicio: 3, ambiente: 5, tiempoEspera: 3 }, recomendaria: true, comentario: "Todo normal, sin quejas pero sin nada especial.", etiquetas: ["fresco", "ambiente-agradable"] },
    { idPedido: 201, numeroMesa: 6, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(17357), calificaciones: { comida: 4, servicio: 5, ambiente: 4, tiempoEspera: 2 }, recomendaria: true, comentario: "Todo normal, sin quejas pero sin nada especial.", etiquetas: ["buen-sabor", "atencion-amable", "demora", "ambiente-agradable"] },
    { idPedido: 203, numeroMesa: 8, idEmpleado: 4, mozo: "Moises Vilchez Aliaga", fecha: hace(16465), calificaciones: { comida: 4, servicio: 4, ambiente: 5, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["fresco", "atencion-amable", "ambiente-agradable"] },
    { idPedido: 207, numeroMesa: 17, idEmpleado: 8, mozo: "Diego Torres Villanueva", fecha: hace(16340), calificaciones: { comida: 5, servicio: 5, ambiente: 5, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 218, numeroMesa: 13, idEmpleado: 9, mozo: "Valeria Castillo Núñez", fecha: hace(15004), calificaciones: { comida: 5, servicio: 5, ambiente: 4, tiempoEspera: 5 }, recomendaria: true, etiquetas: ["fresco", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 219, numeroMesa: 8, idEmpleado: 6, mozo: "Anghelo Lopez Castilla", fecha: hace(14884), calificaciones: { comida: 4, servicio: 3, ambiente: 5, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["buen-sabor", "rapido", "ambiente-agradable"] },
    { idPedido: 231, numeroMesa: 13, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(13603), calificaciones: { comida: 5, servicio: 5, ambiente: 5, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 230, numeroMesa: 2, idEmpleado: 4, mozo: "Moises Vilchez Aliaga", fecha: hace(13579), calificaciones: { comida: 5, servicio: 3, ambiente: 4, tiempoEspera: 4 }, recomendaria: true, comentario: "Los platos ricos, el servicio puede mejorar.", etiquetas: ["buen-sabor", "rapido", "ambiente-agradable"] },
    { idPedido: 232, numeroMesa: 15, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(13551), calificaciones: { comida: 3, servicio: 5, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["atencion-amable", "ambiente-agradable"] },
    { idPedido: 235, numeroMesa: 4, idEmpleado: 8, mozo: "Diego Torres Villanueva", fecha: hace(13520), calificaciones: { comida: 4, servicio: 5, ambiente: 4, tiempoEspera: 5 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 241, numeroMesa: 1, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(12161), calificaciones: { comida: 4, servicio: 3, ambiente: 4, tiempoEspera: 2 }, recomendaria: true, comentario: "Estuvo bien, aunque el local estaba algo ruidoso.", etiquetas: ["buen-sabor", "demora", "ambiente-agradable"] },
    { idPedido: 246, numeroMesa: 3, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(11976), calificaciones: { comida: 3, servicio: 5, ambiente: 3, tiempoEspera: 3 }, recomendaria: true, comentario: "Estuvo bien, aunque el local estaba algo ruidoso.", etiquetas: ["atencion-amable"] },
    { idPedido: 249, numeroMesa: 11, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(11623), calificaciones: { comida: 4, servicio: 4, ambiente: 5, tiempoEspera: 2 }, recomendaria: true, etiquetas: ["fresco", "atencion-amable", "demora", "ambiente-agradable"] },
    { idPedido: 251, numeroMesa: 14, idEmpleado: 4, mozo: "Moises Vilchez Aliaga", fecha: hace(11600), calificaciones: { comida: 4, servicio: 3, ambiente: 3, tiempoEspera: 5 }, recomendaria: true, etiquetas: ["fresco", "rapido"] },
    { idPedido: 253, numeroMesa: 11, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(10661), calificaciones: { comida: 4, servicio: 4, ambiente: 4, tiempoEspera: 5 }, recomendaria: true, comentario: "Todo estuvo excelente, volveremos pronto.", etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 257, numeroMesa: 5, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(10212), calificaciones: { comida: 3, servicio: 5, ambiente: 3, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["atencion-amable"] },
    { idPedido: 262, numeroMesa: 11, idEmpleado: 6, mozo: "Anghelo Lopez Castilla", fecha: hace(9229), calificaciones: { comida: 4, servicio: 4, ambiente: 3, tiempoEspera: 4 }, recomendaria: true, comentario: "La comida muy buena, el tiempo de espera algo largo.", etiquetas: ["fresco", "atencion-amable", "rapido"] },
    { idPedido: 263, numeroMesa: 11, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(9155), calificaciones: { comida: 4, servicio: 5, ambiente: 3, tiempoEspera: 3 }, recomendaria: true, comentario: "Buena comida, pero hubo que esperar un poco.", etiquetas: ["buen-sabor", "atencion-amable"] },
    { idPedido: 268, numeroMesa: 18, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(8734), calificaciones: { comida: 5, servicio: 3, ambiente: 4, tiempoEspera: 2 }, recomendaria: true, comentario: "La comida muy buena, el tiempo de espera algo largo.", etiquetas: ["fresco", "demora", "ambiente-agradable"] },
    { idPedido: 272, numeroMesa: 14, idEmpleado: 6, mozo: "Anghelo Lopez Castilla", fecha: hace(8668), calificaciones: { comida: 5, servicio: 5, ambiente: 5, tiempoEspera: 5 }, recomendaria: true, etiquetas: ["fresco", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 276, numeroMesa: 11, idEmpleado: 8, mozo: "Diego Torres Villanueva", fecha: hace(7817), calificaciones: { comida: 5, servicio: 5, ambiente: 5, tiempoEspera: 5 }, recomendaria: true, comentario: "Una de las mejores cevicherías que he probado.", etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 274, numeroMesa: 13, idEmpleado: 9, mozo: "Valeria Castillo Núñez", fecha: hace(7809), calificaciones: { comida: 5, servicio: 4, ambiente: 4, tiempoEspera: 5 }, recomendaria: true, etiquetas: ["fresco", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 279, numeroMesa: 18, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(7330), calificaciones: { comida: 3, servicio: 3, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, comentario: "Los platos ricos, el servicio puede mejorar.", etiquetas: ["ambiente-agradable"] },
    { idPedido: 282, numeroMesa: 6, idEmpleado: 5, mozo: "Marcos Mori Barona", fecha: hace(7274), calificaciones: { comida: 4, servicio: 3, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["buen-sabor", "ambiente-agradable"] },
    { idPedido: 285, numeroMesa: 12, idEmpleado: 4, mozo: "Moises Vilchez Aliaga", fecha: hace(6375), calificaciones: { comida: 3, servicio: 4, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["atencion-amable", "ambiente-agradable"] },
    { idPedido: 284, numeroMesa: 14, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(6368), calificaciones: { comida: 4, servicio: 5, ambiente: 5, tiempoEspera: 5 }, recomendaria: true, comentario: "Excelente sazón, las porciones son generosas.", etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 292, numeroMesa: 17, idEmpleado: 4, mozo: "Moises Vilchez Aliaga", fecha: hace(5940), calificaciones: { comida: 3, servicio: 5, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["atencion-amable", "ambiente-agradable"] },
    { idPedido: 296, numeroMesa: 15, idEmpleado: 9, mozo: "Valeria Castillo Núñez", fecha: hace(5838), calificaciones: { comida: 4, servicio: 4, ambiente: 4, tiempoEspera: 5 }, recomendaria: true, comentario: "Nos atendieron de maravilla, todo muy rico.", etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 297, numeroMesa: 5, idEmpleado: 6, mozo: "Anghelo Lopez Castilla", fecha: hace(4941), calificaciones: { comida: 4, servicio: 5, ambiente: 4, tiempoEspera: 4 }, recomendaria: true, comentario: "Todo estuvo excelente, volveremos pronto.", etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 300, numeroMesa: 10, idEmpleado: 1, mozo: "Carlos Ramos Quispe", fecha: hace(4839), calificaciones: { comida: 4, servicio: 5, ambiente: 4, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 301, numeroMesa: 3, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(4793), calificaciones: { comida: 5, servicio: 5, ambiente: 3, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["fresco", "atencion-amable", "rapido"] },
    { idPedido: 302, numeroMesa: 13, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(4502), calificaciones: { comida: 3, servicio: 5, ambiente: 5, tiempoEspera: 4 }, recomendaria: true, comentario: "Una de las mejores cevicherías que he probado.", etiquetas: ["atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 303, numeroMesa: 5, idEmpleado: 8, mozo: "Diego Torres Villanueva", fecha: hace(3511), calificaciones: { comida: 4, servicio: 4, ambiente: 3, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable"] },
    { idPedido: 305, numeroMesa: 5, idEmpleado: 1, mozo: "Carlos Ramos Quispe", fecha: hace(3442), calificaciones: { comida: 4, servicio: 5, ambiente: 4, tiempoEspera: 4 }, recomendaria: true, comentario: "Nos atendieron de maravilla, todo muy rico.", etiquetas: ["fresco", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 311, numeroMesa: 1, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(3068), calificaciones: { comida: 5, servicio: 4, ambiente: 5, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["fresco", "atencion-amable", "ambiente-agradable"] },
    { idPedido: 310, numeroMesa: 10, idEmpleado: 7, mozo: "Marta Huamán Flores", fecha: hace(3047), calificaciones: { comida: 4, servicio: 3, ambiente: 4, tiempoEspera: 3 }, recomendaria: true, etiquetas: ["fresco", "ambiente-agradable"] },
    { idPedido: 312, numeroMesa: 11, idEmpleado: 8, mozo: "Diego Torres Villanueva", fecha: hace(2932), calificaciones: { comida: 4, servicio: 4, ambiente: 3, tiempoEspera: 4 }, recomendaria: true, comentario: "Buena comida, pero hubo que esperar un poco.", etiquetas: ["fresco", "atencion-amable", "rapido"] },
    { idPedido: 316, numeroMesa: 11, idEmpleado: 1, mozo: "Carlos Ramos Quispe", fecha: hace(1921), calificaciones: { comida: 4, servicio: 5, ambiente: 5, tiempoEspera: 5 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 317, numeroMesa: 10, idEmpleado: 9, mozo: "Valeria Castillo Núñez", fecha: hace(1906), calificaciones: { comida: 4, servicio: 4, ambiente: 3, tiempoEspera: 5 }, recomendaria: true, comentario: "Los platos ricos, el servicio puede mejorar.", etiquetas: ["buen-sabor", "atencion-amable", "rapido"] },
    { idPedido: 324, numeroMesa: 8, idEmpleado: 6, mozo: "Anghelo Lopez Castilla", fecha: hace(1527), calificaciones: { comida: 5, servicio: 4, ambiente: 5, tiempoEspera: 4 }, recomendaria: true, etiquetas: ["buen-sabor", "atencion-amable", "rapido", "ambiente-agradable"] },
    { idPedido: 333, numeroMesa: 2, idEmpleado: 8, mozo: "Diego Torres Villanueva", fecha: hace(199), calificaciones: { comida: 5, servicio: 5, ambiente: 5, tiempoEspera: 3 }, recomendaria: true, comentario: "Nos atendieron de maravilla, todo muy rico.", etiquetas: ["buen-sabor", "atencion-amable", "ambiente-agradable"] }
]);

// Resenas: 1 a 3 platos de 110 pedidos cerrados
db.resenas.insertMany([
    { idPedido: 4, idPlato: 34, nombrePlato: "Jalea Mixta", categoria: "Fondos", fecha: hace(42429), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 4, idPlato: 40, nombrePlato: "Tres Leches", categoria: "Postres", fecha: hace(42426), estrellas: 5 },
    { idPedido: 10, idPlato: 49, nombrePlato: "Gaseosa (botella)", categoria: "Bebidas", fecha: hace(41979), estrellas: 4 },
    { idPedido: 10, idPlato: 23, nombrePlato: "Causa de Atún", categoria: "Causas", fecha: hace(41952), estrellas: 4 },
    { idPedido: 13, idPlato: 17, nombrePlato: "Tiradito al Ají Amarillo", categoria: "Tiraditos", fecha: hace(41878), estrellas: 4, comentario: "Bien en general, lo recomiendo." },
    { idPedido: 13, idPlato: 50, nombrePlato: "Agua Mineral", categoria: "Bebidas", fecha: hace(41877), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 13, idPlato: 20, nombrePlato: "Tiradito al Rocoto", categoria: "Tiraditos", fecha: hace(41867), estrellas: 3 },
    { idPedido: 17, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(40959), estrellas: 3 },
    { idPedido: 20, idPlato: 16, nombrePlato: "Cebiche de Salmón", categoria: "Cebiches", fecha: hace(40846), estrellas: 4, comentario: "Bien en general, lo recomiendo." },
    { idPedido: 29, idPlato: 29, nombrePlato: "Leche de Tigre", categoria: "Entradas", fecha: hace(38979), estrellas: 5, comentario: "Fresco y con muy buena sazón." },
    { idPedido: 29, idPlato: 37, nombrePlato: "Chicharrón de Pescado", categoria: "Fondos", fecha: hace(38958), estrellas: 1, comentario: "No me gustó, el sabor estaba muy flojo." },
    { idPedido: 30, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(38117), estrellas: 5 },
    { idPedido: 30, idPlato: 38, nombrePlato: "Parihuela", categoria: "Fondos", fecha: hace(38090), estrellas: 2 },
    { idPedido: 33, idPlato: 47, nombrePlato: "Maracuyá Frozen", categoria: "Bebidas", fecha: hace(38088), estrellas: 3 },
    { idPedido: 33, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(38084), estrellas: 4 },
    { idPedido: 30, idPlato: 51, nombrePlato: "Copa de Vino Blanco", categoria: "Bebidas", fecha: hace(38080), estrellas: 2, comentario: "Porción pequeña para el precio." },
    { idPedido: 38, idPlato: 27, nombrePlato: "Anticuchos de Pulpo", categoria: "Anticuchos", fecha: hace(37581), estrellas: 4 },
    { idPedido: 38, idPlato: 50, nombrePlato: "Agua Mineral", categoria: "Bebidas", fecha: hace(37580), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 38, idPlato: 46, nombrePlato: "Limonada Clásica", categoria: "Bebidas", fecha: hace(37577), estrellas: 5, comentario: "Porción generosa y excelente sabor." },
    { idPedido: 40, idPlato: 5, nombrePlato: "Tiradito Bachiche", categoria: "Tiraditos", fecha: hace(37491), estrellas: 4 },
    { idPedido: 40, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(37485), estrellas: 5, comentario: "Uno de los mejores de la carta." },
    { idPedido: 40, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(37481), estrellas: 4 },
    { idPedido: 44, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(36519), estrellas: 4 },
    { idPedido: 44, idPlato: 46, nombrePlato: "Limonada Clásica", categoria: "Bebidas", fecha: hace(36494), estrellas: 4, comentario: "Bien en general, lo recomiendo." },
    { idPedido: 46, idPlato: 50, nombrePlato: "Agua Mineral", categoria: "Bebidas", fecha: hace(36447), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 46, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(36443), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 48, idPlato: 14, nombrePlato: "Cebiche Carretillero", categoria: "Cebiches", fecha: hace(36212), estrellas: 2, comentario: "Porción pequeña para el precio." },
    { idPedido: 48, idPlato: 15, nombrePlato: "Cebiche de Conchas de Abanico", categoria: "Cebiches", fecha: hace(36208), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 53, idPlato: 2, nombrePlato: "Cebiche Criollo", categoria: "Cebiches", fecha: hace(35162), estrellas: 3 },
    { idPedido: 55, idPlato: 38, nombrePlato: "Parihuela", categoria: "Fondos", fecha: hace(35097), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 55, idPlato: 4, nombrePlato: "Cebiche Nikkei", categoria: "Cebiches", fecha: hace(35095), estrellas: 4 },
    { idPedido: 61, idPlato: 28, nombrePlato: "Anticuchos de Pescado", categoria: "Anticuchos", fecha: hace(35042), estrellas: 3 },
    { idPedido: 63, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(34684), estrellas: 3 },
    { idPedido: 72, idPlato: 44, nombrePlato: "Maracuyá Sour", categoria: "Bebidas", fecha: hace(33351), estrellas: 3 },
    { idPedido: 72, idPlato: 15, nombrePlato: "Cebiche de Conchas de Abanico", categoria: "Cebiches", fecha: hace(33341), estrellas: 5 },
    { idPedido: 73, idPlato: 23, nombrePlato: "Causa de Atún", categoria: "Causas", fecha: hace(33328), estrellas: 4 },
    { idPedido: 75, idPlato: 4, nombrePlato: "Cebiche Nikkei", categoria: "Cebiches", fecha: hace(33226), estrellas: 5, comentario: "Uno de los mejores de la carta." },
    { idPedido: 75, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(33204), estrellas: 5 },
    { idPedido: 77, idPlato: 19, nombrePlato: "Tiradito de Atún", categoria: "Tiraditos", fecha: hace(32288), estrellas: 4 },
    { idPedido: 77, idPlato: 3, nombrePlato: "Cebiche Tumbes", categoria: "Cebiches", fecha: hace(32265), estrellas: 3 },
    { idPedido: 79, idPlato: 50, nombrePlato: "Agua Mineral", categoria: "Bebidas", fecha: hace(32230), estrellas: 4 },
    { idPedido: 81, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(32197), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 81, idPlato: 48, nombrePlato: "Cerveza Rubia (botella)", categoria: "Bebidas", fecha: hace(32160), estrellas: 3, comentario: "Normal, esperaba un poco más." },
    { idPedido: 84, idPlato: 21, nombrePlato: "Tiradito de Salmón", categoria: "Tiraditos", fecha: hace(31908), estrellas: 4 },
    { idPedido: 85, idPlato: 46, nombrePlato: "Limonada Clásica", categoria: "Bebidas", fecha: hace(31805), estrellas: 4 },
    { idPedido: 85, idPlato: 33, nombrePlato: "Tequeños de Langostinos", categoria: "Entradas", fecha: hace(31801), estrellas: 5 },
    { idPedido: 86, idPlato: 34, nombrePlato: "Jalea Mixta", categoria: "Fondos", fecha: hace(31743), estrellas: 4, comentario: "Bien en general, lo recomiendo." },
    { idPedido: 86, idPlato: 5, nombrePlato: "Tiradito Bachiche", categoria: "Tiraditos", fecha: hace(31720), estrellas: 4 },
    { idPedido: 86, idPlato: 3, nombrePlato: "Cebiche Tumbes", categoria: "Cebiches", fecha: hace(31711), estrellas: 5, comentario: "Perfecto punto de sazón." },
    { idPedido: 92, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(30441), estrellas: 5, comentario: "Fresco y con muy buena sazón." },
    { idPedido: 92, idPlato: 15, nombrePlato: "Cebiche de Conchas de Abanico", categoria: "Cebiches", fecha: hace(30401), estrellas: 4 },
    { idPedido: 94, idPlato: 51, nombrePlato: "Copa de Vino Blanco", categoria: "Bebidas", fecha: hace(30376), estrellas: 3 },
    { idPedido: 94, idPlato: 4, nombrePlato: "Cebiche Nikkei", categoria: "Cebiches", fecha: hace(30348), estrellas: 4, comentario: "Bien en general, lo recomiendo." },
    { idPedido: 96, idPlato: 36, nombrePlato: "Chaufa de Mariscos", categoria: "Fondos", fecha: hace(30346), estrellas: 3, comentario: "Algo salado para mi gusto." },
    { idPedido: 96, idPlato: 45, nombrePlato: "Chilcano de Pisco", categoria: "Bebidas", fecha: hace(30312), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 96, idPlato: 2, nombrePlato: "Cebiche Criollo", categoria: "Cebiches", fecha: hace(30306), estrellas: 4 },
    { idPedido: 99, idPlato: 4, nombrePlato: "Cebiche Nikkei", categoria: "Cebiches", fecha: hace(29389), estrellas: 4 },
    { idPedido: 100, idPlato: 35, nombrePlato: "Arroz con Mariscos", categoria: "Fondos", fecha: hace(29387), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 99, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(29375), estrellas: 5, comentario: "Fresco y con muy buena sazón." },
    { idPedido: 101, idPlato: 35, nombrePlato: "Arroz con Mariscos", categoria: "Fondos", fecha: hace(29341), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 102, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(29326), estrellas: 5 },
    { idPedido: 101, idPlato: 19, nombrePlato: "Tiradito de Atún", categoria: "Tiraditos", fecha: hace(29318), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 103, idPlato: 12, nombrePlato: "Cebiche Clásico", categoria: "Cebiches", fecha: hace(29300), estrellas: 4 },
    { idPedido: 105, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(29281), estrellas: 3 },
    { idPedido: 106, idPlato: 26, nombrePlato: "Anticuchos de Corazón", categoria: "Anticuchos", fecha: hace(29002), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 115, idPlato: 36, nombrePlato: "Chaufa de Mariscos", categoria: "Fondos", fecha: hace(27956), estrellas: 4 },
    { idPedido: 115, idPlato: 31, nombrePlato: "Chicharrón de Calamar", categoria: "Entradas", fecha: hace(27946), estrellas: 5, comentario: "Porción generosa y excelente sabor." },
    { idPedido: 115, idPlato: 38, nombrePlato: "Parihuela", categoria: "Fondos", fecha: hace(27942), estrellas: 4, comentario: "Bien en general, lo recomiendo." },
    { idPedido: 119, idPlato: 47, nombrePlato: "Maracuyá Frozen", categoria: "Bebidas", fecha: hace(27608), estrellas: 3 },
    { idPedido: 118, idPlato: 45, nombrePlato: "Chilcano de Pisco", categoria: "Bebidas", fecha: hace(27605), estrellas: 3, comentario: "Algo salado para mi gusto." },
    { idPedido: 119, idPlato: 2, nombrePlato: "Cebiche Criollo", categoria: "Cebiches", fecha: hace(27597), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 118, idPlato: 49, nombrePlato: "Gaseosa (botella)", categoria: "Bebidas", fecha: hace(27588), estrellas: 5, comentario: "Uno de los mejores de la carta." },
    { idPedido: 118, idPlato: 15, nombrePlato: "Cebiche de Conchas de Abanico", categoria: "Cebiches", fecha: hace(27584), estrellas: 3, comentario: "Algo salado para mi gusto." },
    { idPedido: 120, idPlato: 46, nombrePlato: "Limonada Clásica", categoria: "Bebidas", fecha: hace(27570), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 130, idPlato: 15, nombrePlato: "Cebiche de Conchas de Abanico", categoria: "Cebiches", fecha: hace(26138), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 131, idPlato: 15, nombrePlato: "Cebiche de Conchas de Abanico", categoria: "Cebiches", fecha: hace(26133), estrellas: 4 },
    { idPedido: 131, idPlato: 45, nombrePlato: "Chilcano de Pisco", categoria: "Bebidas", fecha: hace(26109), estrellas: 5, comentario: "Perfecto punto de sazón." },
    { idPedido: 134, idPlato: 33, nombrePlato: "Tequeños de Langostinos", categoria: "Entradas", fecha: hace(25125), estrellas: 5 },
    { idPedido: 134, idPlato: 50, nombrePlato: "Agua Mineral", categoria: "Bebidas", fecha: hace(25103), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 136, idPlato: 8, nombrePlato: "Suspiro a la Limeña", categoria: "Postres", fecha: hace(25027), estrellas: 4 },
    { idPedido: 136, idPlato: 48, nombrePlato: "Cerveza Rubia (botella)", categoria: "Bebidas", fecha: hace(25012), estrellas: 3 },
    { idPedido: 136, idPlato: 40, nombrePlato: "Tres Leches", categoria: "Postres", fecha: hace(24998), estrellas: 4 },
    { idPedido: 139, idPlato: 7, nombrePlato: "Anticuchos de Pollo", categoria: "Anticuchos", fecha: hace(24982), estrellas: 4 },
    { idPedido: 141, idPlato: 36, nombrePlato: "Chaufa de Mariscos", categoria: "Fondos", fecha: hace(24557), estrellas: 4 },
    { idPedido: 142, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(24556), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 148, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(23306), estrellas: 3, comentario: "Algo salado para mi gusto." },
    { idPedido: 148, idPlato: 50, nombrePlato: "Agua Mineral", categoria: "Bebidas", fecha: hace(23263), estrellas: 3, comentario: "Normal, esperaba un poco más." },
    { idPedido: 150, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(23257), estrellas: 4 },
    { idPedido: 151, idPlato: 14, nombrePlato: "Cebiche Carretillero", categoria: "Cebiches", fecha: hace(23255), estrellas: 5 },
    { idPedido: 150, idPlato: 2, nombrePlato: "Cebiche Criollo", categoria: "Cebiches", fecha: hace(23251), estrellas: 4 },
    { idPedido: 150, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(23235), estrellas: 4 },
    { idPedido: 151, idPlato: 5, nombrePlato: "Tiradito Bachiche", categoria: "Tiraditos", fecha: hace(23234), estrellas: 3 },
    { idPedido: 155, idPlato: 45, nombrePlato: "Chilcano de Pisco", categoria: "Bebidas", fecha: hace(23104), estrellas: 4 },
    { idPedido: 155, idPlato: 25, nombrePlato: "Causa Acevichada", categoria: "Causas", fecha: hace(23101), estrellas: 5, comentario: "Perfecto punto de sazón." },
    { idPedido: 155, idPlato: 51, nombrePlato: "Copa de Vino Blanco", categoria: "Bebidas", fecha: hace(23087), estrellas: 2, comentario: "Porción pequeña para el precio." },
    { idPedido: 163, idPlato: 50, nombrePlato: "Agua Mineral", categoria: "Bebidas", fecha: hace(21763), estrellas: 5, comentario: "Perfecto punto de sazón." },
    { idPedido: 163, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(21746), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 165, idPlato: 3, nombrePlato: "Cebiche Tumbes", categoria: "Cebiches", fecha: hace(21729), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 165, idPlato: 31, nombrePlato: "Chicharrón de Calamar", categoria: "Entradas", fecha: hace(21724), estrellas: 4 },
    { idPedido: 166, idPlato: 34, nombrePlato: "Jalea Mixta", categoria: "Fondos", fecha: hace(21673), estrellas: 3 },
    { idPedido: 166, idPlato: 36, nombrePlato: "Chaufa de Mariscos", categoria: "Fondos", fecha: hace(21669), estrellas: 3 },
    { idPedido: 166, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(21655), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 172, idPlato: 51, nombrePlato: "Copa de Vino Blanco", categoria: "Bebidas", fecha: hace(20745), estrellas: 2 },
    { idPedido: 172, idPlato: 23, nombrePlato: "Causa de Atún", categoria: "Causas", fecha: hace(20719), estrellas: 5, comentario: "Porción generosa y excelente sabor." },
    { idPedido: 177, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(20690), estrellas: 4 },
    { idPedido: 179, idPlato: 51, nombrePlato: "Copa de Vino Blanco", categoria: "Bebidas", fecha: hace(20290), estrellas: 3, comentario: "Normal, esperaba un poco más." },
    { idPedido: 179, idPlato: 44, nombrePlato: "Maracuyá Sour", categoria: "Bebidas", fecha: hace(20285), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 181, idPlato: 14, nombrePlato: "Cebiche Carretillero", categoria: "Cebiches", fecha: hace(20274), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 184, idPlato: 45, nombrePlato: "Chilcano de Pisco", categoria: "Bebidas", fecha: hace(19367), estrellas: 5, comentario: "Delicioso, lo volvería a pedir." },
    { idPedido: 184, idPlato: 19, nombrePlato: "Tiradito de Atún", categoria: "Tiraditos", fecha: hace(19337), estrellas: 4 },
    { idPedido: 184, idPlato: 31, nombrePlato: "Chicharrón de Calamar", categoria: "Entradas", fecha: hace(19326), estrellas: 5 },
    { idPedido: 188, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(19275), estrellas: 4 },
    { idPedido: 189, idPlato: 2, nombrePlato: "Cebiche Criollo", categoria: "Cebiches", fecha: hace(19275), estrellas: 5 },
    { idPedido: 189, idPlato: 32, nombrePlato: "Papa a la Huancaína", categoria: "Entradas", fecha: hace(19273), estrellas: 2, comentario: "Llegó tibio y le faltaba sabor." },
    { idPedido: 189, idPlato: 49, nombrePlato: "Gaseosa (botella)", categoria: "Bebidas", fecha: hace(19254), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 193, idPlato: 39, nombrePlato: "Mazamorra Morada", categoria: "Postres", fecha: hace(18888), estrellas: 4 },
    { idPedido: 193, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(18880), estrellas: 5 },
    { idPedido: 195, idPlato: 49, nombrePlato: "Gaseosa (botella)", categoria: "Bebidas", fecha: hace(17769), estrellas: 5, comentario: "Porción generosa y excelente sabor." },
    { idPedido: 197, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(17530), estrellas: 4, comentario: "Bien en general, lo recomiendo." },
    { idPedido: 197, idPlato: 37, nombrePlato: "Chicharrón de Pescado", categoria: "Fondos", fecha: hace(17508), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 199, idPlato: 7, nombrePlato: "Anticuchos de Pollo", categoria: "Anticuchos", fecha: hace(17403), estrellas: 3, comentario: "Algo salado para mi gusto." },
    { idPedido: 199, idPlato: 14, nombrePlato: "Cebiche Carretillero", categoria: "Cebiches", fecha: hace(17391), estrellas: 5, comentario: "Delicioso, lo volvería a pedir." },
    { idPedido: 201, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(17364), estrellas: 5, comentario: "Uno de los mejores de la carta." },
    { idPedido: 199, idPlato: 49, nombrePlato: "Gaseosa (botella)", categoria: "Bebidas", fecha: hace(17359), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 206, idPlato: 51, nombrePlato: "Copa de Vino Blanco", categoria: "Bebidas", fecha: hace(16393), estrellas: 3 },
    { idPedido: 206, idPlato: 35, nombrePlato: "Arroz con Mariscos", categoria: "Fondos", fecha: hace(16388), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 208, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(16352), estrellas: 5, comentario: "Uno de los mejores de la carta." },
    { idPedido: 211, idPlato: 50, nombrePlato: "Agua Mineral", categoria: "Bebidas", fecha: hace(16035), estrellas: 5, comentario: "Fresco y con muy buena sazón." },
    { idPedido: 211, idPlato: 27, nombrePlato: "Anticuchos de Pulpo", categoria: "Anticuchos", fecha: hace(16008), estrellas: 3 },
    { idPedido: 211, idPlato: 38, nombrePlato: "Parihuela", categoria: "Fondos", fecha: hace(16007), estrellas: 4 },
    { idPedido: 215, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(15933), estrellas: 4 },
    { idPedido: 215, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(15926), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 215, idPlato: 27, nombrePlato: "Anticuchos de Pulpo", categoria: "Anticuchos", fecha: hace(15890), estrellas: 5, comentario: "Uno de los mejores de la carta." },
    { idPedido: 223, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(14619), estrellas: 2 },
    { idPedido: 224, idPlato: 33, nombrePlato: "Tequeños de Langostinos", categoria: "Entradas", fecha: hace(14573), estrellas: 4 },
    { idPedido: 226, idPlato: 8, nombrePlato: "Suspiro a la Limeña", categoria: "Postres", fecha: hace(14468), estrellas: 5 },
    { idPedido: 231, idPlato: 6, nombrePlato: "Causa Limeña", categoria: "Causas", fecha: hace(13640), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 235, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(13524), estrellas: 2 },
    { idPedido: 243, idPlato: 16, nombrePlato: "Cebiche de Salmón", categoria: "Cebiches", fecha: hace(12051), estrellas: 3 },
    { idPedido: 246, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(12045), estrellas: 4 },
    { idPedido: 246, idPlato: 37, nombrePlato: "Chicharrón de Pescado", categoria: "Fondos", fecha: hace(12026), estrellas: 3 },
    { idPedido: 246, idPlato: 2, nombrePlato: "Cebiche Criollo", categoria: "Cebiches", fecha: hace(12005), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 249, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(11666), estrellas: 4 },
    { idPedido: 249, idPlato: 48, nombrePlato: "Cerveza Rubia (botella)", categoria: "Bebidas", fecha: hace(11651), estrellas: 3 },
    { idPedido: 251, idPlato: 12, nombrePlato: "Cebiche Clásico", categoria: "Cebiches", fecha: hace(11640), estrellas: 2 },
    { idPedido: 254, idPlato: 16, nombrePlato: "Cebiche de Salmón", categoria: "Cebiches", fecha: hace(10712), estrellas: 5 },
    { idPedido: 258, idPlato: 2, nombrePlato: "Cebiche Criollo", categoria: "Cebiches", fecha: hace(10236), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 258, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(10230), estrellas: 3 },
    { idPedido: 262, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(9240), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 264, idPlato: 40, nombrePlato: "Tres Leches", categoria: "Postres", fecha: hace(9177), estrellas: 4 },
    { idPedido: 265, idPlato: 4, nombrePlato: "Cebiche Nikkei", categoria: "Cebiches", fecha: hace(9173), estrellas: 4 },
    { idPedido: 264, idPlato: 2, nombrePlato: "Cebiche Criollo", categoria: "Cebiches", fecha: hace(9170), estrellas: 4, comentario: "Bien en general, lo recomiendo." },
    { idPedido: 264, idPlato: 49, nombrePlato: "Gaseosa (botella)", categoria: "Bebidas", fecha: hace(9164), estrellas: 5 },
    { idPedido: 265, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(9161), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 267, idPlato: 16, nombrePlato: "Cebiche de Salmón", categoria: "Cebiches", fecha: hace(8816), estrellas: 4 },
    { idPedido: 267, idPlato: 42, nombrePlato: "Crema Volteada", categoria: "Postres", fecha: hace(8813), estrellas: 4 },
    { idPedido: 267, idPlato: 32, nombrePlato: "Papa a la Huancaína", categoria: "Entradas", fecha: hace(8781), estrellas: 1 },
    { idPedido: 277, idPlato: 51, nombrePlato: "Copa de Vino Blanco", categoria: "Bebidas", fecha: hace(7835), estrellas: 2 },
    { idPedido: 276, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(7827), estrellas: 5, comentario: "Fresco y con muy buena sazón." },
    { idPedido: 277, idPlato: 45, nombrePlato: "Chilcano de Pisco", categoria: "Bebidas", fecha: hace(7806), estrellas: 5 },
    { idPedido: 279, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(7350), estrellas: 5, comentario: "Perfecto punto de sazón." },
    { idPedido: 279, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(7349), estrellas: 5, comentario: "Delicioso, lo volvería a pedir." },
    { idPedido: 280, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(7335), estrellas: 5, comentario: "Delicioso, lo volvería a pedir." },
    { idPedido: 280, idPlato: 36, nombrePlato: "Chaufa de Mariscos", categoria: "Fondos", fecha: hace(7319), estrellas: 5 },
    { idPedido: 280, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(7316), estrellas: 4 },
    { idPedido: 283, idPlato: 20, nombrePlato: "Tiradito al Rocoto", categoria: "Tiraditos", fecha: hace(7256), estrellas: 4 },
    { idPedido: 284, idPlato: 49, nombrePlato: "Gaseosa (botella)", categoria: "Bebidas", fecha: hace(6421), estrellas: 4 },
    { idPedido: 286, idPlato: 25, nombrePlato: "Causa Acevichada", categoria: "Causas", fecha: hace(6398), estrellas: 3, comentario: "Está bien, pero nada especial." },
    { idPedido: 286, idPlato: 17, nombrePlato: "Tiradito al Ají Amarillo", categoria: "Tiraditos", fecha: hace(6358), estrellas: 4 },
    { idPedido: 289, idPlato: 42, nombrePlato: "Crema Volteada", categoria: "Postres", fecha: hace(6342), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 289, idPlato: 49, nombrePlato: "Gaseosa (botella)", categoria: "Bebidas", fecha: hace(6340), estrellas: 4 },
    { idPedido: 287, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(6333), estrellas: 2 },
    { idPedido: 287, idPlato: 48, nombrePlato: "Cerveza Rubia (botella)", categoria: "Bebidas", fecha: hace(6323), estrellas: 3 },
    { idPedido: 289, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(6323), estrellas: 4 },
    { idPedido: 290, idPlato: 51, nombrePlato: "Copa de Vino Blanco", categoria: "Bebidas", fecha: hace(6311), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 293, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(5982), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 293, idPlato: 11, nombrePlato: "Pisco Sour", categoria: "Bebidas", fecha: hace(5952), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 300, idPlato: 32, nombrePlato: "Papa a la Huancaína", categoria: "Entradas", fecha: hace(4889), estrellas: 5, comentario: "Fresco y con muy buena sazón." },
    { idPedido: 300, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(4884), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 301, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(4849), estrellas: 5, comentario: "Fresco y con muy buena sazón." },
    { idPedido: 302, idPlato: 4, nombrePlato: "Cebiche Nikkei", categoria: "Cebiches", fecha: hace(4500), estrellas: 4, comentario: "Muy rico, solo le faltó un poco de ají." },
    { idPedido: 304, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(3563), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 304, idPlato: 37, nombrePlato: "Chicharrón de Pescado", categoria: "Fondos", fecha: hace(3544), estrellas: 4 },
    { idPedido: 306, idPlato: 14, nombrePlato: "Cebiche Carretillero", categoria: "Cebiches", fecha: hace(3392), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 306, idPlato: 49, nombrePlato: "Gaseosa (botella)", categoria: "Bebidas", fecha: hace(3388), estrellas: 5 },
    { idPedido: 310, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(3085), estrellas: 5, comentario: "Fresco y con muy buena sazón." },
    { idPedido: 310, idPlato: 38, nombrePlato: "Parihuela", categoria: "Fondos", fecha: hace(3080), estrellas: 2 },
    { idPedido: 316, idPlato: 46, nombrePlato: "Limonada Clásica", categoria: "Bebidas", fecha: hace(1967), estrellas: 5 },
    { idPedido: 319, idPlato: 5, nombrePlato: "Tiradito Bachiche", categoria: "Tiraditos", fecha: hace(1710), estrellas: 5, comentario: "Perfecto punto de sazón." },
    { idPedido: 319, idPlato: 42, nombrePlato: "Crema Volteada", categoria: "Postres", fecha: hace(1679), estrellas: 4 },
    { idPedido: 322, idPlato: 9, nombrePlato: "Chicha Morada (jarra)", categoria: "Bebidas", fecha: hace(1609), estrellas: 3, comentario: "Normal, esperaba un poco más." },
    { idPedido: 322, idPlato: 45, nombrePlato: "Chilcano de Pisco", categoria: "Bebidas", fecha: hace(1591), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 324, idPlato: 2, nombrePlato: "Cebiche Criollo", categoria: "Cebiches", fecha: hace(1545), estrellas: 2, comentario: "Porción pequeña para el precio." },
    { idPedido: 324, idPlato: 4, nombrePlato: "Cebiche Nikkei", categoria: "Cebiches", fecha: hace(1541), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 325, idPlato: 4, nombrePlato: "Cebiche Nikkei", categoria: "Cebiches", fecha: hace(1540), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 325, idPlato: 51, nombrePlato: "Copa de Vino Blanco", categoria: "Bebidas", fecha: hace(1523), estrellas: 3 },
    { idPedido: 325, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(1521), estrellas: 5, comentario: "Uno de los mejores de la carta." },
    { idPedido: 329, idPlato: 45, nombrePlato: "Chilcano de Pisco", categoria: "Bebidas", fecha: hace(508), estrellas: 5 },
    { idPedido: 329, idPlato: 16, nombrePlato: "Cebiche de Salmón", categoria: "Cebiches", fecha: hace(499), estrellas: 5 },
    { idPedido: 329, idPlato: 1, nombrePlato: "Cebiche Mixto", categoria: "Cebiches", fecha: hace(496), estrellas: 4 },
    { idPedido: 334, idPlato: 44, nombrePlato: "Maracuyá Sour", categoria: "Bebidas", fecha: hace(182), estrellas: 4, comentario: "Buen sabor y buena presentación." },
    { idPedido: 335, idPlato: 38, nombrePlato: "Parihuela", categoria: "Fondos", fecha: hace(171), estrellas: 4, comentario: "Rico, aunque la porción podría ser mayor." },
    { idPedido: 335, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(168), estrellas: 5 },
    { idPedido: 334, idPlato: 10, nombrePlato: "Limonada Frozen", categoria: "Bebidas", fecha: hace(167), estrellas: 4, comentario: "Bien en general, lo recomiendo." },
    { idPedido: 335, idPlato: 4, nombrePlato: "Cebiche Nikkei", categoria: "Cebiches", fecha: hace(130), estrellas: 4, comentario: "Bien en general, lo recomiendo." }
]);

// ---------------------------------------------------------------------
// 4. VERIFICACION
// ---------------------------------------------------------------------
print('\n=== Documentos por coleccion ===');
printjson({ encuestas: db.encuestas.countDocuments(), resenas: db.resenas.countDocuments() });

print('\n=== Indices ===');
printjson(db.encuestas.getIndexes().map(i => i.name));
printjson(db.resenas.getIndexes().map(i => i.name));

// ---------------------------------------------------------------------
// 5. CONSULTAS DE AGREGACION (las mismas que usara el panel del Administrador)
// ---------------------------------------------------------------------
print('\n--- A) Promedios generales de las encuestas ---');
db.encuestas.aggregate([
  { $group: {
      _id: null,
      encuestas:    { $sum: 1 },
      comida:       { $avg: '$calificaciones.comida' },
      servicio:     { $avg: '$calificaciones.servicio' },
      ambiente:     { $avg: '$calificaciones.ambiente' },
      tiempoEspera: { $avg: '$calificaciones.tiempoEspera' },
      recomiendan:  { $sum: { $cond: ['$recomendaria', 1, 0] } } } },
  { $project: {
      _id: 0, encuestas: 1,
      comida: { $round: ['$comida', 2] }, servicio: { $round: ['$servicio', 2] },
      ambiente: { $round: ['$ambiente', 2] }, tiempoEspera: { $round: ['$tiempoEspera', 2] },
      pctRecomienda: { $round: [{ $multiply: [{ $divide: ['$recomiendan', '$encuestas'] }, 100] }, 1] } } }
]).forEach(d => printjson(d));

print('\n--- B) Ranking de empleados por calificacion de servicio ---');
db.encuestas.aggregate([
  { $group: { _id: { id: '$idEmpleado', nombre: '$mozo' }, encuestas: { $sum: 1 }, servicio: { $avg: '$calificaciones.servicio' } } },
  { $match: { encuestas: { $gte: 3 } } },
  { $project: { _id: 0, idEmpleado: '$_id.id', mozo: '$_id.nombre', encuestas: 1, servicio: { $round: ['$servicio', 2] } } },
  { $sort: { servicio: -1, encuestas: -1 } }
]).forEach(d => printjson(d));

print('\n--- C) Distribucion de estrellas de "comida" ---');
db.encuestas.aggregate([
  { $group: { _id: '$calificaciones.comida', cantidad: { $sum: 1 } } },
  { $sort: { _id: -1 } }
]).forEach(d => printjson(d));

print('\n--- D) Etiquetas mas frecuentes ---');
db.encuestas.aggregate([
  { $unwind: '$etiquetas' },
  { $group: { _id: '$etiquetas', veces: { $sum: 1 } } },
  { $sort: { veces: -1 } },
  { $limit: 5 }
]).forEach(d => printjson(d));

print('\n--- E) Platos mejor calificados (minimo 3 resenas) ---');
db.resenas.aggregate([
  { $group: { _id: { id: '$idPlato', nombre: '$nombrePlato' }, resenas: { $sum: 1 }, promedio: { $avg: '$estrellas' } } },
  { $match: { resenas: { $gte: 3 } } },
  { $project: { _id: 0, idPlato: '$_id.id', plato: '$_id.nombre', resenas: 1, promedio: { $round: ['$promedio', 2] } } },
  { $sort: { promedio: -1, resenas: -1 } },
  { $limit: 5 }
]).forEach(d => printjson(d));

print('\n--- F) Platos peor calificados (minimo 3 resenas) ---');
db.resenas.aggregate([
  { $group: { _id: { id: '$idPlato', nombre: '$nombrePlato' }, resenas: { $sum: 1 }, promedio: { $avg: '$estrellas' } } },
  { $match: { resenas: { $gte: 3 } } },
  { $project: { _id: 0, idPlato: '$_id.id', plato: '$_id.nombre', resenas: 1, promedio: { $round: ['$promedio', 2] } } },
  { $sort: { promedio: 1, resenas: -1 } },
  { $limit: 5 }
]).forEach(d => printjson(d));

print('\n--- G) Promedio de resenas por categoria ---');
db.resenas.aggregate([
  { $group: { _id: '$categoria', resenas: { $sum: 1 }, promedio: { $avg: '$estrellas' } } },
  { $project: { _id: 0, categoria: '$_id', resenas: 1, promedio: { $round: ['$promedio', 2] } } },
  { $sort: { promedio: -1 } }
]).forEach(d => printjson(d));

print('\n--- H) Ultimos 3 comentarios de resenas ---');
db.resenas.find({ comentario: { $exists: true } }, { _id: 0, nombrePlato: 1, estrellas: 1, comentario: 1, fecha: 1 })
  .sort({ fecha: -1 }).limit(3).forEach(d => printjson(d));
// ---------------------------------------------------------------------
// 0. USUARIO DE LA APLICACION
// Solo puede leer y escribir en LaMarFeedback (no ve otras bases).
// Debe coincidir con MONGO_URI en .vscode/launch.json.
// ---------------------------------------------------------------------
if (db.getUser('lamar_app')) {
  db.dropUser('lamar_app');
}
db.createUser({
  user: 'lamar_app',
  pwd: 'LaMarMongo2026',
  roles: [{ role: 'readWrite', db: 'LaMarFeedback' }]
});
