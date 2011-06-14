/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.batch.components;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class PastSnapshotFinderByVersionTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldFindByVersion() {
    setupData("shared");

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByVersion finder = new PastSnapshotFinderByVersion(getSession());

    assertThat(finder.findByVersion(currentProjectSnapshot, "1.1").getProjectSnapshotId(), is(1009));
  }

  @Test
  public void shouldReturnPastSnapshotEvenWhenNoPreviousAnalysis() {
    setupData("shared");

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByVersion finder = new PastSnapshotFinderByVersion(getSession());

    PastSnapshot pastSnapshot = finder.findByVersion(currentProjectSnapshot, "1.0");
    assertThat(pastSnapshot.isRelatedToSnapshot(), is(false));
    assertThat(pastSnapshot.getProjectSnapshot(), nullValue());
    assertThat(currentProjectSnapshot.getCreatedAt().getTime() - pastSnapshot.getTargetDate().getTime(), is(1000L * 60));
  }
}
