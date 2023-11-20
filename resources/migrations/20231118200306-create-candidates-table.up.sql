CREATE TABLE candidates
(id SERIAL PRIMARY KEY,
name text not null,
statement text not null,
timestamp TIMESTAMP not null DEFAULT now());

-- add photo later
-- add url to official website
-- add socials
-- add links to other testimonials or articles, etc.
-- add breaking news or top of mind or pinned   statement