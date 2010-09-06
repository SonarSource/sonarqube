CREATE USER ${sonar.jdbc.username} IDENTIFIED BY ${sonar.jdbc.password} DEFAULT TABLESPACE USERS ACCOUNT UNLOCK;
GRANT CONNECT TO ${sonar.jdbc.username};
GRANT RESOURCE TO ${sonar.jdbc.username};
GRANT CREATE TABLE to ${sonar.jdbc.username};
GRANT CREATE SEQUENCE to ${sonar.jdbc.username};