/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.SnapshotDao;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.ReportComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SwitchSnapshotStepTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  DbIdsRepositoryImpl dbIdsRepository = new DbIdsRepositoryImpl();

  SwitchSnapshotStep underTest;

  @Before
  public void before() {
    db.truncateTables();
    System2 system2 = mock(System2.class);
    when(system2.now()).thenReturn(DateUtils.parseDate("2011-09-29").getTime());
    underTest = new SwitchSnapshotStep(new DbClient(db.database(), db.myBatis(), new SnapshotDao()), treeRootHolder, dbIdsRepository);
  }

  @Test
  public void one_switch_with_a_snapshot_and_his_children() {
    db.prepareDbUnit(getClass(), "snapshots.xml");

    Component project = ReportComponent.DUMB_PROJECT;
    treeRootHolder.setRoot(project);
    dbIdsRepository.setSnapshotId(project, 1);

    underTest.execute();

    db.assertDbUnit(getClass(), "snapshots-result.xml", "snapshots");
  }
}
