/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.protocol.internal;

import com.google.protobuf.ByteString;
import java.util.List;
import org.junit.Test;
import org.sonar.scanner.protocol.internal.ScannerInternal.SensorCacheEntry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

public class SensorCacheDataTest {
  @Test
  public void constructor_processes_entries() {
    SensorCacheEntry entry1 = SensorCacheEntry.newBuilder().setKey("key1").setData(ByteString.copyFrom("data1", UTF_8)).build();
    SensorCacheEntry entry2 = SensorCacheEntry.newBuilder().setKey("key2").setData(ByteString.copyFrom("data2", UTF_8)).build();

    SensorCacheData data = new SensorCacheData(List.of(entry1, entry2));
    assertThat(data.getEntries()).containsExactly(entry(entry1.getKey(), entry1.getData()), entry(entry2.getKey(), entry2.getData()));
  }

}
