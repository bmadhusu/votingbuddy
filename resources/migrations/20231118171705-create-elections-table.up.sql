CREATE TABLE elections
(id SERIAL PRIMARY KEY,
electiondate date not null,
state text not null,
level text not null,
is_active boolean,
timestamp TIMESTAMP not null DEFAULT now());