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

import com.google.common.collect.ImmutableList;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.Version;

public class Oracle extends AbstractDialect {

  public static final String ID = "oracle";
  private static final List<String> INIT_STATEMENTS = ImmutableList.of("ALTER SESSION SET NLS_SORT='BINARY'");
  private static final Version MIN_SUPPORTED_VERSION = Version.create(11, 0, 0);


  public Oracle() {
    super(ID, "oracle.jdbc.OracleDriver", "1", "0", "SELECT 1 FROM DUAL");
  }

  @Override
  public boolean matchesJdbcUrl(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:oracle:");
  }

  @Override
  public boolean supportsMigration() {
    return true;
  }

  @Override
  public List<String> getConnectionInitStatements() {
    return INIT_STATEMENTS;
  }

  @Override
  public String getSqlFromDual() {
    return "from dual";
  }

  @Override
  public void init(DatabaseMetaData metaData) throws SQLException {
    checkDbVersion(metaData, MIN_SUPPORTED_VERSION);
    checkDriverVersion(metaData);
  }

  private static void checkDriverVersion(DatabaseMetaData metaData) throws SQLException {
    String driverVersion = metaData.getDriverVersion();
    String[] parts = StringUtils.split(driverVersion, ".");
    int intVersion = Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
    if (intVersion < 1200) {
      throw MessageException.of(String.format(
        "Unsupported Oracle driver version: %s. Minimal supported version is 12.1.", driverVersion));
    }
  }
}
