/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.index;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MeasurePersisterTest extends AbstractDaoTestCase {

  public static final int PROJECT_SNAPSHOT_ID = 3001;
  public static final int PACKAGE_SNAPSHOT_ID = 3002;
  public static final int FILE_SNAPSHOT_ID = 3003;
  public static final int COVERAGE_METRIC_ID = 2;

  private MeasurePersister measurePersister;
  private RuleFinder ruleFinder = mock(RuleFinder.class);
  private ResourcePersister resourcePersister = mock(ResourcePersister.class);
  private MemoryOptimizer memoryOptimizer = mock(MemoryOptimizer.class);
  private Project project = new Project("foo");
  private JavaPackage aPackage = new JavaPackage("org.foo");
  private JavaFile aFile = new JavaFile("org.foo.Bar");
  private Snapshot projectSnapshot = snapshot(PROJECT_SNAPSHOT_ID);
  private Snapshot packageSnapshot = snapshot(PACKAGE_SNAPSHOT_ID);

  @Before
  public void mockResourcePersister() {
    when(resourcePersister.getSnapshotOrFail(project)).thenReturn(projectSnapshot);
    when(resourcePersister.getSnapshot(project)).thenReturn(projectSnapshot);
    when(resourcePersister.getSnapshot(aPackage)).thenReturn(packageSnapshot);

    measurePersister = new MeasurePersister(getMyBatis(), resourcePersister, ruleFinder, memoryOptimizer);
  }

  @Test
  public void shouldInsertMeasure() {
    setupData("empty");

    Measure measure = new Measure(ncloc()).setValue(1234.0);
    measurePersister.saveMeasure(project, measure);

    checkTables("shouldInsertMeasure", "project_measures");
    verify(memoryOptimizer).evictDataMeasure(eq(measure), any(MeasureModel.class));
  }

  @Test
  public void shouldInsertRuleMeasure() {
    setupData("empty");

    Rule rule = Rule.create("pmd", "key");
    when(ruleFinder.findByKey("pmd", "key")).thenReturn(rule);

    Measure measure = new RuleMeasure(ncloc(), rule, RulePriority.MAJOR, 1).setValue(1234.0);
    measurePersister.saveMeasure(project, measure);

    checkTables("shouldInsertRuleMeasure", "project_measures");
  }

  @Test
  public void shouldInsertMeasureWithTextData() {
    setupData("empty");

    measurePersister.saveMeasure(project, new Measure(ncloc()).setData("SHORT"));
    measurePersister.saveMeasure(project, new Measure(ncloc()).setData(StringUtils.repeat("0123456789", 10)));

    checkTables("shouldInsertMeasureWithLargeData", "project_measures", "measure_data");
  }

  @Test
  public void shouldUpdateMeasure() {
    setupData("data");

    Measure measure = new Measure(coverage()).setValue(12.5).setId(1L);
    measurePersister.saveMeasure(project, measure);

    checkTables("shouldUpdateMeasure", "project_measures");
  }

  @Test
  public void shouldAddDelayedMeasureSeveralTimes() {
    setupData("empty");

    Measure measure = new Measure(ncloc());

    measure.setValue(200.0);
    measurePersister.setDelayedMode(true);
    measurePersister.saveMeasure(project, measure);

    measure.setValue(300.0);
    measurePersister.saveMeasure(project, measure);
    measurePersister.dump();

    checkTables("shouldAddDelayedMeasureSeveralTimes", "project_measures");
  }

  @Test
  public void shouldDelaySaving() {
    setupData("empty");

    measurePersister.setDelayedMode(true);
    measurePersister.saveMeasure(project, new Measure(ncloc()).setValue(1234.0));
    measurePersister.saveMeasure(aPackage, new Measure(ncloc()).setValue(50.0));

    assertEmptyTables("project_measures");

    measurePersister.dump();
    checkTables("shouldDelaySaving", "project_measures");
  }

  @Test
  public void shouldNotDelaySavingWithDatabaseOnlyMeasure() {
    setupData("empty");

    measurePersister.setDelayedMode(true);
    measurePersister.saveMeasure(project, new Measure(ncloc()).setValue(1234.0).setPersistenceMode(PersistenceMode.DATABASE));
    measurePersister.saveMeasure(aPackage, new Measure(ncloc()).setValue(50.0));

    checkTables("shouldInsertMeasure", "project_measures");
  }

  @Test
  public void shouldNotSaveBestValues() {
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, new Measure(coverage()).setValue(0.0))).isTrue();
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, new Measure(coverage()).setValue(75.8))).isTrue();
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, new Measure(coverage()).setValue(100.0))).isFalse();
  }

  @Test
  public void shouldNotSaveBestValueMeasuresInDelayedMode() {
    setupData("empty");

    measurePersister.setDelayedMode(true);
    measurePersister.saveMeasure(aFile, new Measure(coverage()).setValue(100.0));

    assertEmptyTables("project_measures");

    measurePersister.dump();

    assertEmptyTables("project_measures");
  }

  @Test
  public void shouldNotSaveMemoryOnlyMeasures() {
    Measure measure = new Measure("ncloc").setPersistenceMode(PersistenceMode.MEMORY);

    assertThat(MeasurePersister.shouldPersistMeasure(aPackage, measure)).isFalse();
  }

  @Test
  public void shouldAlwaysPersistNonFileMeasures() {
    assertThat(MeasurePersister.shouldPersistMeasure(project, new Measure(CoreMetrics.LINES, 200.0))).isTrue();
    assertThat(MeasurePersister.shouldPersistMeasure(aPackage, new Measure(CoreMetrics.LINES, 200.0))).isTrue();
  }

  @Test
  public void shouldNotPersistSomeFileMeasuresWithBestValue() {
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, new Measure(CoreMetrics.LINES, 200.0))).isTrue();
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, new Measure(CoreMetrics.DUPLICATED_LINES_DENSITY, 3.0))).isTrue();

    Measure duplicatedLines = new Measure(CoreMetrics.DUPLICATED_LINES_DENSITY, 0.0);
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, duplicatedLines)).isFalse();

    duplicatedLines.setVariation1(0.0);
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, duplicatedLines)).isFalse();

    duplicatedLines.setVariation1(-3.0);
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, duplicatedLines)).isTrue();
  }

  @Test
  public void nullValueAndNullVariationsShouldBeConsideredAsBestValue() {
    Measure measure = new Measure(CoreMetrics.NEW_VIOLATIONS_KEY);

    assertThat(MeasurePersister.isBestValueMeasure(measure, CoreMetrics.NEW_VIOLATIONS)).isTrue();
  }

  private static Snapshot snapshot(int id) {
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.getId()).thenReturn(id);
    return snapshot;
  }

  private static Metric ncloc() {
    Metric ncloc = mock(Metric.class);
    when(ncloc.getId()).thenReturn(1);
    when(ncloc.getKey()).thenReturn("ncloc");
    return ncloc;
  }

  private static Metric coverage() {
    Metric coverage = mock(Metric.class);
    when(coverage.getId()).thenReturn(COVERAGE_METRIC_ID);
    when(coverage.getKey()).thenReturn("coverage");
    when(coverage.isOptimizedBestValue()).thenReturn(true);
    when(coverage.getBestValue()).thenReturn(100.0);
    return coverage;
  }
}
