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
package org.sonar.plugins.cpd;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasureBuilder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JavaCpdEngineTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  SensorContext context = mock(SensorContext.class);
  DeprecatedDefaultInputFile inputFile;

  @Before
  public void before() throws IOException {
    when(context.measureBuilder()).thenReturn(new DefaultMeasureBuilder());
    inputFile = new DeprecatedDefaultInputFile("src/main/java/Foo.java");
    inputFile.setFile(temp.newFile("Foo.java"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNothingToSave() {
    JavaCpdEngine.save(context, inputFile, null);
    JavaCpdEngine.save(context, inputFile, Collections.EMPTY_LIST);

    verifyZeroInteractions(context);
  }

  @Test
  public void testOneSimpleDuplicationBetweenTwoFiles() {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(200).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATIONS_DATA).onFile(inputFile).withValue("<duplications><g>"
      + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
      + "<b s=\"15\" l=\"200\" r=\"key2\"/>"
      + "</g></duplications>").build());
  }

  @Test
  public void testDuplicationOnSameFile() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key1", 0, 215, 414)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(2).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(400).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATIONS_DATA).onFile(inputFile).withValue("<duplications><g>"
      + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
      + "<b s=\"215\" l=\"200\" r=\"key1\"/>"
      + "</g></duplications>").build());

  }

  @Test
  public void testOneDuplicatedGroupInvolvingMoreThanTwoFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214), new ClonePart("key3", 0, 25, 224)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(200).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATIONS_DATA).onFile(inputFile).withValue("<duplications><g>"
      + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
      + "<b s=\"15\" l=\"200\" r=\"key2\"/>"
      + "<b s=\"25\" l=\"200\" r=\"key3\"/>"
      + "</g></duplications>").build());

  }

  @Test
  public void testTwoDuplicatedGroupsInvolvingThreeFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(
      newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214)),
      newCloneGroup(new ClonePart("key1", 0, 15, 214), new ClonePart("key3", 0, 15, 214)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(2).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(210).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATIONS_DATA).onFile(inputFile).withValue("<duplications>"
      + "<g>"
      + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
      + "<b s=\"15\" l=\"200\" r=\"key2\"/>"
      + "</g>"
      + "<g>"
      + "<b s=\"15\" l=\"200\" r=\"key1\"/>"
      + "<b s=\"15\" l=\"200\" r=\"key3\"/>"
      + "</g>"
      + "</duplications>").build());

  }

  @Test
  public void shouldEscapeXmlEntities() throws IOException {
    InputFile csharpFile = new DeprecatedDefaultInputFile("Loads/File Loads/Subs & Reds/SubsRedsDelivery.cs")
      .setFile(temp.newFile("SubsRedsDelivery.cs"));
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(
      new ClonePart("Loads/File Loads/Subs & Reds/SubsRedsDelivery.cs", 0, 5, 204),
      new ClonePart("Loads/File Loads/Subs & Reds/SubsRedsDelivery2.cs", 0, 15, 214)));
    JavaCpdEngine.save(context, csharpFile, groups);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATIONS_DATA).onFile(csharpFile).withValue("<duplications><g>"
      + "<b s=\"5\" l=\"200\" r=\"Loads/File Loads/Subs &amp; Reds/SubsRedsDelivery.cs\"/>"
      + "<b s=\"15\" l=\"200\" r=\"Loads/File Loads/Subs &amp; Reds/SubsRedsDelivery2.cs\"/>"
      + "</g></duplications>").build());
  }

  private CloneGroup newCloneGroup(ClonePart... parts) {
    return CloneGroup.builder().setLength(0).setOrigin(parts[0]).setParts(Arrays.asList(parts)).build();
  }

}
