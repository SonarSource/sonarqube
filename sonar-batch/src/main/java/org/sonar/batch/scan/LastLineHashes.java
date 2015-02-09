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
package org.sonar.batch.scan;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.bootstrap.ServerClient;

public class LastLineHashes implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(LastLineHashes.class);

  private final ServerClient server;

  public LastLineHashes(ServerClient server) {
    this.server = server;
  }

  public String[] getLineHashes(String fileKey) {
    String hashesFromWs = loadHashesFromWs(fileKey);
    return Iterators.toArray(Splitter.on('\n').split(hashesFromWs).iterator(), String.class);
  }

  private String loadHashesFromWs(String fileKey) {
    TimeProfiler profiler = new TimeProfiler(LOG).setLevelToDebug().start("Load previous line hashes of: " + fileKey);
    try {
      return server.request("/api/sources/hash?key=" + ServerClient.encodeForUrl(fileKey));
    } finally {
      profiler.stop();
    }
  }
}
