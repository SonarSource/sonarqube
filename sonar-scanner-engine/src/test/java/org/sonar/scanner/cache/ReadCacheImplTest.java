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

import java.io.InputStream;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadCacheImplTest {
  private final AnalysisCacheStorage storage = mock(AnalysisCacheStorage.class);
  private final ReadCacheImpl readCache = new ReadCacheImpl(storage);

  @Test
  public void read_delegates_to_storage() {
    InputStream is = mock(InputStream.class);
    when(storage.get("key")).thenReturn(is);
    when(storage.contains("key")).thenReturn(true);
    assertThat(readCache.read("key")).isEqualTo(is);
  }

  @Test
  public void read_fails_if_key_not_found() {
    assertThatThrownBy(() -> readCache.read("unknown")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void contains_delegates_to_storage() {
    when(storage.contains("key")).thenReturn(true);
    assertThat(readCache.contains("key")).isTrue();
  }
}
