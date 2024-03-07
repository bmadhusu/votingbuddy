DO $$
DECLARE
rec RECORD;
BEGIN
FOR rec IN (SELECT tablename FROM pg_tables WHERE tableowner = 'votingbuddy' AND tablename <> 'schema_migrations') LOOP
EXECUTE 'DROP TABLE IF EXISTS ' || rec.tablename || ' CASCADE';
END LOOP;
EXECUTE 'DELETE FROM schema_migrations';
END;
$$