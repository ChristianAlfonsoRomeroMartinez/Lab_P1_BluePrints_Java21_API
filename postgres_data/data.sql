-- Data test

INSERT INTO blueprints (author, name) 
VALUES ('john', 'house');

-- Obtener el ID autogenerado del blueprint recién insertado
INSERT INTO points (x, y, point_order, blueprint_id) VALUES
    (0,  0,  0, (SELECT id FROM blueprints WHERE author='john' AND name='house')),
    (10, 0,  1, (SELECT id FROM blueprints WHERE author='john' AND name='house')),
    (10, 10, 2, (SELECT id FROM blueprints WHERE author='john' AND name='house')),
    (0,  10, 3, (SELECT id FROM blueprints WHERE author='john' AND name='house'));

-- john/garage 

INSERT INTO blueprints (author, name) 
VALUES ('john', 'garage');

INSERT INTO points (x, y, point_order, blueprint_id) VALUES
    (5,  5,  0, (SELECT id FROM blueprints WHERE author='john' AND name='garage')),
    (15, 5,  1, (SELECT id FROM blueprints WHERE author='john' AND name='garage')),
    (15, 15, 2, (SELECT id FROM blueprints WHERE author='john' AND name='garage'));

-- jane/garden

INSERT INTO blueprints (author, name) 
VALUES ('jane', 'garden');

INSERT INTO points (x, y, point_order, blueprint_id) VALUES
    (2, 2, 0, (SELECT id FROM blueprints WHERE author='jane' AND name='garden')),
    (3, 4, 1, (SELECT id FROM blueprints WHERE author='jane' AND name='garden')),
    (6, 7, 2, (SELECT id FROM blueprints WHERE author='jane' AND name='garden'));
