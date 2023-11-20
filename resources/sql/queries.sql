-- :name save-endorsement! :! :n
-- :doc saves a new endorsement record using the keys
INSERT INTO endorsements
(name, subject, message)
VALUES (:name, :subject, :statement)

-- :name get-endorsements :? :*
-- :doc selects all endorsements
SELECT * FROM endorsements

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

