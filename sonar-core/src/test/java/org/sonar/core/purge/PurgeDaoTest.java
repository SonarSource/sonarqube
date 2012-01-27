/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.purge;

import org.apache.ibatis.session.SqlSession;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class PurgeDaoTest extends DaoTestCase {

  private PurgeDao dao;

  @Before
  public void createDao() {
    dao = new PurgeDao(getMyBatis(), new ResourceDao(getMyBatis()));
  }

  /**
   * Test that all related data is deleted.
   */
  @Test
  public void shouldDeleteSnapshot() {
    setupData("shouldDeleteSnapshot");

    SqlSession session = getMyBatis().openSession();
    try {
      // this method does not commit and close the session
      dao.deleteSnapshot(5L, session.getMapper(PurgeMapper.class));
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
    checkTables("shouldDeleteSnapshot",
      "snapshots", "project_measures", "measure_data", "rule_failures", "snapshot_sources", "duplications_index", "events", "dependencies");
  }

  /**
   * Test that all related data is purged.
   */
  @Test
  public void shouldPurgeSnapshot() {
    setupData("shouldPurgeSnapshot");

    SqlSession session = getMyBatis().openSession();
    try {
      // this method does not commit and close the session
      dao.purgeSnapshot(1L, session.getMapper(PurgeMapper.class));
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
    checkTables("shouldPurgeSnapshot",
      "snapshots", "project_measures", "measure_data", "rule_failures", "snapshot_sources", "duplications_index", "events", "dependencies", "reviews");
  }

  @Test
  public void shouldDeleteWastedMeasuresWhenPurgingSnapshot() {
    setupData("shouldDeleteWastedMeasuresWhenPurgingSnapshot");

    SqlSession session = getMyBatis().openSession();
    try {
      // this method does not commit and close the session
      dao.purgeSnapshot(1L, session.getMapper(PurgeMapper.class));
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
    checkTables("shouldDeleteWastedMeasuresWhenPurgingSnapshot", "project_measures");
  }

  @Test
  public void shouldCloseReviewWhenDisablingResource() {
    setupData("shouldCloseReviewWhenDisablingResource");

    SqlSession session = getMyBatis().openSession();
    try {
      // this method does not commit and close the session
      dao.disableResource(1L, session.getMapper(PurgeMapper.class));
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
    checkTables("shouldCloseReviewWhenDisablingResource", /* excluded column */new String[]{"updated_at"}, "reviews");
  }


  @Test
  public void shouldPurgeProject() {
    setupData("shouldPurgeProject");
    dao.purgeProject(1);
    checkTables("shouldPurgeProject", "projects", "snapshots");
  }

  @Test
  public void shouldPurgeDirectoriesAndFiles() {
    setupData("shouldPurgeDirectoriesAndFiles");
    dao.purgeProject(1);
    checkTables("shouldPurgeDirectoriesAndFiles", "projects", "snapshots");
  }

  @Test
  public void shouldDisableResourcesWithoutLastSnapshot() {
    setupData("shouldDisableResourcesWithoutLastSnapshot");
    dao.purgeProject(1);
    checkTables("shouldDisableResourcesWithoutLastSnapshot", "projects", "snapshots");
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

    assertThat(snapshots, hasItem(new SnapshotMatcher(1L, true, false)));
    assertThat(snapshots, hasItem(new SnapshotMatcher(4L, false, false)));
    assertThat(snapshots, hasItem(new SnapshotMatcher(5L, false, true)));
    assertThat(snapshots.size(), is(3));
  }

  @Test
  public void shouldDeleteResource() {
    setupData("shouldDeleteResource");
    SqlSession session = getMyBatis().openSession();
    try {
      // this method does not commit and close the session
      dao.deleteResource(1L, session, session.getMapper(PurgeMapper.class));
      session.commit();

    } finally {
      MyBatis.closeQuietly(session);
    }
    assertEmptyTables("projects", "snapshots", "events");
  }

  @Test
  public void shouldDeleteProject() {
    setupData("shouldDeleteProject");
    dao.deleteProject(1L);
    assertEmptyTables("projects", "snapshots");
  }

  static final class SnapshotMatcher extends BaseMatcher<PurgeableSnapshotDto> {
    long snapshotId;
    boolean isLast;
    boolean hasReadOnlyEvents;

    SnapshotMatcher(long snapshotId, boolean last, boolean hasReadOnlyEvents) {
      this.snapshotId = snapshotId;
      this.isLast = last;
      this.hasReadOnlyEvents = hasReadOnlyEvents;
    }

    public boolean matches(Object o) {
      PurgeableSnapshotDto obj = (PurgeableSnapshotDto) o;
      return obj.getSnapshotId() == snapshotId && obj.isLast() == isLast && obj.hasReadOnlyEvents() == hasReadOnlyEvents;
    }

    public void describeTo(Description description) {
      description
        .appendText("snapshotId").appendValue(snapshotId)
        .appendText("isLast").appendValue(isLast)
        .appendText("hasReadOnlyEvents").appendValue(hasReadOnlyEvents);
    }
  }
}
