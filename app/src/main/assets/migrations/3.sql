ALTER TABLE PlayContexts ADD COLUMN volume integer NOT NULL DEFAULT 100;
UPDATE PlayContexts SET volume = ( SELECT cast(value as integer) FROM Configs WHERE key = 'volume' );
DELETE FROM Configs WHERE key = 'volume';
