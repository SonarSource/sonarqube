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
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.SonarException;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class HashBuilderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private HashBuilder hashBuilder = new HashBuilder();

  @Test
  public void should_compute_hash() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\r\nbar", Charsets.UTF_8, true);

    assertThat(hashBuilder.computeHashNormalizeLineEnds(tempFile, Charsets.UTF_8)).isEqualTo("daef8a22a3f12580beadf086a9e11519");
  }

  @Test
  public void should_normalize_line_ends() throws Exception {
    File file1 = temp.newFile();
    FileUtils.write(file1, "foobar\nfofo", Charsets.UTF_8);
    String hash1 = hashBuilder.computeHashNormalizeLineEnds(file1, Charsets.UTF_8);

    File file2 = temp.newFile();
    FileUtils.write(file2, "foobar\r\nfofo", Charsets.UTF_8);
    String hash2 = hashBuilder.computeHashNormalizeLineEnds(file2, Charsets.UTF_8);

    File file3 = temp.newFile();
    FileUtils.write(file3, "foobar\rfofo", Charsets.UTF_8);
    String hash3 = hashBuilder.computeHashNormalizeLineEnds(file3, Charsets.UTF_8);

    File file4 = temp.newFile();
    FileUtils.write(file4, "foobar\nfofo\n", Charsets.UTF_8);
    String hash4 = hashBuilder.computeHashNormalizeLineEnds(file4, Charsets.UTF_8);

    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).isEqualTo(hash3);
    assertThat(hash1).isNotEqualTo(hash4);
  }

  @Test(expected = SonarException.class)
  public void should_throw_on_not_existing_file() throws Exception {
    File tempFolder = temp.newFolder();
    hashBuilder.computeHashNormalizeLineEnds(new File(tempFolder, "unknowFile.txt"), Charsets.UTF_8);
  }
}
