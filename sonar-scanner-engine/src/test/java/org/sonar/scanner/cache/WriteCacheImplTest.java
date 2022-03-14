/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.io.InputStream;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WriteCacheImplTest {
  private final ReadCacheImpl readCache = mock(ReadCacheImpl.class);
  private final WriteCacheImpl writeCache = new WriteCacheImpl(readCache);

  @Test
  public void write_bytes_adds_entries() {
    byte[] b1 = new byte[] {1, 2, 3};
    byte[] b2 = new byte[] {3, 4};
    writeCache.write("key", b1);
    writeCache.write("key2", b2);

    assertThat(writeCache.getCache()).containsOnly(entry("key", b1), entry("key2", b2));
  }

  @Test
  public void write_inputStream_adds_entries() {
    byte[] b1 = new byte[] {1, 2, 3};
    byte[] b2 = new byte[] {3, 4};
    writeCache.write("key", new ByteArrayInputStream(b1));
    writeCache.write("key2", new ByteArrayInputStream(b2));

    assertThat(writeCache.getCache()).containsOnly(entry("key", b1), entry("key2", b2));
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
    assertThatThrownBy(() ->    writeCache.copyFromPrevious("key"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Previous cache doesn't contain key 'key'");
  }

  @Test
  public void copyFromPrevious_reads_from_readCache() {
    byte[] b = new byte[] {1};
    InputStream value = new ByteArrayInputStream(b);
    when(readCache.contains("key")).thenReturn(true);
    when(readCache.read("key")).thenReturn(value);
    writeCache.copyFromPrevious("key");

    assertThat(writeCache.getCache()).containsOnly(entry("key", b));
  }
}
