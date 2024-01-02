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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.def.Validations;

public abstract class DropIndexChange extends DdlChange {
  private final String indexName;
  private final String tableName;
  private final DbUtilsWrapper dbUtilWrapper;

  protected DropIndexChange(Database db, String indexName, String tableName) {
    this(db, indexName, tableName, new DbUtilsWrapper(), new ValidationsWrapper());
  }
  protected DropIndexChange(Database db, String indexName, String tableName, DbUtilsWrapper dbUtilsWrapper, ValidationsWrapper validationsWrapper) {
    super(db);
    validationsWrapper.validateIndexName(indexName);
    validationsWrapper.validateTableName(tableName);
    this.indexName = indexName;
    this.tableName = tableName;
    this.dbUtilWrapper = dbUtilsWrapper;
  }

  @Override
  public void execute(Context context) throws SQLException {
    findExistingIndexName()
      .map(index -> createDropIndexSqlStatement(getDialect(), index))
      .ifPresent(context::execute);
  }

  private Optional<String> findExistingIndexName() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return dbUtilWrapper.findExistingIndex(connection, tableName, indexName);
    }
  }

  private String createDropIndexSqlStatement(Dialect dialect, String actualIndexName) {
    return switch (dialect.getId()) {
      case MsSql.ID -> "DROP INDEX " + actualIndexName + " ON " + tableName;
      case Oracle.ID -> "DROP INDEX " + actualIndexName;
      case H2.ID, PostgreSql.ID -> "DROP INDEX IF EXISTS " + actualIndexName;
      default -> throw new IllegalStateException("Unsupported dialect for drop of index: " + dialect);
    };
  }

  protected static class DbUtilsWrapper {
    public Optional<String> findExistingIndex(Connection connection, String tableName, String indexName) {
      return DatabaseUtils.findExistingIndex(connection, tableName, indexName);
    }
  }

  protected static class ValidationsWrapper {
    public String validateIndexName(@Nullable String indexName) {
      return Validations.validateIndexName(indexName);
    }
    public String validateTableName(@Nullable String tableName) {
      return Validations.validateTableName(tableName);
    }
  }
}
