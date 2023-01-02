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
import javax.annotation.Nullable;
import org.sonar.scanner.protocol.internal.ScannerInternal.AnalysisCacheMsg;

public class AnalysisCacheMemoryStorage implements AnalysisCacheStorage {
  private final AnalysisCacheLoader loader;
  @Nullable
  private AnalysisCacheMsg cache;

  public AnalysisCacheMemoryStorage(AnalysisCacheLoader loader) {
    this.loader = loader;
  }

  @Override
  public InputStream get(String key) {
    if (!contains(key)) {
      throw new IllegalArgumentException("Key not found: " + key);
    }
    return cache.getMapOrThrow(key).newInput();
  }

  @Override
  public boolean contains(String key) {
    if (cache == null) {
      return false;
    }
    return cache.containsMap(key);
  }

  public void load() {
    cache = loader.load().orElse(null);
  }
}
