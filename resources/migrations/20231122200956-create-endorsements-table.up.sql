CREATE TABLE endorsements
(id SERIAL PRIMARY KEY,
endorserType TEXT,
linkedID INTEGER,
forcandidateid INTEGER REFERENCES candidates(id),
electionid INTEGER REFERENCES elections(id),
sourceUserID TEXT REFERENCES users(login) on delete set null on update cascade,
subject text,
statement text,
is_active boolean,
is_visible boolean,
timestamp TIMESTAMP not null DEFAULT now());