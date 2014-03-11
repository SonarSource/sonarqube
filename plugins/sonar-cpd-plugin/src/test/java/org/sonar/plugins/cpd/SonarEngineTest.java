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
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.test.IsMeasure;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class SonarEngineTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  SensorContext context = mock(SensorContext.class);
  DefaultInputFile inputFile;

  @Before
  public void before() throws IOException {
    inputFile = new DefaultInputFile("src/main/java/Foo.java");
    inputFile.setFile(temp.newFile("Foo.java"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNothingToSave() {
    SonarEngine.save(context, inputFile, null);
    SonarEngine.save(context, inputFile, Collections.EMPTY_LIST);

    verifyZeroInteractions(context);
  }

  @Test
  public void testOneSimpleDuplicationBetweenTwoFiles() {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214)));
    SonarEngine.save(context, inputFile, groups);

    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(
      eq(inputFile),
      argThat(new IsMeasure(CoreMetrics.DUPLICATIONS_DATA, "<duplications><g>"
        + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
        + "<b s=\"15\" l=\"200\" r=\"key2\"/>"
        + "</g></duplications>")));
  }

  @Test
  public void testDuplicationOnSameFile() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key1", 0, 215, 414)));
    SonarEngine.save(context, inputFile, groups);

    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_LINES, 400d);
    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_BLOCKS, 2d);
    verify(context).saveMeasure(
      eq(inputFile),
      argThat(new IsMeasure(CoreMetrics.DUPLICATIONS_DATA, "<duplications><g>"
        + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
        + "<b s=\"215\" l=\"200\" r=\"key1\"/>"
        + "</g></duplications>")));
  }

  @Test
  public void testOneDuplicatedGroupInvolvingMoreThanTwoFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214), new ClonePart("key3", 0, 25, 224)));
    SonarEngine.save(context, inputFile, groups);

    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(
      eq(inputFile),
      argThat(new IsMeasure(CoreMetrics.DUPLICATIONS_DATA, "<duplications><g>"
        + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
        + "<b s=\"15\" l=\"200\" r=\"key2\"/>"
        + "<b s=\"25\" l=\"200\" r=\"key3\"/>"
        + "</g></duplications>")));
  }

  @Test
  public void testTwoDuplicatedGroupsInvolvingThreeFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(
      newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214)),
      newCloneGroup(new ClonePart("key1", 0, 15, 214), new ClonePart("key3", 0, 15, 214)));
    SonarEngine.save(context, inputFile, groups);

    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_BLOCKS, 2d);
    verify(context).saveMeasure(inputFile, CoreMetrics.DUPLICATED_LINES, 210d);
    verify(context).saveMeasure(
      eq(inputFile),
      argThat(new IsMeasure(CoreMetrics.DUPLICATIONS_DATA, "<duplications>"
        + "<g>"
        + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
        + "<b s=\"15\" l=\"200\" r=\"key2\"/>"
        + "</g>"
        + "<g>"
        + "<b s=\"15\" l=\"200\" r=\"key1\"/>"
        + "<b s=\"15\" l=\"200\" r=\"key3\"/>"
        + "</g>"
        + "</duplications>")));
  }

  @Test
  public void shouldEscapeXmlEntities() throws IOException {
    InputFile csharpFile = new DefaultInputFile("Loads/File Loads/Subs & Reds/SubsRedsDelivery.cs")
      .setFile(temp.newFile("SubsRedsDelivery.cs"));
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(
      new ClonePart("Loads/File Loads/Subs & Reds/SubsRedsDelivery.cs", 0, 5, 204),
      new ClonePart("Loads/File Loads/Subs & Reds/SubsRedsDelivery2.cs", 0, 15, 214)));
    SonarEngine.save(context, csharpFile, groups);

    verify(context).saveMeasure(
      eq(csharpFile),
      argThat(new IsMeasure(CoreMetrics.DUPLICATIONS_DATA, "<duplications><g>"
        + "<b s=\"5\" l=\"200\" r=\"Loads/File Loads/Subs &amp; Reds/SubsRedsDelivery.cs\"/>"
        + "<b s=\"15\" l=\"200\" r=\"Loads/File Loads/Subs &amp; Reds/SubsRedsDelivery2.cs\"/>"
        + "</g></duplications>")));
  }

  private CloneGroup newCloneGroup(ClonePart... parts) {
    return CloneGroup.builder().setLength(0).setOrigin(parts[0]).setParts(Arrays.asList(parts)).build();
  }

}
