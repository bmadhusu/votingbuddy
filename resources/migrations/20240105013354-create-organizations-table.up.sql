CREATE TABLE organizations
(EIN TEXT not null,
id SERIAL UNIQUE not null,
primaryUserID TEXT REFERENCES users(login) on delete set null on update cascade,
orgName TEXT not null,
orgType TEXT not null,
is_vetted boolean,
statement TEXT,
created_at TIMESTAMP not null DEFAULT now(),
PRIMARY KEY (EIN));