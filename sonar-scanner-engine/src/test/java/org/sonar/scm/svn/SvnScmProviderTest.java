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
package org.sonar.scm.svn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.scm.ScmProvider;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SvnScmProviderTest {

  // Sample content for unified diffs
  // http://www.gnu.org/software/diffutils/manual/html_node/Example-Unified.html#Example-Unified
  private static final String CONTENT_LAO = "The Way that can be told of is not the eternal Way;\n"
    + "The name that can be named is not the eternal name.\n"
    + "The Nameless is the origin of Heaven and Earth;\n"
    + "The Named is the mother of all things.\n"
    + "Therefore let there always be non-being,\n"
    + "  so we may see their subtlety,\n"
    + "And let there always be being,\n"
    + "  so we may see their outcome.\n"
    + "The two are the same,\n"
    + "But after they are produced,\n"
    + "  they have different names.\n";

  private static final String CONTENT_TZU = "The Nameless is the origin of Heaven and Earth;\n"
    + "The named is the mother of all things.\n"
    + "\n"
    + "Therefore let there always be non-being,\n"
    + "  so we may see their subtlety,\n"
    + "And let there always be being,\n"
    + "  so we may see their outcome.\n"
    + "The two are the same,\n"
    + "But after they are produced,\n"
    + "  they have different names.\n"
    + "They both may be called deep and profound.\n"
    + "Deeper and more profound,\n"
    + "The door of all subtleties!";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SvnConfiguration config = mock(SvnConfiguration.class);
  private SvnTester svnTester;

  @Before
  public void before() throws IOException, SVNException {
    svnTester = new SvnTester(temp.newFolder().toPath());

    Path worktree = temp.newFolder().toPath();
    svnTester.checkout(worktree, "trunk");
    createAndCommitFile(worktree, "file-in-first-commit.xoo");
  }

  @Test
  public void sanityCheck() {
    SvnBlameCommand blameCommand = new SvnBlameCommand(config);
    SvnScmProvider svnScmProvider = new SvnScmProvider(config, blameCommand);
    assertThat(svnScmProvider.key()).isEqualTo("svn");
    assertThat(svnScmProvider.blameCommand()).isEqualTo(blameCommand);
  }

  @Test
  public void testAutodetection() throws IOException {
    ScmProvider scmBranchProvider = newScmProvider();

    File baseDirEmpty = temp.newFolder();
    assertThat(scmBranchProvider.supports(baseDirEmpty)).isFalse();

    File svnBaseDir = temp.newFolder();
    Files.createDirectory(svnBaseDir.toPath().resolve(".svn"));
    assertThat(scmBranchProvider.supports(svnBaseDir)).isTrue();

    File svnBaseDirSubFolder = temp.newFolder();
    Files.createDirectory(svnBaseDirSubFolder.toPath().resolve(".svn"));
    File projectBaseDir = new File(svnBaseDirSubFolder, "folder");
    Files.createDirectory(projectBaseDir.toPath());
    assertThat(scmBranchProvider.supports(projectBaseDir)).isTrue();
  }

  @Test
  public void branchChangedFiles_and_lines_from_diverged() throws IOException, SVNException {
    Path trunk = temp.newFolder().toPath();
    svnTester.checkout(trunk, "trunk");
    createAndCommitFile(trunk, "file-m1.xoo");
    createAndCommitFile(trunk, "file-m2.xoo");
    createAndCommitFile(trunk, "file-m3.xoo");
    createAndCommitFile(trunk, "lao.txt", CONTENT_LAO);

    // create branch from trunk
    svnTester.createBranch("b1");

    // still on trunk
    appendToAndCommitFile(trunk, "file-m3.xoo");
    createAndCommitFile(trunk, "file-m4.xoo");

    Path b1 = temp.newFolder().toPath();
    svnTester.checkout(b1, "branches/b1");
    Files.createDirectories(b1.resolve("sub"));
    createAndCommitFile(b1, "sub/file-b1.xoo");
    appendToAndCommitFile(b1, "file-m1.xoo");
    deleteAndCommitFile(b1, "file-m2.xoo");

    createAndCommitFile(b1, "file-m5.xoo");
    deleteAndCommitFile(b1, "file-m5.xoo");

    svnCopyAndCommitFile(b1, "file-m1.xoo", "file-m1-copy.xoo");
    appendToAndCommitFile(b1, "file-m1.xoo");

    // modify file without committing it -> should not be included (think generated files)
    svnTester.appendToFile(b1, "file-m3.xoo");

    svnTester.update(b1);

    Set<Path> changedFiles = newScmProvider().branchChangedFiles("trunk", b1);
    assertThat(changedFiles)
      .containsExactlyInAnyOrder(
        b1.resolve("sub/file-b1.xoo"),
        b1.resolve("file-m1.xoo"),
        b1.resolve("file-m1-copy.xoo"));

    // use a subset of changed files for .branchChangedLines to verify only requested files are returned
    assertThat(changedFiles.remove(b1.resolve("sub/file-b1.xoo"))).isTrue();

    // generate common sample diff
    createAndCommitFile(b1, "lao.txt", CONTENT_TZU);
    changedFiles.add(b1.resolve("lao.txt"));

    // a file that should not yield any results
    changedFiles.add(b1.resolve("nonexistent"));

    // modify file without committing to it
    svnTester.appendToFile(b1, "file-m1.xoo");

    Map<Path, Set<Integer>> expected = new HashMap<>();
    expected.put(b1.resolve("lao.txt"), new HashSet<>(Arrays.asList(2, 3, 11, 12, 13)));
    expected.put(b1.resolve("file-m1.xoo"), new HashSet<>(Arrays.asList(2, 3, 4)));
    expected.put(b1.resolve("file-m1-copy.xoo"), new HashSet<>(Arrays.asList(1, 2)));

    assertThat(newScmProvider().branchChangedLines("trunk", b1, changedFiles))
      .isEqualTo(expected);

    assertThat(newScmProvider().branchChangedLines("trunk", b1, Collections.singleton(b1.resolve("nonexistent"))))
      .isEmpty();
  }

  @Test
  public void branchChangedFiles_should_return_empty_when_no_local_changes() throws IOException, SVNException {
    Path b1 = temp.newFolder().toPath();
    svnTester.createBranch("b1");
    svnTester.checkout(b1, "branches/b1");

    assertThat(newScmProvider().branchChangedFiles("b1", b1)).isEmpty();
  }

  @Test
  public void branchChangedFiles_should_return_null_when_repo_nonexistent() throws IOException {
    assertThat(newScmProvider().branchChangedFiles("trunk", temp.newFolder().toPath())).isNull();
  }

  @Test
  public void branchChangedFiles_should_return_null_when_dir_nonexistent() {
    assertThat(newScmProvider().branchChangedFiles("trunk", temp.getRoot().toPath().resolve("nonexistent"))).isNull();
  }

  @Test
  public void branchChangedLines_should_return_null_when_repo_nonexistent() throws IOException {
    assertThat(newScmProvider().branchChangedLines("trunk", temp.newFolder().toPath(), Collections.emptySet())).isNull();
  }

  @Test
  public void branchChangedLines_should_return_null_when_dir_nonexistent() {
    assertThat(newScmProvider().branchChangedLines("trunk", temp.getRoot().toPath().resolve("nonexistent"), Collections.emptySet())).isNull();
  }

  @Test
  public void branchChangedLines_should_return_empty_when_no_local_changes() throws IOException, SVNException {
    Path b1 = temp.newFolder().toPath();
    svnTester.createBranch("b1");
    svnTester.checkout(b1, "branches/b1");

    assertThat(newScmProvider().branchChangedLines("b1", b1, Collections.emptySet())).isEmpty();
  }

  @Test
  public void branchChangedLines_should_return_null_when_invalid_diff_format() throws IOException, SVNException {
    Path b1 = temp.newFolder().toPath();
    svnTester.createBranch("b1");
    svnTester.checkout(b1, "branches/b1");

    SvnScmProvider scmProvider = new SvnScmProvider(config, new SvnBlameCommand(config)) {
      @Override
      ChangedLinesComputer newChangedLinesComputer(Path rootBaseDir, Set<Path> changedFiles) {
        throw new IllegalStateException("crash");
      }
    };
    assertThat(scmProvider.branchChangedLines("b1", b1, Collections.emptySet())).isNull();
  }

  @Test
  public void forkDate_returns_null() throws SVNException {
    SvnScmProvider provider = new SvnScmProvider(config, new SvnBlameCommand(config));
    assertThat(provider.forkDate("", Paths.get(""))).isNull();
  }

  @Test
  public void computeChangedPaths_should_not_crash_when_getRepositoryRootURL_getPath_is_empty() throws SVNException {
    // verify assumptions about what SVNKit returns as svn root path for urls like http://svnserver/
    assertThat(SVNURL.parseURIEncoded("http://svnserver/").getPath()).isEmpty();
    assertThat(SVNURL.parseURIEncoded("http://svnserver").getPath()).isEmpty();

    SVNClientManager svnClientManagerMock = mock(SVNClientManager.class);

    SVNWCClient svnwcClientMock = mock(SVNWCClient.class);
    when(svnClientManagerMock.getWCClient()).thenReturn(svnwcClientMock);

    SVNLogClient svnLogClient = mock(SVNLogClient.class);
    when(svnClientManagerMock.getLogClient()).thenReturn(svnLogClient);

    SVNInfo svnInfoMock = mock(SVNInfo.class);
    when(svnwcClientMock.doInfo(any(), any())).thenReturn(svnInfoMock);

    // Simulate repository root on /, SVNKIT then returns an repository root url WITHOUT / at the end.
    when(svnInfoMock.getRepositoryRootURL()).thenReturn(SVNURL.parseURIEncoded("http://svnserver"));
    when(svnInfoMock.getURL()).thenReturn(SVNURL.parseURIEncoded("http://svnserver/myproject/trunk/"));

    assertThat(SvnScmProvider.computeChangedPaths(Paths.get("/"), svnClientManagerMock)).isEmpty();
  }

  private void createAndCommitFile(Path worktree, String filename, String content) throws IOException, SVNException {
    svnTester.createFile(worktree, filename, content);
    svnTester.add(worktree, filename);
    svnTester.commit(worktree);
  }

  private void createAndCommitFile(Path worktree, String filename) throws IOException, SVNException {
    createAndCommitFile(worktree, filename, filename + "\n");
  }

  private void appendToAndCommitFile(Path worktree, String filename) throws IOException, SVNException {
    svnTester.appendToFile(worktree, filename);
    svnTester.commit(worktree);
  }

  private void deleteAndCommitFile(Path worktree, String filename) throws SVNException {
    svnTester.deleteFile(worktree, filename);
    svnTester.commit(worktree);
  }

  private void svnCopyAndCommitFile(Path worktree, String src, String dst) throws SVNException {
    svnTester.copy(worktree, src, dst);
    svnTester.commit(worktree);
  }

  private SvnScmProvider newScmProvider() {
    return new SvnScmProvider(config, new SvnBlameCommand(config));
  }
}
