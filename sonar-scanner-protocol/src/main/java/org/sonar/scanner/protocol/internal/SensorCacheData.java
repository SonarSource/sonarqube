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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.scanner.protocol.internal.ScannerInternal.SensorCacheEntry;

public class SensorCacheData {
  private final Map<String, ByteString> map;

  public SensorCacheData(List<SensorCacheEntry> entries) {
    this.map = Collections.unmodifiableMap(entries.stream().collect(Collectors.toMap(SensorCacheEntry::getKey, SensorCacheEntry::getData)));
  }

  public Map<String, ByteString> getEntries() {
    return map;
  }
}
