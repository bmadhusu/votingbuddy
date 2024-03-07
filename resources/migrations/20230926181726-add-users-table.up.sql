CREATE TABLE users
(login text PRIMARY KEY,
 id SERIAL UNIQUE not null,
 first_name text,
 last_name text,
 password text not null,
 is_admin BOOLEAN,
 last_login TIMESTAMP,
 is_active BOOLEAN,
 created_at TIMESTAMP not null DEFAULT now());

