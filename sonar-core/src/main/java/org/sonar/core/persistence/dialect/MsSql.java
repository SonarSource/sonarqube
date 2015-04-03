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
import org.hibernate.dialect.SQLServerDialect;
import org.sonar.api.database.DatabaseProperties;

import java.sql.Types;

public class MsSql extends AbstractDialect {

  public static final String ID = "mssql";

  public MsSql() {
    super(ID, "sqlserver", "net.sourceforge.jtds.jdbc.Driver", "1", "0", "SELECT 1");
  }

  @Override
  public Class<? extends org.hibernate.dialect.Dialect> getHibernateDialectClass() {
    return MsSqlDialect.class;
  }

  @Override
  public boolean matchesJdbcURL(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:microsoft:sqlserver:")
      || StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:jtds:sqlserver:");
  }

  @Override
  public boolean supportsMigration() {
    return true;
  }

  public static class MsSqlDialect extends SQLServerDialect {
    public MsSqlDialect() {
      super();
      registerColumnType(Types.DOUBLE, "decimal");
      registerColumnType(Types.VARCHAR, 255, "nvarchar($l)");
      registerColumnType(Types.VARCHAR, DatabaseProperties.MAX_TEXT_SIZE, "nvarchar(max)");
      registerColumnType(Types.CHAR, "nchar(1)");
      registerColumnType(Types.CLOB, "nvarchar(max)");
    }

    @Override
    public String getTypeName(int code, int length, int precision, int scale) {
      if (code != 2005) {
        return super.getTypeName(code, length, precision, scale);
      } else {
        return "ntext";
      }
    }
  }
}

