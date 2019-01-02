/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db;

import com.google.common.annotations.VisibleForTesting;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;

import static org.apache.commons.lang.StringUtils.startsWith;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;

public class EmbeddedDatabaseFactory implements Startable {

  private static final String URL_PREFIX = "jdbc:h2:tcp:";

  private final Configuration config;
  private final System2 system2;
  private EmbeddedDatabase embeddedDatabase;

  public EmbeddedDatabaseFactory(Configuration config, System2 system2) {
    this.config = config;
    this.system2 = system2;
  }

  @Override
  public void start() {
    if (embeddedDatabase == null) {
      String jdbcUrl = config.get(JDBC_URL.getKey()).get();
      if (startsWith(jdbcUrl, URL_PREFIX)) {
        embeddedDatabase = createEmbeddedDatabase();
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
  EmbeddedDatabase createEmbeddedDatabase() {
    return new EmbeddedDatabase(config, system2);
  }
}
