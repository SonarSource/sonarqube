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
package org.sonar.batch.index;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeasurePersisterTest extends AbstractDaoTestCase {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String TOO_LONG_FOR_VARCHAR_4000 = StringUtils.repeat("0123456789", 401);

  public static final int PROJECT_SNAPSHOT_ID = 3001;
  public static final int PACKAGE_SNAPSHOT_ID = 3002;
  public static final int FILE_SNAPSHOT_ID = 3003;
  public static final int COVERAGE_METRIC_ID = 2;

  MeasurePersister measurePersister;
  RuleFinder ruleFinder = mock(RuleFinder.class);
  Project project = new Project("foo");
  Directory aDirectory = Directory.create("org/foo");
  File aFile = File.create("org/foo/Bar.java");
  BatchResource projectResource = batchResource(project, PROJECT_SNAPSHOT_ID);
  BatchResource dirResource = batchResource(aDirectory, PACKAGE_SNAPSHOT_ID);
  BatchResource fileResource = batchResource(aFile, FILE_SNAPSHOT_ID);
  MeasureCache measureCache;

  @Before
  public void mockResourcePersister() {
    measureCache = mock(MeasureCache.class);
    ResourceCache resourceCache = mock(ResourceCache.class);
    when(resourceCache.get("foo")).thenReturn(projectResource);
    when(resourceCache.get("foo:org/foo/Bar.java")).thenReturn(fileResource);
    when(resourceCache.get("foo:org/foo")).thenReturn(dirResource);

    MetricFinder metricFinder = mock(MetricFinder.class);
    Metric ncloc = ncloc();
    Metric coverage = coverage();
    when(metricFinder.findByKey(ncloc.getKey())).thenReturn(ncloc);
    when(metricFinder.findByKey(coverage.getKey())).thenReturn(coverage);

    measurePersister = new MeasurePersister(getMyBatis(), ruleFinder, metricFinder, measureCache, resourceCache);
  }

  @Test
  public void should_insert_measure() {
    setupData("empty");

    Measure measure = new Measure(ncloc()).setValue(1234.0);
    when(measureCache.entries()).thenReturn(Arrays.asList(new Cache.Entry<Measure>(new String[] {"foo", "ncloc"}, measure)));
    measurePersister.persist();

    checkTables("shouldInsertMeasure", "project_measures");
  }

  @Test
  public void should_display_message_when_error_during_insert_measure() {
    setupData("empty");

    Measure measure = new Measure(ncloc()).setValue(1234.0).setAlertText(TOO_LONG_FOR_VARCHAR_4000);
    when(measureCache.entries()).thenReturn(Arrays.asList(new Cache.Entry<Measure>(new String[] {"foo", "ncloc"}, measure)));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to save some measures");

    measurePersister.persist();
  }

  @Test
  public void should_insert_rule_measure() {
    setupData("empty");

    Rule rule = Rule.create("pmd", "key");
    when(ruleFinder.findByKey(rule.ruleKey())).thenReturn(rule);

    Measure measure = new RuleMeasure(ncloc(), rule, RulePriority.MAJOR, 1).setValue(1234.0);
    when(measureCache.entries()).thenReturn(Arrays.asList(new Cache.Entry<Measure>(new String[] {"foo", "ncloc"}, measure)));

    measurePersister.persist();

    checkTables("shouldInsertRuleMeasure", "project_measures");
  }

  @Test
  public void should_insert_measure_with_text_data() {
    setupData("empty");

    Measure withLargeData = new Measure(ncloc()).setData(TOO_LONG_FOR_VARCHAR_4000);
    when(measureCache.entries()).thenReturn(Arrays.asList(new Cache.Entry<Measure>(new String[] {"foo", "ncloc"}, withLargeData)));

    measurePersister.persist();

    checkTables("shouldInsertMeasureWithLargeData", "project_measures");
  }

  @Test
  public void should_not_save_best_values() {
    setupData("empty");

    Measure measure = new Measure(coverage()).setValue(100.0);
    when(measureCache.entries()).thenReturn(Arrays.asList(new Cache.Entry<Measure>(new String[] {"foo:org/foo/Bar.java", "coverage"}, measure)));

    measurePersister.persist();

    assertEmptyTables("project_measures");
  }

  @Test
  public void should_not_save_memory_only_measures() {
    setupData("empty");

    Measure measure = new Measure("ncloc").setPersistenceMode(PersistenceMode.MEMORY);
    when(measureCache.entries()).thenReturn(Arrays.asList(new Cache.Entry<Measure>(new String[] {"foo:org/foo/Bar.java", "ncloc"}, measure)));

    measurePersister.persist();

    assertEmptyTables("project_measures");
  }

  @Test
  public void should_always_save_non_file_measures() {
    setupData("empty");

    Measure measure1 = new Measure(ncloc()).setValue(200.0);
    Measure measure2 = new Measure(ncloc()).setValue(300.0);
    when(measureCache.entries()).thenReturn(Arrays.asList(
      new Cache.Entry<Measure>(new String[] {"foo", "ncloc"}, measure1),
      new Cache.Entry<Measure>(new String[] {"foo:org/foo", "ncloc"}, measure2)));

    measurePersister.persist();

    checkTables("shouldAlwaysPersistNonFileMeasures", "project_measures");
  }

  @Test
  public void should_not_save_some_file_measures_with_best_value() {
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
  public void should_not_save_measures_without_data() {
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, new Measure(CoreMetrics.LINES))).isFalse();

    Measure duplicatedLines = new Measure(CoreMetrics.DUPLICATED_LINES_DENSITY);
    assertThat(MeasurePersister.shouldPersistMeasure(aFile, duplicatedLines)).isFalse();
  }

  private static BatchResource batchResource(Resource resource, int id) {
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.getId()).thenReturn(id);
    return new BatchResource(1, resource, null).setSnapshot(snapshot);
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
