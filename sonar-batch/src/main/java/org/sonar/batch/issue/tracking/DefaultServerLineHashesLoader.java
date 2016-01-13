/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.issue.tracking;

import org.sonar.batch.cache.WSLoader.LoadStrategy;

import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;
import org.apache.commons.lang.mutable.MutableBoolean;

import javax.annotation.Nullable;

import org.sonar.batch.util.BatchUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;

public class DefaultServerLineHashesLoader implements ServerLineHashesLoader {

  private final WSLoader wsLoader;

  public DefaultServerLineHashesLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public String[] getLineHashes(String fileKey, @Nullable MutableBoolean fromCache) {
    String hashesFromWs = loadHashesFromWs(fileKey, fromCache);
    return Iterators.toArray(Splitter.on('\n').split(hashesFromWs).iterator(), String.class);
  }

  private String loadHashesFromWs(String fileKey, @Nullable MutableBoolean fromCache) {
    Profiler profiler = Profiler.createIfDebug(Loggers.get(getClass()))
      .addContext("file", fileKey)
      .startDebug("Load line hashes");
    WSLoaderResult<String> result = wsLoader.loadString("/api/sources/hash?key=" + BatchUtils.encodeForUrl(fileKey), LoadStrategy.CACHE_FIRST);
    try {
      if (fromCache != null) {
        fromCache.setValue(result.isFromCache());
      }
      return result.get();
    } finally {
      if (result.isFromCache()) {
        profiler.stopDebug("Load line hashes (done from cache)");
      } else {
        profiler.stopDebug();
      }
    }
  }
}
