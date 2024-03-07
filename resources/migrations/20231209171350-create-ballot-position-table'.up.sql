CREATE TABLE ballotpositions
(propid INTEGER REFERENCES ballotpropositions(id) not null,
author TEXT REFERENCES users(login) on delete set null on update cascade,
fromcandidateid INTEGER REFERENCES candidates(id),
electionid INTEGER REFERENCES elections(id) not null,
position boolean not null,
statement text,
active boolean,
timestamp TIMESTAMP not null DEFAULT now());