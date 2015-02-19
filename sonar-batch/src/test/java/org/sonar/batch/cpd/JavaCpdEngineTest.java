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
package org.sonar.batch.cpd;

import org.sonar.api.batch.sensor.internal.SensorStorage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.duplication.NewDuplication;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
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
  private SensorStorage storage = mock(SensorStorage.class);

  @Before
  public void before() throws IOException {
    when(context.newMeasure()).then(new Answer<Measure>() {
      @Override
      public Measure answer(InvocationOnMock invocation) throws Throwable {
        return new DefaultMeasure(storage);
      }
    });
    when(context.newDuplication()).then(new Answer<NewDuplication>() {
      @Override
      public NewDuplication answer(InvocationOnMock invocation) throws Throwable {
        return new DefaultDuplication(storage);
      }
    });
    inputFile = (DeprecatedDefaultInputFile) new DeprecatedDefaultInputFile("foo", "src/main/java/Foo.java").setLines(300);
    inputFile.setModuleBaseDir(temp.newFolder().toPath());
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
    inputFile.setLines(5);
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 2, 4), new ClonePart("key2", 0, 15, 17)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(1));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(3));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATION_LINES_DATA).onFile(inputFile).withValue("1=0;2=1;3=1;4=1;5=0"));

    verify(storage).store(new DefaultDuplication()
      .originBlock(inputFile, 2, 4)
      .isDuplicatedBy("key2", 15, 17));
  }

  @Test
  public void testDuplicationOnSameFile() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key1", 0, 215, 414)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(2));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(400));

    verify(storage).store(new DefaultDuplication()
      .originBlock(inputFile, 5, 204)
      .isDuplicatedBy("key1", 215, 414));
  }

  @Test
  public void testOneDuplicatedGroupInvolvingMoreThanTwoFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214), new ClonePart("key3", 0, 25, 224)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(1));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(200));

    verify(storage).store(new DefaultDuplication()
      .originBlock(inputFile, 5, 204)
      .isDuplicatedBy("key2", 15, 214)
      .isDuplicatedBy("key3", 25, 224));
  }

  @Test
  public void testTwoDuplicatedGroupsInvolvingThreeFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(
      newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214)),
      newCloneGroup(new ClonePart("key1", 0, 15, 214), new ClonePart("key3", 0, 15, 214)));
    JavaCpdEngine.save(context, inputFile, groups);

    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_FILES).onFile(inputFile).withValue(1));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_BLOCKS).onFile(inputFile).withValue(2));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.DUPLICATED_LINES).onFile(inputFile).withValue(210));

    verify(storage).store(new DefaultDuplication()
      .originBlock(inputFile, 5, 204)
      .isDuplicatedBy("key2", 15, 214));
    verify(storage).store(new DefaultDuplication()
      .originBlock(inputFile, 15, 214)
      .isDuplicatedBy("key3", 15, 214));
  }

  private CloneGroup newCloneGroup(ClonePart... parts) {
    return CloneGroup.builder().setLength(0).setOrigin(parts[0]).setParts(Arrays.asList(parts)).build();
  }

}
