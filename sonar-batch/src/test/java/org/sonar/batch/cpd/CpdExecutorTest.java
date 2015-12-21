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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.batch.cpd.index.SonarDuplicationsIndex;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.protocol.output.BatchReport.Duplicate;
import org.sonar.batch.protocol.output.BatchReport.Duplication;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.core.util.CloseableIterator;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CpdExecutorTest {
  private CpdExecutor executor;
  private Settings settings;
  private SonarDuplicationsIndex index;
  private ReportPublisher publisher;
  private BatchComponentCache componentCache;

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  // private AbstractCpdEngine engine;

  private BatchReportReader reader;
  private BatchComponent batchComponent1;
  private BatchComponent batchComponent2;
  private BatchComponent batchComponent3;

  @Before
  public void setUp() throws IOException {
    File outputDir = temp.newFolder();

    settings = new Settings();
    index = mock(SonarDuplicationsIndex.class);
    publisher = mock(ReportPublisher.class);
    when(publisher.getWriter()).thenReturn(new BatchReportWriter(outputDir));
    componentCache = new BatchComponentCache();
    executor = new CpdExecutor(settings, index, publisher, componentCache);
    reader = new BatchReportReader(outputDir);

    Project p = new Project("foo");
    componentCache.add(p, null).setInputComponent(new DefaultInputModule("foo"));

    batchComponent1 = createComponent("src/Foo.php", 5);
    batchComponent2 = createComponent("src/Foo2.php", 5);
    batchComponent3 = createComponent("src/Foo3.php", 5);
  }

  private BatchComponent createComponent(String relativePath, int lines) {
    org.sonar.api.resources.Resource sampleFile = org.sonar.api.resources.File.create("relativePath").setEffectiveKey("foo:" + relativePath);
    return componentCache.add(sampleFile, null).setInputComponent(new DefaultInputFile("foo", relativePath).setLines(lines));
  }

  @Test
  public void defaultMinimumTokens() {
    assertThat(executor.getMinimumTokens("java")).isEqualTo(100);
  }

  @Test
  public void minimumTokensByLanguage() {
    settings.setProperty("sonar.cpd.java.minimumTokens", "42");
    settings.setProperty("sonar.cpd.php.minimumTokens", "33");
    assertThat(executor.getMinimumTokens("java")).isEqualTo(42);

    settings.setProperty("sonar.cpd.java.minimumTokens", "42");
    settings.setProperty("sonar.cpd.php.minimumTokens", "33");
    assertThat(executor.getMinimumTokens("php")).isEqualTo(33);
  }

  @Test
  public void testNothingToSave() {
    executor.saveDuplications(batchComponent1, Collections.<CloneGroup>emptyList());
    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(0);
  }

  @Test
  public void reportOneSimpleDuplicationBetweenTwoFiles() {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart(batchComponent1.key(), 0, 2, 4), new ClonePart(batchComponent2.key(), 0, 15, 17)));

    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(1);
    assertDuplication(dups[0], 2, 4, batchComponent2.batchId(), 15, 17);
  }

  @Test
  public void reportDuplicationOnSameFile() throws Exception {
    List<CloneGroup> groups = Arrays.asList(newCloneGroup(new ClonePart(batchComponent1.key(), 0, 5, 204), new ClonePart(batchComponent1.key(), 0, 215, 414)));
    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(1);
    assertDuplication(dups[0], 5, 204, null, 215, 414);
  }

  @Test
  public void reportTooManyDuplicates() throws Exception {
    // 1 origin part + 101 duplicates = 102
    List<ClonePart> parts = new ArrayList<>(CpdExecutor.MAX_CLONE_PART_PER_GROUP + 2);
    for (int i = 0; i < CpdExecutor.MAX_CLONE_PART_PER_GROUP + 2; i++) {
      parts.add(new ClonePart(batchComponent1.key(), i, i, i + 1));
    }
    List<CloneGroup> groups = Arrays.asList(CloneGroup.builder().setLength(0).setOrigin(parts.get(0)).setParts(parts).build());
    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(1);
    assertThat(dups[0].getDuplicateList()).hasSize(CpdExecutor.MAX_CLONE_PART_PER_GROUP);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Too many duplication references on file " + batchComponent1.inputComponent() + " for block at line 0. Keep only the first "
        + CpdExecutor.MAX_CLONE_PART_PER_GROUP + " references.");
  }

  @Test
  public void reportTooManyDuplications() throws Exception {
    // 1 origin part + 101 duplicates = 102
    List<CloneGroup> dups = new ArrayList<>(CpdExecutor.MAX_CLONE_GROUP_PER_FILE + 1);
    for (int i = 0; i < CpdExecutor.MAX_CLONE_GROUP_PER_FILE + 1; i++) {
      ClonePart clonePart = new ClonePart(batchComponent1.key(), i, i, i + 1);
      ClonePart dupPart = new ClonePart(batchComponent1.key(), i + 1, i + 1, i + 2);
      dups.add(newCloneGroup(clonePart, dupPart));
    }
    executor.saveDuplications(batchComponent1, dups);

    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(CpdExecutor.MAX_CLONE_GROUP_PER_FILE);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Too many duplication groups on file " + batchComponent1.inputComponent() + ". Keep only the first " + CpdExecutor.MAX_CLONE_GROUP_PER_FILE + " groups.");
  }

  @Test
  public void reportOneDuplicatedGroupInvolvingMoreThanTwoFiles() throws Exception {
    List<CloneGroup> groups = Arrays
      .asList(newCloneGroup(new ClonePart(batchComponent1.key(), 0, 5, 204), new ClonePart(batchComponent2.key(), 0, 15, 214), new ClonePart(batchComponent3.key(), 0, 25, 224)));
    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(1);
    assertDuplication(dups[0], 5, 204, 2);
    assertDuplicate(dups[0].getDuplicate(0), batchComponent2.batchId(), 15, 214);
    assertDuplicate(dups[0].getDuplicate(1), batchComponent3.batchId(), 25, 224);
  }

  @Test
  public void reportTwoDuplicatedGroupsInvolvingThreeFiles() throws Exception {
    List<CloneGroup> groups = Arrays.asList(
      newCloneGroup(new ClonePart(batchComponent1.key(), 0, 5, 204), new ClonePart(batchComponent2.key(), 0, 15, 214)),
      newCloneGroup(new ClonePart(batchComponent1.key(), 0, 15, 214), new ClonePart(batchComponent3.key(), 0, 15, 214)));
    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(2);
    assertDuplication(dups[0], 5, 204, batchComponent2.batchId(), 15, 214);
    assertDuplication(dups[1], 15, 214, batchComponent3.batchId(), 15, 214);
  }
  
  private Duplication[] readDuplications(int expected) {
    assertThat(reader.readComponentDuplications(batchComponent1.batchId())).hasSize(expected);
    Duplication[] duplications = new Duplication[expected];
    CloseableIterator<Duplication> dups = reader.readComponentDuplications(batchComponent1.batchId());
    
    for(int i = 0; i< expected; i++) {
      duplications[i] = dups.next();
    }
    dups.close();
    return duplications;
  }
  
  private void assertDuplicate(Duplicate d, int otherFileRef, int rangeStartLine, int rangeEndLine) {
    assertThat(d.getOtherFileRef()).isEqualTo(otherFileRef);
    assertThat(d.getRange().getStartLine()).isEqualTo(rangeStartLine);
    assertThat(d.getRange().getEndLine()).isEqualTo(rangeEndLine);
  }
  
  private void assertDuplication(Duplication d, int originStartLine, int originEndLine, int numDuplicates) {
    assertThat(d.getOriginPosition().getStartLine()).isEqualTo(originStartLine);
    assertThat(d.getOriginPosition().getEndLine()).isEqualTo(originEndLine);
    assertThat(d.getDuplicateList()).hasSize(numDuplicates);
  }
  
  private void assertDuplication(Duplication d, int originStartLine, int originEndLine, Integer otherFileRef, int rangeStartLine, int rangeEndLine) {
    assertThat(d.getOriginPosition().getStartLine()).isEqualTo(originStartLine);
    assertThat(d.getOriginPosition().getEndLine()).isEqualTo(originEndLine);
    assertThat(d.getDuplicateList()).hasSize(1);
    if(otherFileRef != null) {
    assertThat(d.getDuplicate(0).getOtherFileRef()).isEqualTo(otherFileRef);
    } else {
      assertThat(d.getDuplicate(0).hasOtherFileRef()).isFalse();
    }
    assertThat(d.getDuplicate(0).getRange().getStartLine()).isEqualTo(rangeStartLine);
    assertThat(d.getDuplicate(0).getRange().getEndLine()).isEqualTo(rangeEndLine);
  }
  
  private CloneGroup newCloneGroup(ClonePart... parts) {
    return CloneGroup.builder().setLength(0).setOrigin(parts[0]).setParts(Arrays.asList(parts)).build();
  }
}
