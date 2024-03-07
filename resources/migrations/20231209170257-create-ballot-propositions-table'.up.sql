CREATE TABLE ballotpropositions
(id SERIAL PRIMARY KEY,
author TEXT REFERENCES users(login) on delete set null on update cascade,
propositiontext TEXT not null,
electionid INTEGER REFERENCES elections(id) not null,
active boolean,
timestamp TIMESTAMP not null DEFAULT now());