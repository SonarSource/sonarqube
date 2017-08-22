/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.property.InternalProperties;

import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.core.config.TelemetryProperties.PROP_ENABLE;
import static org.sonar.core.config.TelemetryProperties.PROP_FREQUENCY;
import static org.sonar.core.config.TelemetryProperties.PROP_URL;

@ServerSide
public class TelemetryDaemon implements Startable {
  private static final String THREAD_NAME_PREFIX = "sq-telemetry-service-";
  private static final int SEVEN_DAYS = 7 * 24 * 60 * 60 * 1_000;
  static final String I_PROP_LAST_PING = "telemetry.lastPing";
  static final String I_PROP_OPT_OUT = "telemetry.optOut";
  private static final Logger LOG = Loggers.get(TelemetryDaemon.class);

  private final TelemetryClient telemetryClient;
  private final Configuration config;
  private final InternalProperties internalProperties;
  private final Server server;
  private final System2 system2;

  private ScheduledExecutorService executorService;

  public TelemetryDaemon(TelemetryClient telemetryClient, Configuration config, InternalProperties internalProperties, Server server, System2 system2) {
    this.telemetryClient = telemetryClient;
    this.config = config;
    this.internalProperties = internalProperties;
    this.server = server;
    this.system2 = system2;
  }

  @Override
  public void start() {
    boolean isTelemetryActivated = config.getBoolean(PROP_ENABLE).orElseThrow(() -> new IllegalStateException(String.format("Setting '%s' must be provided.", PROP_URL)));
    if (!internalProperties.read(I_PROP_OPT_OUT).isPresent()) {
      if (!isTelemetryActivated) {
        StringWriter json = new StringWriter();
        try (JsonWriter writer = JsonWriter.of(json)) {
          writer.beginObject();
          writer.prop("id", server.getId());
          writer.endObject();
        }
        telemetryClient.optOut(json.toString());
        internalProperties.write(I_PROP_OPT_OUT, String.valueOf(system2.now()));
        LOG.info("Sharing of SonarQube statistics is disabled.");
      } else {
        internalProperties.write(I_PROP_OPT_OUT, null);
      }
    }

    if (!isTelemetryActivated) {
      return;
    }
    LOG.info("Sharing of SonarQube statistics is enabled.");
    executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder()
        .setNameFormat(THREAD_NAME_PREFIX + "%d")
        .setPriority(Thread.MIN_PRIORITY)
        .build());
    int frequencyInSeconds = frequency();
    executorService.scheduleWithFixedDelay(() -> {
      try {
        Optional<Long> lastPing = internalProperties.read(I_PROP_LAST_PING).map(Long::valueOf);
        long now = system2.now();
        if (lastPing.isPresent() && now - lastPing.get() < SEVEN_DAYS) {
          return;
        }

        StringWriter json = new StringWriter();
        try (JsonWriter writer = JsonWriter.of(json)) {
          writer.beginObject();
          writer.prop("id", server.getId());
          writer.endObject();
        }
        telemetryClient.send(json.toString());
        internalProperties.write(I_PROP_LAST_PING, String.valueOf(startOfDay(now)));
      } catch (Exception e) {
        LOG.debug("Error while checking SonarQube statistics: {}", e.getMessage());
      }
    // do not check at start up to exclude test instance which are not up for a long time
    }, frequencyInSeconds, frequencyInSeconds, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    try {
      executorService.shutdown();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static long startOfDay(long now) {
    return parseDate(formatDate(now)).getTime();
  }

  private int frequency() {
    return config.getInt(PROP_FREQUENCY).orElseThrow(() -> new IllegalStateException(String.format("Setting '%s' must be provided.", PROP_FREQUENCY)));
  }
}
