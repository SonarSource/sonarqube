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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;

@ServerSide
public class TelemetryDaemon implements Startable {
  private static final String THREAD_NAME_PREFIX = "sq-telemetry-service-";
  private static final Logger LOG = Loggers.get(TelemetryDaemon.class);

  private final TelemetryClient telemetryClient;
  private final Server server;
  private final TelemetryFrequency frequencyInSeconds;

  private ScheduledExecutorService executorService;

  public TelemetryDaemon(TelemetryClient telemetryClient, Server server, Configuration config) {
    this.telemetryClient = telemetryClient;
    this.server = server;
    this.frequencyInSeconds = new TelemetryFrequency(config);
  }

  @Override
  public void start() {
    executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder()
        .setNameFormat(THREAD_NAME_PREFIX + "%d")
        .setPriority(Thread.MIN_PRIORITY)
        .build());
    executorService.scheduleWithFixedDelay(() -> {
      try {
        StringWriter json = new StringWriter();
        try (JsonWriter writer = JsonWriter.of(json)) {
          writer.beginObject();
          writer.prop("id", server.getId());
          writer.endObject();
        }
        telemetryClient.send(json.toString());
      } catch (Exception e) {
        // fail silently
      }
    // do not check at start up to exclude test instance which are not up for a long time
    }, frequencyInSeconds.get(), frequencyInSeconds.get(), TimeUnit.SECONDS);
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
}
