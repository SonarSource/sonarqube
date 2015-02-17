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
package org.sonar.batch.sensor.coverage;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.core.config.ExclusionProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoverageExclusionsTest {

  private Settings settings;

  private CoverageExclusions filter;

  @Before
  public void createFilter() {
    settings = new Settings(new PropertyDefinitions(ExclusionProperties.all()));
    filter = new CoverageExclusions(settings);
  }

  @Test
  public void shouldNotFilterNonCoverageMetrics() {
    Measure otherMeasure = mock(Measure.class);
    when(otherMeasure.getMetric()).thenReturn(CoreMetrics.LINES);
    assertThat(filter.accept(mock(Resource.class), otherMeasure)).isTrue();
  }

  @Test
  public void shouldFilterFileBasedOnPattern() {
    Resource resource = File.create("src/org/polop/File.php", null, false);
    Measure coverageMeasure = mock(Measure.class);
    when(coverageMeasure.getMetric()).thenReturn(CoreMetrics.LINES_TO_COVER);

    settings.setProperty("sonar.coverage.exclusions", "src/org/polop/*");
    filter.initPatterns();
    assertThat(filter.accept(resource, coverageMeasure)).isFalse();
  }

  @Test
  public void shouldNotFilterFileBasedOnPattern() {
    Resource resource = File.create("src/org/polop/File.php", null, false);
    Measure coverageMeasure = mock(Measure.class);
    when(coverageMeasure.getMetric()).thenReturn(CoreMetrics.COVERAGE);

    settings.setProperty("sonar.coverage.exclusions", "src/org/other/*");
    filter.initPatterns();
    assertThat(filter.accept(resource, coverageMeasure)).isTrue();
  }
}
