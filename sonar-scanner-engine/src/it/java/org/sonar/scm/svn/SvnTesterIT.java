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

import java.io.IOException;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tmatesoft.svn.core.SVNException;

import static org.assertj.core.api.Assertions.assertThat;

public class SvnTesterIT {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SvnTester tester;

  @Before
  public void before() throws IOException, SVNException {
    tester = new SvnTester(temp.newFolder().toPath());
  }

  @Test
  public void test_init() throws SVNException {
    assertThat(tester.list()).containsExactlyInAnyOrder("trunk", "branches");
  }

  @Test
  public void test_add_and_commit() throws IOException, SVNException {
    assertThat(tester.list("trunk")).isEmpty();

    Path worktree = temp.newFolder().toPath();
    tester.checkout(worktree, "trunk");
    tester.createFile(worktree, "file1");

    tester.add(worktree, "file1");
    tester.commit(worktree);

    assertThat(tester.list("trunk")).containsOnly("file1");
  }

  @Test
  public void test_createBranch() throws IOException, SVNException {
    tester.createBranch("b1");
    assertThat(tester.list()).containsExactlyInAnyOrder("trunk", "branches", "branches/b1");
    assertThat(tester.list("branches")).containsOnly("b1");
  }
}
