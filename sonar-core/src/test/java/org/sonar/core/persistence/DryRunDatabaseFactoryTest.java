/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.dryrun.DryRunCache;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DryRunDatabaseFactoryTest extends AbstractDaoTestCase {
  DryRunDatabaseFactory localDatabaseFactory;
  BasicDataSource dataSource;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private File dryRunCacheFolder;
  private DryRunCache dryRunCache;

  @Before
  public void setUp() throws Exception {
    File tempFolder = temporaryFolder.newFolder();
    dryRunCacheFolder = new File(tempFolder, "dryRun");
    dryRunCache = mock(DryRunCache.class);
    when(dryRunCache.getCacheLocation(anyLong())).thenReturn(dryRunCacheFolder);
    when(dryRunCache.getModificationTimestamp(null)).thenReturn(0L);
    when(dryRunCache.getModificationTimestamp(anyLong())).thenReturn(0L);
    localDatabaseFactory = new DryRunDatabaseFactory(getDatabase(), dryRunCache);
  }

  @After
  public void closeDatabase() throws SQLException {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @Test
  public void should_create_database_without_project() throws IOException, SQLException {
    setupData("should_create_database");

    assertThat(dryRunCacheFolder).doesNotExist();

    byte[] database = localDatabaseFactory.createDatabaseForDryRun(null);
    dataSource = createDatabase(database);

    assertThat(rowCount("metrics")).isEqualTo(2);
    assertThat(rowCount("projects")).isZero();
    assertThat(rowCount("alerts")).isEqualTo(1);

    assertThat(dryRunCacheFolder).isDirectory();
  }

  @Test
  public void should_reuse_database_without_project() throws IOException, SQLException {
    setupData("should_create_database");

    FileUtils.write(new File(dryRunCacheFolder, "123456.h2.db"), "fakeDbContent");

    byte[] database = localDatabaseFactory.createDatabaseForDryRun(null);

    assertThat(new String(database, Charsets.UTF_8)).isEqualTo("fakeDbContent");
  }

  @Test
  public void should_evict_database_without_project() throws IOException, SQLException {
    setupData("should_create_database");

    // There is a DB in cache
    File existingDb = new File(dryRunCacheFolder, "123456.h2.db");
    FileUtils.write(existingDb, "fakeDbContent");

    // But last modification timestamp is greater
    when(dryRunCache.getModificationTimestamp(null)).thenReturn(123457L);

    byte[] database = localDatabaseFactory.createDatabaseForDryRun(null);
    dataSource = createDatabase(database);

    assertThat(rowCount("metrics")).isEqualTo(2);
    assertThat(rowCount("projects")).isZero();
    assertThat(rowCount("alerts")).isEqualTo(1);

    // Previous cached DB was deleted
    verify(dryRunCache).clean(null);
  }

  @Test
  public void should_create_database_with_project() throws IOException, SQLException {
    setupData("should_create_database");

    assertThat(dryRunCacheFolder).doesNotExist();

    byte[] database = localDatabaseFactory.createDatabaseForDryRun(123L);
    dataSource = createDatabase(database);

    assertThat(rowCount("metrics")).isEqualTo(2);
    assertThat(rowCount("projects")).isEqualTo(1);
    assertThat(rowCount("snapshots")).isEqualTo(1);
    assertThat(rowCount("project_measures")).isEqualTo(1);

    assertThat(dryRunCacheFolder).isDirectory();
  }

  @Test
  public void should_reuse_database_with_project() throws IOException, SQLException {
    setupData("should_create_database");

    FileUtils.write(new File(dryRunCacheFolder, "123456.h2.db"), "fakeDbContent");

    byte[] database = localDatabaseFactory.createDatabaseForDryRun(123L);

    assertThat(new String(database, Charsets.UTF_8)).isEqualTo("fakeDbContent");
  }

  @Test
  public void should_evict_database_with_project() throws IOException, SQLException {
    setupData("should_create_database");

    // There is a DB in cache
    File existingDb = new File(dryRunCacheFolder, "123456.h2.db");
    FileUtils.write(existingDb, "fakeDbContent");

    // But last project modification timestamp is greater
    when(dryRunCache.getModificationTimestamp(123L)).thenReturn(123457L);

    byte[] database = localDatabaseFactory.createDatabaseForDryRun(123L);
    dataSource = createDatabase(database);

    assertThat(rowCount("metrics")).isEqualTo(2);
    assertThat(rowCount("projects")).isEqualTo(1);
    assertThat(rowCount("snapshots")).isEqualTo(1);
    assertThat(rowCount("project_measures")).isEqualTo(1);

    // Previous cached DB was deleted
    verify(dryRunCache).clean(123L);
  }

  @Test
  public void should_create_database_with_issues() throws IOException, SQLException {
    setupData("should_create_database_with_issues");

    byte[] database = localDatabaseFactory.createDatabaseForDryRun(399L);
    dataSource = createDatabase(database);

    assertThat(rowCount("issues")).isEqualTo(1);
  }

  @Test
  public void should_export_issues_of_project_tree() throws IOException, SQLException {
    setupData("multi-modules-with-issues");

    // 300 : root module -> export issues of all modules
    byte[] database = localDatabaseFactory.createDatabaseForDryRun(300L);
    dataSource = createDatabase(database);
    assertThat(rowCount("issues")).isEqualTo(1);
    assertThat(rowCount("projects")).isEqualTo(4);
    assertThat(rowCount("snapshots")).isEqualTo(1);
    assertThat(rowCount("project_measures")).isEqualTo(2);
  }

  @Test
  public void should_export_issues_of_sub_module() throws IOException, SQLException {
    setupData("multi-modules-with-issues");

    // 301 : sub module with 1 closed issue and 1 open issue
    byte[] database = localDatabaseFactory.createDatabaseForDryRun(301L);
    dataSource = createDatabase(database);
    assertThat(rowCount("issues")).isEqualTo(1);
    assertThat(rowCount("projects")).isEqualTo(2);
    assertThat(rowCount("snapshots")).isEqualTo(1);
    assertThat(rowCount("project_measures")).isEqualTo(2);
  }

  @Test
  public void should_export_issues_of_sub_module_2() throws IOException, SQLException {
    setupData("multi-modules-with-issues");

    // 302 : sub module without any issues
    byte[] database = localDatabaseFactory.createDatabaseForDryRun(302L);
    dataSource = createDatabase(database);
    assertThat(rowCount("issues")).isEqualTo(0);
  }

  @Test
  public void should_copy_permission_templates_data() throws Exception {
    setupData("should_copy_permission_templates");

    byte[] database = localDatabaseFactory.createDatabaseForDryRun(null);
    dataSource = createDatabase(database);
    assertThat(rowCount("permission_templates")).isEqualTo(1);
    assertThat(rowCount("perm_templates_users")).isEqualTo(1);
    assertThat(rowCount("perm_templates_groups")).isEqualTo(1);
  }

  private BasicDataSource createDatabase(byte[] db) throws IOException {
    File file = temporaryFolder.newFile("db.h2.db");
    Files.write(db, file);
    return new DbTemplate().dataSource("org.h2.Driver", "sonar", "sonar", "jdbc:h2:" + file.getAbsolutePath().replaceAll(".h2.db", ""));
  }

  private int rowCount(String table) {
    return new DbTemplate().getRowCount(dataSource, table);
  }
}
