create user '${jdbcUsername}' IDENTIFIED BY '${jdbcPassword}';
GRANT ALL ON ${jdbcUsername}.* TO '${jdbcUsername}'@'%' IDENTIFIED BY '${jdbcPassword}';
GRANT ALL ON ${jdbcUsername}.* TO '${jdbcUsername}'@'localhost' IDENTIFIED BY '${jdbcPassword}';