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

import com.google.common.io.Files;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;

import javax.sql.DataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.fest.assertions.Assertions.assertThat;

public class PreviewDatabaseFactoryTest extends AbstractDaoTestCase {
  PreviewDatabaseFactory localDatabaseFactory;
  BasicDataSource dataSource;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    localDatabaseFactory = new PreviewDatabaseFactory(getDatabase(), new Profiling(new Settings()));
  }

  @After
  public void closeDatabase() throws SQLException {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @Test
  public void should_create_database_without_project() throws Exception {
    setupData("should_create_database");

    byte[] db = createDb(null);
    dataSource = createDatabase(db);

    assertThat(rowCount("metrics")).isEqualTo(2);
    assertThat(rowCount("projects")).isZero();
    assertThat(rowCount("alerts")).isEqualTo(1);
    assertThat(rowCount("events")).isZero();
    assertThat(rowCount("users")).isEqualTo(3);
    // Verify that password column was not exported into dryRun DB
    assertThat(getUserPassword(dataSource, 1)).isNull();
  }

  private byte[] createDb(Long projectId) throws IOException {
    return FileUtils.readFileToByteArray(localDatabaseFactory.createNewDatabaseForDryRun(projectId, temporaryFolder.newFolder(), "foo"));
  }

  @Test
  public void should_create_database_with_project() throws Exception {
    setupData("should_create_database");

    byte[] database = createDb(123L);
    dataSource = createDatabase(database);

    assertThat(rowCount("metrics")).isEqualTo(2);
    assertThat(rowCount("projects")).isEqualTo(1);
    assertThat(rowCount("snapshots")).isEqualTo(1);
    assertThat(rowCount("project_measures")).isEqualTo(1);
    assertThat(rowCount("events")).isEqualTo(2);
  }

  @Test
  public void should_create_database_with_issues() throws Exception {
    setupData("should_create_database_with_issues");

    byte[] database = createDb(399L);
    dataSource = createDatabase(database);

    assertThat(rowCount("issues")).isEqualTo(1);
  }

  @Test
  public void should_export_issues_of_project_tree() throws Exception {
    setupData("multi-modules-with-issues");

    // 300 : root module -> export issues of all modules
    byte[] database = createDb(300L);
    dataSource = createDatabase(database);
    assertThat(rowCount("issues")).isEqualTo(1);
    assertThat(rowCount("projects")).isEqualTo(4);
    assertThat(rowCount("snapshots")).isEqualTo(4);
    assertThat(rowCount("snapshot_data")).isEqualTo(2);
    assertThat(rowCount("project_measures")).isEqualTo(4);
  }

  @Test
  public void should_export_issues_of_sub_module() throws Exception {
    setupData("multi-modules-with-issues");

    // 301 : sub module with 1 closed issue and 1 open issue
    byte[] database = createDb(301L);
    dataSource = createDatabase(database);
    assertThat(rowCount("issues")).isEqualTo(1);
    assertThat(rowCount("projects")).isEqualTo(2);
    assertThat(rowCount("snapshots")).isEqualTo(2);
    assertThat(rowCount("project_measures")).isEqualTo(4);
  }

  @Test
  public void should_export_issues_of_sub_module_2() throws Exception {
    setupData("multi-modules-with-issues");

    // 302 : sub module without any issues
    byte[] database = createDb(302L);
    dataSource = createDatabase(database);
    assertThat(rowCount("issues")).isEqualTo(0);
  }

  @Test
  public void should_copy_permission_templates_data() throws Exception {
    setupData("should_copy_permission_templates");

    byte[] database = createDb(null);
    dataSource = createDatabase(database);
    assertThat(rowCount("permission_templates")).isEqualTo(1);
    assertThat(rowCount("perm_templates_users")).isEqualTo(1);
    assertThat(rowCount("perm_templates_groups")).isEqualTo(1);
  }

  private BasicDataSource createDatabase(byte[] db) throws IOException {
    File file = temporaryFolder.newFile("db.h2.db");
    Files.write(db, file);
    return new DbTemplate(new Profiling(new Settings())).dataSource("org.h2.Driver", "sonar", "sonar", "jdbc:h2:" + file.getAbsolutePath().replaceAll(".h2.db", ""));
  }

  private int rowCount(String table) {
    return new DbTemplate(new Profiling(new Settings())).getRowCount(dataSource, table);
  }

  public String getUserPassword(DataSource dataSource, int userId) {
    Connection connection = null;
    Statement statement = null;
    ResultSet resultSet = null;
    try {
      connection = dataSource.getConnection();
      statement = connection.createStatement();
      resultSet = statement.executeQuery("SELECT crypted_password FROM users WHERE id=" + userId);

      return resultSet.next() ? resultSet.getString(1) : null;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    } finally {
      DbUtils.closeQuietly(resultSet);
      DbUtils.closeQuietly(statement);
      DbUtils.closeQuietly(connection);
    }
  }
}
