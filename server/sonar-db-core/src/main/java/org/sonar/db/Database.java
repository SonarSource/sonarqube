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
package org.sonar.db;

import javax.sql.DataSource;
import org.picocontainer.Startable;
import org.sonar.db.dialect.Dialect;

/**
 * @since 2.12
 */
public interface Database extends Startable {
  /**
   * Returns the configured datasource. Null as long as start() is not executed.
   */
  DataSource getDataSource();

  /**
   * @return the dialect or null if start() has not been executed
   */
  Dialect getDialect();

  void enableSqlLogging(boolean enable);
}
