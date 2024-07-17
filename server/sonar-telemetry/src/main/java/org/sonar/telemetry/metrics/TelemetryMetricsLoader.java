/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.telemetry.metrics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.platform.Server;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.telemetry.TelemetryMetricsSentDto;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.metrics.schema.BaseMessage;
import org.sonar.telemetry.metrics.schema.Metric;
import org.sonar.telemetry.metrics.util.SentMetricsStorage;

public class TelemetryMetricsLoader {
  private final Server server;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final List<TelemetryDataProvider<?>> providers;

  public TelemetryMetricsLoader(Server server, DbClient dbClient, UuidFactory uuidFactory, List<TelemetryDataProvider<?>> providers) {
    this.server = server;
    this.dbClient = dbClient;
    this.providers = providers;
    this.uuidFactory = uuidFactory;
  }

  public Context loadData() {
    Context context = new Context();
    if (this.providers.isEmpty()) {
      return context;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      List<TelemetryMetricsSentDto> metricsSentDtos = dbClient.telemetryMetricsSentDao().selectAll(dbSession);
      SentMetricsStorage storage = new SentMetricsStorage(metricsSentDtos);

      Map<Dimension, Set<Metric>> telemetryDataMap = new LinkedHashMap<>();
      for (TelemetryDataProvider<?> provider : this.providers) {
        boolean shouldSendMetric = storage.shouldSendMetric(provider.getDimension(), provider.getMetricKey(), provider.getGranularity());
        if (shouldSendMetric) {
          Set<Metric> newMetrics = TelemetryMetricsMapper.mapFromDataProvider(provider);
          telemetryDataMap.computeIfAbsent(provider.getDimension(), k -> new LinkedHashSet<>()).addAll(newMetrics);

          Optional<TelemetryMetricsSentDto> dto = storage.getMetricsSentDto(provider.getDimension(), provider.getMetricKey());
          if (dto.isPresent()) {
            context.addDto(dto.get());
          } else {
            TelemetryMetricsSentDto newDto = new TelemetryMetricsSentDto(
              provider.getMetricKey(), provider.getDimension().getValue()
            );
            context.addDto(newDto);
          }
        }
      }

      Set<BaseMessage> baseMessages = retrieveBaseMessages(telemetryDataMap);
      context.setBaseMessages(baseMessages);
      return context;
    }
  }

  private Set<BaseMessage> retrieveBaseMessages(Map<Dimension, Set<Metric>> metrics) {
    return metrics.entrySet().stream()
      .map(entry -> new BaseMessage.Builder()
        .setMessageUuid(uuidFactory.create())
        .setInstallationId(server.getId())
        .setDimension(entry.getKey())
        .setMetrics(entry.getValue())
        .build())
      .collect(Collectors.toSet());
  }

  public static class Context {

    Set<BaseMessage> baseMessages;
    List<TelemetryMetricsSentDto> metricsSentDtos;

    public Context() {
      baseMessages = new LinkedHashSet<>();
      metricsSentDtos = new ArrayList<>();
    }

    protected void addDto(TelemetryMetricsSentDto dto) {
      this.metricsSentDtos.add(dto);
    }

    protected void setBaseMessages(Set<BaseMessage> baseMessages){
      this.baseMessages = baseMessages;
    }

    public Set<BaseMessage> getMessages() {
      return baseMessages;
    }

    public List<TelemetryMetricsSentDto> getMetricsToUpdate() {
      return metricsSentDtos;
    }
  }

}
