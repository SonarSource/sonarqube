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
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.id.IdentityGenerator;
import org.sonar.api.database.DatabaseProperties;

import java.sql.Types;

/**
 * @since 1.12
 */
public class Derby implements Dialect {

  public String getId() {
    return "derby";
  }

  public String getActiveRecordDialectCode() {
    return "derby";
  }

  public Class<? extends org.hibernate.dialect.Dialect> getHibernateDialectClass() {
    return DerbyWithDecimalDialect.class;
  }

  public boolean matchesJdbcURL(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:derby:");
  }

  public static class DerbyWithDecimalDialect extends DerbyDialect {
    public DerbyWithDecimalDialect() {
      super();
      registerColumnType(Types.DOUBLE, "decimal");
      registerColumnType(Types.VARCHAR, DatabaseProperties.MAX_TEXT_SIZE, "clob");
      registerColumnType(Types.VARBINARY, "blob");

      // Not possible to do alter column types in Derby
      registerColumnType(Types.BIGINT, "integer");
    }

    /**
     * To be compliant with Oracle, we define on each model (ch.hortis.sonar.model classes)
     * a sequence generator. It works on mySQL because strategy = GenerationType.AUTO, so
     * it equals GenerationType.IDENTITY.
     * But on derby, AUTO becomes TABLE instead of IDENTITY. So we explicitly change this behavior.
     */
    @Override
    public Class getNativeIdentifierGeneratorClass() {
      return IdentityGenerator.class;
    }
  }

}
