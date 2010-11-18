/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch;

import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import javax.persistence.Query;

import static org.mockito.Mockito.mock;

public class FinalizeSnapshotsJobTest extends AbstractDbUnitTestCase {


  @Test
  public void shouldUnflagPenultimateLastSnapshot() throws Exception {
    assertAnalysis(11, "shouldUnflagPenultimateLastSnapshot");
  }

  @Test
  public void doNotFailIfNoPenultimateLast() throws Exception {
    assertAnalysis(5, "doNotFailIfNoPenultimateLast");
  }

  @Test
  public void lastSnapshotIsNotUpdatedWhenAnalyzingPastSnapshot() {
    assertAnalysis(6, "lastSnapshotIsNotUpdatedWhenAnalyzingPastSnapshot");
  }

  private void assertAnalysis(int snapshotId, String fixture) {
    setupData("sharedFixture", fixture);

    FinalizeSnapshotsJob sensor = new FinalizeSnapshotsJob(mock(ServerMetadata.class), getSession(), loadSnapshot(snapshotId));
    sensor.execute(new Project("key"), null);

    getSession().stop();
    checkTables(fixture, "snapshots");
  }


  private Snapshot loadSnapshot(int id) {
    Query query = getSession().createQuery("SELECT s FROM Snapshot s WHERE s.id=:id");
    query.setParameter("id", id);
    return (Snapshot) query.getSingleResult();
  }
}
