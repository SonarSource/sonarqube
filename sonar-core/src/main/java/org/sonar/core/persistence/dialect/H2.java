/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.core.persistence.dialect;

import org.apache.commons.lang.StringUtils;
import org.hibernate.dialect.H2Dialect;

/**
 * @since 1.12
 */
public class H2 implements Dialect {

  public static final String ID = "h2";

  public String getId() {
    return ID;
  }

  public String getActiveRecordDialectCode() {
    return "h2";
  }

  public String getActiveRecordJdbcAdapter() {
    return "jdbc";
  }

  public Class<? extends org.hibernate.dialect.Dialect> getHibernateDialectClass() {
    return H2Dialect.class;
  }

  public boolean matchesJdbcURL(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:h2:");
  }

  public String getDefaultDriverClassName() {
    return "org.h2.Driver";
  }

  public String getConnectionInitStatement(String schema) {
    return null;
  }

  public String getTrueSqlValue() {
    return "true";
  }

  public String getFalseSqlValue() {
    return "false";
  }

  public String getValidationQuery() {
    return "SELECT 1";
  }
}
