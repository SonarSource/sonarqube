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
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;
import org.sonar.core.persistence.MyBatis;

import java.util.Arrays;

public class PurgeCommandsTest extends DaoTestCase {
  /**
   * Test that all related data is deleted.
   */
  @Test
  public void shouldDeleteSnapshot() {
    setupData("shouldDeleteSnapshot");

    SqlSession session = getMyBatis().openSession();
    try {
      PurgeCommands.deleteSnapshots(PurgeSnapshotQuery.create().setId(5L), session, session.getMapper(PurgeMapper.class));
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
      PurgeCommands.purgeSnapshots(PurgeSnapshotQuery.create().setId(1L), session, session.getMapper(PurgeMapper.class));
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
      PurgeCommands.purgeSnapshots(PurgeSnapshotQuery.create().setId(1L), session, session.getMapper(PurgeMapper.class));
    } finally {
      MyBatis.closeQuietly(session);
    }
    checkTables("shouldDeleteWastedMeasuresWhenPurgingSnapshot", "project_measures");
  }

  @Test
  public void shouldDeleteResource() {
    setupData("shouldDeleteResource");
    SqlSession session = getMyBatis().openSession();
    try {
      PurgeCommands.deleteResources(Arrays.asList(1L), session, session.getMapper(PurgeMapper.class), session.getMapper(PurgeVendorMapper.class));
    } finally {
      MyBatis.closeQuietly(session);
    }
    assertEmptyTables("projects", "snapshots", "events", "reviews", "review_comments", "authors");
  }

}
