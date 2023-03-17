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
package org.sonar.scanner.cache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.util.Protobuf;
import org.sonar.scanner.protocol.internal.ScannerInternal.SensorCacheEntry;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WriteCacheImplTest {
  private final ReadCacheImpl readCache = mock(ReadCacheImpl.class);
  private final BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private FileStructure fileStructure;
  private WriteCacheImpl writeCache;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    fileStructure = new FileStructure(temp.newFolder());
    writeCache = new WriteCacheImpl(readCache, fileStructure);
  }

  @Test
  public void write_bytes_adds_entries()  {
    byte[] b1 = new byte[] {1, 2, 3};
    byte[] b2 = new byte[] {3, 4};
    writeCache.write("key", b1);
    writeCache.write("key2", b2);

    assertThatCacheContains(Map.of("key", b1, "key2", b2));
  }

  @Test
  public void dont_write_if_its_pull_request()  {
    byte[] b1 = new byte[] {1, 2, 3};
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    writeCache.write("key1", b1);
    writeCache.write("key2", new ByteArrayInputStream(b1));
    assertThatCacheContains(Map.of());
  }

  @Test
  public void write_inputStream_adds_entries()  {
    byte[] b1 = new byte[] {1, 2, 3};
    byte[] b2 = new byte[] {3, 4};
    writeCache.write("key", new ByteArrayInputStream(b1));
    writeCache.write("key2", new ByteArrayInputStream(b2));

    assertThatCacheContains(Map.of("key", b1, "key2", b2));
  }

  @Test
  public void write_throws_IAE_if_writing_same_key_twice() {
    byte[] b1 = new byte[] {1};
    byte[] b2 = new byte[] {2};

    writeCache.write("key", b1);
    assertThatThrownBy(() -> writeCache.write("key", b2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cache already contains key 'key'");
  }

  @Test
  public void copyFromPrevious_throws_IAE_if_read_cache_doesnt_contain_key() {
    assertThatThrownBy(() -> writeCache.copyFromPrevious("key"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Previous cache doesn't contain key 'key'");
  }

  @Test
  public void copyFromPrevious_reads_from_readCache() throws IOException {
    byte[] b = new byte[] {1};
    InputStream value = new ByteArrayInputStream(b);
    when(readCache.contains("key")).thenReturn(true);
    when(readCache.read("key")).thenReturn(value);
    writeCache.copyFromPrevious("key");

    assertThatCacheContains(Map.of("key", b));
  }

  private void assertThatCacheContains(Map<String, byte[]> expectedData) {
    writeCache.close();
    File cacheFile = fileStructure.analysisCache();
    Iterable<SensorCacheEntry> it = () -> Protobuf.readGzipStream(cacheFile, SensorCacheEntry.parser());
    Map<String, byte[]> data = StreamSupport.stream(it.spliterator(), false)
      .collect(Collectors.toMap(SensorCacheEntry::getKey, e -> e.getData().toByteArray()));

    assertThat(data).containsAllEntriesOf(expectedData);
  }
}
