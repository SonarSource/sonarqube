/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.database;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.database.DatabaseProperties;

public class EmbeddedDatabaseFactory {
  private Configuration configuration;
  private EmbeddedDatabase embeddedDatabase;

  public EmbeddedDatabaseFactory(Configuration configuration) {
    this.configuration = configuration;
  }

  public void start() {
    String jdbcUrl = configuration.getString(DatabaseProperties.PROP_URL);
    if ((jdbcUrl!=null) && jdbcUrl.startsWith("jdbc:derby://") && jdbcUrl.contains("create=true") && embeddedDatabase==null) {
      embeddedDatabase = new EmbeddedDatabase(configuration);
      embeddedDatabase.start();
    }
  }

  public void stop() {
    if (embeddedDatabase != null) {
      embeddedDatabase.stop();
    }
  }
}
