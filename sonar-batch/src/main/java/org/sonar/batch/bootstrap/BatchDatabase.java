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
package org.sonar.batch.bootstrap;

import org.sonar.api.config.Settings;
import org.sonar.core.persistence.DefaultDatabase;

import java.util.Properties;

/**
 * @since 2.12
 */
public class BatchDatabase extends DefaultDatabase {

  public BatchDatabase(Settings settings,
    // The dependency on JdbcDriverHolder is required to be sure that the JDBC driver
    // has been downloaded and injected into classloader
    JdbcDriverHolder jdbcDriverHolder) {
    super(settings);
  }

  @Override
  protected void doCompleteProperties(Properties properties) {
    // three connections are required : one for Hibernate, one for MyBatis for regular operations,
    // and one for the SemaphoreUpdater
    // Note that Hibernate will be removed soon
    properties.setProperty("sonar.jdbc.initialSize", "3");
    properties.setProperty("sonar.jdbc.maxActive", "3");
    // SONAR-2965
    properties.setProperty("sonar.jdbc.defaultAutoCommit", "false");
  }
}
