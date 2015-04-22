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
package org.sonar.core.purge;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PurgeDaoTest extends AbstractDaoTestCase {

  private PurgeDao sut;
  private System2 system2;
  private DbSession dbSession;

  private static PurgeableSnapshotDto getById(List<PurgeableSnapshotDto> snapshots, long id) {
    for (PurgeableSnapshotDto snapshot : snapshots) {
      if (snapshot.getSnapshotId() == id) {
        return snapshot;
      }
    }
    return null;
  }

  @Before
  public void before() {
    system2 = mock(System2.class);
    when(system2.now()).thenReturn(1450000000000L);
    dbSession = getMyBatis().openSession(false);

    sut = new PurgeDao(getMyBatis(), new ResourceDao(getMyBatis(), system2), new PurgeProfiler(), system2);
  }

  @After
  public void after() {
    MyBatis.closeQuietly(dbSession);
  }

  @Test
  public void shouldDeleteAbortedBuilds() {
    setupData("shouldDeleteAbortedBuilds");
    sut.purge(newConfigurationWith30Days(), PurgeListener.EMPTY);
    checkTables("shouldDeleteAbortedBuilds", "snapshots");
  }

  @Test
  public void should_purge_project() {
    setupData("shouldPurgeProject");
    sut.purge(newConfigurationWith30Days(), PurgeListener.EMPTY);
    checkTables("shouldPurgeProject", "projects", "snapshots");
  }

  private PurgeConfiguration newConfigurationWith30Days() {
    return new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 30);
  }

  private PurgeConfiguration newConfigurationWith30Days(System2 system2) {
    return new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 30, system2);
  }

  @Test
  public void delete_file_sources_of_disabled_resources() {
    setupData("delete_file_sources_of_disabled_resources");
    sut.purge(newConfigurationWith30Days(system2), PurgeListener.EMPTY);
    checkTables("delete_file_sources_of_disabled_resources", "file_sources");
  }

  @Test
  public void shouldDeleteHistoricalDataOfDirectoriesAndFiles() {
    setupData("shouldDeleteHistoricalDataOfDirectoriesAndFiles");
    sut.purge(new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[]{Scopes.DIRECTORY, Scopes.FILE}, 30), PurgeListener.EMPTY);
    checkTables("shouldDeleteHistoricalDataOfDirectoriesAndFiles", "projects", "snapshots");
  }

  @Test
  public void disable_resources_without_last_snapshot() {
    setupData("disable_resources_without_last_snapshot");
    sut.purge(newConfigurationWith30Days(system2), PurgeListener.EMPTY);
    checkTables("disable_resources_without_last_snapshot", new String[]{"issue_close_date", "issue_update_date"}, "projects", "snapshots", "issues");
  }

  @Test
  public void shouldDeleteSnapshots() {
    setupData("shouldDeleteSnapshots");
    sut.deleteSnapshots(PurgeSnapshotQuery.create().setIslast(false).setResourceId(1L));
    checkTables("shouldDeleteSnapshots", "snapshots");
  }

  @Test
  public void shouldSelectPurgeableSnapshots() {
    setupData("shouldSelectPurgeableSnapshots");
    List<PurgeableSnapshotDto> snapshots = sut.selectPurgeableSnapshots(1L);

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
    setupData("shouldDeleteProject");
    sut.deleteResourceTree(new IdUuidPair(1L, "A"));
    assertEmptyTables("projects", "snapshots", "action_plans", "issues", "issue_changes", "file_sources");
  }

  @Test
  public void should_delete_old_closed_issues() {
    setupData("should_delete_old_closed_issues");
    sut.purge(newConfigurationWith30Days(), PurgeListener.EMPTY);
    checkTables("should_delete_old_closed_issues", "issues", "issue_changes");
  }

  @Test
  public void should_delete_all_closed_issues() {
    setupData("should_delete_all_closed_issues");
    sut.purge(new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 0), PurgeListener.EMPTY);
    checkTables("should_delete_all_closed_issues", "issues", "issue_changes");
  }

  @Test
  public void select_purgeable_file_uuids_and_only_them() {
    setupData("select_purgeable_file_uuids");

    List<String> uuids = sut.selectPurgeableFiles(dbSession, 1L);

    assertThat(uuids).containsOnly("GHIJ");
  }
}
