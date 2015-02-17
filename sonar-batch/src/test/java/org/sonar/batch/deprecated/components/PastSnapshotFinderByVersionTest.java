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
package org.sonar.batch.deprecated.components;

import org.sonar.batch.components.PastSnapshot;

import org.sonar.batch.deprecated.components.PastSnapshotFinderByVersion;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import static org.assertj.core.api.Assertions.assertThat;

public class PastSnapshotFinderByVersionTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldFindByVersion() {
    setupData("shared");

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByVersion finder = new PastSnapshotFinderByVersion(getSession());

    PastSnapshot foundSnapshot = finder.findByVersion(currentProjectSnapshot, "1.1");
    assertThat(foundSnapshot.getProjectSnapshotId()).isEqualTo(1009);
    assertThat(foundSnapshot.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_VERSION);
  }

  @Test
  public void testIfNoVersionFound() {
    setupData("shared");

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByVersion finder = new PastSnapshotFinderByVersion(getSession());

    PastSnapshot foundSnapshot = finder.findByVersion(currentProjectSnapshot, "2.1");
    assertThat(foundSnapshot.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_VERSION);
    assertThat(foundSnapshot.getProjectSnapshot()).isNull();
    assertThat(foundSnapshot.getModeParameter()).isNull();
  }

}
