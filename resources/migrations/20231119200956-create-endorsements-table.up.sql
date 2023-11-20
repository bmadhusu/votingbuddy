CREATE TABLE endorsements
(id SERIAL PRIMARY KEY,
name INTEGER references candidates(id),
subject text,
statement text,
timestamp TIMESTAMP not null DEFAULT now());