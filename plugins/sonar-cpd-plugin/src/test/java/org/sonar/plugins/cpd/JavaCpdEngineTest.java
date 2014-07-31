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
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasureBuilder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.duplication.DefaultDuplicationBuilder;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JavaCpdEngineTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  SensorContext context = mock(SensorContext.class);
  DeprecatedDefaultInputFile inputFile;
  private DefaultDuplicationBuilder duplicationBuilder;

  @Before
  public void before() throws IOException {
    when(context.measureBuilder()).thenReturn(new DefaultMeasureBuilder());
    inputFile = new DeprecatedDefaultInputFile("src/main/java/Foo.java");
    DuplicationCache duplicationCache = mock(DuplicationCache.class);
    duplicationBuilder = spy(new DefaultDuplicationBuilder(inputFile, duplicationCache));
    when(context.duplicationBuilder(any(InputFile.class))).thenReturn(duplicationBuilder);
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

    InOrder inOrder = Mockito.inOrder(duplicationBuilder);
    inOrder.verify(duplicationBuilder).originBlock(5, 204);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key2", 15, 214);
    inOrder.verify(duplicationBuilder).done();
  }

  @Test
  public void testDuplicationOnSameFile() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key1", 0, 215, 414)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(2).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(400).build());

    InOrder inOrder = Mockito.inOrder(duplicationBuilder);
    inOrder.verify(duplicationBuilder).originBlock(5, 204);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key1", 215, 414);
    inOrder.verify(duplicationBuilder).done();
  }

  @Test
  public void testOneDuplicatedGroupInvolvingMoreThanTwoFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214), new ClonePart("key3", 0, 25, 224)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(200).build());

    InOrder inOrder = Mockito.inOrder(duplicationBuilder);
    inOrder.verify(duplicationBuilder).originBlock(5, 204);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key2", 15, 214);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key3", 25, 224);
    inOrder.verify(duplicationBuilder).done();
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

    InOrder inOrder = Mockito.inOrder(duplicationBuilder);
    inOrder.verify(duplicationBuilder).originBlock(5, 204);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key2", 15, 214);
    inOrder.verify(duplicationBuilder).originBlock(15, 214);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key3", 15, 214);
    inOrder.verify(duplicationBuilder).done();
  }

  private CloneGroup newCloneGroup(ClonePart... parts) {
    return CloneGroup.builder().setLength(0).setOrigin(parts[0]).setParts(Arrays.asList(parts)).build();
  }

}
