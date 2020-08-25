/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tmatesoft.svn.core.SVNException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FindForkTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static SvnTester svnTester;

  private static Path trunk;
  private static Path b1;
  private static Path b2;

  private FindFork findFork;

  @BeforeClass
  public static void before() throws IOException, SVNException {
    svnTester = new SvnTester(temp.newFolder().toPath());

    trunk = temp.newFolder("trunk").toPath();
    svnTester.checkout(trunk, "trunk");
    createAndCommitFile(trunk, "file-1-commit-in-trunk.xoo");
    createAndCommitFile(trunk, "file-2-commit-in-trunk.xoo");
    createAndCommitFile(trunk, "file-3-commit-in-trunk.xoo");
    svnTester.checkout(trunk, "trunk");

    svnTester.createBranch("b1");
    b1 = temp.newFolder("branches", "b1").toPath();
    svnTester.checkout(b1, "branches/b1");
    createAndCommitFile(b1, "file-1-commit-in-b1.xoo");
    createAndCommitFile(b1, "file-2-commit-in-b1.xoo");
    createAndCommitFile(b1, "file-3-commit-in-b1.xoo");
    svnTester.checkout(b1, "branches/b1");

    svnTester.createBranch("branches/b1", "b2");
    b2 = temp.newFolder("branches", "b2").toPath();
    svnTester.checkout(b2, "branches/b2");

    createAndCommitFile(b2, "file-1-commit-in-b2.xoo");
    createAndCommitFile(b2, "file-2-commit-in-b2.xoo");
    createAndCommitFile(b2, "file-3-commit-in-b2.xoo");
    svnTester.checkout(b2, "branches/b2");
  }

  @Before
  public void setUp() {
    SvnConfiguration configurationMock = mock(SvnConfiguration.class);
    findFork = new FindFork(configurationMock);
  }

  @Test
  public void testEmptyBranch() throws SVNException, IOException {
    svnTester.createBranch("empty");
    Path empty = temp.newFolder("branches", "empty").toPath();

    svnTester.checkout(empty, "branches/empty");
    ForkPoint forkPoint = findFork.find(empty, "unknown");
    assertThat(forkPoint).isNull();
  }

  @Test
  public void returnNoDate() throws SVNException {
    FindFork findFork = new FindFork(mock(SvnConfiguration.class)) {
      @Override
      public ForkPoint find(Path location, String referenceBranch) {
        return null;
      }
    };

    assertThat(findFork.findDate(Paths.get(""), "branch")).isNull();
  }

  @Test
  public void testTrunk() throws SVNException {
    ForkPoint forkPoint = findFork.find(trunk, "unknown");
    assertThat(forkPoint).isNull();
  }

  @Test
  public void testB1() throws SVNException {
    ForkPoint forkPoint = findFork.find(b1, "trunk");
    assertThat(forkPoint.commit()).isEqualTo("5");
  }

  @Test
  public void testB2() throws SVNException {
    ForkPoint forkPoint = findFork.find(b2, "branches/b1");
    assertThat(forkPoint.commit()).isEqualTo("9");
  }

  @Test
  public void testB2Date() throws SVNException {
    assertThat(findFork.findDate(b2, "branches/b1")).isNotNull();
  }

  @Test
  public void testB2FromTrunk() throws SVNException {
    ForkPoint forkPoint = findFork.find(b2, "trunk");
    assertThat(forkPoint.commit()).isEqualTo("5");
  }

  private static void createAndCommitFile(Path worktree, String filename, String content) throws IOException, SVNException {
    svnTester.createFile(worktree, filename, content);
    svnTester.add(worktree, filename);
    svnTester.commit(worktree);
  }

  private static void createAndCommitFile(Path worktree, String filename) throws IOException, SVNException {
    createAndCommitFile(worktree, filename, filename + "\n");
  }
}
