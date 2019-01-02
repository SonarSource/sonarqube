/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.telemetry;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.property.InternalProperties;

import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_FREQUENCY_IN_SECONDS;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;
import static org.sonar.server.telemetry.TelemetryDataJsonWriter.writeTelemetryData;

@ServerSide
public class TelemetryDaemon implements Startable {
  private static final String THREAD_NAME_PREFIX = "sq-telemetry-service-";
  private static final int SEVEN_DAYS = 7 * 24 * 60 * 60 * 1_000;
  private static final String I_PROP_LAST_PING = "telemetry.lastPing";
  private static final String I_PROP_OPT_OUT = "telemetry.optOut";
  private static final Logger LOG = Loggers.get(TelemetryDaemon.class);

  private final TelemetryDataLoader dataLoader;
  private final TelemetryClient telemetryClient;
  private final Configuration config;
  private final InternalProperties internalProperties;
  private final System2 system2;

  private ScheduledExecutorService executorService;

  public TelemetryDaemon(TelemetryDataLoader dataLoader, TelemetryClient telemetryClient, Configuration config, InternalProperties internalProperties, System2 system2) {
    this.dataLoader = dataLoader;
    this.telemetryClient = telemetryClient;
    this.config = config;
    this.internalProperties = internalProperties;
    this.system2 = system2;
  }

  @Override
  public void start() {
    boolean isTelemetryActivated = config.getBoolean(SONAR_TELEMETRY_ENABLE.getKey())
      .orElseThrow(() -> new IllegalStateException(String.format("Setting '%s' must be provided.", SONAR_TELEMETRY_URL.getKey())));
    boolean hasOptOut = internalProperties.read(I_PROP_OPT_OUT).isPresent();
    if (!isTelemetryActivated && !hasOptOut) {
      optOut();
      internalProperties.write(I_PROP_OPT_OUT, String.valueOf(system2.now()));
      LOG.info("Sharing of SonarQube statistics is disabled.");
    }
    if (isTelemetryActivated && hasOptOut) {
      internalProperties.write(I_PROP_OPT_OUT, null);
    }
    if (!isTelemetryActivated) {
      return;
    }
    LOG.info("Sharing of SonarQube statistics is enabled.");
    executorService = Executors.newSingleThreadScheduledExecutor(newThreadFactory());
    int frequencyInSeconds = frequency();
    executorService.scheduleWithFixedDelay(telemetryCommand(), frequencyInSeconds, frequencyInSeconds, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    try {
      if (executorService == null) {
        return;
      }
      executorService.shutdown();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static ThreadFactory newThreadFactory() {
    return new ThreadFactoryBuilder()
      .setNameFormat(THREAD_NAME_PREFIX + "%d")
      .setPriority(Thread.MIN_PRIORITY)
      .build();
  }

  private Runnable telemetryCommand() {
    return () -> {
      try {
        long now = system2.now();
        if (shouldUploadStatistics(now)) {
          uploadStatistics();
          internalProperties.write(I_PROP_LAST_PING, String.valueOf(startOfDay(now)));
        }
      } catch (Exception e) {
        LOG.debug("Error while checking SonarQube statistics: {}", e.getMessage());
      }
      // do not check at start up to exclude test instance which are not up for a long time
    };
  }

  private void optOut() {
    StringWriter json = new StringWriter();
    try (JsonWriter writer = JsonWriter.of(json)) {
      writer.beginObject();
      writer.prop("id", dataLoader.loadServerId());
      writer.endObject();
    }
    telemetryClient.optOut(json.toString());
  }

  private void uploadStatistics() throws IOException {
    TelemetryData statistics = dataLoader.load();
    StringWriter jsonString = new StringWriter();
    try (JsonWriter json = JsonWriter.of(jsonString)) {
      writeTelemetryData(json, statistics);
    }
    telemetryClient.upload(jsonString.toString());
  }

  private boolean shouldUploadStatistics(long now) {
    Optional<Long> lastPing = internalProperties.read(I_PROP_LAST_PING).map(Long::valueOf);
    return !lastPing.isPresent() || now - lastPing.get() >= SEVEN_DAYS;
  }

  private static long startOfDay(long now) {
    return parseDate(formatDate(new Date(now))).getTime();
  }

  private int frequency() {
    return config.getInt(SONAR_TELEMETRY_FREQUENCY_IN_SECONDS.getKey())
      .orElseThrow(() -> new IllegalStateException(String.format("Setting '%s' must be provided.", SONAR_TELEMETRY_FREQUENCY_IN_SECONDS)));
  }
}
