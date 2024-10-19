/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExportMetricsStepIT {

  private static final MetricDto NCLOC = new MetricDto()
    .setUuid("1")
    .setKey("ncloc")
    .setShortName("Lines of code")
    .setEnabled(true);
  private static final MetricDto COVERAGE = new MetricDto()
    .setUuid("2")
    .setKey("coverage")
    .setShortName("Coverage")
    .setEnabled(true);


  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  MutableMetricRepository metricsHolder = new MutableMetricRepositoryImpl();
  FakeDumpWriter dumpWriter = new FakeDumpWriter();
  ExportMetricsStep underTest = new ExportMetricsStep(dbTester.getDbClient(), metricsHolder, dumpWriter);

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    dbTester.getDbClient().metricDao().insert(dbTester.getSession(), NCLOC, COVERAGE);
    dbTester.commit();
  }

  @Test
  public void export_zero_metrics() {
    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("0 metrics exported");
  }

  @Test
  public void export_metrics() {
    metricsHolder.add(NCLOC.getUuid());
    metricsHolder.add(COVERAGE.getUuid());

    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(Level.DEBUG)).contains("2 metrics exported");
    List<ProjectDump.Metric> exportedMetrics = dumpWriter.getWrittenMessagesOf(DumpElement.METRICS);

    ProjectDump.Metric ncloc = exportedMetrics.stream().filter(input -> input.getRef() == 0).findAny().orElseThrow();
    assertThat(ncloc.getRef()).isZero();
    assertThat(ncloc.getKey()).isEqualTo("ncloc");
    assertThat(ncloc.getName()).isEqualTo("Lines of code");

    ProjectDump.Metric coverage = exportedMetrics.stream().filter(input -> input.getRef() == 1).findAny().orElseThrow();
    assertThat(coverage.getRef()).isOne();
    assertThat(coverage.getKey()).isEqualTo("coverage");
    assertThat(coverage.getName()).isEqualTo("Coverage");
  }

  @Test
  public void throw_ISE_if_error() {
    metricsHolder.add(NCLOC.getUuid());
    dumpWriter.failIfMoreThan(0, DumpElement.METRICS);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Metric Export failed after processing 0 metrics successfully");
  }

  @Test
  public void test_getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Export metrics");
  }
}
