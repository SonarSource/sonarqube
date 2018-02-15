/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.startup;

import org.picocontainer.Startable;
import org.sonar.db.Database;
import org.sonar.db.dialect.MySql;

/**
 * This class will be check the configuration for a SonarQube cluster
 *
 * See SONAR-10420
 */
public class ClusterConfigurationCheck implements Startable {

  private final Database database;

  public ClusterConfigurationCheck(Database database) {
    this.database = database;
  }

  @Override
  public void start() {
    if (MySql.ID.equals(database.getDialect().getId())) {
      throw new IllegalStateException("MySQL is not supported for Data Center Edition. Please connect to a supported database: Oracle, PostgreSQL, Microsoft SQL Server.");
    }
  }

  public void stop() {
    // Nothing to do
  }
}
