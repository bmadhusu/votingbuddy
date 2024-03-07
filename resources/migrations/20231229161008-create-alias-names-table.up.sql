CREATE TABLE aliasnames
(ocdid TEXT not null,
officialname TEXT not null,
altname TEXT not null,
created_at TIMESTAMP not null DEFAULT now());