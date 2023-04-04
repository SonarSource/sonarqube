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

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scm.git.ProcessWrapperFactory.ProcessWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.scm.git.NativeGitBlameCommand.BLAME_COMMAND;
import static org.sonar.scm.git.NativeGitBlameCommand.GIT_DIR_ARGUMENT;
import static org.sonar.scm.git.NativeGitBlameCommand.GIT_DIR_FLAG;
import static org.sonar.scm.git.NativeGitBlameCommand.GIT_DIR_FORCE_FLAG;
import static org.sonar.scm.git.GitUtils.createFile;
import static org.sonar.scm.git.GitUtils.createRepository;
import static org.sonar.scm.git.Utils.javaUnzip;

@RunWith(DataProviderRunner.class)
public class NativeGitBlameCommandTest {
  private static final String DUMMY_JAVA = "src/main/java/org/dummy/Dummy.java";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public LogTester logTester = new LogTester();
  private final ProcessWrapperFactory processWrapperFactory = new ProcessWrapperFactory();
  private final NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(System2.INSTANCE, processWrapperFactory);

  @Before
  public void skipTestsIfNoGitFound() {
    assumeTrue(blameCommand.checkIfEnabled());
  }

  @Test
  public void should_read_lines_only_based_on_new_line() throws Exception {
    Path baseDir = createNewTempFolder().toPath();
    String filePath = "file.txt";
    createFile(filePath, "test1\rtest2\r\ttest3", baseDir);
    Git git = createRepository(baseDir);
    createFile(filePath, "line", baseDir);
    commit(git, filePath);

    List<BlameLine> blame = blameCommand.blame(baseDir, "file.txt");
    assertThat(blame).hasSize(1);
  }

  @Test
  public void blame_collects_all_lines() throws Exception {
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);
    File baseDir = new File(projectDir, "dummy-git");

    List<BlameLine> blame = blameCommand.blame(baseDir.toPath(), DUMMY_JAVA);

    Date revisionDate1 = DateUtils.parseDateTime("2012-07-17T16:12:48+0200");
    String revision1 = "6b3aab35a3ea32c1636fee56f996e677653c48ea";
    String author1 = "david@gageot.net";

    // second commit, which has a commit date different than the author date
    Date revisionDate2 = DateUtils.parseDateTime("2015-05-19T13:31:09+0200");
    String revision2 = "0d269c1acfb8e6d4d33f3c43041eb87e0df0f5e7";
    String author2 = "duarte.meneses@sonarsource.com";

    List<BlameLine> expectedBlame = new LinkedList<>();
    for (int i = 0; i < 25; i++) {
      expectedBlame.add(new BlameLine().revision(revision1).date(revisionDate1).author(author1));
    }
    for (int i = 0; i < 3; i++) {
      expectedBlame.add(new BlameLine().revision(revision2).date(revisionDate2).author(author2));
    }
    for (int i = 0; i < 1; i++) {
      expectedBlame.add(new BlameLine().revision(revision1).date(revisionDate1).author(author1));
    }

    assertThat(blame).isEqualTo(expectedBlame);
  }

  @Test
  public void blame_different_author_and_committer() throws Exception {
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git-different-committer.zip", projectDir);
    File baseDir = new File(projectDir, "dummy-git");

    List<BlameLine> blame = blameCommand.blame(baseDir.toPath(), DUMMY_JAVA);

    Date revisionDate1 = DateUtils.parseDateTime("2012-07-17T16:12:48+0200");
    String revision1 = "6b3aab35a3ea32c1636fee56f996e677653c48ea";
    String author1 = "david@gageot.net";

    // second commit, which has a commit date different than the author date
    Date revisionDate2 = DateUtils.parseDateTime("2022-10-11T14:14:26+0200");
    String revision2 = "7609f824d5ff7018bebf107cdbe4edcc901b574f";
    String author2 = "duarte.meneses@sonarsource.com";

    List<BlameLine> expectedBlame = new LinkedList<>();
    for (int i = 0; i < 25; i++) {
      expectedBlame.add(new BlameLine().revision(revision1).date(revisionDate1).author(author1));
    }
    for (int i = 0; i < 3; i++) {
      expectedBlame.add(new BlameLine().revision(revision2).date(revisionDate2).author(author2));
    }
    for (int i = 0; i < 1; i++) {
      expectedBlame.add(new BlameLine().revision(revision1).date(revisionDate1).author(author1));
    }

    assertThat(blame).isEqualTo(expectedBlame);
  }

  @Test
  public void git_blame_uses_safe_local_repository() throws Exception {
    File projectDir = createNewTempFolder();
    File baseDir = new File(projectDir, "dummy-git");

    ProcessWrapperFactory mockFactory = mock(ProcessWrapperFactory.class);
    ProcessWrapper mockProcess = mock(ProcessWrapper.class);
    String gitCommand = "git";
    when(mockFactory.create(any(), any(), anyString(), anyString(), anyString(), anyString(),
      anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
      .then(invocation -> mockProcess);

    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(gitCommand, System2.INSTANCE, mockFactory);
    blameCommand.blame(baseDir.toPath(), DUMMY_JAVA);

    verify(mockFactory).create(any(), any(), eq(gitCommand),
      eq(GIT_DIR_FLAG),
      eq(String.format(GIT_DIR_ARGUMENT, baseDir.toPath())),
      eq(GIT_DIR_FORCE_FLAG),
      eq(baseDir.toPath().toString()),
      eq(BLAME_COMMAND),
      anyString(), anyString(), anyString(), eq(DUMMY_JAVA));
  }

  @Test
  public void modified_file_returns_no_blame() throws Exception {
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);

    Path baseDir = projectDir.toPath().resolve("dummy-git");

    // Emulate a modification
    Files.write(baseDir.resolve(DUMMY_JAVA), "modification and \n some new line".getBytes());

    assertThat(blameCommand.blame(baseDir, DUMMY_JAVA)).isEmpty();
  }

  @Test
  public void throw_exception_if_symlink_found() throws Exception {
    assumeTrue(!System2.INSTANCE.isOsWindows());
    File projectDir = temp.newFolder();
    javaUnzip("dummy-git.zip", projectDir);

    Path baseDir = projectDir.toPath().resolve("dummy-git");
    String relativePath2 = "src/main/java/org/dummy/Dummy2.java";

    // Create symlink
    Files.createSymbolicLink(baseDir.resolve(relativePath2), baseDir.resolve(DUMMY_JAVA));

    blameCommand.blame(baseDir, DUMMY_JAVA);
    assertThatThrownBy(() -> blameCommand.blame(baseDir, relativePath2)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void git_should_be_detected() {
    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(System2.INSTANCE, processWrapperFactory);
    assertThat(blameCommand.checkIfEnabled()).isTrue();
  }

  @Test
  public void git_should_not_be_detected() {
    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand("randomcmdthatwillneverbefound", System2.INSTANCE, processWrapperFactory);
    assertThat(blameCommand.checkIfEnabled()).isFalse();
  }

  @Test
  public void git_should_not_be_enabled_if_version_command_is_not_found() {
    ProcessWrapperFactory mockedCmd = mockGitVersionCommand("error: unknown option `version'");
    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(System2.INSTANCE, mockedCmd);
    assertThat(blameCommand.checkIfEnabled()).isFalse();
  }

  @Test
  public void git_should_not_be_enabled_if_version_command_does_not_return_string_output() {
    ProcessWrapperFactory mockedCmd = mockGitVersionCommand(null);
    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(System2.INSTANCE, mockedCmd);
    assertThat(blameCommand.checkIfEnabled()).isFalse();
  }

  @Test
  public void git_should_be_enabled_if_version_is_equal_or_greater_than_required_minimum() {
    Stream.of(
      "git version 2.24.0",
      "git version 2.25.2.1",
      "git version 2.24.1.1.windows.2",
      "git version 2.25.1.msysgit.2"
    ).forEach(output -> {
      ProcessWrapperFactory mockedCmd = mockGitVersionCommand(output);
      mockGitWhereOnWindows(mockedCmd);
      when(mockedCmd.create(isNull(), any(), eq("C:\\mockGit.exe"), eq("--version"))).then(invocation -> {
        var argument = (Consumer<String>) invocation.getArgument(1);
        argument.accept(output);
        return mock(ProcessWrapper.class);
      });

      NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(System2.INSTANCE, mockedCmd);
      assertThat(blameCommand.checkIfEnabled()).isTrue();
    });
  }

  @Test
  public void git_should_not_be_enabled_if_version_is_less_than_required_minimum() {
    ProcessWrapperFactory mockFactory = mockGitVersionCommand("git version 1.9.0");
    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(System2.INSTANCE, mockFactory);
    assertThat(blameCommand.checkIfEnabled()).isFalse();
  }

  @Test
  public void throw_exception_if_command_fails() throws Exception {
    Path baseDir = temp.newFolder().toPath();
    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand("randomcmdthatwillneverbefound", System2.INSTANCE, processWrapperFactory);
    assertThatThrownBy(() -> blameCommand.blame(baseDir, "file")).isInstanceOf(IOException.class);
  }

  @Test
  public void blame_without_email_doesnt_fail() throws Exception {
    Path baseDir = temp.newFolder().toPath();
    Git git = createRepository(baseDir);
    String filePath = "file.txt";
    createFile(filePath, "line", baseDir);
    commitWithNoEmail(git, filePath);

    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(System2.INSTANCE, processWrapperFactory);
    assertThat(blameCommand.checkIfEnabled()).isTrue();
    List<BlameLine> blame = blameCommand.blame(baseDir, filePath);
    assertThat(blame).hasSize(1);
    BlameLine blameLine = blame.get(0);
    assertThat(blameLine.author()).isNull();
    assertThat(blameLine.revision()).isNotNull();
    assertThat(blameLine.date()).isNotNull();
  }

  @Test
  public void blame_mail_with_spaces_doesnt_fail() throws Exception {
    Path baseDir = temp.newFolder().toPath();
    Git git = createRepository(baseDir);
    String filePath = "file.txt";
    createFile(filePath, "line", baseDir);
    commit(git, filePath, "my DOT name AT server DOT com");

    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(System2.INSTANCE, processWrapperFactory);
    assertThat(blameCommand.checkIfEnabled()).isTrue();
    List<BlameLine> blame = blameCommand.blame(baseDir, filePath);
    assertThat(blame).hasSize(1);
    assertThat(blame.get(0).author()).isEqualTo("my DOT name AT server DOT com");
  }

  @Test
  public void do_not_execute() throws Exception {
    Path baseDir = temp.newFolder().toPath();
    Git git = createRepository(baseDir);
    String filePath = "file.txt";
    createFile(filePath, "line", baseDir);
    commitWithNoEmail(git, filePath);

    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(System2.INSTANCE, processWrapperFactory);
    assertThat(blameCommand.checkIfEnabled()).isTrue();
    List<BlameLine> blame = blameCommand.blame(baseDir, filePath);
    assertThat(blame).hasSize(1);
    BlameLine blameLine = blame.get(0);
    assertThat(blameLine.author()).isNull();
    assertThat(blameLine.revision()).isNotNull();
    assertThat(blameLine.date()).isNotNull();
  }

  @Test
  public void execution_on_windows_should_fallback_to_full_path() {
    logTester.setLevel(Level.DEBUG);
    System2 system2 = mock(System2.class);
    when(system2.isOsWindows()).thenReturn(true);

    ProcessWrapperFactory mockFactory = mock(ProcessWrapperFactory.class);
    ProcessWrapper mockProcess = mock(ProcessWrapper.class);
    mockGitWhereOnWindows(mockFactory);

    when(mockFactory.create(isNull(), any(), eq("C:\\mockGit.exe"), eq("--version"))).then(invocation -> {
      var argument = (Consumer<String>) invocation.getArgument(1);
      argument.accept("git version 2.30.1");
      return mockProcess;
    });

    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(system2, mockFactory);
    assertThat(blameCommand.checkIfEnabled()).isTrue();
    assertThat(logTester.logs()).contains("Found git.exe at C:\\mockGit.exe");
  }

  @Test
  public void execution_on_windows_is_disabled_if_git_not_on_path() {
    System2 system2 = mock(System2.class);
    when(system2.isOsWindows()).thenReturn(true);
    when(system2.property("PATH")).thenReturn("C:\\some-path;C:\\some-another-path");

    ProcessWrapperFactory mockFactory = mock(ProcessWrapperFactory.class);
    mockGitWhereOnWindows(mockFactory);

    NativeGitBlameCommand blameCommand = new NativeGitBlameCommand(system2, mockFactory);
    assertThat(blameCommand.checkIfEnabled()).isFalse();
  }

  private void commitWithNoEmail(Git git, String path) throws GitAPIException {
    commit(git, path, "");
  }

  private void commit(Git git, String path) throws GitAPIException {
    commit(git, path, "email@email.com");
  }

  private void commit(Git git, String path, String email) throws GitAPIException {
    git.add().addFilepattern(path).call();
    git.commit().setCommitter("joe", email).setMessage("msg").call();
  }

  private File createNewTempFolder() throws IOException {
    // This is needed for Windows, otherwise the created File point to invalid (shortened by Windows) temp folder path
    return temp.newFolder().toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toFile();
  }

  private void mockGitWhereOnWindows(ProcessWrapperFactory processWrapperFactory) {
    when(processWrapperFactory.create(isNull(), any(), eq("C:\\Windows\\System32\\where.exe"), eq("$PATH:git.exe"))).then(invocation -> {
      var argument = (Consumer<String>) invocation.getArgument(1);
      argument.accept("C:\\mockGit.exe");
      return mock(ProcessWrapper.class);
    });
  }

  private ProcessWrapperFactory mockGitVersionCommand(String commandOutput) {
    ProcessWrapperFactory mockFactory = mock(ProcessWrapperFactory.class);
    ProcessWrapper mockProcess = mock(ProcessWrapper.class);

    when(mockFactory.create(isNull(), any(), eq("git"), eq("--version"))).then(invocation -> {
      var argument = (Consumer<String>) invocation.getArgument(1);
      argument.accept(commandOutput);
      return mockProcess;
    });

    return mockFactory;
  }
}
