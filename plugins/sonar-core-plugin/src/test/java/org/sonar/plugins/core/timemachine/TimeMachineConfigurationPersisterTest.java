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
package org.sonar.plugins.core.timemachine;

import org.sonar.batch.components.TimeMachineConfiguration;

import org.sonar.batch.components.PastSnapshot;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.index.ResourceCache;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeMachineConfigurationPersisterTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldSaveConfigurationInSnapshotsTable() {
    setupData("shared");

    TimeMachineConfiguration timeMachineConfiguration = mock(TimeMachineConfiguration.class);
    PastSnapshot vs1 = new PastSnapshot("days", DateUtils.parseDate("2009-01-25"), getSession().getSingleResult(Snapshot.class, "id", 100))
      .setModeParameter("30").setIndex(1);
    PastSnapshot vs3 = new PastSnapshot("version", DateUtils.parseDate("2008-12-13"), getSession().getSingleResult(Snapshot.class, "id", 300))
      .setModeParameter("1.2.3").setIndex(3);
    when(timeMachineConfiguration.getProjectPastSnapshots()).thenReturn(Arrays.asList(vs1, vs3));
    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1000);

    ResourceCache resourceCache = new ResourceCache();
    Project project = new Project("foo");
    resourceCache.add(project, null).setSnapshot(projectSnapshot);

    TimeMachineConfigurationPersister persister = new TimeMachineConfigurationPersister(timeMachineConfiguration, resourceCache, getSession());

    persister.persistConfiguration(project);

    checkTables("shouldSaveConfigurationInSnapshotsTable", "snapshots");
  }
}
