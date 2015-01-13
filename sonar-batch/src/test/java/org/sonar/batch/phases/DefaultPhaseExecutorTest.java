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
package org.sonar.batch.phases;

import org.junit.Test;
import org.sonar.batch.index.MeasurePersister;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.batch.index.ScanPersister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultPhaseExecutorTest {

  @Test
  public void shouldSortPersisters() {
    ScanPersister otherPersister = mock(ScanPersister.class);
    MeasurePersister measurePersister = new MeasurePersister(null, null, null, null, null);
    ResourcePersister resourcePersister = new ResourcePersister(null, null, null, null, null);
    ScanPersister[] persisters = new ScanPersister[] {otherPersister, measurePersister, resourcePersister};
    DefaultPhaseExecutor executor = new DefaultPhaseExecutor(null, null, null, null, null, null,
      null, null, null, null, null, persisters, null, null, null, null, null, null, null, null);
    assertThat(executor.sortedPersisters()).containsSubsequence(resourcePersister, measurePersister);

    persisters = new ScanPersister[] {measurePersister, resourcePersister, otherPersister};
    executor = new DefaultPhaseExecutor(null, null, null, null, null, null,
      null, null, null, null, null, persisters, null, null, null, null, null, null, null, null);
    assertThat(executor.sortedPersisters()).containsSubsequence(resourcePersister, measurePersister);

    persisters = new ScanPersister[] {measurePersister, otherPersister, resourcePersister};
    executor = new DefaultPhaseExecutor(null, null, null, null, null, null,
      null, null, null, null, null, persisters, null, null, null, null, null, null, null, null);
    assertThat(executor.sortedPersisters()).containsSubsequence(resourcePersister, measurePersister);
  }

}
