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
package org.sonar.scanner.repository;

import java.io.IOException;
import java.io.InputStream;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonarqube.ws.NewCodePeriods;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;

import static org.sonar.api.impl.utils.ScannerUtils.encodeForUrl;

public class DefaultNewCodePeriodLoader implements NewCodePeriodLoader {
  private static final String WS_URL = "/api/new_code_periods/show.protobuf";

  private final DefaultScannerWsClient wsClient;

  public DefaultNewCodePeriodLoader(DefaultScannerWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override public NewCodePeriods.ShowWSResponse load(String projectKey, String branchName) {
    String url = WS_URL + "?project=" + encodeForUrl(projectKey) + "&branch=" + encodeForUrl(branchName);
    try {
      return call(url);
    } catch (HttpException | IOException e) {
      throw new IllegalStateException("Failed to get the New Code definition: " + e.getMessage(), e);
    }
  }

  private NewCodePeriods.ShowWSResponse call(String url) throws IOException {
    GetRequest getRequest = new GetRequest(url);
    try (InputStream is = wsClient.call(getRequest).contentStream()) {
      return NewCodePeriods.ShowWSResponse.parseFrom(is);
    }
  }
}
