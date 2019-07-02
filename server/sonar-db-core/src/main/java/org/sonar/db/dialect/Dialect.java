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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import org.sonar.api.utils.MessageException;

public interface Dialect {

  String getId();

  /**
   * Used to autodetect dialect from connection URL
   */
  boolean matchesJdbcUrl(String jdbcConnectionURL);

  String getDefaultDriverClassName();

  List<String> getConnectionInitStatements();

  String getTrueSqlValue();

  String getFalseSqlValue();

  String getSqlFromDual();

  /**
   * Query used to validate the jdbc connection.
   */
  String getValidationQuery();

  /**
   * Fetch size to be used when scrolling large result sets.
   */
  default int getScrollDefaultFetchSize() {
    return 200;
  }

  /**
   * Indicates whether DB migration can be perform on the DB vendor implementation associated with the current dialect.
   */
  boolean supportsMigration();

  boolean supportsUpsert();

  /**
   * This method is called when connecting for the first
   * time to the database.
   *
   * @throws MessageException when validation error must be displayed to user
   * @throws SQLException in case of error to run the validations
   */
  void init(DatabaseMetaData metaData) throws SQLException;
}
