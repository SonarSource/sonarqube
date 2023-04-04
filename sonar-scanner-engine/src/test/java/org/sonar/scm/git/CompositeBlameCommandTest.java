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
package org.sonar.scm.git;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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
import java.util.stream.IntStream;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.scm.git.strategy.BlameStrategy;
import org.sonar.scm.git.strategy.DefaultBlameStrategy.BlameAlgorithmEnum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.scm.git.Utils.javaUnzip;
import static org.sonar.scm.git.strategy.DefaultBlameStrategy.BlameAlgorithmEnum.GIT_FILES_BLAME;
import static org.sonar.scm.git.strategy.DefaultBlameStrategy.BlameAlgorithmEnum.GIT_NATIVE_BLAME;

@RunWith(DataProviderRunner.class)
public class CompositeBlameCommandTest {
  private static final String DUMMY_JAVA = "src/main/java/org/dummy/Dummy.java";
  private final ProcessWrapperFactory processWrapperFactory = new ProcessWrapperFactory();
  private final JGitBlameCommand jGitBlameCommand = new JGitBlameCommand();
  private final NativeGitBlameCommand nativeGitBlameCommand = new NativeGitBlameCommand(System2.INSTANCE, processWrapperFactory);
  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);

  private final PathResolver pathResolver = new PathResolver();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();
  private final BlameCommand.BlameInput input = mock(BlameCommand.BlameInput.class);

  @DataProvider
  public static List<Object> blameAlgorithms() {
    return Arrays.stream(BlameAlgorithmEnum.values()).collect(Collectors.toList());
  }

  @Test
  public void use_jgit_if_native_git_disabled() throws IOException {
    logTester.setLevel(Level.DEBUG);
    NativeGitBlameCommand gitCmd = new NativeGitBlameCommand("invalidcommandnotfound", System2.INSTANCE, processWrapperFactory);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, gitCmd, (p, f) -> GIT_NATIVE_BLAME);
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);

    File baseDir = new File(projectDir, "dummy-git");
    setUpBlameInputWithFile(baseDir.toPath());
    TestBlameOutput output = new TestBlameOutput();
    blameCmd.blame(input, output);

    assertThat(logTester.logs(Level.DEBUG)).contains("Using GIT_NATIVE_BLAME strategy to blame files");
    assertThat(output.blame).hasSize(1);
    assertThat(output.blame.get(input.filesToBlame().iterator().next())).hasSize(29);
  }

  @Test
  public void blame_shouldCallStrategyWithCorrectSpecifications() throws IOException {

    BlameStrategy strategyMock = mock(BlameStrategy.class);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, nativeGitBlameCommand, strategyMock);

    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);

    File baseDir = new File(projectDir, "dummy-git");
    setUpBlameInputWithFile(baseDir.toPath());
    TestBlameOutput output = new TestBlameOutput();
    blameCmd.blame(input, output);

    verify(strategyMock).getBlameAlgorithm(intThat((i) -> i > 0), intThat(i -> i == 1));
  }

  @Test
  public void fallback_to_jgit_if_native_git_fails() throws Exception {
    logTester.setLevel(Level.DEBUG);
    NativeGitBlameCommand gitCmd = mock(NativeGitBlameCommand.class);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, gitCmd, (p, f) -> GIT_NATIVE_BLAME);
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);

    File baseDir = new File(projectDir, "dummy-git");
    when(gitCmd.checkIfEnabled()).thenReturn(true);
    when(gitCmd.blame(baseDir.toPath(), DUMMY_JAVA)).thenThrow(new IllegalStateException());
    setUpBlameInputWithFile(baseDir.toPath());
    TestBlameOutput output = new TestBlameOutput();
    blameCmd.blame(input, output);

    assertThat(logTester.logs(Level.DEBUG)).contains("Using GIT_NATIVE_BLAME strategy to blame files");
    assertThat(output.blame).hasSize(1);
    assertThat(output.blame.get(input.filesToBlame().iterator().next())).hasSize(29);

    // only tried once
    verify(gitCmd).blame(any(Path.class), any(String.class));
    assertThat(logTester.logs()).contains("Native git blame failed. Falling back to jgit: src/main/java/org/dummy/Dummy.java");
  }

  @Test
  @UseDataProvider("blameAlgorithms")
  public void skip_files_not_committed(BlameAlgorithmEnum strategy) throws Exception {
    // skip if git not installed
    if (strategy == GIT_NATIVE_BLAME) {
      assumeTrue(nativeGitBlameCommand.checkIfEnabled());
    }

    JGitBlameCommand jgit = mock(JGitBlameCommand.class);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jgit, nativeGitBlameCommand, (p, f) -> strategy);
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
  @UseDataProvider("blameAlgorithms")
  public void skip_files_when_head_commit_is_missing(BlameAlgorithmEnum strategy) throws IOException {
    // skip if git not installed
    assumeTrue(nativeGitBlameCommand.checkIfEnabled());

    JGitBlameCommand jgit = mock(JGitBlameCommand.class);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jgit, nativeGitBlameCommand, (p, f) -> strategy);
    File projectDir = createNewTempFolder();
    javaUnzip("no-head-git.zip", projectDir);

    File baseDir = new File(projectDir, "no-head-git");
    setUpBlameInputWithFile(baseDir.toPath());
    TestBlameOutput output = new TestBlameOutput();
    blameCmd.blame(input, output);

    assertThat(output.blame).isEmpty();
    verifyNoInteractions(jgit);

    assertThat(logTester.logs(Level.WARN))
      .contains("Could not find HEAD commit");
  }

  @Test
  @UseDataProvider("blameAlgorithms")
  public void return_early_when_shallow_clone_detected(BlameAlgorithmEnum strategy) throws IOException {
    CompositeBlameCommand blameCommand = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, nativeGitBlameCommand, (p, f) -> strategy);

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
    CompositeBlameCommand blameCommand = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, nativeGitBlameCommand, (p, f) -> GIT_FILES_BLAME);

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
  @UseDataProvider("blameAlgorithms")
  public void dont_fail_with_symlink(BlameAlgorithmEnum strategy) throws IOException {
    CompositeBlameCommand blameCommand = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, nativeGitBlameCommand, (p, f) -> strategy);

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
    assertThat(output.blame).isNotEmpty();
  }

  @Test
  public void return_early_when_clone_with_reference_detected() throws IOException {
    CompositeBlameCommand blameCommand = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, nativeGitBlameCommand, (p, f) -> GIT_FILES_BLAME);

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
      .haveAtLeastOne(new Condition<>(s -> s.startsWith("This git repository references another local repository which is not well supported"),
        "log for reference detected"));

    // contains commits referenced from the old clone and commits in the new clone
    assertThat(output.blame).containsKey(inputFile);
    assertThat(output.blame.get(inputFile).stream().map(BlameLine::revision))
      .containsOnly("6b3aab35a3ea32c1636fee56f996e677653c48ea", "843c7c30d7ebd9a479e8f1daead91036c75cbc4e", "0d269c1acfb8e6d4d33f3c43041eb87e0df0f5e7");
    verifyNoInteractions(analysisWarnings);
  }

  @Test
  @UseDataProvider("blameAlgorithms")
  public void blame_on_nested_module(BlameAlgorithmEnum strategy) throws IOException {
    CompositeBlameCommand blameCommand = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, nativeGitBlameCommand, (p, f) -> strategy);
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
    when(input.filesToBlame()).thenReturn(List.of(inputFile));
    blameCommand.blame(input, blameResult);

    Date revisionDate = DateUtils.parseDateTime("2012-07-17T16:12:48+0200");
    String revision = "6b3aab35a3ea32c1636fee56f996e677653c48ea";
    String author = "david@gageot.net";
    verify(blameResult).blameResult(inputFile,
      IntStream.range(0, 26)
        .mapToObj(i -> new BlameLine().revision(revision).date(revisionDate).author(author))
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
    // This is needed for Windows, otherwise the created File points to invalid (shortened by Windows) temp folder path
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
