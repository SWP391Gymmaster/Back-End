USE swp391_group3;

SELECT LOWER(TRIM(email)) AS normalized_email, COUNT(*) AS matches,
       GROUP_CONCAT(id ORDER BY id) AS user_ids
FROM users
GROUP BY LOWER(TRIM(email))
HAVING COUNT(*) > 1;

SELECT LOWER(TRIM(user_name)) AS normalized_user_name, COUNT(*) AS matches,
       GROUP_CONCAT(id ORDER BY id) AS user_ids
FROM users
GROUP BY LOWER(TRIM(user_name))
HAVING COUNT(*) > 1;

SELECT id, user_name, email
FROM users
WHERE CHAR_LENGTH(TRIM(email)) > 254
   OR CHAR_LENGTH(TRIM(user_name)) NOT BETWEEN 3 AND 50
   OR CHAR_LENGTH(TRIM(full_name)) > 100
   OR CHAR_LENGTH(TRIM(phone)) > 30
   OR TRIM(user_name) REGEXP '[^A-Za-z0-9_.-]'
   OR TRIM(email) = '' OR TRIM(user_name) = '';

SHOW CREATE TABLE users;
