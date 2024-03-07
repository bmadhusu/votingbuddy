CREATE TABLE candidates
(id SERIAL UNIQUE not null,
candidateName text not null,
party text,
officeID integer REFERENCES offices(id),
is_vetted boolean,
primaryuserid TEXT REFERENCES users(login) on delete set null on update cascade,
statement text not null,
timestamp TIMESTAMP not null DEFAULT now(),
PRIMARY KEY (candidateName, officeID ));

-- add delegated userIDs list
-- add OCD (maybe JSON)
-- soci
-- add photo later
-- add url to official website
-- add socials
-- add links to other testimonials or articles, etc.
-- add breaking news or top of mind or pinned statement