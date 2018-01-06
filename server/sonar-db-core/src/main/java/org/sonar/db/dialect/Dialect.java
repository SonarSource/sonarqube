/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.List;

/**
 * @since 1.12
 */
public interface Dialect {

  /**
   * @return the sonar dialect Id to be matched with the sonar.jdbc.dialect property when provided
   */
  String getId();

  /**
   * Used to autodetect a dialect for a given driver URL
   *
   * @param jdbcConnectionURL a jdbc driver url such as jdbc:mysql://localhost:3306/sonar
   * @return true if the dialect supports surch url
   */
  boolean matchesJdbcURL(String jdbcConnectionURL);

  /**
   * @since 2.13
   */
  String getDefaultDriverClassName();

  List<String> getConnectionInitStatements();

  /**
   * @since 2.14
   */
  String getTrueSqlValue();

  /**
   * @since 2.14
   */
  String getFalseSqlValue();

  /**
   * Query used to validate the jdbc connection.
   *
   * @since 3.2
   */
  String getValidationQuery();

  /**
   * Fetch size to be used when scrolling large result sets.
   *
   * @since 5.0
   */
  int getScrollDefaultFetchSize();

  /**
   * Fetch size to scroll one row at a time. It sounds strange because obviously value is 1 in most cases,
   * but it's different on MySQL...
   *
   * @since 5.0
   */
  int getScrollSingleRowFetchSize();

  /**
   * Indicates whether DB migration can be perform on the DB vendor implementation associated with the current dialect.
   *
   * @return a boolean
   */
  boolean supportsMigration();
}
