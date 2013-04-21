/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.database;

import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.core.persistence.dialect.H2;

public class EmbeddedDatabaseFactory {
  private final Settings settings;
  private final H2 dialect;
  private EmbeddedDatabase embeddedDatabase;

  public EmbeddedDatabaseFactory(Settings settings) {
    this.settings = settings;
    dialect = new H2();
  }

  public void start() {
    if (embeddedDatabase == null) {
      String jdbcUrl = settings.getString(DatabaseProperties.PROP_URL);
      if (dialect.matchesJdbcURL(jdbcUrl)) {
        embeddedDatabase = new EmbeddedDatabase(settings);
        embeddedDatabase.start();
      }
    }
  }

  public void stop() {
    if (embeddedDatabase != null) {
      embeddedDatabase.stop();
    }
  }
}
