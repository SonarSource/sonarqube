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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport.Duplication;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.core.util.CloseableIterator;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractCpdEngineTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private AbstractCpdEngine engine;

  private BatchReportReader reader;
  private DefaultInputFile inputFile1;
  private BatchComponent batchComponent1;
  private BatchComponent batchComponent2;
  private BatchComponent batchComponent3;

  @Before
  public void before() throws IOException {
    File outputDir = temp.newFolder();
    ReportPublisher reportPublisher = mock(ReportPublisher.class);
    when(reportPublisher.getWriter()).thenReturn(new BatchReportWriter(outputDir));
    reader = new BatchReportReader(outputDir);
    BatchComponentCache componentCache = new BatchComponentCache();
    Project p = new Project("foo");
    componentCache.add(p, null).setInputComponent(new DefaultInputModule("foo"));
    org.sonar.api.resources.Resource sampleFile = org.sonar.api.resources.File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    inputFile1 = new DefaultInputFile("foo", "src/Foo.php").setLines(5);
    batchComponent1 = componentCache.add(sampleFile, null).setInputComponent(inputFile1);
    org.sonar.api.resources.Resource sampleFile2 = org.sonar.api.resources.File.create("src/Foo2.php").setEffectiveKey("foo:src/Foo2.php");
    batchComponent2 = componentCache.add(sampleFile2, null).setInputComponent(new DefaultInputFile("foo", "src/Foo2.php").setLines(5));
    org.sonar.api.resources.Resource sampleFile3 = org.sonar.api.resources.File.create("src/Foo3.php").setEffectiveKey("foo:src/Foo3.php");
    batchComponent3 = componentCache.add(sampleFile3, null).setInputComponent(new DefaultInputFile("foo", "src/Foo3.php").setLines(5));
    engine = new AbstractCpdEngine(reportPublisher, componentCache) {

      @Override
      boolean isLanguageSupported(String language) {
        return false;
      }

      @Override
      void analyse(String language, SensorContext context) {
      }
    };
  }

  @Test
  public void testNothingToSave() {
    engine.saveDuplications(inputFile1, Collections.EMPTY_LIST);

    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(0);
  }

  @Test
  public void testOneSimpleDuplicationBetweenTwoFiles() {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart(batchComponent1.key(), 0, 2, 4), new ClonePart(batchComponent2.key(), 0, 15, 17)));
    engine.saveDuplications(inputFile1, groups);

    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(1);
    CloseableIterator<Duplication> dups = reader.readComponentDuplications(batchComponent1.batchId());
    Duplication duplication = dups.next();
    dups.close();
    assertThat(duplication.getOriginPosition().getStartLine()).isEqualTo(2);
    assertThat(duplication.getOriginPosition().getEndLine()).isEqualTo(4);
    assertThat(duplication.getDuplicateList()).hasSize(1);
    assertThat(duplication.getDuplicate(0).getOtherFileRef()).isEqualTo(batchComponent2.batchId());
    assertThat(duplication.getDuplicate(0).getRange().getStartLine()).isEqualTo(15);
    assertThat(duplication.getDuplicate(0).getRange().getEndLine()).isEqualTo(17);
  }

  @Test
  public void testDuplicationOnSameFile() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart(batchComponent1.key(), 0, 5, 204), new ClonePart(batchComponent1.key(), 0, 215, 414)));
    engine.saveDuplications(inputFile1, groups);

    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(1);
    CloseableIterator<Duplication> dups = reader.readComponentDuplications(batchComponent1.batchId());
    Duplication duplication = dups.next();
    dups.close();
    assertThat(duplication.getOriginPosition().getStartLine()).isEqualTo(5);
    assertThat(duplication.getOriginPosition().getEndLine()).isEqualTo(204);
    assertThat(duplication.getDuplicateList()).hasSize(1);
    assertThat(duplication.getDuplicate(0).hasOtherFileRef()).isFalse();
    assertThat(duplication.getDuplicate(0).getRange().getStartLine()).isEqualTo(215);
    assertThat(duplication.getDuplicate(0).getRange().getEndLine()).isEqualTo(414);
  }

  @Test
  public void testTooManyDuplicates() throws Exception {
    // 1 origin part + 101 duplicates = 102
    List<ClonePart> parts = new ArrayList<>(AbstractCpdEngine.MAX_CLONE_PART_PER_GROUP + 2);
    for (int i = 0; i < AbstractCpdEngine.MAX_CLONE_PART_PER_GROUP + 2; i++) {
      parts.add(new ClonePart(batchComponent1.key(), i, i, i + 1));
    }
    List<CloneGroup> groups = Arrays.asList(CloneGroup.builder().setLength(0).setOrigin(parts.get(0)).setParts(parts).build());
    engine.saveDuplications(inputFile1, groups);

    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(1);
    CloseableIterator<Duplication> dups = reader.readComponentDuplications(batchComponent1.batchId());
    Duplication duplication = dups.next();
    dups.close();
    assertThat(duplication.getDuplicateList()).hasSize(AbstractCpdEngine.MAX_CLONE_PART_PER_GROUP);

    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Too many duplication references on file " + inputFile1.relativePath() + " for block at line 0. Keep only the first "
      + AbstractCpdEngine.MAX_CLONE_PART_PER_GROUP + " references.");
  }

  @Test
  public void testTooManyDuplications() throws Exception {
    // 1 origin part + 101 duplicates = 102
    List<CloneGroup> dups = new ArrayList<>(AbstractCpdEngine.MAX_CLONE_GROUP_PER_FILE + 1);
    for (int i = 0; i < AbstractCpdEngine.MAX_CLONE_GROUP_PER_FILE + 1; i++) {
      ClonePart clonePart = new ClonePart(batchComponent1.key(), i, i, i + 1);
      ClonePart dupPart = new ClonePart(batchComponent1.key(), i + 1, i + 1, i + 2);
      dups.add(newCloneGroup(clonePart, dupPart));
    }
    engine.saveDuplications(inputFile1, dups);

    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(AbstractCpdEngine.MAX_CLONE_GROUP_PER_FILE);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Too many duplication groups on file " + inputFile1.relativePath() + ". Keep only the first " + AbstractCpdEngine.MAX_CLONE_GROUP_PER_FILE + " groups.");
  }

  @Test
  public void testOneDuplicatedGroupInvolvingMoreThanTwoFiles() throws Exception {
    List<CloneGroup> groups = Arrays
      .asList(newCloneGroup(new ClonePart(batchComponent1.key(), 0, 5, 204), new ClonePart(batchComponent2.key(), 0, 15, 214), new ClonePart(batchComponent3.key(), 0, 25, 224)));
    engine.saveDuplications(inputFile1, groups);

    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(1);
    CloseableIterator<Duplication> dups = reader.readComponentDuplications(batchComponent1.batchId());
    Duplication duplication = dups.next();
    dups.close();
    assertThat(duplication.getOriginPosition().getStartLine()).isEqualTo(5);
    assertThat(duplication.getOriginPosition().getEndLine()).isEqualTo(204);
    assertThat(duplication.getDuplicateList()).hasSize(2);
    assertThat(duplication.getDuplicate(0).getOtherFileRef()).isEqualTo(batchComponent2.batchId());
    assertThat(duplication.getDuplicate(0).getRange().getStartLine()).isEqualTo(15);
    assertThat(duplication.getDuplicate(0).getRange().getEndLine()).isEqualTo(214);
    assertThat(duplication.getDuplicate(1).getOtherFileRef()).isEqualTo(batchComponent3.batchId());
    assertThat(duplication.getDuplicate(1).getRange().getStartLine()).isEqualTo(25);
    assertThat(duplication.getDuplicate(1).getRange().getEndLine()).isEqualTo(224);
  }

  @Test
  public void testTwoDuplicatedGroupsInvolvingThreeFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(
      newCloneGroup(new ClonePart(batchComponent1.key(), 0, 5, 204), new ClonePart(batchComponent2.key(), 0, 15, 214)),
      newCloneGroup(new ClonePart(batchComponent1.key(), 0, 15, 214), new ClonePart(batchComponent3.key(), 0, 15, 214)));
    engine.saveDuplications(inputFile1, groups);

    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(2);
    CloseableIterator<Duplication> dups = reader.readComponentDuplications(batchComponent1.batchId());
    Duplication duplication1 = dups.next();
    Duplication duplication2 = dups.next();
    dups.close();
    assertThat(duplication1.getOriginPosition().getStartLine()).isEqualTo(5);
    assertThat(duplication1.getOriginPosition().getEndLine()).isEqualTo(204);
    assertThat(duplication1.getDuplicateList()).hasSize(1);
    assertThat(duplication1.getDuplicate(0).getOtherFileRef()).isEqualTo(batchComponent2.batchId());
    assertThat(duplication1.getDuplicate(0).getRange().getStartLine()).isEqualTo(15);
    assertThat(duplication1.getDuplicate(0).getRange().getEndLine()).isEqualTo(214);

    assertThat(duplication2.getOriginPosition().getStartLine()).isEqualTo(15);
    assertThat(duplication2.getOriginPosition().getEndLine()).isEqualTo(214);
    assertThat(duplication2.getDuplicateList()).hasSize(1);
    assertThat(duplication2.getDuplicate(0).getOtherFileRef()).isEqualTo(batchComponent3.batchId());
    assertThat(duplication2.getDuplicate(0).getRange().getStartLine()).isEqualTo(15);
    assertThat(duplication2.getDuplicate(0).getRange().getEndLine()).isEqualTo(214);
  }

  private CloneGroup newCloneGroup(ClonePart... parts) {
    return CloneGroup.builder().setLength(0).setOrigin(parts[0]).setParts(Arrays.asList(parts)).build();
  }

}
