/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.jpa.session;

import org.sonar.api.utils.SonarException;
import org.sonar.core.persistence.Database;

import java.sql.Connection;
import java.sql.SQLException;

public class DefaultDatabaseConnector extends AbstractDatabaseConnector {

  public DefaultDatabaseConnector(Database database) {
    super(database);
  }

  @Override
  public void start() {
    createDatasource();
    super.start();
  }

  private void createDatasource() {
    try {
      CustomHibernateConnectionProvider.setDatasourceForConfig(database.getDataSource());
    } catch (Exception e) {
      throw new SonarException("Fail to connect to database", e);
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return database != null && database.getDataSource() != null ? database.getDataSource().getConnection() : null;
  }
}
