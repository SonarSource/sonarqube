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
package org.sonar.scanner.report;

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
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scm.ScmConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ChangedLinesPublisherTest {
  private static final String TARGET_BRANCH = "target";
  private static final Path BASE_DIR = Paths.get("/root");

  private ScmConfiguration scmConfiguration = mock(ScmConfiguration.class);
  private InputModuleHierarchy inputModuleHierarchy = mock(InputModuleHierarchy.class);
  private InputComponentStore inputComponentStore = mock(InputComponentStore.class);
  private BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private ScannerReportWriter writer;
  private ScmProvider provider = mock(ScmProvider.class);
  private DefaultInputProject project = mock(DefaultInputProject.class);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ChangedLinesPublisher publisher = new ChangedLinesPublisher(scmConfiguration, project, inputComponentStore, branchConfiguration);

  @Before
  public void setUp() {
    writer = new ScannerReportWriter(temp.getRoot());
    when(branchConfiguration.isShortOrPullRequest()).thenReturn(true);
    when(scmConfiguration.isDisabled()).thenReturn(false);
    when(scmConfiguration.provider()).thenReturn(provider);
    when(branchConfiguration.targetScmBranch()).thenReturn(TARGET_BRANCH);
    when(project.getBaseDir()).thenReturn(BASE_DIR);
  }

  @Test
  public void skip_if_scm_is_disabled() {
    when(scmConfiguration.isDisabled()).thenReturn(true);
    publisher.publish(writer);
    verifyZeroInteractions(inputComponentStore, inputModuleHierarchy, provider);
    assertNotPublished();
  }

  @Test
  public void skip_if_not_pr_or_slb() {
    when(branchConfiguration.isShortOrPullRequest()).thenReturn(false);
    publisher.publish(writer);
    verifyZeroInteractions(inputComponentStore, inputModuleHierarchy, provider);
    assertNotPublished();
  }

  @Test
  public void skip_if_target_branch_is_null() {
    when(branchConfiguration.targetScmBranch()).thenReturn(null);
    publisher.publish(writer);
    verifyZeroInteractions(inputComponentStore, inputModuleHierarchy, provider);
    assertNotPublished();
  }

  @Test
  public void skip_if_no_scm_provider() {
    when(scmConfiguration.provider()).thenReturn(null);
    publisher.publish(writer);
    verifyZeroInteractions(inputComponentStore, inputModuleHierarchy, provider);
    assertNotPublished();
  }

  @Test
  public void skip_if_scm_provider_returns_null() {
    publisher.publish(writer);
    assertNotPublished();
  }

  @Test
  public void write_changed_files() {
    DefaultInputFile fileWithChangedLines = createInputFile("path1");
    DefaultInputFile fileWithoutChangedLines = createInputFile("path2");
    Set<Path> paths = new HashSet<>(Arrays.asList(BASE_DIR.resolve("path1"), BASE_DIR.resolve("path2")));
    Set<Integer> lines = new HashSet<>(Arrays.asList(1, 10));
    when(provider.branchChangedLines(TARGET_BRANCH, BASE_DIR, paths)).thenReturn(Collections.singletonMap(BASE_DIR.resolve("path1"), lines));
    when(inputComponentStore.allChangedFilesToPublish()).thenReturn(Arrays.asList(fileWithChangedLines, fileWithoutChangedLines));

    publisher.publish(writer);

    assertPublished(fileWithChangedLines, lines);
    assertPublished(fileWithoutChangedLines, Collections.emptySet());
  }

  private DefaultInputFile createInputFile(String path) {
    return new TestInputFileBuilder("module", path)
      .setProjectBaseDir(BASE_DIR)
      .setModuleBaseDir(BASE_DIR)
      .build();
  }

  private void assertPublished(DefaultInputFile file, Set<Integer> lines) {
    assertThat(new File(temp.getRoot(), "changed-lines-" + file.scannerId() + ".pb")).exists();
    ScannerReportReader reader = new ScannerReportReader(temp.getRoot());
    assertThat(reader.readComponentChangedLines(file.scannerId()).getLineList()).containsExactlyElementsOf(lines);
  }

  private void assertNotPublished(DefaultInputFile file) {
    assertThat(new File(temp.getRoot(), "changed-lines-" + file.scannerId() + ".pb")).doesNotExist();
  }

  private void assertNotPublished() {
    assertThat(temp.getRoot().list()).isEmpty();
  }

}
