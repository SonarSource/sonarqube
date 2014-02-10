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

public class FileMetadataTest {

  private static final String EXPECTED_HASH_WITHOUT_LATEST_EOL = "c80cc50d65ace6c4eb63f189d274dbeb";
  private static final String EXPECTED_HASH_WITH_LATEST_EOL = "bf77e51d219e7d7d643faac86f1b5d15";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void windows_without_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\r\nbar\r\nbaz", Charsets.UTF_8, true);

    FileMetadata.Metadata metadata = FileMetadata.INSTANCE.read(tempFile, Charsets.UTF_8);
    assertThat(metadata.lines).isEqualTo(3L);
    assertThat(metadata.hash).isEqualTo(EXPECTED_HASH_WITHOUT_LATEST_EOL);
  }

  @Test
  public void windows_with_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\r\nbar\r\nbaz\r\n", Charsets.UTF_8, true);

    FileMetadata.Metadata metadata = FileMetadata.INSTANCE.read(tempFile, Charsets.UTF_8);
    assertThat(metadata.lines).isEqualTo(3L);
    assertThat(metadata.hash).isEqualTo(EXPECTED_HASH_WITH_LATEST_EOL);
  }

  @Test
  public void unix_without_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\nbar\nbaz", Charsets.UTF_8, true);

    FileMetadata.Metadata metadata = FileMetadata.INSTANCE.read(tempFile, Charsets.UTF_8);
    assertThat(metadata.lines).isEqualTo(3L);
    assertThat(metadata.hash).isEqualTo(EXPECTED_HASH_WITHOUT_LATEST_EOL);
  }

  @Test
  public void unix_with_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\nbar\nbaz\n", Charsets.UTF_8, true);

    FileMetadata.Metadata metadata = FileMetadata.INSTANCE.read(tempFile, Charsets.UTF_8);
    assertThat(metadata.lines).isEqualTo(3L);
    assertThat(metadata.hash).isEqualTo(EXPECTED_HASH_WITH_LATEST_EOL);
  }

  @Test
  public void mix_of_newlines_with_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\nbar\r\nbaz\n", Charsets.UTF_8, true);

    FileMetadata.Metadata metadata = FileMetadata.INSTANCE.read(tempFile, Charsets.UTF_8);
    assertThat(metadata.lines).isEqualTo(3L);
    assertThat(metadata.hash).isEqualTo(EXPECTED_HASH_WITH_LATEST_EOL);
  }

  @Test
  public void mix_of_newlines_without_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\nbar\r\nbaz", Charsets.UTF_8, true);

    FileMetadata.Metadata metadata = FileMetadata.INSTANCE.read(tempFile, Charsets.UTF_8);
    assertThat(metadata.lines).isEqualTo(3L);
    assertThat(metadata.hash).isEqualTo(EXPECTED_HASH_WITHOUT_LATEST_EOL);
  }

  @Test
  public void should_throw_if_file_does_not_exist() throws Exception {
    File tempFolder = temp.newFolder();
    File file = new File(tempFolder, "doesNotExist.txt");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to read file '" + file.getAbsolutePath() + "' with encoding 'UTF-8'");

    FileMetadata.INSTANCE.read(file, Charsets.UTF_8);
  }
}
