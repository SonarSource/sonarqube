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
package org.sonar.api.batch.sensor.test.internal;

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.test.Coverage.CoverageType;
import org.sonar.api.measures.CoreMetrics;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultCoverageTest {

  private InputFile main = new DefaultInputFile("foo", "src/Foo.php").setType(InputFile.Type.MAIN);

  @Test
  public void testCreation() {
    DefaultCoverage coverage = new DefaultCoverage()
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .lineHits(1, 2)
      .lineHits(2, 5)
      .conditions(1, 2, 1);

    assertThat(coverage.file()).isEqualTo(main);
    assertThat(coverage.type()).isEqualTo(CoverageType.UNIT);
  }

  @Test
  public void testSaveUnitTests() {
    SensorStorage storage = mock(SensorStorage.class);
    new DefaultCoverage(storage)
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .lineHits(1, 2)
      .lineHits(2, 5)
      .lineHits(3, 0)
      .lineHits(4, 0)
      .conditions(1, 2, 1)
      .save();

    verify(storage).store(new DefaultMeasure<Integer>()
      .onFile(main)
      .forMetric(CoreMetrics.LINES_TO_COVER)
      .withValue(4));
    verify(storage).store(new DefaultMeasure<Integer>()
      .onFile(main)
      .forMetric(CoreMetrics.UNCOVERED_LINES)
      .withValue(2));
    verify(storage).store(new DefaultMeasure<String>()
      .onFile(main)
      .forMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA)
      .withValue("1=2;2=5;3=0;4=0"));
    verify(storage).store(new DefaultMeasure<Integer>()
      .onFile(main)
      .forMetric(CoreMetrics.CONDITIONS_TO_COVER)
      .withValue(2));
    verify(storage).store(new DefaultMeasure<Integer>()
      .onFile(main)
      .forMetric(CoreMetrics.UNCOVERED_CONDITIONS)
      .withValue(1));
    verify(storage).store(new DefaultMeasure<String>()
      .onFile(main)
      .forMetric(CoreMetrics.COVERED_CONDITIONS_BY_LINE)
      .withValue("1=1"));
    verify(storage).store(new DefaultMeasure<String>()
      .onFile(main)
      .forMetric(CoreMetrics.CONDITIONS_BY_LINE)
      .withValue("1=2"));
  }

}
