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

import org.sonar.batch.deprecated.components.PastSnapshotFinderByPreviousAnalysis;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class PastSnapshotFinderByPreviousAnalysisTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldFindPreviousAnalysis() {
    setupData("shouldFindPreviousAnalysis");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByPreviousAnalysis finder = new PastSnapshotFinderByPreviousAnalysis(getSession());

    PastSnapshot pastSnapshot = finder.findByPreviousAnalysis(projectSnapshot);
    assertThat(pastSnapshot.getProjectSnapshotId(), is(1009));
  }

  @Test
  public void shouldReturnPastSnapshotEvenWhenNoPreviousAnalysis() {
    setupData("shouldNotFindPreviousAnalysis");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByPreviousAnalysis finder = new PastSnapshotFinderByPreviousAnalysis(getSession());

    PastSnapshot pastSnapshot = finder.findByPreviousAnalysis(projectSnapshot);
    assertThat(pastSnapshot.isRelatedToSnapshot(), is(false));
    assertThat(pastSnapshot.getProjectSnapshot(), nullValue());
    assertThat(pastSnapshot.getDate(), nullValue());
  }
}
