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
package org.sonar.core.persistence.dialect;

import org.apache.commons.lang.StringUtils;
import org.hibernate.dialect.MySQLDialect;
import org.sonar.api.database.DatabaseProperties;

import java.sql.Types;

/**
 * @since 1.12
 */
public class MySql extends AbstractDialect {

  public static final String ID = "mysql";

  public MySql() {
    super(ID, "mysql", "com.mysql.jdbc.Driver", "true", "false", "SELECT 1");
  }

  @Override
  public Class<? extends org.hibernate.dialect.Dialect> getHibernateDialectClass() {
    return MySqlWithDecimalDialect.class;
  }

  @Override
  public boolean matchesJdbcURL(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:mysql:");
  }

  public static class MySqlWithDecimalDialect extends MySQLDialect {
    public MySqlWithDecimalDialect() {
      super();
      registerColumnType(Types.DOUBLE, "decimal precision");
      registerColumnType(Types.VARCHAR, DatabaseProperties.MAX_TEXT_SIZE, "longtext");
      registerColumnType(Types.CLOB, "longtext");
      registerColumnType(Types.BLOB, "blob");
    }
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
}
