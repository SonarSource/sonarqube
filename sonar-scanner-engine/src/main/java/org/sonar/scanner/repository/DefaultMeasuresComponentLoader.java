/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import static org.sonar.api.impl.utils.ScannerUtils.encodeForUrl;

import java.io.IOException;
import java.io.InputStream;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.ComponentWsResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

public class DefaultMeasuresComponentLoader implements MeasuresComponentLoader {

    private static final Logger LOG = Loggers.get(MeasuresComponentLoader.class);

    private static final String WS_URL = "/api/measures/component.protobuf";

    private final DefaultScannerWsClient wsClient;

    public DefaultMeasuresComponentLoader(DefaultScannerWsClient wsClient) {
        this.wsClient = wsClient;
    }

    @Override
    public ComponentWsResponse load(String componentKey, String branchName) {
        String url = WS_URL + "?additionalFields=period&metricKeys=projects&component=" + encodeForUrl(componentKey) + "&branch=" + encodeForUrl(branchName);
        try {
            return call(url);
        } catch (HttpException | IOException e) {
            LOG.debug("Failed to get component measures {}:{} due to {}", componentKey, branchName, e.getMessage());
            return null;
        }
    }

    private Measures.ComponentWsResponse call(String url) throws IOException {
        GetRequest getRequest = new GetRequest(url);
        try (WsResponse wsResponse = wsClient.call(getRequest)) {
            try (InputStream is = wsResponse.contentStream()) {
                return Measures.ComponentWsResponse.parseFrom(is);
            }
        }
    }
}
