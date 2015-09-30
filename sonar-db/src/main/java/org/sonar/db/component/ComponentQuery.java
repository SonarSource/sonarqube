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

package org.sonar.db.component;

import javax.annotation.Nullable;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;

import static com.google.common.base.Preconditions.checkArgument;

public class ComponentQuery {
  private final Database database;
  private final String nameOrKeyQuery;
  private final String[] qualifiers;

  public ComponentQuery(Database database, @Nullable String nameOrKeyQuery, String... qualifiers) {
    checkArgument(qualifiers.length > 0, "At least one qualifier must be provided");

    this.database = database;
    this.nameOrKeyQuery = nameOrKeyQueryToSql(nameOrKeyQuery);
    this.qualifiers = qualifiers;
  }

  public String getNameOrKeyQuery() {
    return nameOrKeyQuery;
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  private String nameOrKeyQueryToSql(@Nullable String nameOrKeyQuery) {
    if (nameOrKeyQuery == null) {
      return null;
    }

    return appendEscapeForSomeDb(
      escapePercentAndUnderscore(nameOrKeyQuery) + "%")
        .toLowerCase();
  }

  /**
   * Replace escape percent and underscore by adding a slash just before
   */
  private static String escapePercentAndUnderscore(String value) {
    return value
      .replaceAll("%", "\\\\%")
      .replaceAll("_", "\\\\_");
  }

  private String appendEscapeForSomeDb(String value) {
    return isOracleOrMsSql()
      ? (value + " ESCAPE '\\'")
      : value;
  }

  private boolean isOracleOrMsSql() {
    return database.getDialect().getId().equals(Oracle.ID) || database.getDialect().getId().equals(MsSql.ID);
  }
}
