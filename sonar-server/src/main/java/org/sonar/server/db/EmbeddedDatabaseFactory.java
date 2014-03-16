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
package org.sonar.server.db;

import com.google.common.annotations.VisibleForTesting;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;

public class EmbeddedDatabaseFactory implements Startable {
  private final Settings settings;
  private EmbeddedDatabase embeddedDatabase;

  public EmbeddedDatabaseFactory(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void start() {
    if (embeddedDatabase == null) {
      String jdbcUrl = settings.getString(DatabaseProperties.PROP_URL);
      if (jdbcUrl.startsWith("jdbc:h2:tcp:")) {
        embeddedDatabase = getEmbeddedDatabase(settings);
        embeddedDatabase.start();
      }
    }
  }

  @Override
  public void stop() {
    if (embeddedDatabase != null) {
      embeddedDatabase.stop();
      embeddedDatabase = null;
    }
  }

  @VisibleForTesting
  EmbeddedDatabase getEmbeddedDatabase(Settings settings) {
    return new EmbeddedDatabase(settings);
  }
}
