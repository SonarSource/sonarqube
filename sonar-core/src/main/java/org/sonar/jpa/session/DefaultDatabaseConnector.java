/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.jpa.session;

import org.sonar.api.utils.SonarException;
import org.sonar.jpa.entity.SchemaMigration;
import org.sonar.persistence.Database;

import java.sql.Connection;
import java.sql.SQLException;

public class DefaultDatabaseConnector extends AbstractDatabaseConnector {

  public DefaultDatabaseConnector(Database database) {
    super(database);
  }

  @Override
  public boolean isOperational() {
    if (isStarted() && getDatabaseVersion() != SchemaMigration.LAST_VERSION) {
      // connector was started and connection OK but schema version was not OK
      // call start again to check if this is now ok (schema created by rails)
      start();
    }
    return super.isOperational();
  }

  @Override
  public void start() {
    if (!isStarted()) {
      createDatasource();
    }
    if (!super.isOperational()) {
      super.start();
    }
  }

  private void createDatasource() {
    try {
      CustomHibernateConnectionProvider.setDatasourceForConfig(database.getDataSource());
    } catch (Exception e) {
      throw new SonarException("Fail to connect to database", e);
    }
  }

  public Connection getConnection() throws SQLException {
    return database != null && database.getDataSource() != null ? database.getDataSource().getConnection() : null;
  }
}