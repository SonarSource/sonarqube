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

import java.text.ParseException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class PastSnapshotFinderByPreviousAnalysisTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldFindPreviousAnalysis() throws ParseException {
    setupData("shouldFindPreviousAnalysis");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByPreviousAnalysis finder = new PastSnapshotFinderByPreviousAnalysis(projectSnapshot, getSession());

    PastSnapshot pastSnapshot = finder.findByPreviousAnalysis();
    assertThat(pastSnapshot.getProjectSnapshotId(), is(1009));
  }

  @Test
  public void shouldNotFindPreviousAnalysis() throws ParseException {
    setupData("shouldNotFindPreviousAnalysis");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByPreviousAnalysis finder = new PastSnapshotFinderByPreviousAnalysis(projectSnapshot, getSession());

    assertNull(finder.findByPreviousAnalysis());
  }
}
