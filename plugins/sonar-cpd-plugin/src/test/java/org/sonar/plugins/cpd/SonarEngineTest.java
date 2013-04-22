/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.File;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.scan.filesystem.PathResolver.RelativePath;
import org.sonar.api.test.IsMeasure;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SonarEngineTest {

  private SensorContext context;
  private Resource<?> resource;

  @Before
  public void setUp() {
    context = mock(SensorContext.class);
    resource = new JavaFile("key1");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetResource() {
    PathResolver pathResolver = mock(PathResolver.class);
    ModuleFileSystem fileSystem = mock(ModuleFileSystem.class);
    RelativePath relativePath = new RelativePath(null, "com/foo/Bar.java");
    when(pathResolver.relativePath(anyCollection(), any(java.io.File.class))).thenReturn(relativePath);

    SonarEngine engine = new SonarEngine(null, fileSystem, pathResolver, null);
    Resource<?> resource = engine.getResource(new java.io.File(""));

    assertThat(resource.getKey()).isEqualTo("com.foo.Bar");
    assertThat(resource).isInstanceOf(JavaFile.class);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNothingToSave() {
    SonarEngine.save(context, resource, null);
    SonarEngine.save(context, resource, Collections.EMPTY_LIST);

    verifyZeroInteractions(context);
  }

  @Test
  public void testOneSimpleDuplicationBetweenTwoFiles() {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214)));
    SonarEngine.save(context, resource, groups);

    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(
        eq(resource),
        argThat(new IsMeasure(CoreMetrics.DUPLICATIONS_DATA, "<duplications><g>"
          + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
          + "<b s=\"15\" l=\"200\" r=\"key2\"/>"
          + "</g></duplications>")));
  }

  @Test
  public void testDuplicationOnSameFile() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key1", 0, 215, 414)));
    SonarEngine.save(context, resource, groups);

    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_LINES, 400d);
    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_BLOCKS, 2d);
    verify(context).saveMeasure(
        eq(resource),
        argThat(new IsMeasure(CoreMetrics.DUPLICATIONS_DATA, "<duplications><g>"
          + "<b s=\"5\" l=\"200\" r=\"key1\"/>"
          + "<b s=\"215\" l=\"200\" r=\"key1\"/>"
          + "</g></duplications>")));
  }

  @Test
  public void testOneDuplicatedGroupInvolvingMoreThanTwoFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart("key1", 0, 5, 204), new ClonePart("key2", 0, 15, 214), new ClonePart("key3", 0, 25, 224)));
    SonarEngine.save(context, resource, groups);

    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(
        eq(resource),
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
    SonarEngine.save(context, resource, groups);

    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_BLOCKS, 2d);
    verify(context).saveMeasure(resource, CoreMetrics.DUPLICATED_LINES, 210d);
    verify(context).saveMeasure(
        eq(resource),
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
  public void shouldEscapeXmlEntities() {
    File csharpFile = new File("Loads/File Loads/Subs & Reds/SubsRedsDelivery.cs");
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
