-- USUARIOS DE PRUEBA: se cargan SOLO con el perfil "dev" (ver application-dev.yml).
-- Las contraseñas están almacenadas con BCrypt.
--   juan@gmail.com    (CLIENTE)         contraseña: 123456
--   pedro@gmail.com   (VENDEDOR)        contraseña: 123456
--   adminn@gmail.com  (ADMINISTRADOR)   contraseña: Admin#Polera2026!
-- Son datos de desarrollo: cambia o elimina estas cuentas antes de cualquier uso real.

INSERT IGNORE INTO usuario
    (id, run, nombre, apellidos, correo, password, region_id, comuna_id, rol, activo, fecha_creacion)
VALUES
    (1, '36322977', 'Juan', NULL, 'juan@gmail.com',
     '$2b$10$zcHl5G7w9XF2R2w3clnwXujN7Nic.bdSMoq//xUkjPR316eFTO6lm',
     (SELECT id FROM region WHERE nombre = 'Metropolitana de Santiago'),
     (SELECT c.id FROM comuna c JOIN region r ON r.id = c.region_id
       WHERE r.nombre = 'Metropolitana de Santiago' AND c.nombre = 'Santiago'),
     'CLIENTE', 1, NOW()),
    (2, '347985368', 'Pedro', NULL, 'pedro@gmail.com',
     '$2b$10$zcHl5G7w9XF2R2w3clnwXujN7Nic.bdSMoq//xUkjPR316eFTO6lm',
     (SELECT id FROM region WHERE nombre = 'Biobío'),
     (SELECT c.id FROM comuna c JOIN region r ON r.id = c.region_id
       WHERE r.nombre = 'Biobío' AND c.nombre = 'Concepción'),
     'VENDEDOR', 1, NOW()),
    (3, '79679801', 'Admin', NULL, 'adminn@gmail.com',
     '$2b$10$PWVNPmSLH.mr.3V/zHqGxOd2jr.PA7BAWM2AhRW77m4hHdLh2nxru',
     (SELECT id FROM region WHERE nombre = 'Biobío'),
     (SELECT c.id FROM comuna c JOIN region r ON r.id = c.region_id
       WHERE r.nombre = 'Biobío' AND c.nombre = 'Concepción'),
     'ADMINISTRADOR', 1, NOW());
