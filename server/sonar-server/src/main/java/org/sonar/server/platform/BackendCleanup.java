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
package org.sonar.server.platform;

import com.google.common.collect.ImmutableMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.version.SqTables;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.view.index.ViewIndexDefinition;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@ServerSide
public class BackendCleanup {

  private static final String[] ANALYSIS_TABLES = {
    "ce_activity", "ce_queue", "ce_task_input", "ce_scanner_context",
    "duplications_index", "events", "issues", "issue_changes", "manual_measures",
    "notifications", "project_links", "project_measures", "projects",
    "snapshots", "file_sources", "webhook_deliveries"
  };
  private static final String[] RESOURCE_RELATED_TABLES = {
    "group_roles", "user_roles", "properties"
  };
  private static final Map<String, TableCleaner> TABLE_CLEANERS = ImmutableMap.of(
    "organizations", BackendCleanup::truncateOrganizations,
    "users", BackendCleanup::truncateUsers,
    "groups", BackendCleanup::truncateGroups,
    "internal_properties", BackendCleanup::truncateInternalProperties,
    "schema_migrations", BackendCleanup::truncateSchemaMigrations);

  private final EsClient esClient;
  private final DbClient dbClient;

  public BackendCleanup(EsClient esClient, DbClient dbClient) {
    this.esClient = esClient;
    this.dbClient = dbClient;
  }

  public void clearAll() {
    clearDb();
    clearIndexes();
  }

  public void clearDb() {
    try (DbSession dbSession = dbClient.openSession(false);
      Connection connection = dbSession.getConnection();
      Statement ddlStatement = connection.createStatement()) {
      for (String tableName : SqTables.TABLES) {
        Optional.ofNullable(TABLE_CLEANERS.get(tableName))
          .orElse(BackendCleanup::truncateDefault)
          .clean(tableName, ddlStatement, connection);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to clear db", e);
    }
  }

  public void clearIndexes() {
    Loggers.get(getClass()).info("Truncate Elasticsearch indices");
    try {
      esClient.prepareClearCache().get();

      for (String index : esClient.prepareState().get().getState().getMetaData().getConcreteAllIndices()) {
        clearIndex(new IndexType(index, index));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to clear indexes", e);
    }
  }

  /**
   * Reset data in order to to be in same state as a fresh installation (but without having to drop db and restart the server).
   *
   * Please be careful when updating this method as it's called by Orchestrator.
   */
  public void resetData() {
    try (DbSession dbSession = dbClient.openSession(false);
      Connection connection = dbSession.getConnection()) {

      truncateAnalysisTables(connection);
      deleteManualRules(connection);
      truncateInternalProperties(null, null, connection);
      truncateUsers(null, null, connection);
      truncateOrganizations(null, null, connection);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to reset data", e);
    }

    clearIndex(IssueIndexDefinition.INDEX_TYPE_ISSUE);
    clearIndex(ViewIndexDefinition.INDEX_TYPE_VIEW);
    clearIndex(ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES);
    clearIndex(ComponentIndexDefinition.INDEX_TYPE_COMPONENT);
  }

  private void truncateAnalysisTables(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      // Clear inspection tables
      for (String table : ANALYSIS_TABLES) {
        statement.execute(createTruncateSql(table.toLowerCase(Locale.ENGLISH)));
        // commit is useless on some databases
        connection.commit();
      }
      // Clear resource related tables
      for (String table : RESOURCE_RELATED_TABLES) {
        statement.execute("DELETE FROM " + table + " WHERE resource_id IS NOT NULL");
        connection.commit();
      }
    }
  }

  private String createTruncateSql(String table) {
    if (dbClient.getDatabase().getDialect().getId().equals(Oracle.ID)) {
      // truncate operation is needs to lock the table on Oracle. Unfortunately
      // it fails sometimes in our QA environment because table is locked.
      // We never found the root cause (no locks found when displaying them just after
      // receiving the error).
      // Workaround is to use "delete" operation. It does not require lock on table.
      return "DELETE FROM " + table;
    }
    return "TRUNCATE TABLE " + table;
  }

  private static void deleteManualRules(Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM rules WHERE rules.plugin_name='manual'")) {
      statement.execute();
      // commit is useless on some databases
      connection.commit();
    }
  }

  /**
   * Completely remove a index with all types
   */
  public void clearIndex(IndexType indexType) {
    BulkIndexer.delete(esClient, indexType, esClient.prepareSearch(indexType.getIndex()).setQuery(matchAllQuery()));
  }

  @FunctionalInterface
  private interface TableCleaner {
    void clean(String tableName, Statement ddlStatement, Connection connection) throws SQLException;
  }

  private static void truncateDefault(String tableName, Statement ddlStatement, Connection connection) throws SQLException {
    ddlStatement.execute("TRUNCATE TABLE " + tableName.toLowerCase(Locale.ENGLISH));
    // commit is useless on some databases
    connection.commit();
  }

  /**
   * Default organization must never be deleted
   */
  private static void truncateOrganizations(String tableName, Statement ddlStatement, Connection connection) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement("delete from organizations where kee <> ?")) {
      preparedStatement.setString(1, "default-organization");
      preparedStatement.execute();
      // commit is useless on some databases
      connection.commit();
    }
  }

  /**
   * User admin must never be deleted.
   */
  private static void truncateUsers(String tableName, Statement ddlStatement, Connection connection) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement("delete from users where login <> ?")) {
      preparedStatement.setString(1, "admin");
      preparedStatement.execute();
      // commit is useless on some databases
      connection.commit();
    }
    // "admin" is not flagged as root by default
    try (PreparedStatement preparedStatement = connection.prepareStatement("update users set is_root=?")) {
      preparedStatement.setBoolean(1, false);
      preparedStatement.execute();
      // commit is useless on some databases
      connection.commit();
    }
  }

  /**
   * Groups sonar-users is referenced by the default organization as its default group.
   */
  private static void truncateGroups(String tableName, Statement ddlStatement, Connection connection) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement("delete from groups where name <> ?")) {
      preparedStatement.setString(1, "sonar-users");
      preparedStatement.execute();
      // commit is useless on some databases
      connection.commit();
    }
  }

  /**
   * Internal property {@link InternalProperties#DEFAULT_ORGANIZATION} must never be deleted.
   */
  private static void truncateInternalProperties(String tableName, Statement ddlStatement, Connection connection) throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement("delete from internal_properties where kee not in (?,?)")) {
      preparedStatement.setString(1, InternalProperties.DEFAULT_ORGANIZATION);
      preparedStatement.setString(2, InternalProperties.SERVER_ID_CHECKSUM);
      preparedStatement.execute();
      // commit is useless on some databases
      connection.commit();
    }
  }

  /**
   * Data in SCHEMA_MIGRATIONS table is inserted when DB is created and should not be altered afterwards.
   */
  private static void truncateSchemaMigrations(String tableName, Statement ddlStatement, Connection connection) {
    // do nothing
  }

}
