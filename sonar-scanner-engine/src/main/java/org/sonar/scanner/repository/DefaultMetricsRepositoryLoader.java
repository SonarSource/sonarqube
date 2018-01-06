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
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonarqube.ws.client.GetRequest;

public class DefaultMetricsRepositoryLoader implements MetricsRepositoryLoader {

  private static final String METRICS_SEARCH_URL = "/api/metrics/search?f=name,description,direction,qualitative,custom&ps=500&p=";
  private ScannerWsClient wsClient;

  public DefaultMetricsRepositoryLoader(ScannerWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public MetricsRepository load() {
    List<Metric> metrics = new ArrayList<>();
    try {
      loadFromPaginatedWs(metrics);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load metrics", e);
    }
    return new MetricsRepository(metrics);
  }

  private void loadFromPaginatedWs(List<Metric> metrics) throws IOException {
    int page = 1;
    WsMetricsResponse response;
    do {
      GetRequest getRequest = new GetRequest(METRICS_SEARCH_URL + page);
      try (Reader reader = wsClient.call(getRequest).contentReader()) {
        response = GsonHelper.create().fromJson(reader, WsMetricsResponse.class);
        for (WsMetric metric : response.metrics) {
          metrics.add(new Metric.Builder(metric.getKey(), metric.getName(), ValueType.valueOf(metric.getType()))
            .create()
            .setDirection(metric.getDirection())
            .setQualitative(metric.isQualitative())
            .setUserManaged(metric.isCustom())
            .setDescription(metric.getDescription())
            .setId(metric.getId()));
        }
      }
      page++;
    } while (response.getP() < (response.getTotal() / response.getPs() + 1));
  }

  private static class WsMetric {
    private int id;
    private String key;
    private String type;
    private String name;
    private String description;
    private int direction;
    private boolean qualitative;
    private boolean custom;

    public int getId() {
      return id;
    }

    public String getKey() {
      return key;
    }

    public String getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public int getDirection() {
      return direction;
    }

    public boolean isQualitative() {
      return qualitative;
    }

    public boolean isCustom() {
      return custom;
    }
  }

  private static class WsMetricsResponse {

    private List<WsMetric> metrics = new ArrayList<>();

    private int total;

    private int p;

    private int ps;

    public int getTotal() {
      return total;
    }

    public int getP() {
      return p;
    }

    public int getPs() {
      return ps;
    }

  }
}
