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

package org.sonar.server.computation.step;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class SwitchSnapshotStepTest {

  @ClassRule
  public static DbTester db = new DbTester();

  SwitchSnapshotStep sut;

  @Before
  public void before() {
    db.truncateTables();
    System2 system2 = mock(System2.class);
    when(system2.now()).thenReturn(DateUtils.parseDate("2011-09-29").getTime());
    this.sut = new SwitchSnapshotStep(new DbClient(db.database(), db.myBatis(), new SnapshotDao(system2)));
  }

  @Test
  public void one_switch_with_a_snapshot_and_his_children() throws IOException {
    db.prepareDbUnit(getClass(), "snapshots.xml");
    ComputationContext context = new ComputationContext(AnalysisReportDto.newForTests(1L).setSnapshotId(1L),
      ComponentTesting.newProjectDto());

    sut.execute(context);

    db.assertDbUnit(getClass(), "snapshots-result.xml", "snapshots");
  }

  @Test(expected = IllegalStateException.class)
  public void throw_IllegalStateException_when_not_finding_snapshot() throws IOException {
    db.prepareDbUnit(getClass(), "empty.xml");
    ComputationContext context = new ComputationContext(AnalysisReportDto.newForTests(1L).setSnapshotId(1L),
      ComponentTesting.newProjectDto());

    sut.execute(context);
  }
}
