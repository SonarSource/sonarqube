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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static java.util.Collections.unmodifiableMap;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkNotNull;

public class WriteCacheImpl implements ScannerWriteCache {
  private final ReadCache readCache;
  private final BranchConfiguration branchConfiguration;
  private final Map<String, byte[]> cache = new HashMap<>();

  public WriteCacheImpl(ReadCache readCache, BranchConfiguration branchConfiguration) {
    this.readCache = readCache;
    this.branchConfiguration = branchConfiguration;
  }

  @Override
  public void write(String key, InputStream data) {
    checkNotNull(data);
    checkKey(key);
    if (branchConfiguration.isPullRequest()) {
      return;
    }
    try {
      byte[] arr = data.readAllBytes();
      cache.put(key, arr);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read stream", e);
    }
  }

  @Override
  public void write(String key, byte[] data) {
    checkNotNull(data);
    checkKey(key);
    if (branchConfiguration.isPullRequest()) {
      return;
    }
    cache.put(key, Arrays.copyOf(data, data.length));
  }

  @Override
  public void copyFromPrevious(String key) {
    checkArgument(readCache.contains(key), "Previous cache doesn't contain key '%s'", key);
    checkKey(key);
    if (branchConfiguration.isPullRequest()) {
      return;
    }
    try {
      cache.put(key, readCache.read(key).readAllBytes());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read plugin cache for key " + key, e);
    }
  }

  @Override
  public Map<String, byte[]> getCache() {
    return unmodifiableMap(cache);
  }

  private void checkKey(String key) {
    checkNotNull(key);
    checkArgument(!cache.containsKey(key), "Cache already contains key '%s'", key);
  }
}
