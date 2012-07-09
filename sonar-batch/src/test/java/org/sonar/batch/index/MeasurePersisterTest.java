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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MeasurePersisterTest extends AbstractDbUnitTestCase {

  public static final int PROJECT_SNAPSHOT_ID = 3001;
  public static final int PACKAGE_SNAPSHOT_ID = 3002;
  public static final int FILE_SNAPSHOT_ID = 3003;
  public static final int COVERAGE_METRIC_ID = 2;

  private MeasurePersister measurePersister;
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

    measurePersister = new MeasurePersister(getSession(), resourcePersister, null, memoryOptimizer);
  }

  @Test
  public void shouldInsertMeasure() {
    setupData("shared");

    Measure measure = new Measure(ncloc()).setValue(1234.0);
    measurePersister.saveMeasure(project, measure);

    checkTables("shouldInsertMeasure", "project_measures");
  }

  @Test
  public void shouldRegisterPersistedMeasureToMemoryOptimizer() {
    Measure measure = new Measure(ncloc()).setValue(1234.0);
    measurePersister.saveMeasure(project, measure);

    verify(memoryOptimizer).evictDataMeasure(eq(measure), any(MeasureModel.class));
  }

  @Test
  public void shouldUpdateMeasure() {
    setupData("shared");

    Measure measure = new Measure(coverage()).setValue(12.5).setId(1L);
    measurePersister.saveMeasure(project, measure);

    checkTables("shouldUpdateMeasure", "project_measures");
  }

  @Test
  public void shouldAddDelayedMeasureSeveralTimes() {
    Measure measure = new Measure(ncloc()).setValue(200.0);

    measurePersister.setDelayedMode(true);
    measurePersister.saveMeasure(project, measure);

    measure.setValue(300.0);
    measurePersister.saveMeasure(project, measure);
    measurePersister.dump();

    List<MeasureModel> coverageMeasures = getSession().getResults(MeasureModel.class, "snapshotId", PROJECT_SNAPSHOT_ID, "metricId", 1);
    assertThat(coverageMeasures).onProperty("value").containsExactly(300.0);
  }

  @Test
  public void shouldDelaySaving() {
    setupData("shared");

    measurePersister.setDelayedMode(true);
    measurePersister.saveMeasure(project, new Measure(ncloc()).setValue(1234.0));
    measurePersister.saveMeasure(aPackage, new Measure(ncloc()).setValue(50.0));

    assertThat(getSession().getResults(MeasureModel.class, "metricId", 1)).isEmpty();

    measurePersister.dump();
    checkTables("shouldDelaySaving", "project_measures");
  }

  @Test
  public void shouldNotDelaySavingWithDatabaseOnlyMeasure() {
    setupData("shared");

    measurePersister.setDelayedMode(true);
    measurePersister.saveMeasure(project, new Measure(ncloc()).setValue(1234.0).setPersistenceMode(PersistenceMode.DATABASE)); // database
    measurePersister.saveMeasure(aPackage, new Measure(ncloc()).setValue(50.0)); // database + memory

    checkTables("shouldNotDelaySavingWithDatabaseOnlyMeasure", "project_measures");
  }

  @Test
  public void shouldNotSaveBestValues() {
    Measure measure = new Measure(coverage()).setValue(0.0);
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, measure)).isTrue();

    measure = new Measure(coverage()).setValue(75.8);
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, measure)).isTrue();

    measure = new Measure(coverage()).setValue(100.0);
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, measure)).isFalse();
  }

  @Test
  public void shouldNotSaveBestValueMeasuresInDelayedMode() {
    measurePersister.setDelayedMode(true);
    measurePersister.saveMeasure(aFile, new Measure(coverage()).setValue(100.0));

    assertThat(getSession().getResults(MeasureModel.class, "metricId", COVERAGE_METRIC_ID, "snapshotId", FILE_SNAPSHOT_ID)).isEmpty();

    measurePersister.dump();

    assertThat(getSession().getResults(MeasureModel.class, "metricId", COVERAGE_METRIC_ID, "snapshotId", FILE_SNAPSHOT_ID)).isEmpty();
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
