create database ${sonar.jdbc.username};
USE ${sonar.jdbc.username};
sp_addalias ${sonar.jdbc.username}, dbo;
