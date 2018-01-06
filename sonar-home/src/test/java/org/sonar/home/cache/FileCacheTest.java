/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.home.cache;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class FileCacheTest {
  private FileHashes fileHashes;
  private FileCache cache;

  @Before
  public void setUp() throws IOException {
    fileHashes = mock(FileHashes.class);
    cache = new FileCache(tempFolder.getRoot(), fileHashes, mock(Logger.class));
  }

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void not_in_cache() throws IOException {
    assertThat(cache.get("sonar-foo-plugin-1.5.jar", "ABCDE")).isNull();
  }

  @Test
  public void found_in_cache() throws IOException {
    // populate the cache. Assume that hash is correct.
    File cachedFile = new File(new File(cache.getDir(), "ABCDE"), "sonar-foo-plugin-1.5.jar");
    FileUtils.write(cachedFile, "body");

    assertThat(cache.get("sonar-foo-plugin-1.5.jar", "ABCDE")).isNotNull().exists().isEqualTo(cachedFile);
  }

  @Test
  public void fail_to_download() {
    when(fileHashes.of(any(File.class))).thenReturn("ABCDE");

    FileCache.Downloader downloader = new FileCache.Downloader() {
      public void download(String filename, File toFile) throws IOException {
        throw new IOException("fail");
      }
    };
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to download");
    cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", downloader);
  }

  @Test
  public void fail_create_hash_dir() throws IOException {
    File file = tempFolder.newFile();
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to create user cache");
    cache = new FileCache(file, fileHashes, mock(Logger.class));
  }

  @Test
  public void fail_to_create_hash_dir() throws IOException {
    when(fileHashes.of(any(File.class))).thenReturn("ABCDE");

    File hashDir = new File(cache.getDir(), "ABCDE");
    hashDir.createNewFile();
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to create cache directory");
    cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", mock(FileCache.Downloader.class));
  }

  @Test
  public void download_and_add_to_cache() throws IOException {
    when(fileHashes.of(any(File.class))).thenReturn("ABCDE");

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
  public void download_and_add_to_cache_compressed_file() throws IOException {
    when(fileHashes.of(any(File.class))).thenReturn("ABCDE");

    FileCache.Downloader downloader = new FileCache.Downloader() {
      public void download(String filename, File toFile) throws IOException {
        FileUtils.copyFile(compressedFile(), toFile);
      }
    };
    File cachedFile = cache.getCompressed("sonar-foo-plugin-1.5.pack.gz", "ABCDE", downloader);
    assertThat(cachedFile).isNotNull().exists().isFile();

    assertThat(cachedFile.getName()).isEqualTo("sonar-foo-plugin-1.5.jar");
    assertThat(cachedFile.getParentFile().list()).containsOnly("sonar-foo-plugin-1.5.jar", "sonar-foo-plugin-1.5.pack.gz");
    assertThat(cachedFile.getParentFile().getParentFile()).isEqualTo(cache.getDir());
  }

  @Test
  public void dont_download_compressed_file_if_jar_exists() throws IOException {
    when(fileHashes.of(any(File.class))).thenReturn("ABCDE");
    FileCache.Downloader downloader = mock(FileCache.Downloader.class);

    File hashDir = new File(cache.getDir(), "ABCDE");
    hashDir.mkdirs();
    File jar = new File(new File(cache.getDir(), "ABCDE"), "sonar-foo-plugin-1.5.jar");
    jar.createNewFile();
    File cachedFile = cache.getCompressed("sonar-foo-plugin-1.5.pack.gz", "ABCDE", downloader);
    assertThat(cachedFile).isNotNull().exists().isFile();

    assertThat(cachedFile.getName()).isEqualTo("sonar-foo-plugin-1.5.jar");
    assertThat(cachedFile.getParentFile().list()).containsOnly("sonar-foo-plugin-1.5.jar");
    assertThat(cachedFile.getParentFile().getParentFile()).isEqualTo(cache.getDir());

    verifyZeroInteractions(downloader);
  }

  @Test
  public void dont_download_compressed_file_if_it_exists() throws IOException {
    when(fileHashes.of(any(File.class))).thenReturn("ABCDE");
    FileCache.Downloader downloader = mock(FileCache.Downloader.class);

    File hashDir = new File(cache.getDir(), "ABCDE");
    hashDir.mkdirs();
    FileUtils.copyFile(compressedFile(), new File(hashDir, "sonar-foo-plugin-1.5.pack.gz"));
    File cachedFile = cache.getCompressed("sonar-foo-plugin-1.5.pack.gz", "ABCDE", downloader);
    assertThat(cachedFile).isNotNull().exists().isFile();

    assertThat(cachedFile.getName()).isEqualTo("sonar-foo-plugin-1.5.jar");
    assertThat(cachedFile.getParentFile().list()).containsOnly("sonar-foo-plugin-1.5.jar", "sonar-foo-plugin-1.5.pack.gz");
    assertThat(cachedFile.getParentFile().getParentFile()).isEqualTo(cache.getDir());

    verifyZeroInteractions(downloader);
  }

  @Test
  public void download_corrupted_file() throws IOException {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("INVALID HASH");

    when(fileHashes.of(any(File.class))).thenReturn("VWXYZ");

    FileCache.Downloader downloader = new FileCache.Downloader() {
      public void download(String filename, File toFile) throws IOException {
        FileUtils.write(toFile, "corrupted body");
      }
    };
    cache.get("sonar-foo-plugin-1.5.jar", "ABCDE", downloader);
  }

  @Test
  public void concurrent_download() throws IOException {
    when(fileHashes.of(any(File.class))).thenReturn("ABCDE");

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

  private File compressedFile() {
    return new File("src/test/resources/test.pack.gz");
  }
}
