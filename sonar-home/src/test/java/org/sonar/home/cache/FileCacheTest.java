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
package org.sonar.home.cache;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.home.log.Slf4jLog;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileCacheTest {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Slf4jLog log = new Slf4jLog(FileCacheTest.class);

  @Test
  public void not_in_cache() throws IOException {
    FileCache cache = FileCache.create(tempFolder.newFolder(), log);
    assertThat(cache.get("sonar-foo-plugin-1.5.jar", "ABCDE")).isNull();
  }

  @Test
  public void found_in_cache() throws IOException {
    FileCache cache = FileCache.create(tempFolder.newFolder(), log);

    // populate the cache. Assume that hash is correct.
    File cachedFile = new File(new File(cache.getDir(), "ABCDE"), "sonar-foo-plugin-1.5.jar");
    FileUtils.write(cachedFile, "body");

    assertThat(cache.get("sonar-foo-plugin-1.5.jar", "ABCDE")).isNotNull().exists().isEqualTo(cachedFile);
  }

  @Test
  public void download_and_add_to_cache() throws IOException {
    FileHashes hashes = mock(FileHashes.class);
    FileCache cache = new FileCache(tempFolder.newFolder(), log, hashes);
    when(hashes.of(any(File.class))).thenReturn("ABCDE");

    FileCache.Downloader downloader = new FileCache.Downloader() {
      public void download(String filename, File toFile) throws IOException {
        FileUtils.write(toFile, "body");
      }
    };
    File cachedFile = cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", downloader);
    assertThat(cachedFile).isNotNull().exists().isFile();
    assertThat(cachedFile.getName()).isEqualTo("sonar-foo-plugin-1.5.jar");
    assertThat(cachedFile.getParentFile().getParentFile()).isEqualTo(cache.getDir());
    assertThat(FileUtils.readFileToString(cachedFile)).isEqualTo("body");
  }

  @Test
  public void download_corrupted_file() throws IOException {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("INVALID HASH");

    FileHashes hashes = mock(FileHashes.class);
    FileCache cache = new FileCache(tempFolder.newFolder(), log, hashes);
    when(hashes.of(any(File.class))).thenReturn("VWXYZ");

    FileCache.Downloader downloader = new FileCache.Downloader() {
      public void download(String filename, File toFile) throws IOException {
        FileUtils.write(toFile, "corrupted body");
      }
    };
    cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", downloader);
  }

  @Test
  public void concurrent_download() throws IOException {
    FileHashes hashes = mock(FileHashes.class);
    when(hashes.of(any(File.class))).thenReturn("ABCDE");
    final FileCache cache = new FileCache(tempFolder.newFolder(), log, hashes);

    FileCache.Downloader downloader = new FileCache.Downloader() {
      public void download(String filename, File toFile) throws IOException {
        // Emulate a concurrent download that adds file to cache before
        File cachedFile = new File(new File(cache.getDir(), "ABCDE"), "sonar-foo-plugin-1.5.jar");
        FileUtils.write(cachedFile, "downloaded by other");

        FileUtils.write(toFile, "downloaded by me");
      }
    };

    // do not fail
    File cachedFile = cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", downloader);
    assertThat(cachedFile).isNotNull().exists().isFile();
    assertThat(cachedFile.getName()).isEqualTo("sonar-foo-plugin-1.5.jar");
    assertThat(cachedFile.getParentFile().getParentFile()).isEqualTo(cache.getDir());
    assertThat(FileUtils.readFileToString(cachedFile)).contains("downloaded by");
  }
}
