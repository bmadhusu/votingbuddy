CREATE TABLE users
(login text PRIMARY KEY,
 first_name text,
 last_name text,
 admin BOOLEAN,
 last_login TIMESTAMP,
 is_active BOOLEAN,
 password text not null,
 created_at TIMESTAMP not null DEFAULT now());

