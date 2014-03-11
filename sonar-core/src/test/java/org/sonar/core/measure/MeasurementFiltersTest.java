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
package org.sonar.core.measure;

import org.fest.util.Arrays;
import org.junit.Test;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MeasurementFiltersTest {

  private MeasurementFilters filters;

  @Test
  public void shouldAcceptEverythingWithEmptyFilters() {
    filters = new MeasurementFilters();
    Resource resource = mock(Resource.class);
    Measure measure = mock(Measure.class);
    assertThat(filters.accept(resource, measure)).isTrue();
  }

  @Test
  public void shouldAcceptIfAllFiltersAccept() {
    Resource resource = mock(Resource.class);
    Measure measure = mock(Measure.class);
    MeasurementFilter filter1 = mock(MeasurementFilter.class);
    when(filter1.accept(resource, measure)).thenReturn(true);
    MeasurementFilter filter2 = mock(MeasurementFilter.class);
    when(filter2.accept(resource, measure)).thenReturn(true);

    filters = new MeasurementFilters(Arrays.array(filter1, filter2));
    assertThat(filters.accept(resource, measure)).isTrue();
  }

  @Test
  public void shouldNnotAcceptIfOneFilterDoesntAccept() {
    Resource resource = mock(Resource.class);
    Measure measure = mock(Measure.class);
    MeasurementFilter filter1 = mock(MeasurementFilter.class);
    when(filter1.accept(resource, measure)).thenReturn(false);
    MeasurementFilter filter2 = mock(MeasurementFilter.class);

    filters = new MeasurementFilters(Arrays.array(filter1, filter2));
    assertThat(filters.accept(resource, measure)).isFalse();
    verifyZeroInteractions(filter2);
  }
}
