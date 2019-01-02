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
package org.sonar.db.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Loggers;

public class MySql extends AbstractDialect {

  public static final String ID = "mysql";
  private static final Version MIN_SUPPORTED_VERSION = Version.create(5, 6, 0);

  public MySql() {
    super(ID, "com.mysql.jdbc.Driver", "true", "false", "SELECT 1");
  }

  @Override
  public boolean matchesJdbcUrl(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:mysql:");
  }

  @Override
  public int getScrollDefaultFetchSize() {
    return Integer.MIN_VALUE;
  }

  @Override
  public int getScrollSingleRowFetchSize() {
    return Integer.MIN_VALUE;
  }

  @Override
  public boolean supportsMigration() {
    return true;
  }

  @Override
  public String getSqlFromDual() {
    return "from dual";
  }

  @Override
  public void init(DatabaseMetaData metaData) throws SQLException {
    checkDbVersion(metaData, MIN_SUPPORTED_VERSION);

    Loggers.get(getClass()).warn("MySQL support is deprecated and will be dropped soon.");
  }
}
