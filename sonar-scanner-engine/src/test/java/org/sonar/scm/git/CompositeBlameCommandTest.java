/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scm.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.scm.git.Utils.javaUnzip;

public class CompositeBlameCommandTest {
  private static final String DUMMY_JAVA = "src/main/java/org/dummy/Dummy.java";
  private final PathResolver pathResolver = new PathResolver();
  private final ProcessWrapperFactory processWrapperFactory = new ProcessWrapperFactory();
  private final JGitBlameCommand jGitBlameCommand = new JGitBlameCommand();
  private final GitBlameCommand gitBlameCommand = new GitBlameCommand(System2.INSTANCE, processWrapperFactory);
  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
  private final CompositeBlameCommand blameCommand = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, gitBlameCommand);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();
  private final BlameCommand.BlameInput input = mock(BlameCommand.BlameInput.class);

  @Test
  public void use_jgit_if_native_git_disabled() throws IOException {
    GitBlameCommand gitCmd = new GitBlameCommand("invalidcommandnotfound", System2.INSTANCE, processWrapperFactory);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, gitCmd);
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);

    File baseDir = new File(projectDir, "dummy-git");
    setUpBlameInputWithFile(baseDir.toPath());
    TestBlameOutput output = new TestBlameOutput();
    blameCmd.blame(input, output);
    assertThat(output.blame).hasSize(1);
    assertThat(output.blame.get(input.filesToBlame().iterator().next())).hasSize(29);
  }

  @Test
  public void fallback_to_jgit_if_native_git_fails() throws Exception {
    GitBlameCommand gitCmd = mock(GitBlameCommand.class);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, gitCmd);
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);

    File baseDir = new File(projectDir, "dummy-git");
    when(gitCmd.checkIfEnabled()).thenReturn(true);
    when(gitCmd.blame(baseDir.toPath(), DUMMY_JAVA)).thenThrow(new IllegalStateException());
    setUpBlameInputWithFile(baseDir.toPath());
    TestBlameOutput output = new TestBlameOutput();
    blameCmd.blame(input, output);
    assertThat(output.blame).hasSize(1);
    assertThat(output.blame.get(input.filesToBlame().iterator().next())).hasSize(29);

    // only tried once
    verify(gitCmd).blame(any(Path.class), any(String.class));
    assertThat(logTester.logs()).contains("Native git blame failed. Falling back to jgit: src/main/java/org/dummy/Dummy.java");
  }

  @Test
  public void skip_files_not_committed() throws Exception {
    // skip if git not installed
    assumeTrue(gitBlameCommand.checkIfEnabled());

    JGitBlameCommand jgit = mock(JGitBlameCommand.class);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jgit, gitBlameCommand);
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);

    File baseDir = new File(projectDir, "dummy-git");
    setUpBlameInputWithFile(baseDir.toPath());
    TestBlameOutput output = new TestBlameOutput();
    blameCmd.blame(input, output);
    assertThat(output.blame).hasSize(1);
    assertThat(output.blame.get(input.filesToBlame().iterator().next())).hasSize(29);

    // never had to fall back to jgit
    verifyNoInteractions(jgit);
  }

  @Test
  public void skip_files_when_head_commit_is_missing() throws IOException {
    // skip if git not installed
    assumeTrue(gitBlameCommand.checkIfEnabled());

    JGitBlameCommand jgit = mock(JGitBlameCommand.class);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jgit, gitBlameCommand);
    File projectDir = createNewTempFolder();
    javaUnzip("no-head-git.zip", projectDir);

    File baseDir = new File(projectDir, "no-head-git");
    setUpBlameInputWithFile(baseDir.toPath());
    TestBlameOutput output = new TestBlameOutput();
    blameCmd.blame(input, output);

    assertThat(output.blame).isEmpty();
    verifyNoInteractions(jgit);

    assertThat(logTester.logs())
      .contains("Could not find HEAD commit");
  }

  @Test
  public void use_native_git_by_default() throws IOException {
    // skip test if git is not installed
    assumeTrue(gitBlameCommand.checkIfEnabled());
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);
    File baseDir = new File(projectDir, "dummy-git");

    JGitBlameCommand jgit = mock(JGitBlameCommand.class);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jgit, gitBlameCommand);

    setUpBlameInputWithFile(baseDir.toPath());
    TestBlameOutput output = new TestBlameOutput();
    blameCmd.blame(input, output);
    assertThat(output.blame).hasSize(1);
    assertThat(output.blame.get(input.filesToBlame().iterator().next())).hasSize(29);
    verifyNoInteractions(jgit);
  }

  @Test
  public void return_early_when_shallow_clone_detected() throws IOException {
    File projectDir = createNewTempFolder();
    javaUnzip("shallow-git.zip", projectDir);

    File baseDir = new File(projectDir, "shallow-git");

    setUpBlameInputWithFile(baseDir.toPath());

    // register warning with default wrapper
    BlameCommand.BlameOutput output = mock(BlameCommand.BlameOutput.class);
    blameCommand.blame(input, output);
    verify(analysisWarnings).addUnique(startsWith("Shallow clone detected"));
  }

  @Test
  public void fail_if_not_git_project() throws IOException {
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);

    File baseDir = new File(projectDir, "dummy-git");

    // Delete .git
    FileUtils.forceDelete(new File(baseDir, ".git"));

    setUpBlameInputWithFile(baseDir.toPath());

    BlameCommand.BlameOutput blameResult = mock(BlameCommand.BlameOutput.class);

    assertThatThrownBy(() -> blameCommand.blame(input, blameResult))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("Not inside a Git work tree: ");
  }

  @Test
  public void dont_fail_with_symlink() throws IOException {
    assumeTrue(!System2.INSTANCE.isOsWindows());
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);

    File baseDir = new File(projectDir, "dummy-git");
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);
    String relativePath2 = "src/main/java/org/dummy/Dummy2.java";
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA)
      .setModuleBaseDir(baseDir.toPath())
      .build();
    DefaultInputFile inputFile2 = new TestInputFileBuilder("foo", relativePath2)
      .setModuleBaseDir(baseDir.toPath())
      .build();

    // Create symlink
    Files.createSymbolicLink(inputFile2.file().toPath(), inputFile.file().toPath());

    when(input.filesToBlame()).thenReturn(Arrays.asList(inputFile, inputFile2));
    TestBlameOutput output = new TestBlameOutput();
    blameCommand.blame(input, output);
  }

  @Test
  public void return_early_when_clone_with_reference_detected() throws IOException {
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git-reference-clone.zip", projectDir);

    Path baseDir = projectDir.toPath().resolve("dummy-git2");

    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);

    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA).setModuleBaseDir(baseDir).build();
    when(input.filesToBlame()).thenReturn(Collections.singleton(inputFile));

    // register warning
    TestBlameOutput output = new TestBlameOutput();
    blameCommand.blame(input, output);

    assertThat(logTester.logs())
      .anyMatch(s -> s.contains("This git repository references another local repository which is not well supported"));

    // contains commits referenced from the old clone and commits in the new clone
    assertThat(output.blame).containsKey(inputFile);
    assertThat(output.blame.get(inputFile).stream().map(BlameLine::revision))
      .containsOnly("6b3aab35a3ea32c1636fee56f996e677653c48ea", "843c7c30d7ebd9a479e8f1daead91036c75cbc4e", "0d269c1acfb8e6d4d33f3c43041eb87e0df0f5e7");
    verifyNoInteractions(analysisWarnings);
  }

  @Test
  public void blame_on_nested_module() throws IOException {
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git-nested.zip", projectDir);
    File baseDir = new File(projectDir, "dummy-git-nested/dummy-project");
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA)
      .setModuleBaseDir(baseDir.toPath())
      .build();
    fs.add(inputFile);

    BlameCommand.BlameOutput blameResult = mock(BlameCommand.BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Arrays.asList(inputFile));
    blameCommand.blame(input, blameResult);

    Date revisionDate = DateUtils.parseDateTime("2012-07-17T16:12:48+0200");
    String revision = "6b3aab35a3ea32c1636fee56f996e677653c48ea";
    String author = "david@gageot.net";
    verify(blameResult).blameResult(inputFile,
      Stream.generate(() -> new BlameLine().revision(revision).date(revisionDate).author(author))
        .limit(26)
        .collect(Collectors.toList()));
  }

  private BlameCommand.BlameInput setUpBlameInputWithFile(Path baseDir) {
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);

    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA).setModuleBaseDir(baseDir).build();
    when(input.filesToBlame()).thenReturn(Collections.singleton(inputFile));
    return input;
  }

  private File createNewTempFolder() throws IOException {
    // This is needed for Windows, otherwise the created File point to invalid (shortened by Windows) temp folder path
    return temp.newFolder().toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toFile();
  }

  private static class TestBlameOutput implements BlameCommand.BlameOutput {
    private final Map<InputFile, List<BlameLine>> blame = new LinkedHashMap<>();

    @Override
    public void blameResult(InputFile inputFile, List<BlameLine> list) {
      blame.put(inputFile, list);
    }
  }
}
