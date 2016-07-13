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
package org.sonar.scanner.repository;

import org.sonar.scanner.bootstrap.BatchWsClient;
import org.sonar.scanner.protocol.input.GlobalRepositories;
import org.sonarqube.ws.client.GetRequest;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.IOUtils;

public class DefaultGlobalRepositoriesLoader implements GlobalRepositoriesLoader {

  private static final String BATCH_GLOBAL_URL = "/batch/global";
  private BatchWsClient wsClient;

  public DefaultGlobalRepositoriesLoader(BatchWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public GlobalRepositories load() {
    GetRequest getRequest = new GetRequest(BATCH_GLOBAL_URL);
    String str;
    try (Reader reader = wsClient.call(getRequest).contentReader()) {
      str = IOUtils.toString(reader);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return GlobalRepositories.fromJson(str);
  }
}
