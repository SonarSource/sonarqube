/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class FileHashDigestTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_compute_hash() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\r\nbar", Charsets.UTF_8, true);

    assertThat(FileHashDigest.INSTANCE.hash(tempFile, Charsets.UTF_8)).isEqualTo("daef8a22a3f12580beadf086a9e11519");
  }

  @Test
  public void should_normalize_line_ends() throws Exception {
    File file1 = temp.newFile();
    FileUtils.write(file1, "foobar\nfofo", Charsets.UTF_8);
    String hash1 = FileHashDigest.INSTANCE.hash(file1, Charsets.UTF_8);

    File file2 = temp.newFile();
    FileUtils.write(file2, "foobar\r\nfofo", Charsets.UTF_8);
    String hash2 = FileHashDigest.INSTANCE.hash(file2, Charsets.UTF_8);

    File file3 = temp.newFile();
    FileUtils.write(file3, "foobar\rfofo", Charsets.UTF_8);
    String hash3 = FileHashDigest.INSTANCE.hash(file3, Charsets.UTF_8);

    File file4 = temp.newFile();
    FileUtils.write(file4, "foobar\nfofo\n", Charsets.UTF_8);
    String hash4 = FileHashDigest.INSTANCE.hash(file4, Charsets.UTF_8);

    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).isEqualTo(hash3);
    assertThat(hash1).isNotEqualTo(hash4);
  }

  @Test
  public void should_throw_if_file_does_not_exist() throws Exception {
    File tempFolder = temp.newFolder();
    File file = new File(tempFolder, "doesNotExist.txt");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to compute hash of file " + file.getAbsolutePath() + " with charset UTF-8");

    FileHashDigest.INSTANCE.hash(file, Charsets.UTF_8);
  }
}
