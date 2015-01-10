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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.test.Coverage.CoverageType;
import org.sonar.api.measures.CoreMetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultCoverageTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

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
  public void testSaveLines() {
    SensorStorage storage = mock(SensorStorage.class);
    new DefaultCoverage(storage)
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .lineHits(1, 2)
      .lineHits(2, 5)
      .lineHits(3, 0)
      .lineHits(4, 0)
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
    verifyNoMoreInteractions(storage);
  }

  @Test
  public void testSaveConditions() {
    SensorStorage storage = mock(SensorStorage.class);
    new DefaultCoverage(storage)
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .conditions(1, 2, 1)
      .save();

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
    verifyNoMoreInteractions(storage);
  }

  @Test
  public void testSaveLinesAndConditions() {
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
    verifyNoMoreInteractions(storage);
  }

  @Test
  public void dontSaveTwice() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultCoverage coverage = new DefaultCoverage(storage)
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .lineHits(1, 2)
      .lineHits(2, 5)
      .lineHits(3, 0)
      .lineHits(4, 0);
    coverage.save();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("This object was already saved");

    coverage.save();
  }

  @Test
  public void fileIsMain() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Coverage is only supported on main files [test/FooTest.php]");

    new DefaultCoverage()
      .onFile(new DefaultInputFile("foo", "test/FooTest.php").setType(InputFile.Type.TEST))
      .ofType(CoverageType.UNIT);
  }

  @Test
  public void lineHitsValidation() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Line number should be positive and non zero [src/Foo.php:0]");

    new DefaultCoverage()
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .lineHits(0, 2);
  }

  @Test
  public void hitsPositive() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Hits should be positive [src/Foo.php:1]");

    new DefaultCoverage()
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .lineHits(1, -1);
  }

  @Test
  public void hitsNoDuplicate() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Hits already saved on line [src/Foo.php:1]");

    new DefaultCoverage()
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .lineHits(1, 2)
      .lineHits(1, 1);
  }

  @Test
  public void lineConditionValidation() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Line number should be positive and non zero [src/Foo.php:0]");

    new DefaultCoverage()
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .conditions(0, 2, 2);
  }

  @Test
  public void conditionsPositive() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Number of conditions should be positive [src/Foo.php:1]");

    new DefaultCoverage()
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .conditions(1, -1, 0);
  }

  @Test
  public void coveredConditionsPositive() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Number of covered conditions should be positive [src/Foo.php:1]");

    new DefaultCoverage()
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .conditions(1, 1, -1);
  }

  @Test
  public void coveredConditionsVsConditions() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Number of covered conditions can't exceed conditions [src/Foo.php:1]");

    new DefaultCoverage()
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .conditions(1, 2, 3);
  }

  @Test
  public void conditionsNoDuplicate() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Conditions already saved on line [src/Foo.php:1]");

    new DefaultCoverage()
      .onFile(main)
      .ofType(CoverageType.UNIT)
      .conditions(1, 4, 3)
      .conditions(1, 4, 2);
  }

}
