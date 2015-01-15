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
package org.sonar.core.persistence;

import org.apache.commons.dbcp.BasicDataSource;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.SonarException;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import java.io.File;
import java.sql.SQLException;

public class PreviewDatabaseFactory implements ServerComponent {
  private static final String DIALECT = "h2";
  private static final String DRIVER = "org.h2.Driver";
  private static final String URL = "jdbc:h2:";
  public static final String H2_FILE_SUFFIX = ".h2.db";
  private static final String SONAR = "sonar";
  private static final String USER = SONAR;
  private static final String PASSWORD = SONAR;

  private final Database database;
  private final Profiling profiling;

  public PreviewDatabaseFactory(Database database, Profiling profiling) {
    this.database = database;
    this.profiling = profiling;
  }

  public File createNewDatabaseForDryRun(Long projectId, File destFolder, String dbFileName) {
    StopWatch watch = profiling.start("previewdb", Level.BASIC);

    String h2Name = destFolder.getAbsolutePath() + File.separator + dbFileName;

    try {
      DataSource source = database.getDataSource();
      BasicDataSource destination = create(DIALECT, DRIVER, USER, PASSWORD, URL + h2Name + ";LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0");

      copy(source, destination, projectId);
      close(destination);

      File dbFile = new File(h2Name + H2_FILE_SUFFIX);

      long size = dbFile.length();
      String message = "";
      if (projectId == null) {
        message = "Preview Database created, size is " + size + " bytes";
      } else {
        message = "Preview Database for project " + projectId + " created, size is " + size + " bytes";
      }
      watch.stop(message);

      return dbFile;

    } catch (SQLException e) {
      throw new SonarException("Unable to create database for DryRun", e);
    }

  }

  private void copy(DataSource source, DataSource dest, @Nullable Long projectId) {
    DbTemplate template = new DbTemplate(profiling);
    template
      .copyTable(source, dest, "characteristics")
      .copyTable(source, dest, "permission_templates")
      .copyTable(source, dest, "perm_templates_users")
      .copyTable(source, dest, "perm_templates_groups")
      .copyTable(source, dest, "rules")
      .copyTable(source, dest, "rules_parameters")
      .copyTableColumns(source, dest, "users", new String[] {"id", "login", "name", "active"});
    if (projectId != null) {
      template.copyTable(source, dest, "projects", projectQuery(projectId, false));

      template.copyTable(source, dest, "events", "SELECT * FROM events WHERE resource_id=" + projectId);

      StringBuilder snapshotQuery = new StringBuilder()
        // All snapshots of root_project for alerts on differential periods
        .append("SELECT * FROM snapshots WHERE project_id=")
        .append(projectId);
      template.copyTable(source, dest, "snapshots", snapshotQuery.toString());

      // All measures of snapshots of root project for alerts on differential periods
      template.copyTable(source, dest, "project_measures", "SELECT m.* FROM project_measures m INNER JOIN snapshots s on m.snapshot_id=s.id "
        + "WHERE s.project_id=" + projectId);

      StringBuilder issueQuery = new StringBuilder()
        .append("SELECT issues.* FROM issues")
        .append(" INNER JOIN (")
        .append(projectQuery(projectId, true))
        .append(") resources")
        .append(" ON issues.component_id=resources.id")
        .append(" AND status <> '").append(Issue.STATUS_CLOSED).append("'");
      template.copyTable(source, dest, "issues", issueQuery.toString());
    }
  }

  private String projectQuery(Long projectId, boolean returnOnlyIds) {
    return new StringBuilder()
      .append("SELECT p.").append(returnOnlyIds ? "id" : "*")
      .append(" FROM projects p INNER JOIN snapshots s ON p.id = s.project_id")
      .append(" WHERE s.islast=").append(database.getDialect().getTrueSqlValue())
      .append(" AND s.root_project_id=").append(projectId)
      .append(" UNION")
      .append(" SELECT p.").append(returnOnlyIds ? "id" : "*")
      .append(" FROM projects p")
      .append(" WHERE p.id=").append(projectId)
      .append(" OR p.root_id=").append(projectId).toString();
  }

  private BasicDataSource create(String dialect, String driver, String user, String password, String url) {
    BasicDataSource dataSource = new DbTemplate(profiling).dataSource(driver, user, password, url);
    new DbTemplate(profiling).createSchema(dataSource, dialect);
    return dataSource;
  }

  private void close(BasicDataSource destination) throws SQLException {
    destination.close();
  }

}
