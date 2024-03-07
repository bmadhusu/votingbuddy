CREATE TABLE offices
(id SERIAL UNIQUE not null,
ocdid TEXT not null,
officeName TEXT not null,
role TEXT not null,
level TEXT not null,
incumbent JSONB not null,
timestamp TIMESTAMP not null DEFAULT now(),
PRIMARY KEY (ocdid, officeName, role, level, incumbent));