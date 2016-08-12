/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.task.projectanalysis.step;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolderRule;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_NCLOC;
import static org.sonar.api.measures.CoreMetrics.NEW_NCLOC_KEY;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class ReportNewLinesMeasureStepTest {
  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int SUB_MODULE_1_REF = 123;
  private static final int SUB_MODULE_2_REF = 126;
  private static final int DIRECTORY_REF = 1234;
  private static final int DIRECTORY_2_REF = 1235;
  private static final int FILE_1_REF = 12341;
  private static final int FILE_2_REF = 12342;
  private static final int FILE_3_REF = 1261;
  private static final int FILE_4_REF = 1262;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, MODULE_REF)
            .addChildren(
              builder(MODULE, SUB_MODULE_1_REF)
                .addChildren(
                  builder(DIRECTORY, DIRECTORY_REF)
                    .addChildren(
                      builder(FILE, FILE_1_REF).build(),
                      builder(FILE, FILE_2_REF).build())
                    .build(),
                  builder(DIRECTORY, DIRECTORY_2_REF).build())
                .build(),
              builder(MODULE, SUB_MODULE_2_REF)
                .addChildren(
                  builder(FILE, FILE_3_REF).build(),
                  builder(FILE, FILE_4_REF).build())
                .build())
            .build())
        .build());
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule().setPeriods(
    new Period(2, "mode_p_1", null, parseDate("2009-12-25").getTime(), "u1"),
    new Period(5, "mode_p_5", null, parseDate("2011-02-18").getTime(), "u2"));
  @Rule
  public ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_NCLOC)
    .add(NCLOC_DATA);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  NewLinesMeasureStep underTest = new NewLinesMeasureStep(treeRootHolder, periodsHolder, metricRepository, measureRepository, scmInfoRepository);

  @Test
  public void compute_new_ncloc() {
    setChangesets(FILE_1_REF, FILE_2_REF, FILE_3_REF, FILE_4_REF);
    setNclocsExcept(FILE_1_REF, 2, 4, 6);
    setNclocsExcept(FILE_2_REF);
    setNclocsExcept(FILE_3_REF, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    setNclocsExcept(FILE_4_REF, 1, 2);

    underTest.execute();

    assertRawMeasureValue(FILE_1_REF, NEW_NCLOC_KEY, 12 - 1 - 3);
    assertRawMeasureValue(FILE_2_REF, NEW_NCLOC_KEY, 12 - 1);
    assertRawMeasureValue(FILE_3_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(FILE_4_REF, NEW_NCLOC_KEY, 12 - 1 - 2);
    assertRawMeasureValue(DIRECTORY_REF, NEW_NCLOC_KEY, 19);
    assertRawMeasureValue(DIRECTORY_2_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(SUB_MODULE_1_REF, NEW_NCLOC_KEY, 19);
    assertRawMeasureValue(SUB_MODULE_2_REF, NEW_NCLOC_KEY, 9);
    assertRawMeasureValue(MODULE_REF, NEW_NCLOC_KEY, 28);
    assertRawMeasureValue(ROOT_REF, NEW_NCLOC_KEY, 28);
  }

  @Test
  public void compute_with_no_changeset() {
    setNclocsExcept(FILE_1_REF);

    underTest.execute();

    assertNoRawMeasureValue();
  }

  @Test
  public void compute_with_no_ncloc_data() {
    underTest.execute();

    assertNoRawMeasureValue();
  }

  private void assertNoRawMeasureValue() {
    assertRawMeasureValue(FILE_1_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(FILE_2_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(FILE_3_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(FILE_4_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(DIRECTORY_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(DIRECTORY_2_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(SUB_MODULE_1_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(SUB_MODULE_2_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(MODULE_REF, NEW_NCLOC_KEY, 0);
    assertRawMeasureValue(ROOT_REF, NEW_NCLOC_KEY, 0);
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, double period2Value) {
    assertRawMeasureValue(componentRef, metricKey, period2Value, 0d);
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, double period2Value, double period5Value) {
    MeasureVariations variations = measureRepository.getAddedRawMeasure(componentRef, metricKey).get().getVariations();
    assertThat(variations.getVariation2()).isEqualTo(period2Value);
    assertThat(variations.getVariation5()).isEqualTo(period5Value);
  }

  private void setNclocsExcept(int componentRef, Integer... lineNumbersNotLineOfCode) {
    List<Integer> notLocNumbers = Arrays.asList(lineNumbersNotLineOfCode);
    Map<Integer, Integer> nclocData = IntStream.rangeClosed(1, 12)
      .filter(lineNumber -> !notLocNumbers.contains(lineNumber))
      .boxed()
      .collect(Collectors.toMap(Function.identity(), lineNumber -> 1));
    measureRepository.addRawMeasure(componentRef, NCLOC_DATA_KEY, Measure.newMeasureBuilder().create(KeyValueFormat.format(nclocData)));
  }

  private void setChangesets(int... componentRefs) {
    Arrays.stream(componentRefs)
      .forEach(componentRef -> scmInfoRepository.setScmInfo(componentRef,
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        // line 3 is older, part of no period
        Changeset.newChangesetBuilder().setDate(parseDate("2007-01-15").getTime()).setRevision("rev-2").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build(),
        Changeset.newChangesetBuilder().setDate(parseDate("2011-01-01").getTime()).setRevision("rev-1").build()));
  }

}
