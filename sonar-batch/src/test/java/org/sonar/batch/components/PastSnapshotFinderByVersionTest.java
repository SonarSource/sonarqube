/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class PastSnapshotFinderByVersionTest extends AbstractDbUnitTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldFindByVersion() {
    setupData("shared");

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByVersion finder = new PastSnapshotFinderByVersion(getSession());

    assertThat(finder.findByVersion(currentProjectSnapshot, "1.1").getProjectSnapshotId()).isEqualTo(1009);
  }

  @Test
  public void failIfUnknownVersion() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unknown project version: 0.1.2");

    setupData("shared");

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByVersion finder = new PastSnapshotFinderByVersion(getSession());

    finder.findByVersion(currentProjectSnapshot, "0.1.2");
  }
}
