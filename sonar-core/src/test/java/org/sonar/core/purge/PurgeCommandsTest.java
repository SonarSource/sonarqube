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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class PurgeCommandsTest extends AbstractDaoTestCase {

  private PurgeProfiler profiler;

  @Before
  public void prepare() {
    profiler = new PurgeProfiler();
  }

  /**
   * Test that all related data is deleted.
   */
  @Test
  public void shouldDeleteSnapshot() {
    setupData("shouldDeleteSnapshot");

    SqlSession session = getMyBatis().openSession();
    try {
      new PurgeCommands(session, profiler).deleteSnapshots(PurgeSnapshotQuery.create().setId(5L));
    } finally {
      MyBatis.closeQuietly(session);
    }
    checkTables("shouldDeleteSnapshot", "snapshots", "project_measures", "duplications_index", "events", "dependencies");
  }

  /**
   * Test that SQL queries execution do not fail with a huge number of parameter
   */
  @Test
  public void should_not_fail_when_deleting_huge_number_of_snapshots() {
    SqlSession session = getMyBatis().openSession();
    try {
      new PurgeCommands(session, profiler).deleteSnapshots(getHugeNumberOfIds());
    } finally {
      MyBatis.closeQuietly(session);
    }
    // The goal of this test is only to check that the query do no fail, not to check result
  }

  /**
   * Test that all related data is purged.
   */
  @Test
  public void shouldPurgeSnapshot() {
    setupData("shouldPurgeSnapshot");

    SqlSession session = getMyBatis().openSession();
    try {
      new PurgeCommands(session, profiler).purgeSnapshots(PurgeSnapshotQuery.create().setId(1L));
    } finally {
      MyBatis.closeQuietly(session);
    }
    checkTables("shouldPurgeSnapshot",
      "snapshots", "project_measures", "duplications_index", "events", "dependencies");
  }

  @Test
  public void delete_wasted_measures_when_purging_snapshot() {
    setupData("shouldDeleteWastedMeasuresWhenPurgingSnapshot");

    SqlSession session = getMyBatis().openSession();
    try {
      new PurgeCommands(session, profiler).purgeSnapshots(PurgeSnapshotQuery.create().setId(1L));
    } finally {
      MyBatis.closeQuietly(session);
    }
    checkTables("shouldDeleteWastedMeasuresWhenPurgingSnapshot", "project_measures");
  }

  /**
   * Test that SQL queries execution do not fail with a huge number of parameter
   */
  @Test
  public void should_not_fail_when_purging_huge_number_of_snapshots() {
    SqlSession session = getMyBatis().openSession();
    try {
      new PurgeCommands(session, profiler).purgeSnapshots(getHugeNumberOfIds());
    } finally {
      MyBatis.closeQuietly(session);
    }
    // The goal of this test is only to check that the query do no fail, not to check result
  }

  @Test
  public void shouldDeleteResource() {
    setupData("shouldDeleteResource");
    try (SqlSession session = getMyBatis().openSession()) {
      new PurgeCommands(session, profiler).deleteResources(newArrayList(new IdUuidPair(1L, "1")));
    }

    assertEmptyTables("projects", "snapshots", "events", "issues", "issue_changes", "authors");
  }

  /**
   * Test that SQL queries execution do not fail with a huge number of parameter
   */
  @Test
  public void should_not_fail_when_deleting_huge_number_of_resources() {
    SqlSession session = getMyBatis().openSession();
    try {
      new PurgeCommands(session, profiler).deleteResources(getHugeNumberOfIdUuids());
    } finally {
      MyBatis.closeQuietly(session);
    }
    // The goal of this test is only to check that the query do no fail, not to check result
  }

  private List<IdUuidPair> getHugeNumberOfIdUuids() {
    List<IdUuidPair> hugeNbOfSnapshotIds = newArrayList();
    for (long i = 0; i < 4500; i++) {
      hugeNbOfSnapshotIds.add(new IdUuidPair(i, String.valueOf(i)));
    }
    return hugeNbOfSnapshotIds;
  }

  private List<Long> getHugeNumberOfIds() {
    List<Long> hugeNbOfSnapshotIds = newArrayList();
    for (long i = 0; i < 4500; i++) {
      hugeNbOfSnapshotIds.add(i);
    }
    return hugeNbOfSnapshotIds;
  }

}
