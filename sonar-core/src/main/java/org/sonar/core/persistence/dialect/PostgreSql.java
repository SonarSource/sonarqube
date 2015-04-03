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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.hibernate.dialect.PostgreSQLDialect;

import java.sql.Types;
import java.util.List;

/**
 * @since 1.12
 */
public class PostgreSql extends AbstractDialect {

  public static final String ID = "postgresql";
  static final List<String> INIT_STATEMENTS = ImmutableList.of("SET standard_conforming_strings=on", "SET backslash_quote=off");

  public PostgreSql() {
    super(ID, "postgre", "org.postgresql.Driver", "true", "false", "SELECT 1");
  }

  @Override
  public Class<? extends org.hibernate.dialect.Dialect> getHibernateDialectClass() {
    return PostgreSQLWithDecimalDialect.class;
  }

  @Override
  public boolean matchesJdbcURL(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:postgresql:");
  }

  @Override
  public List<String> getConnectionInitStatements() {
    return INIT_STATEMENTS;
  }

  @Override
  public boolean supportsMigration() {
    return true;
  }

  public static class PostgreSQLWithDecimalDialect extends PostgreSQLDialect {

    public PostgreSQLWithDecimalDialect() {
      super();
      registerColumnType(Types.DOUBLE, "numeric($p,$s)");
    }
    @Override
    public Class getNativeIdentifierGeneratorClass() {
      return PostgreSQLSequenceGenerator.class;
    }

  }
}
