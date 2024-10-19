/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

public class H2 extends AbstractDialect {

  public static final String ID = "h2";

  public H2() {
    super(ID, "org.h2.Driver", "true", "false", "SELECT 1");
  }

  @Override
  public boolean matchesJdbcUrl(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:h2:");
  }

  @Override
  public boolean supportsMigration() {
    return false;
  }

  @Override
  public boolean supportsNullNotDistinct() {
    return true;
  }

  @Override
  public void init(DatabaseMetaData metaData) {
    LoggerFactory.getLogger(getClass()).warn("H2 database should be used for evaluation purpose only.");
  }
}
