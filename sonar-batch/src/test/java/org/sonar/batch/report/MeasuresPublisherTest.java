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
package org.sonar.batch.report;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.sensor.duplication.Duplication;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.*;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.measure.MeasureCache;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeasuresPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MeasureCache measureCache;
  private DuplicationCache duplicationCache;
  private MeasuresPublisher publisher;
  private org.sonar.api.resources.File aFile = org.sonar.api.resources.File.create("org/foo/Bar.java");

  private org.sonar.api.resources.Resource sampleFile;

  @Before
  public void prepare() {
    Project p = new Project("foo").setAnalysisDate(new Date(1234567L));
    ResourceCache resourceCache = new ResourceCache();
    sampleFile = org.sonar.api.resources.File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    resourceCache.add(p, null).setSnapshot(new Snapshot().setId(2));
    resourceCache.add(sampleFile, null);
    measureCache = mock(MeasureCache.class);
    duplicationCache = mock(DuplicationCache.class);
    when(measureCache.byResource(any(Resource.class))).thenReturn(Collections.<Measure>emptyList());
    when(duplicationCache.byComponent(anyString())).thenReturn(Collections.<DefaultDuplication>emptyList());
    publisher = new MeasuresPublisher(resourceCache, measureCache, duplicationCache);
  }

  @Test
  public void publishMeasures() throws Exception {

    Measure measure1 = new Measure<>(CoreMetrics.COVERAGE)
      .setValue(2.0)
      .setAlertStatus(Level.ERROR)
      .setAlertText("Foo")
      .setTendency(-1)
      .setCharacteristic(mock(Characteristic.class));
    // No value on new_xxx
    Measure measure2 = new Measure<>(CoreMetrics.NEW_BLOCKER_VIOLATIONS)
      .setVariation1(1.0)
      .setVariation2(2.0)
      .setVariation3(3.0)
      .setVariation4(4.0)
      .setVariation5(5.0);
    // Manual measure
    Measure manual = new Measure<>(new Metric<>("manual_metric", ValueType.BOOL))
      .setValue(1.0)
      .setDescription("Manual");
    // Rule measure
    RuleMeasure ruleMeasureBySeverity = RuleMeasure.createForPriority(CoreMetrics.NCLOC, RulePriority.BLOCKER, 1.0);
    RuleMeasure ruleMeasureByRule = RuleMeasure.createForRule(CoreMetrics.NCLOC, RuleKey.of("squid", "S12345"), 1.0);
    // Sqale rating have both a value and a data
    Measure rating = new Measure<>(CoreMetrics.SQALE_RATING)
      .setValue(2.0)
      .setData("A");
    // Long measure
    Measure longMeasure = new Measure<>(CoreMetrics.TECHNICAL_DEBT)
      .setValue(1.0);
    // String value
    Measure stringMeasure = new Measure<>(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION)
      .setData("foo bar");

    when(measureCache.byResource(sampleFile)).thenReturn(Arrays.asList(measure1, measure2, manual, ruleMeasureBySeverity, ruleMeasureByRule, rating, longMeasure, stringMeasure));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    publisher.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);

    assertThat(reader.readComponentMeasures(1)).hasSize(0);
    List<org.sonar.batch.protocol.output.BatchReport.Measure> componentMeasures = reader.readComponentMeasures(2);
    assertThat(componentMeasures).hasSize(8);
    assertThat(componentMeasures.get(0).getDoubleValue()).isEqualTo(2.0);
    assertThat(componentMeasures.get(0).getAlertStatus()).isEqualTo("ERROR");
    assertThat(componentMeasures.get(0).getAlertText()).isEqualTo("Foo");

  }

  @Test
  public void publishMeasureAndDuplication() throws Exception {

    Measure measure1 = new Measure<>(CoreMetrics.COVERAGE)
      .setValue(2.0);

    when(measureCache.byResource(sampleFile)).thenReturn(Arrays.asList(measure1));
    DefaultDuplication dup1 = new DefaultDuplication()
      .setOriginBlock(new Duplication.Block("foo:src/Foo.php", 1, 10))
      .isDuplicatedBy("foo:src/Foo.php", 20, 50);
    DefaultDuplication dup2 = new DefaultDuplication()
      .setOriginBlock(new Duplication.Block("foo:src/Foo.php", 1, 10))
      .isDuplicatedBy("another", 20, 50);
    when(duplicationCache.byComponent("foo:src/Foo.php")).thenReturn(Arrays.asList(dup1, dup2));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    publisher.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);

    assertThat(reader.readComponentMeasures(1)).hasSize(0);
    List<org.sonar.batch.protocol.output.BatchReport.Measure> componentMeasures = reader.readComponentMeasures(2);
    assertThat(componentMeasures).hasSize(2);
    assertThat(componentMeasures.get(0).getStringValue())
      .isEqualTo(
        "<duplications><g><b s=\"1\" l=\"10\" r=\"foo:src/Foo.php\"/><b s=\"20\" l=\"31\" r=\"foo:src/Foo.php\"/></g><g><b s=\"1\" l=\"10\" r=\"foo:src/Foo.php\"/><b s=\"20\" l=\"31\" r=\"another\"/></g></duplications>");

  }

  @Test
  public void should_not_save_some_file_measures_with_best_value() {
    assertThat(MeasuresPublisher.shouldPersistMeasure(aFile, new Measure(CoreMetrics.LINES, 200.0))).isTrue();
    assertThat(MeasuresPublisher.shouldPersistMeasure(aFile, new Measure(CoreMetrics.DUPLICATED_LINES_DENSITY, 3.0))).isTrue();

    Measure duplicatedLines = new Measure(CoreMetrics.DUPLICATED_LINES_DENSITY, 0.0);
    assertThat(MeasuresPublisher.shouldPersistMeasure(aFile, duplicatedLines)).isFalse();

    duplicatedLines.setVariation1(0.0);
    assertThat(MeasuresPublisher.shouldPersistMeasure(aFile, duplicatedLines)).isFalse();

    duplicatedLines.setVariation1(-3.0);
    assertThat(MeasuresPublisher.shouldPersistMeasure(aFile, duplicatedLines)).isTrue();
  }

  @Test
  public void should_not_save_measures_without_data() {
    assertThat(MeasuresPublisher.shouldPersistMeasure(aFile, new Measure(CoreMetrics.LINES))).isFalse();

    Measure duplicatedLines = new Measure(CoreMetrics.DUPLICATED_LINES_DENSITY);
    assertThat(MeasuresPublisher.shouldPersistMeasure(aFile, duplicatedLines)).isFalse();
  }

}
