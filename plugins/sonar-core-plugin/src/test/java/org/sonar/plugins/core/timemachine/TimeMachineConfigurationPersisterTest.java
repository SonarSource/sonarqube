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
package org.sonar.plugins.core.timemachine;

import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeMachineConfigurationPersisterTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldSaveVariationConfigurationInSnapshotsTable() {
    setupData("shared");

    TimeMachineConfiguration conf = mock(TimeMachineConfiguration.class);
    PastSnapshot vs1 = new PastSnapshot(1, "days", getSession().getSingleResult(Snapshot.class, "id", 100))
        .setModeParameter("30");
    PastSnapshot vs3 = new PastSnapshot(3, "version", getSession().getSingleResult(Snapshot.class, "id", 300))
        .setModeParameter("1.2.3");
    when(conf.getProjectPastSnapshots()).thenReturn(Arrays.asList(vs1, vs3));
    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1000);

    TimeMachineConfigurationPersister persister = new TimeMachineConfigurationPersister(conf, projectSnapshot, getSession());
    persister.start();

    checkTables("shouldSaveVariationConfigurationInSnapshotsTable", "snapshots");
  }
}
