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
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplicationBuilder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasureBuilder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
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
  private FileLinesContextFactory contextFactory;
  private FileLinesContext linesContext;

  @Before
  public void before() throws IOException {
    when(context.measureBuilder()).thenReturn(new DefaultMeasureBuilder());
    inputFile = new DeprecatedDefaultInputFile("foo", "src/main/java/Foo.java");
    duplicationBuilder = spy(new DefaultDuplicationBuilder(inputFile));
    when(context.duplicationBuilder(any(InputFile.class))).thenReturn(duplicationBuilder);
    inputFile.setFile(temp.newFile("Foo.java"));
    contextFactory = mock(FileLinesContextFactory.class);
    linesContext = mock(FileLinesContext.class);
    when(contextFactory.createFor(inputFile)).thenReturn(linesContext);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNothingToSave() {
    JavaCpdEngine.save(context, inputFile, null, contextFactory);
    JavaCpdEngine.save(context, inputFile, Collections.EMPTY_LIST, contextFactory);

    verifyZeroInteractions(context);
  }

  @Test
  public void testOneSimpleDuplicationBetweenTwoFiles() {
    inputFile.setLines(300);
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214)));
    JavaCpdEngine.save(context, inputFile, groups, contextFactory);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(200).build());

    InOrder inOrder = Mockito.inOrder(duplicationBuilder);
    inOrder.verify(duplicationBuilder).originBlock(5, 204);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key2", 15, 214);
    inOrder.verify(duplicationBuilder).build();

    verify(linesContext).setIntValue(CoreMetrics.DUPLICATION_LINES_DATA_KEY, 1, 0);
    verify(linesContext).setIntValue(CoreMetrics.DUPLICATION_LINES_DATA_KEY, 4, 0);
    verify(linesContext).setIntValue(CoreMetrics.DUPLICATION_LINES_DATA_KEY, 5, 1);
    verify(linesContext).setIntValue(CoreMetrics.DUPLICATION_LINES_DATA_KEY, 204, 1);
    verify(linesContext).setIntValue(CoreMetrics.DUPLICATION_LINES_DATA_KEY, 205, 0);
  }

  @Test
  public void testDuplicationOnSameFile() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key1", 0, 215, 414)));
    JavaCpdEngine.save(context, inputFile, groups, contextFactory);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(2).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(400).build());

    InOrder inOrder = Mockito.inOrder(duplicationBuilder);
    inOrder.verify(duplicationBuilder).originBlock(5, 204);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key1", 215, 414);
    inOrder.verify(duplicationBuilder).build();
  }

  @Test
  public void testOneDuplicatedGroupInvolvingMoreThanTwoFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214), new ClonePart("key3", 0, 25, 224)));
    JavaCpdEngine.save(context, inputFile, groups, contextFactory);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(200).build());

    InOrder inOrder = Mockito.inOrder(duplicationBuilder);
    inOrder.verify(duplicationBuilder).originBlock(5, 204);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key2", 15, 214);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key3", 25, 224);
    inOrder.verify(duplicationBuilder).build();

    verify(context).saveDuplications(inputFile, Arrays.asList(
      new DuplicationGroup(new DuplicationGroup.Block("foo:src/main/java/Foo.java", 5, 200))
        .addDuplicate(new DuplicationGroup.Block("key2", 15, 200))
        .addDuplicate(new DuplicationGroup.Block("key3", 25, 200))
      ));
  }

  @Test
  public void testTwoDuplicatedGroupsInvolvingThreeFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(
      newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214)),
      newCloneGroup(new ClonePart("key1", 0, 15, 214), new ClonePart("key3", 0, 15, 214)));
    JavaCpdEngine.save(context, inputFile, groups, contextFactory);

    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(2).build());
    verify(context).addMeasure(new DefaultMeasureBuilder().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(210).build());

    InOrder inOrder = Mockito.inOrder(duplicationBuilder);
    inOrder.verify(duplicationBuilder).originBlock(5, 204);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key2", 15, 214);
    inOrder.verify(duplicationBuilder).originBlock(15, 214);
    inOrder.verify(duplicationBuilder).isDuplicatedBy("key3", 15, 214);
    inOrder.verify(duplicationBuilder).build();
  }

  private CloneGroup newCloneGroup(ClonePart... parts) {
    return CloneGroup.builder().setLength(0).setOrigin(parts[0]).setParts(Arrays.asList(parts)).build();
  }

}
