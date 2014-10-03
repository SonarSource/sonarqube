/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.scm.git;

import com.google.common.io.Closeables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.scm.BlameCommand.BlameResult;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JGitBlameCommandTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testBlame() throws IOException {
    File projectDir = temp.newFolder();
    javaUnzip(new File("test-repos/dummy-git.zip"), projectDir);

    JGitBlameCommand jGitBlameCommand = new JGitBlameCommand();

    DefaultFileSystem fs = new DefaultFileSystem();
    fs.setBaseDir(new File(projectDir, "dummy-git"));
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/main/java/org/dummy/Dummy.java");
    fs.add(inputFile);

    BlameResult blameResult = mock(BlameResult.class);
    jGitBlameCommand.blame(fs, Arrays.<InputFile>asList(inputFile), blameResult);

    Date revisionDate = DateUtils.parseDateTime("2012-07-17T16:12:48+0200");
    String revision = "6b3aab35a3ea32c1636fee56f996e677653c48ea";
    String author = "david@gageot.net";
    verify(blameResult).add(inputFile,
      Arrays.asList(
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author),
        new BlameLine(revisionDate, revision, author)));

  }

  @Test
  public void testBlameOnModifiedFile() throws IOException {
    File projectDir = temp.newFolder();
    javaUnzip(new File("test-repos/dummy-git.zip"), projectDir);

    JGitBlameCommand jGitBlameCommand = new JGitBlameCommand();

    DefaultFileSystem fs = new DefaultFileSystem();
    File baseDir = new File(projectDir, "dummy-git");
    fs.setBaseDir(baseDir);
    String relativePath = "src/main/java/org/dummy/Dummy.java";
    DefaultInputFile inputFile = new DefaultInputFile("foo", relativePath);
    fs.add(inputFile);

    // Emulate a modification
    FileUtils.write(new File(baseDir, relativePath), "modification and \n some new line", true);

    BlameResult blameResult = mock(BlameResult.class);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to blame file " + relativePath + ". No blame info at line 27. Is file commited?");
    jGitBlameCommand.blame(fs, Arrays.<InputFile>asList(inputFile), blameResult);
  }

  @Test
  public void testBlameOnNewFile() throws IOException {
    File projectDir = temp.newFolder();
    javaUnzip(new File("test-repos/dummy-git.zip"), projectDir);

    JGitBlameCommand jGitBlameCommand = new JGitBlameCommand();

    DefaultFileSystem fs = new DefaultFileSystem();
    File baseDir = new File(projectDir, "dummy-git");
    fs.setBaseDir(baseDir);
    String relativePath = "src/main/java/org/dummy/Dummy.java";
    String relativePath2 = "src/main/java/org/dummy/Dummy2.java";
    DefaultInputFile inputFile = new DefaultInputFile("foo", relativePath);
    fs.add(inputFile);
    DefaultInputFile inputFile2 = new DefaultInputFile("foo", relativePath2);
    fs.add(inputFile2);

    // Emulate a new file
    FileUtils.copyFile(new File(baseDir, relativePath), new File(baseDir, relativePath2));

    BlameResult blameResult = mock(BlameResult.class);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to blame file " + relativePath2 + ". No blame info at line 1. Is file commited?");
    jGitBlameCommand.blame(fs, Arrays.<InputFile>asList(inputFile, inputFile2), blameResult);
  }

  private static void javaUnzip(File zip, File toDir) {
    try {
      ZipFile zipFile = new ZipFile(zip);
      try {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          File to = new File(toDir, entry.getName());
          if (entry.isDirectory()) {
            FileUtils.forceMkdir(to);
          } else {
            File parent = to.getParentFile();
            if (parent != null) {
              FileUtils.forceMkdir(parent);
            }

            OutputStream fos = new FileOutputStream(to);
            try {
              IOUtils.copy(zipFile.getInputStream(entry), fos);
            } finally {
              Closeables.closeQuietly(fos);
            }
          }
        }
      } finally {
        zipFile.close();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to unzip " + zip + " to " + toDir, e);
    }
  }

}
