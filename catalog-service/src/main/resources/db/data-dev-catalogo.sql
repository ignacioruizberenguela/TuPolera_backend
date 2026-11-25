-- DATOS DE EJEMPLO: se cargan SOLO con el perfil "dev" (ver application-dev.yml).
-- Equivalen a las categorías y productos iniciales del frontend. INSERT IGNORE lo hace repetible:
-- al reiniciar no se pisan los cambios (por ejemplo, el stock) hechos después.
-- Las imágenes son los nombres de imagen que el frontend ya resuelve (imagen1, imagen2, ...).

INSERT IGNORE INTO categoria (id, nombre, descripcion, imagen, activa) VALUES (1, 'Poleras', 'Poleras básicas, cómodas y a precios accesibles.', 'imagen1', 1);
INSERT IGNORE INTO categoria_talla (categoria_id, orden, talla) VALUES (1, 0, 'S');
INSERT IGNORE INTO categoria_talla (categoria_id, orden, talla) VALUES (1, 1, 'M');
INSERT IGNORE INTO categoria_talla (categoria_id, orden, talla) VALUES (1, 2, 'L');
INSERT IGNORE INTO categoria_talla (categoria_id, orden, talla) VALUES (1, 3, 'XL');
INSERT IGNORE INTO categoria_atributo (categoria_id, clave, nombre, tipo, obligatorio, orden) VALUES (1, 'tipo_manga', 'Tipo de manga', 'seleccion', 1, 0);
INSERT IGNORE INTO categoria_atributo_opcion (atributo_id, orden, opcion) VALUES ((SELECT id FROM categoria_atributo WHERE categoria_id = 1 AND clave = 'tipo_manga'), 0, 'Corta');
INSERT IGNORE INTO categoria_atributo_opcion (atributo_id, orden, opcion) VALUES ((SELECT id FROM categoria_atributo WHERE categoria_id = 1 AND clave = 'tipo_manga'), 1, 'Larga');
INSERT IGNORE INTO categoria_medida (categoria_id, clave, nombre, unidad, solicitar_cliente, registrar_producto, orden) VALUES (1, 'contorno_pecho', 'Contorno de pecho', 'cm', 1, 1, 0);
INSERT IGNORE INTO categoria_medida (categoria_id, clave, nombre, unidad, solicitar_cliente, registrar_producto, orden) VALUES (1, 'ancho_hombros', 'Ancho de hombros', 'cm', 1, 1, 1);
INSERT IGNORE INTO categoria_medida (categoria_id, clave, nombre, unidad, solicitar_cliente, registrar_producto, orden) VALUES (1, 'largo_prenda', 'Largo de prenda', 'cm', 0, 1, 2);
INSERT IGNORE INTO categoria (id, nombre, descripcion, imagen, activa) VALUES (2, 'Polerones', 'Polerones abrigadores para el día a día.', 'imagen3', 1);
INSERT IGNORE INTO categoria_talla (categoria_id, orden, talla) VALUES (2, 0, 'S');
INSERT IGNORE INTO categoria_talla (categoria_id, orden, talla) VALUES (2, 1, 'M');
INSERT IGNORE INTO categoria_talla (categoria_id, orden, talla) VALUES (2, 2, 'L');
INSERT IGNORE INTO categoria_talla (categoria_id, orden, talla) VALUES (2, 3, 'XL');
INSERT IGNORE INTO categoria_atributo (categoria_id, clave, nombre, tipo, obligatorio, orden) VALUES (2, 'tipo_capucha', 'Tipo de capucha', 'seleccion', 1, 0);
INSERT IGNORE INTO categoria_atributo_opcion (atributo_id, orden, opcion) VALUES ((SELECT id FROM categoria_atributo WHERE categoria_id = 2 AND clave = 'tipo_capucha'), 0, 'Con capucha');
INSERT IGNORE INTO categoria_atributo_opcion (atributo_id, orden, opcion) VALUES ((SELECT id FROM categoria_atributo WHERE categoria_id = 2 AND clave = 'tipo_capucha'), 1, 'Sin capucha');
INSERT IGNORE INTO categoria_medida (categoria_id, clave, nombre, unidad, solicitar_cliente, registrar_producto, orden) VALUES (2, 'contorno_pecho', 'Contorno de pecho', 'cm', 1, 1, 0);
INSERT IGNORE INTO categoria_medida (categoria_id, clave, nombre, unidad, solicitar_cliente, registrar_producto, orden) VALUES (2, 'ancho_hombros', 'Ancho de hombros', 'cm', 1, 1, 1);
INSERT IGNORE INTO categoria_medida (categoria_id, clave, nombre, unidad, solicitar_cliente, registrar_producto, orden) VALUES (2, 'largo_prenda', 'Largo de prenda', 'cm', 0, 1, 2);
INSERT IGNORE INTO categoria (id, nombre, descripcion, imagen, activa) VALUES (3, 'Gorras', 'Gorras y gorros de uso diario en talla única.', 'imagen5', 1);
INSERT IGNORE INTO categoria_talla (categoria_id, orden, talla) VALUES (3, 0, 'Única');
INSERT IGNORE INTO categoria_atributo (categoria_id, clave, nombre, tipo, obligatorio, orden) VALUES (3, 'tipo_gorro', 'Tipo de gorro', 'seleccion', 1, 0);
INSERT IGNORE INTO categoria_atributo_opcion (atributo_id, orden, opcion) VALUES ((SELECT id FROM categoria_atributo WHERE categoria_id = 3 AND clave = 'tipo_gorro'), 0, 'Jockey');
INSERT IGNORE INTO categoria_atributo_opcion (atributo_id, orden, opcion) VALUES ((SELECT id FROM categoria_atributo WHERE categoria_id = 3 AND clave = 'tipo_gorro'), 1, 'Trucker');
INSERT IGNORE INTO categoria_atributo_opcion (atributo_id, orden, opcion) VALUES ((SELECT id FROM categoria_atributo WHERE categoria_id = 3 AND clave = 'tipo_gorro'), 2, 'Beanie');
INSERT IGNORE INTO categoria_medida (categoria_id, clave, nombre, unidad, solicitar_cliente, registrar_producto, orden) VALUES (3, 'contorno_cabeza', 'Contorno de cabeza', 'cm', 1, 1, 0);
INSERT IGNORE INTO categoria_medida (categoria_id, clave, nombre, unidad, solicitar_cliente, registrar_producto, orden) VALUES (3, 'profundidad', 'Profundidad', 'cm', 0, 1, 1);

INSERT IGNORE INTO producto (id, codigo, nombre, descripcion, precio, descuento, color, categoria_id, imagen, stock_critico, activo, fecha_creacion, fecha_actualizacion) VALUES (1, 'POL-001', 'Urban Red', 'Polera liviana y cómoda, ideal para un estilo casual y urbano.', 12990, 50, 'rojo', 1, 'imagen1', 5, 1, NOW(), NOW());
INSERT IGNORE INTO producto_atributo (producto_id, clave, valor) VALUES (1, 'tipo_manga', 'Corta');
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (1, 'S', 5, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'S'), 'contorno_pecho', 96);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'S'), 'ancho_hombros', 42);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'S'), 'largo_prenda', 66);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (1, 'M', 10, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'M'), 'contorno_pecho', 102);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'M'), 'ancho_hombros', 44);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'M'), 'largo_prenda', 68);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (1, 'L', 8, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'L'), 'contorno_pecho', 108);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'L'), 'ancho_hombros', 46);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'L'), 'largo_prenda', 70);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (1, 'XL', 3, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'XL'), 'contorno_pecho', 114);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'XL'), 'ancho_hombros', 48);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 1 AND talla = 'XL'), 'largo_prenda', 72);

INSERT IGNORE INTO producto (id, codigo, nombre, descripcion, precio, descuento, color, categoria_id, imagen, stock_critico, activo, fecha_creacion, fecha_actualizacion) VALUES (2, 'POL-002', 'Black Street', 'Diseño de manga larga pensado para complementar outfits modernos.', 14990, 10, 'negro', 1, 'imagen2', 5, 1, NOW(), NOW());
INSERT IGNORE INTO producto_atributo (producto_id, clave, valor) VALUES (2, 'tipo_manga', 'Larga');
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (2, 'S', 6, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'S'), 'contorno_pecho', 97);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'S'), 'ancho_hombros', 43);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'S'), 'largo_prenda', 67);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (2, 'M', 12, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'M'), 'contorno_pecho', 103);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'M'), 'ancho_hombros', 45);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'M'), 'largo_prenda', 69);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (2, 'L', 4, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'L'), 'contorno_pecho', 109);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'L'), 'ancho_hombros', 47);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'L'), 'largo_prenda', 71);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (2, 'XL', 0, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'XL'), 'contorno_pecho', 115);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'XL'), 'ancho_hombros', 49);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 2 AND talla = 'XL'), 'largo_prenda', 73);

INSERT IGNORE INTO producto (id, codigo, nombre, descripcion, precio, descuento, color, categoria_id, imagen, stock_critico, activo, fecha_creacion, fecha_actualizacion) VALUES (3, 'PLN-001', 'Black Hoodie', 'Prenda abrigadora y versátil para disfrutar los días más fríos.', 24990, 25, 'negro', 2, 'imagen3', 5, 1, NOW(), NOW());
INSERT IGNORE INTO producto_atributo (producto_id, clave, valor) VALUES (3, 'tipo_capucha', 'Con capucha');
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (3, 'S', 2, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'S'), 'contorno_pecho', 108);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'S'), 'ancho_hombros', 46);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'S'), 'largo_prenda', 66);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (3, 'M', 9, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'M'), 'contorno_pecho', 114);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'M'), 'ancho_hombros', 48);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'M'), 'largo_prenda', 68);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (3, 'L', 7, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'L'), 'contorno_pecho', 120);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'L'), 'ancho_hombros', 50);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'L'), 'largo_prenda', 70);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (3, 'XL', 5, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'XL'), 'contorno_pecho', 126);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'XL'), 'ancho_hombros', 52);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 3 AND talla = 'XL'), 'largo_prenda', 72);

INSERT IGNORE INTO producto (id, codigo, nombre, descripcion, precio, descuento, color, categoria_id, imagen, stock_critico, activo, fecha_creacion, fecha_actualizacion) VALUES (4, 'PLN-002', 'Red Classic', 'Estilo sencillo y cómodo que combina fácilmente con distintos looks.', 22990, 15, 'rojo', 2, 'imagen4', 5, 1, NOW(), NOW());
INSERT IGNORE INTO producto_atributo (producto_id, clave, valor) VALUES (4, 'tipo_capucha', 'Sin capucha');
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (4, 'S', 4, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'S'), 'contorno_pecho', 106);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'S'), 'ancho_hombros', 45);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'S'), 'largo_prenda', 65);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (4, 'M', 8, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'M'), 'contorno_pecho', 112);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'M'), 'ancho_hombros', 47);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'M'), 'largo_prenda', 67);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (4, 'L', 6, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'L'), 'contorno_pecho', 118);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'L'), 'ancho_hombros', 49);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'L'), 'largo_prenda', 69);
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (4, 'XL', 1, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'XL'), 'contorno_pecho', 124);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'XL'), 'ancho_hombros', 51);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 4 AND talla = 'XL'), 'largo_prenda', 71);

INSERT IGNORE INTO producto (id, codigo, nombre, descripcion, precio, descuento, color, categoria_id, imagen, stock_critico, activo, fecha_creacion, fecha_actualizacion) VALUES (5, 'GOR-001', 'Yellow Trucker', 'Gorra fresca y llamativa, perfecta para complementar un look casual.', 8990, 20, 'amarillo', 3, 'imagen5', 5, 1, NOW(), NOW());
INSERT IGNORE INTO producto_atributo (producto_id, clave, valor) VALUES (5, 'tipo_gorro', 'Trucker');
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (5, 'Única', 14, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 5 AND talla = 'Única'), 'contorno_cabeza', 58);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 5 AND talla = 'Única'), 'profundidad', 11);

INSERT IGNORE INTO producto (id, codigo, nombre, descripcion, precio, descuento, color, categoria_id, imagen, stock_critico, activo, fecha_creacion, fecha_actualizacion) VALUES (6, 'GOR-002', 'Black Jockey', 'Diseño clásico y versátil para usar en cualquier ocasión.', 8990, 10, 'negro', 3, 'imagen6', 5, 1, NOW(), NOW());
INSERT IGNORE INTO producto_atributo (producto_id, clave, valor) VALUES (6, 'tipo_gorro', 'Jockey');
INSERT IGNORE INTO producto_variante (producto_id, talla, stock, version) VALUES (6, 'Única', 3, 0);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 6 AND talla = 'Única'), 'contorno_cabeza', 57);
INSERT IGNORE INTO variante_medida (variante_id, clave, valor) VALUES ((SELECT id FROM producto_variante WHERE producto_id = 6 AND talla = 'Única'), 'profundidad', 10);

