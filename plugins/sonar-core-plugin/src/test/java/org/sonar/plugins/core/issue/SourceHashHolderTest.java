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
package org.sonar.plugins.core.issue;

import static org.mockito.Mockito.verify;

import org.mockito.Mockito;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.resources.Resource;
import org.sonar.batch.scan.LastSnapshots;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourceHashHolderTest {

  SourceHashHolder sourceHashHolder;

  SonarIndex index;
  LastSnapshots lastSnapshots;
  Resource resource;

  @Before
  public void setUp() {
    index = mock(SonarIndex.class);
    lastSnapshots = mock(LastSnapshots.class);
    resource = mock(Resource.class);

    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, resource);
  }

  @Test
  public void should_lazy_load_source() {
    final String source = "source";
    when(index.getSource(resource)).thenReturn(source);

    assertThat(sourceHashHolder.getSource()).isEqualTo(source);
    verify(index).getSource(resource);

    assertThat(sourceHashHolder.getSource()).isEqualTo(source);
    Mockito.verifyNoMoreInteractions(index);
  }

  @Test
  public void should_lazy_load_reference_source() {
    final String source = "source";
    when(lastSnapshots.getSource(resource)).thenReturn(source);

    assertThat(sourceHashHolder.getReferenceSource()).isEqualTo(source);
    verify(lastSnapshots).getSource(resource);

    assertThat(sourceHashHolder.getReferenceSource()).isEqualTo(source);
    Mockito.verifyNoMoreInteractions(lastSnapshots);
  }

  @Test
  public void should_have_null_reference_source_for_null_resource() {
    sourceHashHolder = new SourceHashHolder(index, lastSnapshots, null);

    assertThat(sourceHashHolder.getReferenceSource()).isNull();
    Mockito.verifyNoMoreInteractions(lastSnapshots);
  }
}
