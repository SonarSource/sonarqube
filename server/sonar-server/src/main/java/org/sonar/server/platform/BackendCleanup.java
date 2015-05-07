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
package org.sonar.server.platform;

import org.apache.commons.dbutils.DbUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.api.ServerSide;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.es.EsClient;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.view.index.ViewIndexDefinition;

import java.sql.Connection;
import java.sql.SQLException;

@ServerSide
public class BackendCleanup {

  private static final String[] INSPECTION_TABLES = {
    "action_plans", "authors", "dependencies", "duplications_index", "events", "graphs", "issues", "issue_changes", "manual_measures",
    "notifications", "project_links", "project_measures", "projects", "resource_index",
    "semaphores", "snapshots", "file_sources"
  };
  private static final String[] RESOURCE_RELATED_TABLES = {
    "group_roles", "user_roles", "properties"
  };
  private final EsClient esClient;
  private final MyBatis myBatis;

  public BackendCleanup(EsClient esClient, MyBatis myBatis) {
    this.esClient = esClient;
    this.myBatis = myBatis;
  }

  public void clearAll() {
    clearDb();
    clearIndexes();
  }

  public void clearDb() {
    DbSession dbSession = myBatis.openSession(false);
    Connection connection = dbSession.getConnection();
    try {
      for (String table : DatabaseVersion.TABLES) {
        try {
          connection.createStatement().execute("TRUNCATE TABLE " + table.toLowerCase());
          // commit is useless on some databases
          connection.commit();
        } catch (Exception e) {
          throw new IllegalStateException("Fail to truncate db table " + table, e);
        }
      }

    } finally {
      DbUtils.closeQuietly(connection);
      MyBatis.closeQuietly(dbSession);
    }
  }

  public void clearIndexes() {
    Loggers.get(getClass()).info("Truncate Elasticsearch indices");
    try {
      esClient.prepareClearCache()
        .get();
      esClient.prepareDeleteByQuery(esClient.prepareState().get()
        .getState().getMetaData().concreteAllIndices())
        .setQuery(QueryBuilders.matchAllQuery())
        .get();
      esClient.prepareRefresh(esClient.prepareState().get()
        .getState().getMetaData().concreteAllIndices())
        .setForce(true)
        .get();
      esClient.prepareFlush(esClient.prepareState().get()
        .getState().getMetaData().concreteAllIndices())
        .get();
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
    DbSession dbSession = myBatis.openSession(false);
    Connection connection = dbSession.getConnection();
    try {
      // Clear inspection tables
      for (String table : INSPECTION_TABLES) {
        try {
          connection.createStatement().execute("TRUNCATE TABLE " + table.toLowerCase());
          // commit is useless on some databases
          connection.commit();
        } catch (Exception e) {
          throw new IllegalStateException("Fail to truncate db table " + table, e);
        }
      }

      // Clear resource related tables
      for (String relatedTable : RESOURCE_RELATED_TABLES) {
        deleteWhereResourceIdNotNull(relatedTable, connection);
      }

      deleteManualRules(connection);

      // Clear inspection indexes
      clearIndex(IssueIndexDefinition.INDEX);
      clearIndex(SourceLineIndexDefinition.INDEX);
      clearIndex(ViewIndexDefinition.INDEX);

    } finally {
      dbSession.close();
      DbUtils.closeQuietly(connection);
    }
  }

  private void deleteWhereResourceIdNotNull(String tableName, Connection connection) {
    try {
      connection.prepareStatement("DELETE FROM " + tableName + " WHERE resource_id IS NOT NULL").execute();
      // commit is useless on some databases
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to delete table : " + tableName, e);
    }
  }

  private void deleteManualRules(Connection connection) {
    try {
      connection.prepareStatement("DELETE FROM rules WHERE rules.plugin_name='manual'").execute();
      // commit is useless on some databases
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to remove manual rules", e);
    }
  }

  /**
   * Completely remove a index with all types
   */
  public void clearIndex(String indexName) {
    esClient.prepareDeleteByQuery(esClient.prepareState().get()
      .getState().getMetaData().concreteIndices(new String[] {indexName}))
      .setQuery(QueryBuilders.matchAllQuery())
      .get();
  }

  /**
   * Remove only the type of an index
   */
  public void clearIndexType(IndexDefinition indexDefinition) {
    esClient.prepareDeleteByQuery(esClient.prepareState().get()
      .getState().getMetaData().concreteIndices(new String[] {indexDefinition.getIndexName()})).setTypes(indexDefinition.getIndexType())
      .setQuery(QueryBuilders.matchAllQuery())
      .get();
  }
}
