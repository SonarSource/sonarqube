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
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;

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
}
