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

package org.sonar.server.component.db;

import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.component.SnapshotQuery;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.core.component.SnapshotQuery.SORT_ORDER.ASC;
import static org.sonar.core.component.SnapshotQuery.SORT_ORDER.DESC;

public class SnapshotDaoTest extends AbstractDaoTestCase {

  DbSession session;

  SnapshotDao sut;

  @Before
  public void createDao() {
    session = getMyBatis().openSession(false);
    sut = new SnapshotDao();
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void get_by_key() {
    setupData("shared");

    SnapshotDto result = sut.selectNullableById(session, 3L);
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(3L);
    assertThat(result.getComponentId()).isEqualTo(3L);
    assertThat(result.getRootProjectId()).isEqualTo(1L);
    assertThat(result.getParentId()).isEqualTo(2L);
    assertThat(result.getRootId()).isEqualTo(1L);
    assertThat(result.getStatus()).isEqualTo("P");
    assertThat(result.getLast()).isTrue();
    assertThat(result.getPurgeStatus()).isEqualTo(1);
    assertThat(result.getDepth()).isEqualTo(1);
    assertThat(result.getScope()).isEqualTo("DIR");
    assertThat(result.getQualifier()).isEqualTo("PAC");
    assertThat(result.getVersion()).isEqualTo("2.1-SNAPSHOT");
    assertThat(result.getPath()).isEqualTo("1.2.");

    assertThat(result.getPeriodMode(1)).isEqualTo("days1");
    assertThat(result.getPeriodModeParameter(1)).isEqualTo("30");
    assertThat(result.getPeriodDate(1)).isEqualTo(1316815200000L);
    assertThat(result.getPeriodMode(2)).isEqualTo("days2");
    assertThat(result.getPeriodModeParameter(2)).isEqualTo("31");
    assertThat(result.getPeriodDate(2)).isEqualTo(1316901600000L);
    assertThat(result.getPeriodMode(3)).isEqualTo("days3");
    assertThat(result.getPeriodModeParameter(3)).isEqualTo("32");
    assertThat(result.getPeriodDate(3)).isEqualTo(1316988000000L);
    assertThat(result.getPeriodMode(4)).isEqualTo("days4");
    assertThat(result.getPeriodModeParameter(4)).isEqualTo("33");
    assertThat(result.getPeriodDate(4)).isEqualTo(1317074400000L);
    assertThat(result.getPeriodMode(5)).isEqualTo("days5");
    assertThat(result.getPeriodModeParameter(5)).isEqualTo("34");
    assertThat(result.getPeriodDate(5)).isEqualTo(1317160800000L);

    assertThat(result.getCreatedAt()).isEqualTo(1228172400000L);
    assertThat(result.getBuildDate()).isEqualTo(1317247200000L);

    assertThat(sut.selectNullableById(session, 999L)).isNull();
  }

  @Test
  public void lastSnapshot_returns_null_when_no_last_snapshot() {
    setupData("empty");

    SnapshotDto snapshot = sut.selectLastSnapshotByComponentId(session, 123L);

    assertThat(snapshot).isNull();
  }

  @Test
  public void lastSnapshot_from_one_resource() {
    setupData("snapshots");

    SnapshotDto snapshot = sut.selectLastSnapshotByComponentId(session, 2L);

    assertThat(snapshot).isNotNull();
    assertThat(snapshot.getId()).isEqualTo(4L);
  }

  @Test
  public void lastSnapshot_from_one_resource_without_last_is_null() {
    setupData("snapshots");

    SnapshotDto snapshot = sut.selectLastSnapshotByComponentId(session, 5L);

    assertThat(snapshot).isNull();
  }

  @Test
  public void snapshot_and_child_retrieved() {
    setupData("snapshots");

    List<SnapshotDto> snapshots = sut.selectSnapshotAndChildrenOfProjectScope(session, 1L);

    assertThat(snapshots).isNotEmpty();
    assertThat(snapshots).extracting("id").containsOnly(1L, 6L);
  }

  @Test
  public void select_snapshots_by_component_id() {
    setupData("snapshots");

    List<SnapshotDto> snapshots = sut.selectSnapshotsByComponentId(session, 1L);

    assertThat(snapshots).hasSize(3);
  }

  @Test
  public void select_snapshots_by_query() {
    setupData("select_snapshots_by_query");

    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery())).hasSize(6);

    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(1L))).hasSize(3);

    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(1L).setVersion("2.2-SNAPSHOT"))).extracting("id").containsOnly(3L);

    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(1L).setIsLast(true))).extracting("id").containsOnly(1L);
    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(1L).setIsLast(false))).extracting("id").containsOnly(2L, 3L);

    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(1L).setCreatedAfter(1228172400002L))).extracting("id").containsOnly(2L, 3L);
    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(1L).setCreatedBefore(1228172400002L))).extracting("id").containsOnly(1L);

    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(2L).setStatus("P"))).hasSize(1);
    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(2L).setStatus("U"))).hasSize(1);

    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(1L).setSort(BY_DATE, ASC)).get(0).getId()).isEqualTo(1L);
    assertThat(sut.selectSnapshotsByQuery(session, new SnapshotQuery().setComponentId(1L).setSort(BY_DATE, DESC)).get(0).getId()).isEqualTo(3L);
  }

  @Test
  public void select_previous_version_snapshots() throws Exception {
    setupData("select_previous_version_snapshots");

    List<SnapshotDto> snapshots = sut.selectPreviousVersionSnapshots(session, 1L, "1.2-SNAPSHOT");
    assertThat(snapshots).hasSize(2);

    SnapshotDto firstSnapshot = snapshots.get(0);
    assertThat(firstSnapshot.getVersion()).isEqualTo("1.1");

    // All snapshots are returned on an unknown version
    assertThat(sut.selectPreviousVersionSnapshots(session, 1L, "UNKNOWN")).hasSize(3);
  }

  @Test
  public void insert() {
    setupData("empty");

    SnapshotDto dto = defaultSnapshot().setCreatedAt(1403042400000L);

    sut.insert(session, dto);
    session.commit();

    assertThat(dto.getId()).isNotNull();
    checkTables("insert", "snapshots");
  }

  @Test
  public void set_snapshot_and_children_to_false_and_status_processed() {
    setupData("snapshots");
    SnapshotDto snapshot = defaultSnapshot().setId(1L);

    sut.updateSnapshotAndChildrenLastFlagAndStatus(session, snapshot, false, SnapshotDto.STATUS_PROCESSED);
    session.commit();

    List<SnapshotDto> snapshots = sut.selectSnapshotAndChildrenOfProjectScope(session, 1L);
    assertThat(snapshots).hasSize(2);
    assertThat(snapshots).extracting("id").containsOnly(1L, 6L);
    assertThat(snapshots).extracting("last").containsOnly(false);
    assertThat(snapshots).extracting("status").containsOnly(SnapshotDto.STATUS_PROCESSED);
  }

  @Test
  public void set_snapshot_and_children_isLast_flag_to_false() {
    setupData("snapshots");
    SnapshotDto snapshot = defaultSnapshot().setId(1L);

    sut.updateSnapshotAndChildrenLastFlag(session, snapshot, false);
    session.commit();

    List<SnapshotDto> snapshots = sut.selectSnapshotAndChildrenOfProjectScope(session, 1L);
    assertThat(snapshots).hasSize(2);
    assertThat(snapshots).extracting("id").containsOnly(1L, 6L);
    assertThat(snapshots).extracting("last").containsOnly(false);
  }

  @Test
  public void is_last_snapshot_when_no_previous_snapshot() {
    SnapshotDto snapshot = defaultSnapshot();

    boolean isLast = sut.isLast(snapshot, null);

    assertThat(isLast).isTrue();
  }

  @Test
  public void is_last_snapshot_when_previous_snapshot_is_older() {
    Date today = new Date();
    Date yesterday = DateUtils.addDays(today, -1);

    SnapshotDto snapshot = defaultSnapshot().setCreatedAt(today.getTime());
    SnapshotDto previousLastSnapshot = defaultSnapshot().setCreatedAt(yesterday.getTime());

    boolean isLast = sut.isLast(snapshot, previousLastSnapshot);

    assertThat(isLast).isTrue();
  }

  @Test
  public void is_not_last_snapshot_when_previous_snapshot_is_newer() {
    Date today = new Date();
    Date yesterday = DateUtils.addDays(today, -1);

    SnapshotDto snapshot = defaultSnapshot().setCreatedAt(yesterday.getTime());
    SnapshotDto previousLastSnapshot = defaultSnapshot().setCreatedAt(today.getTime());

    boolean isLast = sut.isLast(snapshot, previousLastSnapshot);

    assertThat(isLast).isFalse();
  }

  private static SnapshotDto defaultSnapshot() {
    return new SnapshotDto()
      .setComponentId(3L)
      .setRootProjectId(1L)
      .setParentId(2L)
      .setRootId(1L)
      .setStatus("P")
      .setLast(true)
      .setPurgeStatus(1)
      .setDepth(1)
      .setScope("DIR")
      .setQualifier("PAC")
      .setVersion("2.1-SNAPSHOT")
      .setPath("1.2.")
      .setPeriodMode(1, "days1")
      .setPeriodMode(2, "days2")
      .setPeriodMode(3, "days3")
      .setPeriodMode(4, "days4")
      .setPeriodMode(5, "days5")
      .setPeriodParam(1, "30")
      .setPeriodParam(2, "31")
      .setPeriodParam(3, "32")
      .setPeriodParam(4, "33")
      .setPeriodParam(5, "34")
      .setPeriodDate(1, 1_500_000_000_001L)
      .setPeriodDate(2, 1_500_000_000_002L)
      .setPeriodDate(3, 1_500_000_000_003L)
      .setPeriodDate(4, 1_500_000_000_004L)
      .setPeriodDate(5, 1_500_000_000_005L)
      .setBuildDate(1_500_000_000_006L);
  }
}
