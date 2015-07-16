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
package org.sonar.db.purge;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PurgeDaoTest {

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  PurgeDao underTest = dbTester.getDbClient().purgeDao();

  @Test
  public void shouldDeleteAbortedBuilds() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteAbortedBuilds.xml");
    underTest.purge(newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "shouldDeleteAbortedBuilds-result.xml", "snapshots");
  }

  @Test
  public void should_purge_project() {
    dbTester.prepareDbUnit(getClass(), "shouldPurgeProject.xml");
    underTest.purge(newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "shouldPurgeProject-result.xml", "projects", "snapshots");
  }

  @Test
  public void delete_file_sources_of_disabled_resources() {
    dbTester.prepareDbUnit(getClass(), "delete_file_sources_of_disabled_resources.xml");
    underTest.purge(newConfigurationWith30Days(system2), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "delete_file_sources_of_disabled_resources-result.xml", "file_sources");
  }

  @Test
  public void shouldDeleteHistoricalDataOfDirectoriesAndFiles() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles.xml");
    underTest.purge(new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[]{Scopes.DIRECTORY, Scopes.FILE}, 30), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles-result.xml", "projects", "snapshots");
  }

  @Test
  public void disable_resources_without_last_snapshot() {
    dbTester.prepareDbUnit(getClass(), "disable_resources_without_last_snapshot.xml");
    when(system2.now()).thenReturn(1450000000000L);
    underTest.purge(newConfigurationWith30Days(system2), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "disable_resources_without_last_snapshot-result.xml", new String[] {"issue_close_date", "issue_update_date"}, "projects", "snapshots",
      "issues");
  }

  @Test
  public void shouldDeleteSnapshots() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteSnapshots.xml");
    underTest.deleteSnapshots(PurgeSnapshotQuery.create().setIslast(false).setResourceId(1L), new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "shouldDeleteSnapshots-result.xml", "snapshots");
  }

  @Test
  public void shouldSelectPurgeableSnapshots() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectPurgeableSnapshots.xml");
    List<PurgeableSnapshotDto> snapshots = underTest.selectPurgeableSnapshots(1L);

    assertThat(snapshots).hasSize(3);
    assertThat(getById(snapshots, 1L).isLast()).isTrue();
    assertThat(getById(snapshots, 1L).hasEvents()).isFalse();
    assertThat(getById(snapshots, 4L).isLast()).isFalse();
    assertThat(getById(snapshots, 4L).hasEvents()).isFalse();
    assertThat(getById(snapshots, 5L).isLast()).isFalse();
    assertThat(getById(snapshots, 5L).hasEvents()).isTrue();
  }

  @Test
  public void should_delete_project_and_associated_data() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteProject.xml");
    underTest.deleteResourceTree(new IdUuidPair(1L, "A"), new PurgeProfiler());
    assertThat(dbTester.countRowsOfTable("projects")).isZero();
    assertThat(dbTester.countRowsOfTable("snapshots")).isZero();
    assertThat(dbTester.countRowsOfTable("action_plans")).isZero();
    assertThat(dbTester.countRowsOfTable("issues")).isZero();
    assertThat(dbTester.countRowsOfTable("issue_changes")).isZero();
    assertThat(dbTester.countRowsOfTable("file_sources")).isZero();
  }

  @Test
  public void should_delete_old_closed_issues() {
    dbTester.prepareDbUnit(getClass(), "should_delete_old_closed_issues.xml");
    underTest.purge(newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "should_delete_old_closed_issues-result.xml", "issues", "issue_changes");
  }

  @Test
  public void should_delete_all_closed_issues() {
    dbTester.prepareDbUnit(getClass(), "should_delete_all_closed_issues.xml");
    underTest.purge(new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 0), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "should_delete_all_closed_issues-result.xml", "issues", "issue_changes");
  }

  private static PurgeableSnapshotDto getById(List<PurgeableSnapshotDto> snapshots, long id) {
    for (PurgeableSnapshotDto snapshot : snapshots) {
      if (snapshot.getSnapshotId() == id) {
        return snapshot;
      }
    }
    return null;
  }

  private static PurgeConfiguration newConfigurationWith30Days() {
    return new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 30);
  }

  private static PurgeConfiguration newConfigurationWith30Days(System2 system2) {
    return new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 30, system2);
  }
}
