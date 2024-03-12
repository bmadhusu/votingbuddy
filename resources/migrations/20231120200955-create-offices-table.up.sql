CREATE TABLE offices
(id SERIAL UNIQUE not null,
office_info JSONB not null,
incumbent JSONB not null,
timestamp TIMESTAMP not null DEFAULT now(),
PRIMARY KEY (office_info, incumbent));