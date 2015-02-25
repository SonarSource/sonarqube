/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.issue.tracking;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.bootstrap.ServerClient;

public class DefaultServerLineHashesLoader implements ServerLineHashesLoader {

  private final ServerClient server;

  public DefaultServerLineHashesLoader(ServerClient server) {
    this.server = server;
  }

  @Override
  public String[] getLineHashes(String fileKey) {
    String hashesFromWs = loadHashesFromWs(fileKey);
    return Iterators.toArray(Splitter.on('\n').split(hashesFromWs).iterator(), String.class);
  }

  private String loadHashesFromWs(String fileKey) {
    Profiler profiler = Profiler.createIfDebug(Loggers.get(getClass()))
      .addContext("file", fileKey)
      .startDebug("Load line hashes");
    try {
      return server.request("/api/sources/hash?key=" + ServerClient.encodeForUrl(fileKey));
    } finally {
      profiler.stopDebug();
    }
  }
}
