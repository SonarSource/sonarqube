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
package org.sonar.jpa.dialect;

import org.apache.commons.lang.StringUtils;
import org.hibernate.dialect.MySQLDialect;
import org.sonar.api.database.DatabaseProperties;

import java.sql.Types;

/**
 * @since 1.12
 */
public class MySql implements Dialect {

  public String getId() {
    return "mysql";
  }

  public String getActiveRecordDialectCode() {
    return "mysql";
  }

  public Class<? extends org.hibernate.dialect.Dialect> getHibernateDialectClass() {
    return MySqlWithDecimalDialect.class;
  }

  public boolean matchesJdbcURL(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:mysql:");
  }

  public static class MySqlWithDecimalDialect extends MySQLDialect {
    public MySqlWithDecimalDialect() {
      super();
      registerColumnType(Types.DOUBLE, "decimal precision");
      registerColumnType(Types.VARCHAR, DatabaseProperties.MAX_TEXT_SIZE, "mediumtext");
      registerColumnType(Types.CLOB, "mediumtext");
      registerColumnType(Types.BLOB, "blob");
    }
  }

}
