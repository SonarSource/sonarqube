create user '${sonar.jdbc.username}' IDENTIFIED BY '${sonar.jdbc.password}';
GRANT ALL ON ${sonar.jdbc.username}.* TO '${sonar.jdbc.username}'@'%' IDENTIFIED BY '${sonar.jdbc.password}';
GRANT ALL ON ${sonar.jdbc.username}.* TO '${sonar.jdbc.username}'@'localhost' IDENTIFIED BY '${sonar.jdbc.password}';