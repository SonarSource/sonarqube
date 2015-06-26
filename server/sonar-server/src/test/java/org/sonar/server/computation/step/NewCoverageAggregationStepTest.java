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

import com.google.common.base.Function;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import javax.annotation.Nonnull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepoEntry;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

@RunWith(DataProviderRunner.class)
public class NewCoverageAggregationStepTest {

  private static final DumbComponent MOST_SIMPLE_ONE_FILE_TREE = builder(PROJECT, 1)
    .addChildren(
      builder(MODULE, 2)
        .addChildren(
          builder(DIRECTORY, 3)
            .addChildren(
              builder(FILE, 4).build())
            .build()
        ).build()
    ).build();

  private static final DumbComponent MANY_FILES_TREE = builder(PROJECT, 1)
    .addChildren(
      builder(MODULE, 11)
        .addChildren(
          builder(DIRECTORY, 111)
            .addChildren(
              builder(FILE, 1111).build(),
              builder(FILE, 1112).build()
            ).build(),
          builder(DIRECTORY, 112)
            .addChildren(
              builder(FILE, 1121).build(),
              builder(FILE, 1122).build(),
              builder(FILE, 1123).build()
            ).build()
        ).build(),
      builder(MODULE, 12)
        .addChildren(
          builder(DIRECTORY, 121)
            .addChildren(
              builder(FILE, 1211).build(),
              builder(FILE, 1212).build()
            ).build(),
          builder(DIRECTORY, 122).build()
        ).build(),
      builder(MODULE, 13).build()
    ).build();

  private static final String SOME_MODE = "some mode";
  private static final long SOME_SNAPSHOT_DATE = 1234L;
  private static final long SOME_SNAPSHOT_ID = 876L;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public PeriodsHolderRule periodHolder = new PeriodsHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.NEW_LINES_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_IT_LINES_TO_COVER)
    .add(CoreMetrics.NEW_IT_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_IT_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_OVERALL_LINES_TO_COVER)
    .add(CoreMetrics.NEW_OVERALL_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private NewCoverageAggregationStep underTest = new NewCoverageAggregationStep(treeRootHolder, periodHolder, metricRepository, measureRepository);

  @DataProvider
  public static Object[][] trees_without_FILE_COMPONENT() {
    return new Object[][] {
      {builder(PROJECT, 1).build()},
      {builder(PROJECT, 1).addChildren(builder(MODULE, 2).build()).build()},
      {builder(PROJECT, 1).addChildren(builder(MODULE, 2).addChildren(builder(DIRECTORY, 3).build()).build()).build()},
    };
  }

  @Test
  @UseDataProvider("trees_without_FILE_COMPONENT")
  public void no_measures_added_if_there_is_only_PROJECT_component(Component root) {
    treeRootHolder.setRoot(root);

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measures_added_if_there_is_no_rawMeasure_on_FILE_component() {
    treeRootHolder.setRoot(MOST_SIMPLE_ONE_FILE_TREE);

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measures_added_if_there_is_rawMeasure_has_no_variation_on_FILE_component() {
    treeRootHolder.setRoot(MOST_SIMPLE_ONE_FILE_TREE);
    measureRepository.addRawMeasure(4, NEW_LINES_TO_COVER_KEY, newMeasureBuilder().createNoValue());

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(1).isEmpty()).isTrue();
    assertThat(measureRepository.getNewRawMeasures(2).isEmpty()).isTrue();
    assertThat(measureRepository.getNewRawMeasures(3).isEmpty()).isTrue();
    assertThat(measureRepository.getNewRawMeasures(4).isEmpty()).isTrue();
  }

  @Test
  public void no_measures_added_if_there_is_rawMeasure_is_not_NOVALUE_on_FILE_component() {
    treeRootHolder.setRoot(MOST_SIMPLE_ONE_FILE_TREE);
    measureRepository.addRawMeasure(4, NEW_LINES_TO_COVER_KEY, newMeasureBuilder().setVariations(new MeasureVariations(1d)).create("some value"));

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(1).isEmpty()).isTrue();
    assertThat(measureRepository.getNewRawMeasures(2).isEmpty()).isTrue();
    assertThat(measureRepository.getNewRawMeasures(3).isEmpty()).isTrue();
    assertThat(measureRepository.getNewRawMeasures(4).isEmpty()).isTrue();
  }

  @Test
  public void no_measures_added_if_there_is_no_period() {
    treeRootHolder.setRoot(MOST_SIMPLE_ONE_FILE_TREE);
    periodHolder.setPeriods();
    measureRepository.addRawMeasure(4, NEW_LINES_TO_COVER_KEY, createMeasure(2d));

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(1).isEmpty()).isTrue();
    assertThat(measureRepository.getNewRawMeasures(2).isEmpty()).isTrue();
    assertThat(measureRepository.getNewRawMeasures(3).isEmpty()).isTrue();
    assertThat(measureRepository.getNewRawMeasures(4).isEmpty()).isTrue();
  }

  @Test
  public void measures_added_even_if_rawMeasure_has_variation_0_on_FILE_component() {
    treeRootHolder.setRoot(MOST_SIMPLE_ONE_FILE_TREE);
    periodHolder.setPeriods(createPeriod(2));
    measureRepository.addRawMeasure(4, NEW_LINES_TO_COVER_KEY, createMeasure(0d));

    underTest.execute();

    MeasureRepoEntry expectedEntry = entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(0d));
    assertThat(toEntries(measureRepository.getNewRawMeasures(1))).containsOnly(expectedEntry);
    assertThat(toEntries(measureRepository.getNewRawMeasures(2))).containsOnly(expectedEntry);
    assertThat(toEntries(measureRepository.getNewRawMeasures(3))).containsOnly(expectedEntry);
    assertThat(measureRepository.getNewRawMeasures(4).isEmpty()).isTrue();
  }

  @Test
  public void measures_added_on_all_components_but_FILE_and_are_sum_of_childrens_value() {
    treeRootHolder.setRoot(MANY_FILES_TREE);
    periodHolder.setPeriods(createPeriod(2));
    for (Integer fileComponentRef : of(1111, 1112, 1121, 1122, 1123, 1211, 1212)) {
      measureRepository.addRawMeasure(fileComponentRef, NEW_LINES_TO_COVER_KEY, createMeasure((double) fileComponentRef));
    }

    underTest.execute();

    assertThat(toEntries(measureRepository.getNewRawMeasures(111))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(1111 + 1112))
      );
    assertThat(toEntries(measureRepository.getNewRawMeasures(112))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(1121 + 1122 + 1123))
      );
    assertThat(toEntries(measureRepository.getNewRawMeasures(121))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(1211 + 1212))
      );
    assertThat(measureRepository.getNewRawMeasures(122).isEmpty()).isTrue();
    assertThat(toEntries(measureRepository.getNewRawMeasures(11))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(1111 + 1112 + 1121 + 1122 + 1123))
      );
    assertThat(toEntries(measureRepository.getNewRawMeasures(12))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(1211 + 1212))
      );
    assertThat(measureRepository.getNewRawMeasures(13).isEmpty()).isTrue();
    assertThat(toEntries(measureRepository.getNewRawMeasures(1))).containsOnly(
      entryOf(NEW_LINES_TO_COVER_KEY, createMeasure(1111 + 1112 + 1121 + 1122 + 1123 + 1211 + 1212))
      );
  }

  @Test
  public void verify_measures_are_created_for_all_metrics() {
    treeRootHolder.setRoot(MOST_SIMPLE_ONE_FILE_TREE);
    periodHolder.setPeriods(createPeriod(2));
    for (Metric metric : metricRepository.getAll()) {
      measureRepository.addRawMeasure(4, metric.getKey(), createMeasure(metric.getKey().hashCode()));
    }

    underTest.execute();

    MeasureRepoEntry[] expectedEntries = from(metricRepository.getAll())
      .transform(new Function<Metric, MeasureRepoEntry>() {
        @Override
        @Nonnull
        public MeasureRepoEntry apply(@Nonnull Metric input) {
          return entryOf(input.getKey(), createMeasure(input.getKey().hashCode()));
        }
      }).toArray(MeasureRepoEntry.class);

    assertThat(toEntries(measureRepository.getNewRawMeasures(1))).containsOnly(expectedEntries);
    assertThat(toEntries(measureRepository.getNewRawMeasures(2))).containsOnly(expectedEntries);
    assertThat(toEntries(measureRepository.getNewRawMeasures(3))).containsOnly(expectedEntries);
    assertThat(measureRepository.getNewRawMeasures(4).isEmpty()).isTrue();
  }

  @Test
  public void verify_measures_are_created_for_all_periods() {
    treeRootHolder.setRoot(MOST_SIMPLE_ONE_FILE_TREE);
    periodHolder.setPeriods(createPeriod(1), createPeriod(2), createPeriod(3), createPeriod(4), createPeriod(5));
    Measure measure = newMeasureBuilder().setVariations(new MeasureVariations(5d, 4d, 3d, 2d, 1d)).createNoValue();
    measureRepository.addRawMeasure(4, NEW_LINES_TO_COVER_KEY, measure);

    underTest.execute();

    assertThat(toEntries(measureRepository.getNewRawMeasures(1))).containsOnly(entryOf(NEW_LINES_TO_COVER_KEY, measure));
    assertThat(toEntries(measureRepository.getNewRawMeasures(2))).containsOnly(entryOf(NEW_LINES_TO_COVER_KEY, measure));
    assertThat(toEntries(measureRepository.getNewRawMeasures(3))).containsOnly(entryOf(NEW_LINES_TO_COVER_KEY, measure));
    assertThat(measureRepository.getNewRawMeasures(4).isEmpty()).isTrue();
  }

  @Test
  public void verify_description() {
    assertThat(underTest.getDescription()).isEqualTo("Aggregates New Coverage measures");

  }

  private Measure createMeasure(double variation2) {
    return newMeasureBuilder().setVariations(new MeasureVariations(null, variation2)).createNoValue();
  }

  private static Period createPeriod(int periodIndex) {
    return new Period(periodIndex, SOME_MODE, null, SOME_SNAPSHOT_DATE, SOME_SNAPSHOT_ID);
  }
}
