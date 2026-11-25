INSERT INTO reports (id, reporter_name, description, waste_category, latitude, longitude, address, status, created_at, updated_at) VALUES
  ('11111111-0000-0000-0000-000000000001', 'Amira', 'Mattresses and furniture dumped on the pavement', 'BULKY',
   36.8065, 10.1815, 'Rue de Marseille', 'SUBMITTED', now(), now()),
  ('11111111-0000-0000-0000-000000000002', 'Karim', 'Construction rubble behind the market', 'CONSTRUCTION',
   36.8190, 10.1658, 'Marche Central', 'SUBMITTED', now(), now()),
  ('11111111-0000-0000-0000-000000000003', 'Salma', 'Overflowing household waste next to the school', 'HOUSEHOLD',
   36.7948, 10.1730, 'Avenue de la Liberte', 'SUBMITTED', now(), now()),
  ('11111111-0000-0000-0000-000000000004', 'Youssef', 'Tyres left in a vacant lot', 'OTHER',
   36.8012, 10.1900, 'Lot 12', 'SUBMITTED', now(), now());

INSERT INTO collection_points (id, name, description, latitude, longitude, address, waste_types, active, created_at) VALUES
  ('cccccccc-0000-0000-0000-000000000001', 'Centre Ville Drop-off', 'Open 8:00-18:00', 36.8000, 10.1800,
   'Avenue Habib Bourguiba', 'BULKY,HOUSEHOLD', TRUE, now()),
  ('cccccccc-0000-0000-0000-000000000002', 'La Marsa Recycling Hub', 'Construction debris accepted', 36.8780, 10.3250,
   'Route de la Marsa', 'BULKY,CONSTRUCTION,OTHER', TRUE, now());
