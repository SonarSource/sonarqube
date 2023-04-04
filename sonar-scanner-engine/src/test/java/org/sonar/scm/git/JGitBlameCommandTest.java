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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.sonar.scm.git.Utils.javaUnzip;

public class JGitBlameCommandTest {

  private static final String DUMMY_JAVA = "src/main/java/org/dummy/Dummy.java";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private final JGitBlameCommand jGitBlameCommand = new JGitBlameCommand();
  private Path baseDir;

  @Before
  public void before() throws IOException {
    File projectDir = createNewTempFolder();
    javaUnzip("dummy-git.zip", projectDir);
    baseDir = projectDir.toPath().resolve("dummy-git");
  }

  @Test
  public void blame_returns_all_lines() {


    try (Git git = loadRepository(baseDir)) {
      List<BlameLine> blameLines = jGitBlameCommand.blame(git, DUMMY_JAVA);

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

      assertThat(blameLines).isEqualTo(expectedBlame);
    }
  }

  @Test
  public void modified_file_returns_no_blame() throws IOException {

    // Emulate a modification
    Files.write(baseDir.resolve(DUMMY_JAVA), "modification and \n some new line".getBytes());

    try (Git git = loadRepository(baseDir)) {
      assertThat(jGitBlameCommand.blame(git, DUMMY_JAVA)).isEmpty();
    }
  }

  @Test
  public void new_file_returns_no_blame() throws IOException {
    String relativePath2 = "src/main/java/org/dummy/Dummy2.java";

    // Emulate a new file
    FileUtils.copyFile(new File(baseDir.toFile(), DUMMY_JAVA), new File(baseDir.toFile(), relativePath2));

    try (Git git = loadRepository(baseDir)) {
      assertThat(jGitBlameCommand.blame(git, DUMMY_JAVA)).hasSize(29);
      assertThat(jGitBlameCommand.blame(git, relativePath2)).isEmpty();
    }
  }

  @Test
  public void symlink_doesnt_fail() throws IOException {
    assumeTrue(!System2.INSTANCE.isOsWindows());
    String relativePath2 = "src/main/java/org/dummy/Dummy2.java";

    // Create symlink
    Files.createSymbolicLink(baseDir.resolve(relativePath2), baseDir.resolve(DUMMY_JAVA));

    try (Git git = loadRepository(baseDir)) {
      jGitBlameCommand.blame(git, DUMMY_JAVA);
      jGitBlameCommand.blame(git, relativePath2);
    }
  }

  private Git loadRepository(Path dir) {
    Repository repo = JGitUtils.buildRepository(dir);
    return Git.wrap(repo);
  }

  private File createNewTempFolder() throws IOException {
    //This is needed for Windows, otherwise the created File point to invalid (shortened by Windows) temp folder path
    return temp.newFolder().toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toFile();
  }
}
