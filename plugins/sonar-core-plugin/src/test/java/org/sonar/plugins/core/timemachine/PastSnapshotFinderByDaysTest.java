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

import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PastSnapshotFinderByDaysTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldGetNextSnapshot() throws ParseException {
    setupData("shared");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1009); // 2008-11-16
    PastSnapshotFinderByDays finder = new PastSnapshotFinderByDays(projectSnapshot, getSession());

    assertThat(finder.findFromDays(50).getProjectSnapshotId(), is(1000));
  }

  @Test
  public void shouldIgnoreUnprocessedSnapshots() throws ParseException {
    setupData("shared");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1009); // 2008-11-16
    PastSnapshotFinderByDays finder = new PastSnapshotFinderByDays(projectSnapshot, getSession());

    assertThat(finder.findFromDays(7).getProjectSnapshotId(), is(1006));
  }

  @Test
  public void shouldNotFindSelf() throws ParseException {
    setupData("shared");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1009); // 2008-11-16
    PastSnapshotFinderByDays finder = new PastSnapshotFinderByDays(projectSnapshot, getSession());

    assertThat(finder.findFromDays(1), nullValue());
  }

}
