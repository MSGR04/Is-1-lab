
TRUNCATE TABLE tickets RESTART IDENTITY CASCADE;
TRUNCATE TABLE venues  RESTART IDENTITY CASCADE;
TRUNCATE TABLE events  RESTART IDENTITY CASCADE;
TRUNCATE TABLE persons RESTART IDENTITY CASCADE;

INSERT INTO persons (id, eyeColor, hairColor, location_x, location_y, location_z, passportID, nationality)
VALUES
    (1, 'RED',    'YELLOW', 10.5,  20.0,  30.5, 'PERS-0001', 'GERMANY'),
    (2, 'ORANGE', 'RED',    0.0,   1.0,   2.0,  'PERS-0002', 'JAPAN'),
    (3, NULL,     'YELLOW', -5.25, 99.0,  13.37,'PERS-0003', 'ITALY');

INSERT INTO events (id, name, tickets_count, eventType)
VALUES
    (1, 'ESports Final',  1200, 'E_SPORTS'),
    (2, 'Local Football', NULL, 'FOOTBALL'),
    (3, 'Art Expo 2026',  300,  'EXPOSITION');

INSERT INTO venues (id, name, capacity, type, address_zip_code, town_x, town_y, town_z)
VALUES
    (1, 'Mega Stadium',  50000, 'STADIUM', '1234567',  1.0,  2.0,  3.0),
    (2, 'Downtown Bar',  120,   'BAR',     NULL,      10.0, 11.0, 12.0),
    (3, 'City Theatre',  900,   'THEATRE', '7654321',  -7.0, 0.0,  7.0);

