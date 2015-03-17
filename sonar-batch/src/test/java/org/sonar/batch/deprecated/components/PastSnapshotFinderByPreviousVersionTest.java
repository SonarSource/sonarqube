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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class PastSnapshotFinderByPreviousVersionTest extends AbstractDbUnitTestCase {

  private PastSnapshotFinderByPreviousVersion finder;

  @Before
  public void before() throws Exception {
    finder = new PastSnapshotFinderByPreviousVersion(getSession(), getMyBatis());
  }

  @Test
  public void shouldFindByPreviousVersion() {
    setupData("with-previous-version");

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1003);
    PastSnapshot foundSnapshot = finder.findByPreviousVersion(currentProjectSnapshot);
    assertThat(foundSnapshot.getProjectSnapshotId()).isEqualTo(1001);
    assertThat(foundSnapshot.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(foundSnapshot.getModeParameter()).isEqualTo("1.1");
  }

  @Test
  public void shouldFindByPreviousVersionWhenPreviousVersionDeleted() {
    setupData("with-previous-version-deleted");

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1003);
    PastSnapshot foundSnapshot = finder.findByPreviousVersion(currentProjectSnapshot);
    assertThat(foundSnapshot.getProjectSnapshotId()).isEqualTo(1000);
    assertThat(foundSnapshot.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(foundSnapshot.getModeParameter()).isEqualTo("1.0");
  }

  @Test
  public void testWithNoPreviousVersion() {
    setupData("no-previous-version");

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1003);
    PastSnapshot foundSnapshot = finder.findByPreviousVersion(currentProjectSnapshot);
    assertThat(foundSnapshot.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(foundSnapshot.getProjectSnapshot()).isNull();
    assertThat(foundSnapshot.getModeParameter()).isNull();
  }

}
