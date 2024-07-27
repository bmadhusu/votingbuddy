CREATE TABLE electionsoffices
(officeID INTEGER REFERENCES offices(id) on delete set null on update cascade,
electionID INTEGER REFERENCES elections(id) on delete set null on update cascade,
PRIMARY KEY (officeID, electionID ));