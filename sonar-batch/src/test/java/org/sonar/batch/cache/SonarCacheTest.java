/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.cache;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class SonarCacheTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private SonarCache cache;

  @Before
  public void prepare() throws IOException {
    cache = SonarCache.create().setCacheLocation(tempFolder.newFolder()).build();
  }

  @Test
  public void testCacheExternalFile() throws IOException {
    // Create a file outside the cache
    File fileToCache = tempFolder.newFile();
    FileUtils.write(fileToCache, "Sample data");
    // Put it in the cache
    String md5 = cache.cacheFile(fileToCache, "foo.txt");
    // Verify the temporary location was created to do the copy in the cache in 2 stages
    File tmpCache = new File(cache.getCacheLocation(), "tmp");
    assertThat(tmpCache).exists();
    // The tmp location should be empty as the file was moved inside the cache
    assertThat(tmpCache.list()).isEmpty();
    // Verify it is present in the cache folder
    File fileInCache = new File(new File(cache.getCacheLocation(), md5), "foo.txt");
    assertThat(fileInCache).exists();
    String content = FileUtils.readFileToString(fileInCache);
    assertThat(content).isEqualTo("Sample data");
    // Now retrieve from cache API
    File fileFromCache = cache.getFileFromCache("foo.txt", md5);
    assertThat(fileFromCache.getCanonicalPath()).isEqualTo(fileInCache.getCanonicalPath());
  }

  @Test
  public void testCacheInternalFile() throws IOException {
    // Create a file in the cache temp location
    File fileToCache = cache.getTemporaryFile();
    // Verify the temporary location was created
    File tmpCache = new File(cache.getCacheLocation(), "tmp");
    assertThat(tmpCache).exists();
    assertThat(tmpCache.list().length).isEqualTo(1);

    FileUtils.write(fileToCache, "Sample data");
    String md5 = cache.cacheFile(fileToCache, "foo.txt");
    // Verify it is present in the cache folder
    File fileInCache = new File(new File(cache.getCacheLocation(), md5), "foo.txt");
    assertThat(fileInCache).exists();
    String content = FileUtils.readFileToString(fileInCache);
    assertThat(content).isEqualTo("Sample data");
    // Now retrieve from cache API
    File fileFromCache = cache.getFileFromCache("foo.txt", md5);
    assertThat(fileFromCache.getCanonicalPath()).isEqualTo(fileInCache.getCanonicalPath());
  }

  @Test
  public void testGetFileNotInCache() throws IOException {
    File fileFromCache = cache.getFileFromCache("foo.txt", "mockmd5");
    assertThat(fileFromCache).isNull();
  }

}
