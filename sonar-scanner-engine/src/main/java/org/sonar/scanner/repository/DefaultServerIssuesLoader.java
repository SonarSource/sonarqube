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
package org.sonar.scanner.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.scanner.util.ScannerUtils;
import org.sonarqube.ws.client.GetRequest;

public class DefaultServerIssuesLoader implements ServerIssuesLoader {

  private final ScannerWsClient wsClient;

  public DefaultServerIssuesLoader(ScannerWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public void load(String componentKey, Consumer<ServerIssue> consumer) {
    GetRequest getRequest = new GetRequest("/batch/issues.protobuf?key=" + ScannerUtils.encodeForUrl(componentKey));
    InputStream is = wsClient.call(getRequest).contentStream();
    parseIssues(is, consumer);
  }

  private static void parseIssues(InputStream is, Consumer<ServerIssue> consumer) {
    try {
      ServerIssue previousIssue = ServerIssue.parseDelimitedFrom(is);
      while (previousIssue != null) {
        consumer.accept(previousIssue);
        previousIssue = ServerIssue.parseDelimitedFrom(is);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get previous issues", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
