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
package org.sonar.server.computation.step;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.measure.newcoverage.NewCoverageMetricKeys;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_BY_LINE_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_UNCOVERED_LINES_KEY;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.batch.protocol.output.BatchReport.Changesets;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.computation.measure.MeasureVariations.newMeasureVariationsBuilder;

public class NewCoverageMeasuresStepTest {
  private static final NewCoverageMetricKeys SOME_COVERAGE_METRIC_KEYS = new SomeNewCoverageMetricKeys();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    // adds metrics referenced by SomeNewCoverageMetricKeys
    .add(CoreMetrics.COVERAGE_LINE_HITS_DATA)
    .add(CoreMetrics.CONDITIONS_BY_LINE)
    .add(CoreMetrics.COVERED_CONDITIONS_BY_LINE)
    .add(CoreMetrics.NEW_LINES_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_CONDITIONS);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private NewCoverageMeasuresStep underTest = new NewCoverageMeasuresStep(treeRootHolder, periodsHolder, reportReader, measureRepository, metricRepository,
    SOME_COVERAGE_METRIC_KEYS);
  public static final DumbComponent FILE_COMPONENT = DumbComponent.builder(Component.Type.FILE, 1).setFileAttributes(new FileAttributes(false, null)).build();

  @Before
  public void setUp() throws Exception {
    periodsHolder.setPeriods(
      new Period(2, "mode_p_1", null, parseDate("2009-12-25").getTime(), 1),
      new Period(5, "mode_p_5", null, parseDate("2011-02-18").getTime(), 2));
  }

  @Test
  public void no_measure_for_PROJECT_component() {
    treeRootHolder.setRoot(DumbComponent.builder(Component.Type.PROJECT, 1).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measure_for_MODULE_component() {
    treeRootHolder.setRoot(DumbComponent.builder(Component.Type.MODULE, 1).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measure_for_DIRECTORY_component() {
    treeRootHolder.setRoot(DumbComponent.builder(Component.Type.DIRECTORY, 1).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measure_for_unit_test_FILE_component() {
    treeRootHolder.setRoot(DumbComponent.builder(Component.Type.FILE, 1).setFileAttributes(new FileAttributes(true, null)).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measures_for_FILE_component_without_code() {
    treeRootHolder.setRoot(DumbComponent.builder(Component.Type.FILE, 1).setFileAttributes(new FileAttributes(false, null)).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measures_for_FILE_component_without_CoverageData() {
    DumbComponent fileComponent = DumbComponent.builder(Component.Type.FILE, 1).setFileAttributes(new FileAttributes(false, null)).build();

    treeRootHolder.setRoot(fileComponent);
    reportReader.putChangesets(Changesets.newBuilder()
      .setComponentRef(fileComponent.getRef())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2008-05-18").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void verify_computation_of_measures_for_new_lines() {
    treeRootHolder.setRoot(FILE_COMPONENT);
    reportReader.putChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(Changesets.Changeset.newBuilder().build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2007-01-15").getTime())
        .build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2011-01-01").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(2)
      .build());
    measureRepository.addRawMeasure(FILE_COMPONENT.getRef(), COVERAGE_LINE_HITS_DATA_KEY, newMeasureBuilder().create("10=2;11=3"));

    underTest.execute();

    assertThat(toEntries(measureRepository.getNewRawMeasures(FILE_COMPONENT.getRef()))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(1d, null)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(0d, null)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(0d, null)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(0d, null))
      );
  }

  @Test
  public void verify_computation_of_measures_for_new_conditions() {
    treeRootHolder.setRoot(FILE_COMPONENT);
    reportReader.putChangesets(Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(Changesets.Changeset.newBuilder().build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2007-01-15").getTime())
        .build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2011-01-01").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(2)
      .build()
      );
    measureRepository.addRawMeasure(FILE_COMPONENT.getRef(), COVERAGE_LINE_HITS_DATA_KEY, newMeasureBuilder().create("10=2;11=3"));
    measureRepository.addRawMeasure(FILE_COMPONENT.getRef(), CONDITIONS_BY_LINE_KEY, newMeasureBuilder().create("11=4"));
    measureRepository.addRawMeasure(FILE_COMPONENT.getRef(), COVERED_CONDITIONS_BY_LINE_KEY, newMeasureBuilder().create("11=1"));

    underTest.execute();

    assertThat(toEntries(measureRepository.getNewRawMeasures(FILE_COMPONENT.getRef()))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(1d, null)),
      entryOf(NEW_UNCOVERED_LINES_KEY, createMeasure(0d, null)),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, createMeasure(4d, null)),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, createMeasure(3d, null))
      );
  }

  @Test
  public void verify_measure_of_condition_not_computed_if_there_is_none() {
    treeRootHolder.setRoot(FILE_COMPONENT);
    reportReader.putChangesets(Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(Changesets.Changeset.newBuilder().build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2007-01-15").getTime())
        .build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2011-01-01").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(2)
      .build()
      );

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void verify_no_measure_when_nothing_has_changed() {
    treeRootHolder.setRoot(FILE_COMPONENT);
    reportReader.putChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2008-08-02").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .build());
    measureRepository.addRawMeasure(FILE_COMPONENT.getRef(), COVERAGE_LINE_HITS_DATA_KEY, newMeasureBuilder().create("2=1;3=1"));
    measureRepository.addRawMeasure(FILE_COMPONENT.getRef(), CONDITIONS_BY_LINE_KEY, newMeasureBuilder().create("2=1"));
    measureRepository.addRawMeasure(FILE_COMPONENT.getRef(), COVERED_CONDITIONS_BY_LINE_KEY, newMeasureBuilder().create("2=1"));

    underTest.execute();

    assertThat(toEntries(measureRepository.getNewRawMeasures(FILE_COMPONENT.getRef()))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, newMeasureBuilder().createNoValue()),
      entryOf(NEW_UNCOVERED_LINES_KEY, newMeasureBuilder().createNoValue()),
      entryOf(NEW_CONDITIONS_TO_COVER_KEY, newMeasureBuilder().createNoValue()),
      entryOf(NEW_UNCOVERED_CONDITIONS_KEY, newMeasureBuilder().createNoValue())
      );
  }

  private static Measure createMeasure(@Nullable Double variationPeriod2, @Nullable Double variationPeriod5) {
    MeasureVariations.Builder variationBuilder = newMeasureVariationsBuilder();
    if (variationPeriod2 != null) {
      variationBuilder.setVariation(2, variationPeriod2);
    }
    if (variationPeriod5 != null) {
      variationBuilder.setVariation(5, variationPeriod5);
    }
    return newMeasureBuilder()
      .setVariations(variationBuilder.build())
      .createNoValue();
  }

  private static class SomeNewCoverageMetricKeys implements NewCoverageMetricKeys {
    @Override
    public String coverageLineHitsData() {
      return COVERAGE_LINE_HITS_DATA_KEY;
    }

    @Override
    public String conditionsByLine() {
      return CONDITIONS_BY_LINE_KEY;
    }

    @Override
    public String coveredConditionsByLine() {
      return COVERED_CONDITIONS_BY_LINE_KEY;
    }

    @Override
    public String newLinesToCover() {
      return NEW_LINES_TO_COVER_KEY;
    }

    @Override
    public String newUncoveredLines() {
      return NEW_UNCOVERED_LINES_KEY;
    }

    @Override
    public String newConditionsToCover() {
      return NEW_CONDITIONS_TO_COVER_KEY;
    }

    @Override
    public String newUncoveredConditions() {
      return NEW_UNCOVERED_CONDITIONS_KEY;
    }
  }
}
