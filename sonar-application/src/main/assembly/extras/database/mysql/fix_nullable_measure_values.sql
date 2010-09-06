# this script must be executed when the column PROJECT_MEASURES.VALUE is NOT NULL. 
# It occurs sometimes when database schema has been created before Sonar 1.8

ALTER TABLE sonar.project_measures MODIFY COLUMN value DECIMAL(30,20) DEFAULT NULL;
  