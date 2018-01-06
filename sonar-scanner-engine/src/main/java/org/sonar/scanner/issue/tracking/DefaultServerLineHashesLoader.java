/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.issue.tracking;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.io.Reader;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.util.ScannerUtils;
import org.sonarqube.ws.client.GetRequest;

public class DefaultServerLineHashesLoader implements ServerLineHashesLoader {
  private ScannerWsClient wsClient;

  public DefaultServerLineHashesLoader(ScannerWsClient wsClient) {
    this.wsClient = wsClient;
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

    GetRequest getRequest = new GetRequest("/api/sources/hash?key=" + ScannerUtils.encodeForUrl(fileKey));
    Reader reader = wsClient.call(getRequest).contentReader();
    try {
      return IOUtils.toString(reader);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } finally {
      profiler.stopDebug();
    }
  }
}
