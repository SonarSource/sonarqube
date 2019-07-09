/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.cpd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.util.CloseableIterator;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.protocol.output.ScannerReport.Duplicate;
import org.sonar.scanner.protocol.output.ScannerReport.Duplication;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CpdExecutorTest {
  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private CpdExecutor executor;
  private ExecutorService executorService = mock(ExecutorService.class);
  private CpdSettings settings = mock(CpdSettings.class);
  private ReportPublisher publisher = mock(ReportPublisher.class);
  private SonarCpdBlockIndex index = new SonarCpdBlockIndex(publisher, settings);
  private ScannerReportReader reader;
  private DefaultInputFile batchComponent1;
  private DefaultInputFile batchComponent2;
  private DefaultInputFile batchComponent3;
  private File baseDir;
  private InputComponentStore componentStore;

  @Before
  public void setUp() throws IOException {
    File outputDir = temp.newFolder();
    baseDir = temp.newFolder();
    when(publisher.getWriter()).thenReturn(new ScannerReportWriter(outputDir));

    DefaultInputProject project = TestInputFileBuilder.newDefaultInputProject("foo", baseDir);
    componentStore = new InputComponentStore(mock(BranchConfiguration.class));
    executor = new CpdExecutor(settings, index, publisher, componentStore, executorService);
    reader = new ScannerReportReader(outputDir);

    batchComponent1 = createComponent("src/Foo.php", 5);
    batchComponent2 = createComponent("src/Foo2.php", 5);
    batchComponent3 = createComponent("src/Foo3.php", 5);
  }

  @Test
  public void dont_fail_if_nothing_to_save() {
    executor.saveDuplications(batchComponent1, Collections.<CloneGroup>emptyList());
    assertThat(reader.readComponentDuplications(batchComponent1.scannerId())).isExhausted();
  }

  @Test
  public void should_save_single_duplication() {
    List<CloneGroup> groups = Collections.singletonList(newCloneGroup(
      new ClonePart(batchComponent1.key(), 0, 2, 4),
      new ClonePart(batchComponent2.key(), 0, 15, 17)));

    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(1);
    assertDuplication(dups[0], 2, 4, batchComponent2.scannerId(), 15, 17);
  }

  @Test
  public void should_save_duplication_on_same_file() {
    List<CloneGroup> groups = Collections.singletonList(newCloneGroup(
      new ClonePart(batchComponent1.key(), 0, 5, 204),
      new ClonePart(batchComponent1.key(), 0, 215, 414)));
    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(1);
    assertDuplication(dups[0], 5, 204, null, 215, 414);
  }

  @Test
  public void should_limit_number_of_references() {
    // 1 origin part + 101 duplicates = 102
    List<ClonePart> parts = new ArrayList<>(CpdExecutor.MAX_CLONE_PART_PER_GROUP + 2);
    for (int i = 0; i < CpdExecutor.MAX_CLONE_PART_PER_GROUP + 2; i++) {
      parts.add(new ClonePart(batchComponent1.key(), i, i, i + 1));
    }
    List<CloneGroup> groups = Collections.singletonList(CloneGroup.builder().setLength(0).setOrigin(parts.get(0)).setParts(parts).build());
    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(1);
    assertThat(dups[0].getDuplicateList()).hasSize(CpdExecutor.MAX_CLONE_PART_PER_GROUP);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Too many duplication references on file " + batchComponent1 + " for block at line 0. Keep only the first "
        + CpdExecutor.MAX_CLONE_PART_PER_GROUP + " references.");
  }

  @Test
  public void should_limit_number_of_clones() {
    // 1 origin part + 101 duplicates = 102
    List<CloneGroup> dups = new ArrayList<>(CpdExecutor.MAX_CLONE_GROUP_PER_FILE + 1);
    for (int i = 0; i < CpdExecutor.MAX_CLONE_GROUP_PER_FILE + 1; i++) {
      ClonePart clonePart = new ClonePart(batchComponent1.key(), i, i, i + 1);
      ClonePart dupPart = new ClonePart(batchComponent1.key(), i + 1, i + 1, i + 2);
      dups.add(newCloneGroup(clonePart, dupPart));
    }
    executor.saveDuplications(batchComponent1, dups);

    assertThat(reader.readComponentDuplications(batchComponent1.scannerId())).toIterable().hasSize(CpdExecutor.MAX_CLONE_GROUP_PER_FILE);

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Too many duplication groups on file " + batchComponent1 + ". Keep only the first " + CpdExecutor.MAX_CLONE_GROUP_PER_FILE + " groups.");
  }

  @Test
  public void should_save_duplication_involving_three_files() {
    List<CloneGroup> groups = Collections.singletonList(newCloneGroup(
      new ClonePart(batchComponent1.key(), 0, 5, 204),
      new ClonePart(batchComponent2.key(), 0, 15, 214),
      new ClonePart(batchComponent3.key(), 0, 25, 224)));
    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(1);
    assertDuplication(dups[0], 5, 204, 2);
    assertDuplicate(dups[0].getDuplicate(0), batchComponent2.scannerId(), 15, 214);
    assertDuplicate(dups[0].getDuplicate(1), batchComponent3.scannerId(), 25, 224);
  }

  @Test
  public void should_save_two_duplicated_groups_involving_three_files() {
    List<CloneGroup> groups = Arrays.asList(
      newCloneGroup(new ClonePart(batchComponent1.key(), 0, 5, 204),
        new ClonePart(batchComponent2.key(), 0, 15, 214)),
      newCloneGroup(new ClonePart(batchComponent1.key(), 0, 15, 214),
        new ClonePart(batchComponent3.key(), 0, 15, 214)));
    executor.saveDuplications(batchComponent1, groups);

    Duplication[] dups = readDuplications(2);
    assertDuplication(dups[0], 5, 204, batchComponent2.scannerId(), 15, 214);
    assertDuplication(dups[1], 15, 214, batchComponent3.scannerId(), 15, 214);
  }

  @Test
  public void should_ignore_missing_component() {
    Block block = Block.builder()
      .setBlockHash(new ByteArray("AAAABBBBCCCC"))
      .setResourceId("unknown")
      .build();
    index.insert(batchComponent1, Collections.singletonList(block));
    executor.execute();

    verify(executorService).shutdown();
    verifyNoMoreInteractions(executorService);
    readDuplications(batchComponent1, 0);
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Resource not found in component store: unknown. Skipping CPD computation for it");
  }

  @Test
  public void should_timeout() {
    Block block = Block.builder()
      .setBlockHash(new ByteArray("AAAABBBBCCCC"))
      .setResourceId(batchComponent1.key())
      .build();
    index.insert(batchComponent1, Collections.singletonList(block));
    when(executorService.submit(ArgumentMatchers.any(Callable.class))).thenReturn(new CompletableFuture());
    executor.execute(1);

    readDuplications(0);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .usingElementComparator((l, r) -> l.matches(r) ? 0 : 1)
      .containsOnly(
        "Timeout during detection of duplications for .*Foo.php");
  }

  private DefaultInputFile createComponent(String relativePath, int lines) {
    return createComponent(relativePath, lines, f -> {
    });
  }

  private DefaultInputFile createComponent(String relativePath, int lines, Consumer<TestInputFileBuilder> config) {
    TestInputFileBuilder fileBuilder = new TestInputFileBuilder("foo", relativePath)
      .setModuleBaseDir(baseDir.toPath())
      .setLines(lines);
    config.accept(fileBuilder);
    DefaultInputFile file = fileBuilder.build();
    componentStore.put("foo", file);
    return file;
  }

  private Duplication[] readDuplications(int expected) {
    return readDuplications(batchComponent1, expected);
  }

  private Duplication[] readDuplications(DefaultInputFile file, int expected) {
    assertThat(reader.readComponentDuplications(file.scannerId())).toIterable().hasSize(expected);
    Duplication[] duplications = new Duplication[expected];
    CloseableIterator<Duplication> dups = reader.readComponentDuplications(file.scannerId());

    for (int i = 0; i < expected; i++) {
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

  private void assertDuplication(Duplication d, int originStartLine, int originEndLine, @Nullable Integer otherFileRef, int rangeStartLine, int rangeEndLine) {
    assertThat(d.getOriginPosition().getStartLine()).isEqualTo(originStartLine);
    assertThat(d.getOriginPosition().getEndLine()).isEqualTo(originEndLine);
    assertThat(d.getDuplicateList()).hasSize(1);
    if (otherFileRef != null) {
      assertThat(d.getDuplicate(0).getOtherFileRef()).isEqualTo(otherFileRef);
    } else {
      assertThat(d.getDuplicate(0).getOtherFileRef()).isEqualTo(0);
    }
    assertThat(d.getDuplicate(0).getRange().getStartLine()).isEqualTo(rangeStartLine);
    assertThat(d.getDuplicate(0).getRange().getEndLine()).isEqualTo(rangeEndLine);
  }

  private CloneGroup newCloneGroup(ClonePart... parts) {
    return CloneGroup.builder().setLength(0).setOrigin(parts[0]).setParts(Arrays.asList(parts)).build();
  }
}
