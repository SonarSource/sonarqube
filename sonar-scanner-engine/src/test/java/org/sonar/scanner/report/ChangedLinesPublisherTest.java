/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.report;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ReferenceBranchSupplier;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonar.scm.git.GitScmProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ChangedLinesPublisherTest {
  private static final String TARGET_BRANCH = "target";
  private static final Path BASE_DIR = Paths.get("/root");

  private final ScmConfiguration scmConfiguration = mock(ScmConfiguration.class);
  private final InputModuleHierarchy inputModuleHierarchy = mock(InputModuleHierarchy.class);
  private final InputComponentStore inputComponentStore = mock(InputComponentStore.class);
  private final BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private final ReferenceBranchSupplier referenceBranchSupplier = mock(ReferenceBranchSupplier.class);
  private ScannerReportWriter writer;
  private FileStructure fileStructure;
  private final ScmProvider provider = mock(ScmProvider.class);
  private final DefaultInputProject project = mock(DefaultInputProject.class);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private final ChangedLinesPublisher publisher = new ChangedLinesPublisher(scmConfiguration, project, inputComponentStore, branchConfiguration, referenceBranchSupplier);

  @Before
  public void setUp() {
    fileStructure = new FileStructure(temp.getRoot());
    writer = new ScannerReportWriter(fileStructure);
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    when(scmConfiguration.isDisabled()).thenReturn(false);
    when(scmConfiguration.provider()).thenReturn(provider);
    when(branchConfiguration.targetBranchName()).thenReturn(TARGET_BRANCH);
    when(project.getBaseDir()).thenReturn(BASE_DIR);
  }

  @Test
  public void skip_if_scm_is_disabled() {
    when(scmConfiguration.isDisabled()).thenReturn(true);
    publisher.publish(writer);
    verifyNoInteractions(inputComponentStore, inputModuleHierarchy, provider);
    assertNotPublished();
  }

  @Test
  public void skip_if_not_pr() {
    when(branchConfiguration.isPullRequest()).thenReturn(false);
    publisher.publish(writer);
    verifyNoInteractions(inputComponentStore, inputModuleHierarchy, provider);
    assertNotPublished();
  }

  @Test
  public void skip_if_target_branch_is_null() {
    when(branchConfiguration.targetBranchName()).thenReturn(null);
    publisher.publish(writer);
    verifyNoInteractions(inputComponentStore, inputModuleHierarchy, provider);
    assertNotPublished();
  }

  @Test
  public void skip_if_no_scm_provider() {
    when(scmConfiguration.provider()).thenReturn(null);
    publisher.publish(writer);
    verifyNoInteractions(inputComponentStore, inputModuleHierarchy, provider);
    assertNotPublished();
  }

  @Test
  public void skip_if_scm_provider_returns_null() {
    publisher.publish(writer);
    assertNotPublished();
  }

  @Test
  public void write_changed_files() {
    DefaultInputFile fileWithChangedLines = createInputFile("path1", "l1\nl2\nl3\n");
    DefaultInputFile fileNotReturned = createInputFile("path2", "l1\nl2\nl3\n");
    DefaultInputFile fileWithoutChangedLines = createInputFile("path3", "l1\nl2\nl3\n");

    Set<Path> paths = new HashSet<>(Arrays.asList(BASE_DIR.resolve("path1"), BASE_DIR.resolve("path2"), BASE_DIR.resolve("path3")));
    Set<Integer> lines = new HashSet<>(Arrays.asList(1, 10));
    when(provider.branchChangedLines(TARGET_BRANCH, BASE_DIR, paths))
      .thenReturn(ImmutableMap.of(BASE_DIR.resolve("path1"), lines, BASE_DIR.resolve("path3"), Collections.emptySet()));
    when(inputComponentStore.allChangedFilesToPublish()).thenReturn(Arrays.asList(fileWithChangedLines, fileNotReturned, fileWithoutChangedLines));

    publisher.publish(writer);

    assertPublished(fileWithChangedLines, new HashSet<>(Arrays.asList(1, 10)));
    assertPublished(fileWithoutChangedLines, Collections.emptySet());
    assertPublished(fileNotReturned, Collections.emptySet());

    assumeThat(logTester.logs()).contains("File '/root/path2' was detected as changed but without having changed lines");
  }

  @Test
  public void write_changed_file_with_GitScmProvider() {
    GitScmProvider provider = mock(GitScmProvider.class);
    when(scmConfiguration.provider()).thenReturn(provider);
    Set<Integer> lines = new HashSet<>(Arrays.asList(1, 10));
    when(provider.branchChangedLines(eq(TARGET_BRANCH), eq(BASE_DIR), anySet()))
      .thenReturn(ImmutableMap.of(BASE_DIR.resolve("path1"), lines, BASE_DIR.resolve("path3"), Collections.emptySet()));

    publisher.publish(writer);

    verify(provider).branchChangedLinesWithFileMovementDetection(eq(TARGET_BRANCH), eq(BASE_DIR), anyMap());
  }

  @Test
  public void write_last_line_as_changed_if_all_other_lines_are_changed_and_last_line_is_empty() {
    DefaultInputFile fileWithChangedLines = createInputFile("path1", "l1\nl2\nl3\n");
    DefaultInputFile fileWithoutChangedLines = createInputFile("path2", "l1\nl2\nl3\n");
    Set<Path> paths = new HashSet<>(Arrays.asList(BASE_DIR.resolve("path1"), BASE_DIR.resolve("path2")));
    Set<Integer> lines = new HashSet<>(Arrays.asList(1, 2, 3));
    when(provider.branchChangedLines(TARGET_BRANCH, BASE_DIR, paths)).thenReturn(Collections.singletonMap(BASE_DIR.resolve("path1"), lines));
    when(inputComponentStore.allChangedFilesToPublish()).thenReturn(Arrays.asList(fileWithChangedLines, fileWithoutChangedLines));

    publisher.publish(writer);

    assertPublished(fileWithChangedLines, new HashSet<>(Arrays.asList(1, 2, 3, 4)));
    assertPublished(fileWithoutChangedLines, Collections.emptySet());
  }

  @Test
  public void do_not_write_last_line_as_changed_if_its_not_empty() {
    DefaultInputFile fileWithChangedLines = createInputFile("path1", "l1\nl2\nl3\nl4");
    DefaultInputFile fileWithoutChangedLines = createInputFile("path2", "l1\nl2\nl3\nl4");
    Set<Path> paths = new HashSet<>(Arrays.asList(BASE_DIR.resolve("path1"), BASE_DIR.resolve("path2")));
    Set<Integer> lines = new HashSet<>(Arrays.asList(1, 2, 3));
    when(provider.branchChangedLines(TARGET_BRANCH, BASE_DIR, paths)).thenReturn(Collections.singletonMap(BASE_DIR.resolve("path1"), lines));
    when(inputComponentStore.allChangedFilesToPublish()).thenReturn(Arrays.asList(fileWithChangedLines, fileWithoutChangedLines));

    publisher.publish(writer);

    assertPublished(fileWithChangedLines, new HashSet<>(Arrays.asList(1, 2, 3)));
    assertPublished(fileWithoutChangedLines, Collections.emptySet());
  }

  private DefaultInputFile createInputFile(String path, String contents) {
    return new TestInputFileBuilder("module", path)
      .setContents(contents)
      .setProjectBaseDir(BASE_DIR)
      .setModuleBaseDir(BASE_DIR)
      .build();
  }

  private void assertPublished(DefaultInputFile file, Set<Integer> lines) {
    assertThat(new File(temp.getRoot(), "changed-lines-" + file.scannerId() + ".pb")).exists();
    ScannerReportReader reader = new ScannerReportReader(fileStructure);
    assertThat(reader.readComponentChangedLines(file.scannerId()).getLineList()).containsExactlyElementsOf(lines);
  }

  private void assertNotPublished() {
    assertThat(temp.getRoot()).isEmptyDirectory();
  }

}
