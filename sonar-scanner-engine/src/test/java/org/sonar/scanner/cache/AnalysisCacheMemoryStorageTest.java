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

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.scanner.protocol.internal.ScannerInternal.SensorCacheEntry;
import org.sonar.scanner.protocol.internal.SensorCacheData;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AnalysisCacheMemoryStorageTest {
  private final AnalysisCacheLoader loader = mock(AnalysisCacheLoader.class);
  private final AnalysisCacheMemoryStorage storage = new AnalysisCacheMemoryStorage(loader);

  @Test
  public void storage_loads_with_loader() throws IOException {
    SensorCacheData data = new SensorCacheData(List.of(SensorCacheEntry.newBuilder()
      .setKey("key1")
      .setData(ByteString.copyFrom("value1", UTF_8))
      .build()));
    when(loader.load()).thenReturn(Optional.of(data));

    storage.load();
    verify(loader).load();
    assertThat(IOUtils.toString(storage.get("key1"), UTF_8)).isEqualTo("value1");
    assertThat(storage.contains("key1")).isTrue();
  }

  @Test
  public void get_throws_IAE_if_doesnt_contain_key() {
    when(loader.load()).thenReturn(Optional.empty());
    storage.load();
    assertThat(storage.contains("key1")).isFalse();
    assertThatThrownBy(() -> storage.get("key1")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void get_throws_IAE_if_no_cache() {
    when(loader.load()).thenReturn(Optional.empty());
    storage.load();
    assertThat(storage.contains("key1")).isFalse();
    assertThatThrownBy(() -> storage.get("key1")).isInstanceOf(IllegalArgumentException.class);
  }
}
