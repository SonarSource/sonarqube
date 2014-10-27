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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.resource.ResourceDao;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PurgeDaoTest extends AbstractDaoTestCase {

  System2 system2;

  PurgeDao dao;

  @Before
  public void createDao() {
    system2 = mock(System2.class);
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-04-09").getTime());

    dao = new PurgeDao(getMyBatis(), new ResourceDao(getMyBatis(), system2), new PurgeProfiler(), system2);
  }

  @Test
  public void shouldDeleteAbortedBuilds() {
    setupData("shouldDeleteAbortedBuilds");
    dao.purge(new PurgeConfiguration(1L, new String[0], 30));
    checkTables("shouldDeleteAbortedBuilds", "snapshots");
  }

  @Test
  public void shouldPurgeProject() {
    setupData("shouldPurgeProject");
    dao.purge(new PurgeConfiguration(1L, new String[0], 30));
    checkTables("shouldPurgeProject", "projects", "snapshots");
  }

  @Test
  public void shouldDeleteHistoricalDataOfDirectoriesAndFiles() {
    setupData("shouldDeleteHistoricalDataOfDirectoriesAndFiles");
    dao.purge(new PurgeConfiguration(1L, new String[] {Scopes.DIRECTORY, Scopes.FILE}, 30));
    checkTables("shouldDeleteHistoricalDataOfDirectoriesAndFiles", "projects", "snapshots");
  }

  @Test
  public void disable_resources_without_last_snapshot() {
    setupData("disable_resources_without_last_snapshot");
    dao.purge(new PurgeConfiguration(1L, new String[0], 30, system2));
    checkTables("disable_resources_without_last_snapshot", "projects", "snapshots", "issues");
  }

  @Test
  public void shouldDeleteSnapshots() {
    setupData("shouldDeleteSnapshots");
    dao.deleteSnapshots(PurgeSnapshotQuery.create().setIslast(false).setResourceId(1L));
    checkTables("shouldDeleteSnapshots", "snapshots");
  }

  @Test
  public void shouldSelectPurgeableSnapshots() {
    setupData("shouldSelectPurgeableSnapshots");
    List<PurgeableSnapshotDto> snapshots = dao.selectPurgeableSnapshots(1L);

    assertThat(snapshots).hasSize(3);
    assertThat(getById(snapshots, 1L).isLast()).isTrue();
    assertThat(getById(snapshots, 1L).hasEvents()).isFalse();
    assertThat(getById(snapshots, 4L).isLast()).isFalse();
    assertThat(getById(snapshots, 4L).hasEvents()).isFalse();
    assertThat(getById(snapshots, 5L).isLast()).isFalse();
    assertThat(getById(snapshots, 5L).hasEvents()).isTrue();
  }

  private static PurgeableSnapshotDto getById(List<PurgeableSnapshotDto> snapshots, long id) {
    for (PurgeableSnapshotDto snapshot : snapshots) {
      if (snapshot.getSnapshotId() == id) {
        return snapshot;
      }
    }
    return null;
  }

  @Test
  public void shouldDeleteProject() {
    setupData("shouldDeleteProject");
    dao.deleteResourceTree(1L);
    assertEmptyTables("projects", "snapshots", "action_plans", "issues", "issue_changes");
  }

  @Test
  public void should_delete_old_closed_issues() {
    setupData("should_delete_old_closed_issues");
    dao.purge(new PurgeConfiguration(1L, new String[0], 30));
    checkTables("should_delete_old_closed_issues", "issues", "issue_changes");
  }

  @Test
  public void should_delete_all_closed_issues() {
    setupData("should_delete_all_closed_issues");
    dao.purge(new PurgeConfiguration(1L, new String[0], 0));
    checkTables("should_delete_all_closed_issues", "issues", "issue_changes");
  }
}
