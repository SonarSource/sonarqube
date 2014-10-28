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

package org.sonar.server.computation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.server.component.db.SnapshotDao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SwitchSnapshotStepTest {
  @Rule
  public TestDatabase db = new TestDatabase();

  private DbSession session;
  private SwitchSnapshotStep sut;

  @Before
  public void before() {
    this.session = db.myBatis().openSession(false);

    System2 system2 = mock(System2.class);
    when(system2.now()).thenReturn(DateUtils.parseDate("2011-09-29").getTime());
    this.sut = new SwitchSnapshotStep(new SnapshotDao(system2));
  }

  @After
  public void after() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void one_switch_with_a_snapshot_and_his_children() {
    db.prepareDbUnit(getClass(), "snapshots.xml");

    sut.execute(session, AnalysisReportDto.newForTests(1L).setSnapshotId(1L));
    session.commit();

    db.assertDbUnit(getClass(), "snapshots-result.xml", "snapshots");
  }

  @Test(expected = IllegalStateException.class)
  public void throw_IllegalStateException_when_not_finding_snapshot() {
    db.prepareDbUnit(getClass(), "empty.xml");

    sut.execute(session, AnalysisReportDto.newForTests(1L).setSnapshotId(1L));
  }
}
