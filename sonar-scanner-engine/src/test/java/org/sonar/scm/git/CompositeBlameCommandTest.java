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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.config.Configuration;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.sonar.scm.git.Utils.javaUnzip;

public class CompositeBlameCommandTest {
  private static final String DUMMY_JAVA = "src/main/java/org/dummy/Dummy.java";
  private final PathResolver pathResolver = new PathResolver();
  private final ProcessWrapperFactory processWrapperFactory = new ProcessWrapperFactory();
  private final JGitBlameCommand jGitBlameCommand = new JGitBlameCommand();
  private final GitBlameCommand gitBlameCommand = new GitBlameCommand(System2.INSTANCE, processWrapperFactory);
  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
  private final CompositeBlameCommand blameCommand = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, gitBlameCommand, mock(org.sonar.api.config.Configuration.class));

  private File projectDir;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();
  private final BlameCommand.BlameInput input = mock(BlameCommand.BlameInput.class);

  @Before
  public void setUp() throws Exception {
    projectDir = temp.newFolder();
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.cleanDirectory(projectDir);
    //noinspection ResultOfMethodCallIgnored
    projectDir.delete();
  }

  @Test
  public void use_jgit_if_native_git_disabled() throws IOException {
    GitBlameCommand gitCmd = new GitBlameCommand("invalidcommandnotfound", System2.INSTANCE, processWrapperFactory);
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, gitCmd, mock(org.sonar.api.config.Configuration.class));
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
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jGitBlameCommand, gitCmd, mock(org.sonar.api.config.Configuration.class));
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
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jgit, gitBlameCommand, mock(org.sonar.api.config.Configuration.class));
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
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jgit, gitBlameCommand, mock(org.sonar.api.config.Configuration.class));
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
    BlameCommand blameCmd = new CompositeBlameCommand(analysisWarnings, pathResolver, jgit, gitBlameCommand, mock(org.sonar.api.config.Configuration.class));

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

  @Test
  public void shouldIgnoreSubmodulesWhenNotCheckedOut() throws IOException {

    javaUnzip("submodule-git.zip", projectDir);

    CompositeBlameCommand compositeBlameCommand = newCompositeBlameCommand("true");

    File baseDir = new File(projectDir, "submodule-git");

    FileUtils.deleteDirectory(new File(baseDir, "lib"));

    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "lib/file")
            .setModuleBaseDir(baseDir.toPath())
            .build();
    fs.add(inputFile);

    GitIgnoreCommand ignoreCommand = new GitIgnoreCommand(mock(Configuration.class));
    ignoreCommand.init(baseDir.toPath());

    BlameCommand.BlameOutput blameResult = mock(BlameCommand.BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Collections.singletonList(inputFile));

    compositeBlameCommand.blame(input, blameResult);

    List<BlameLine> expectedBlame = new LinkedList<>();
    verify(blameResult, never()).blameResult(inputFile, expectedBlame);
  }

  @Test
  public void shouldNotRetrieveBlameInformationWhenFileIsModifiedButNotCommitted() throws IOException {

    javaUnzip("dummy-git.zip", projectDir);

    CompositeBlameCommand compositeBlameCommand = newCompositeBlameCommand("true");

    File baseDir = new File(projectDir, "dummy-git");
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);
    String modifiedFile = "src/main/java/org/dummy/AnotherDummy.java";
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", modifiedFile)
            .setModuleBaseDir(baseDir.toPath())
            .build();
    fs.add(inputFile);

    // Emulate a modification
    Files.write(baseDir.toPath().resolve(modifiedFile), "modification and \n some new line".getBytes());

    BlameCommand.BlameOutput blameResult = mock(BlameCommand.BlameOutput.class);

    when(input.filesToBlame()).thenReturn(Collections.singletonList(inputFile));
    compositeBlameCommand.blame(input, blameResult);
    verify(blameResult, never()).blameResult(any(),argThat(List::isEmpty));
  }

  @Test
  public void shouldNotRetrieveBlameInformationWhenFileIsAddedButNotCommitted() throws IOException {

    javaUnzip("dummy-git.zip", projectDir);

    CompositeBlameCommand compositeBlameCommand = newCompositeBlameCommand("true");

    File baseDir = new File(projectDir, "dummy-git");
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA)
            .setModuleBaseDir(baseDir.toPath())
            .build();
    fs.add(inputFile);

    // Emulate file creation
    Files.write(baseDir.toPath().resolve(DUMMY_JAVA), "modification and \n some new line".getBytes());

    try (Repository repository = JGitUtils.buildRepository(baseDir.toPath()); Git git = Git.wrap(repository)) {
      git.add().addFilepattern(DUMMY_JAVA).call();
    } catch ( GitAPIException e) {
      fail("Failed when adding file to repository");
    }

    BlameCommand.BlameOutput blameResult = mock(BlameCommand.BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Collections.singletonList(inputFile));

    compositeBlameCommand.blame(input, blameResult);

    verify(blameResult, never()).blameResult(any(),argThat(List::isEmpty));
  }

  @Test
  public void shouldRetrieveBlameInformationWhenFileIsModifiedAndCommitted() throws IOException {

    javaUnzip("dummy-git.zip", projectDir);

    CompositeBlameCommand compositeBlameCommand = newCompositeBlameCommand("true");

    File baseDir = new File(projectDir, "dummy-git");
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);
    String modifiedFile = "src/main/java/org/dummy/AnotherDummy.java";
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", modifiedFile)
            .setModuleBaseDir(baseDir.toPath())
            .build();
    fs.add(inputFile);

    // Emulate a modification
    Files.write(baseDir.toPath().resolve(modifiedFile), "modification and \n some new line".getBytes());

    addAndCommitFile(baseDir, modifiedFile);

    BlameCommand.BlameOutput blameResult = mock(BlameCommand.BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Collections.singletonList(inputFile));

    compositeBlameCommand.blame(input, blameResult);

    verify(blameResult).blameResult(any(),argThat(list -> list.size() == 2));
  }

  @Test
  public void shouldRetrieveBlameInformationWhenFileIsAddedAndCommitted() throws IOException {

    javaUnzip("dummy-git.zip", projectDir);

    CompositeBlameCommand compositeBlameCommand = newCompositeBlameCommand("true");

    File baseDir = new File(projectDir, "dummy-git");
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA)
            .setModuleBaseDir(baseDir.toPath())
            .build();
    fs.add(inputFile);

    // Emulate a modification
    Files.write(baseDir.toPath().resolve(DUMMY_JAVA), "Added new file\n Line 1\nLine 2".getBytes());

    addAndCommitFile(baseDir, DUMMY_JAVA);

    BlameCommand.BlameOutput blameResult = mock(BlameCommand.BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Collections.singletonList(inputFile));

    compositeBlameCommand.blame(input, blameResult);

    verify(blameResult).blameResult(any(),argThat(list -> list.size() == 3));
  }

  @Test
  public void shouldRetrieveBlameInformationForAllFilesWhenFileIsAddedAndCommitted() throws IOException {
    javaUnzip("dummy-git.zip", projectDir);

    CompositeBlameCommand compositeBlameCommand = newCompositeBlameCommand("true");

    File baseDir = new File(projectDir, "dummy-git");
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);
    DefaultInputFile newFile = new TestInputFileBuilder("foo", DUMMY_JAVA)
            .setModuleBaseDir(baseDir.toPath())
            .build();
    fs.add(newFile);

    DefaultInputFile existingFile = new TestInputFileBuilder("foo", "src/main/java/org/dummy/AnotherDummy.java")
            .setModuleBaseDir(baseDir.toPath())
            .build();
    fs.add(existingFile);

    // Emulate creation of new file
    Files.write(baseDir.toPath().resolve(DUMMY_JAVA), "Added new file\n Line 1\nLine 2".getBytes());

    addAndCommitFile(baseDir, DUMMY_JAVA);

    BlameCommand.BlameOutput blameResult = mock(BlameCommand.BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Arrays.asList(newFile, existingFile));

    compositeBlameCommand.blame(input, blameResult);

    verify(blameResult).blameResult(eq(newFile),argThat(list -> list.size() == 3));
    verify(blameResult).blameResult(eq(existingFile),argThat(list -> list.size() == 29));
  }

  @Test
  public void shouldNotRetrieveBlameInformationForADeletedFileThatIsNotCommitted() throws IOException {
    javaUnzip("dummy-git.zip", projectDir);

    CompositeBlameCommand compositeBlameCommand = newCompositeBlameCommand("true");

    File baseDir = new File(projectDir, "dummy-git");
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);

    DefaultInputFile existingFile = new TestInputFileBuilder("foo", "src/main/java/org/dummy/AnotherDummy.java")
            .setModuleBaseDir(baseDir.toPath())
            .build();
    fs.add(existingFile);

    FileUtils.forceDelete(new File(baseDir, "src/main/java/org/dummy/AnotherDummy.java"));

    BlameCommand.BlameOutput blameResult = mock(BlameCommand.BlameOutput.class);
    when(input.filesToBlame()).thenReturn(Collections.singletonList(existingFile));

    compositeBlameCommand.blame(input, blameResult);

    verify(blameResult, never()).blameResult(existingFile,new LinkedList<>());
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

  private void addAndCommitFile(File baseDir, String dummyJava) {
    try (Repository repository = JGitUtils.buildRepository(baseDir.toPath()); Git git = Git.wrap(repository)) {
      StoredConfig config = git.getRepository().getConfig();
      config.setBoolean(ConfigConstants.CONFIG_COMMIT_SECTION, null, "gpgsign", false);
      git.add().addFilepattern(dummyJava).call();
      git.commit().setMessage("Dummy commit").setAuthor("dummy", "dummy@dummy.com").call();
    } catch (GitAPIException e) {
      fail("Failed when adding file to repository");
    }
  }

  private CompositeBlameCommand newCompositeBlameCommand(String submoduleEnabled) {
    Configuration configuration = mock(Configuration.class);
    JGitBlameCommand jgit = mock(JGitBlameCommand.class);
    when(configuration.get("sonar.scm.submodules.included")).thenReturn(java.util.Optional.of(submoduleEnabled));
    return new CompositeBlameCommand(mock(AnalysisWarnings.class), new PathResolver(), jgit, gitBlameCommand, configuration);
  }


}
