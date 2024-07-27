-- :name save-endorsement! :! :n
-- :doc saves a new endorsement record using the keys
INSERT INTO endorsements
(forcandidateid, linkedid, electionid, endorsertype, sourceuserid, is_active, is_visible, subject, statement)
VALUES (:forcandidateid, :linkedid, :electionid, :endorsertype, :sourceuserid, :is_active, :is_visible, :subject, :statement)
RETURNING *;


-- :name get-endorsements :? :*
-- :doc selects all endorsements
SELECT c.candidateName, e.statement, subject, orgname, e.timestamp FROM endorsements e INNER JOIN organizations o
ON e.linkedID = o.id INNER JOIN candidates c
ON e.forcandidateid = c.id

-- :name get-endorsements-by-office :? :*
-- :doc selects all endorsements by office
SELECT c.candidateName, of.*, e.statement, subject, orgname, e.timestamp
FROM endorsements e INNER JOIN organizations org
ON e.linkedID = org.id INNER JOIN candidates c
ON e.forcandidateid = c.id inner join offices of
on c.officeid = of.id inner join electionsoffices eo
on of.id = eo.officeid inner join elections el
on eo.electionid = el.id 
where el.id = :electionID
and of.office_info @> any(array[:offices]::jsonb[])
--and of.office_info @> any(:offices)::jsonb[]
--and of.office_info in (:v*:offices)


-- :name get-endorsements-by-author :? :*
-- :doc selects all endorsements posted by author
SELECT c.candidateName, e.statement, subject, orgname, e.timestamp FROM endorsements e INNER JOIN organizations o
ON e.linkedID = o.id INNER JOIN candidates c
ON e.forcandidateid = c.id INNER JOIN offices of
ON c.officeid = of.id INNER JOIN electionsoffices eo
ON of.id = eo.officeid INNER JOIN elections el
ON eo.electionid = el.id
WHERE el.id = :electionID
AND orgname = :author

-- :name get-endorsements-by-organization :? :*
-- :doc selects all endorsements posted by an org
SELECT c.candidateName, e.statement, subject, orgname, e.timestamp FROM endorsements e INNER JOIN organizations o
ON e.linkedID = o.id INNER JOIN candidates c
ON e.forcandidateid = c.id
WHERE endorsertype = 'organization' and linkedid = :orgID

-- :name get-endorsements-by-candidate :? :*
-- :doc selects all endorsements posted by an org
SELECT c.candidateName, e.statement, subject, orgname, e.timestamp FROM endorsements e INNER JOIN organizations o
ON e.linkedID = o.id INNER JOIN candidates c
ON e.forcandidateid = c.id
WHERE endorsertype = 'candidate' and linkedid = :candID

-- :name get-endorsements-for-candidate :? :*
-- :doc selects all endorsements for a candidate
SELECT c.candidateName, e.statement, subject, orgname, e.timestamp FROM endorsements e INNER JOIN organizations o
ON e.linkedID = o.id INNER JOIN candidates c
ON e.forcandidateid = c.id
WHERE forcandidateid = :candID


-- :name save-candidate! :! :n
-- :doc saves a new candidate record using the keys
INSERT INTO candidates
(name, statement)
VALUES (:name, :statement)

-- :name get-candidates :? :*
-- :doc selects all candidates
SELECT * FROM candidates

-- :name create-user!* :! :n
-- :doc creates a new user with the provided login and hashed password
INSERT INTO users
(login, password)
VALUES (:login, :password)

-- :name get-user-for-auth* :? :1
-- :doc selects a user for authentication
SELECT * FROM users
WHERE login = :login

