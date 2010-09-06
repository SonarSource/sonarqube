#
# Drop Sonar database and user.
#
# Command: mysql -u root -p < drop_database.sql
#

DROP DATABASE IF EXISTS sonar;
DROP USER 'sonar'@'localhost';
DROP USER 'sonar'@'%';
