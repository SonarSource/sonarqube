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

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import static org.hamcrest.core.Is.is;

public class PastSnapshotFinderTest {

  @Test
  public void shouldFindByNumberOfDays() {
    PastSnapshotFinderByDays finderByDays = mock(PastSnapshotFinderByDays.class);
    when(finderByDays.findInDays(30)).thenReturn(new Snapshot());

    PastSnapshotFinder finder = new PastSnapshotFinder(finderByDays, null);
    PastSnapshot variationSnapshot = finder.find(1, "30");

    verify(finderByDays).findInDays(30);
    assertNotNull(variationSnapshot);
    assertThat(variationSnapshot.getIndex(), is(1));
    assertThat(variationSnapshot.getConfigurationMode(), is("days"));
    assertThat(variationSnapshot.getConfigurationModeParameter(), is("30"));
  }

  @Test
  public void shouldNotFindByNumberOfDays() {
    PastSnapshotFinderByDays finderByDays = mock(PastSnapshotFinderByDays.class);

    PastSnapshotFinder finder = new PastSnapshotFinder(finderByDays, null);
    PastSnapshot variationSnapshot = finder.find(1, "30");

    verify(finderByDays).findInDays(30);
    assertNull(variationSnapshot);
  }
}
