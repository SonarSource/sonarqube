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
package org.sonar.telemetry;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.util.AbstractStoppableScheduledExecutorServiceImpl;
import org.sonar.server.util.GlobalLockManager;
import org.sonar.telemetry.legacy.TelemetryData;
import org.sonar.telemetry.legacy.TelemetryDataJsonWriter;
import org.sonar.telemetry.legacy.TelemetryDataLoader;
import org.sonar.telemetry.metrics.TelemetryMetricsLoader;
import org.sonar.telemetry.metrics.schema.BaseMessage;
import org.sonar.telemetry.metrics.util.MessageSerializer;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_FREQUENCY_IN_SECONDS;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;

@ServerSide
public class TelemetryDaemon extends AbstractStoppableScheduledExecutorServiceImpl<ScheduledExecutorService> {
  public static final String I_PROP_MESSAGE_SEQUENCE = "telemetry.messageSeq";

  private static final String THREAD_NAME_PREFIX = "sq-telemetry-service-";
  private static final int ONE_DAY = 24 * 60 * 60 * 1_000;
  private static final String I_PROP_LAST_PING = "telemetry.lastPing";
  private static final String I_PROP_OPT_OUT = "telemetry.optOut";
  private static final String LOCK_NAME = "TelemetryStat";
  private static final Logger LOG = LoggerFactory.getLogger(TelemetryDaemon.class);
  private static final String LOCK_DELAY_SEC = "sonar.telemetry.lock.delay";

  private final TelemetryDataLoader dataLoader;
  private final TelemetryDataJsonWriter dataJsonWriter;
  private final TelemetryClient telemetryClient;
  private final GlobalLockManager lockManager;
  private final Configuration config;
  private final InternalProperties internalProperties;
  private final System2 system2;
  private final TelemetryMetricsLoader telemetryMetricsLoader;
  private final DbClient dbClient;

  public TelemetryDaemon(TelemetryDataLoader dataLoader, TelemetryDataJsonWriter dataJsonWriter, TelemetryClient telemetryClient, Configuration config,
    InternalProperties internalProperties, GlobalLockManager lockManager, System2 system2, TelemetryMetricsLoader telemetryMetricsLoader, DbClient dbClient) {
    super(Executors.newSingleThreadScheduledExecutor(newThreadFactory()));
    this.dataLoader = dataLoader;
    this.dataJsonWriter = dataJsonWriter;
    this.telemetryClient = telemetryClient;
    this.config = config;
    this.internalProperties = internalProperties;
    this.lockManager = lockManager;
    this.system2 = system2;
    this.telemetryMetricsLoader = telemetryMetricsLoader;
    this.dbClient = dbClient;
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
    int frequencyInSeconds = frequency();
    scheduleWithFixedDelay(telemetryCommand(), frequencyInSeconds, frequencyInSeconds, TimeUnit.SECONDS);
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

        if (!lockManager.tryLock(LOCK_NAME, lockDuration())) {
          return;
        }

        long now = system2.now();
        if (shouldUploadStatistics(now)) {
          uploadMetrics();
          uploadLegacyTelemetry();

          updateTelemetryProps(now);
        }
      } catch (Exception e) {
        LOG.debug("Error while checking SonarQube statistics: {}", e.getMessage(), e);
      }
      // do not check at start up to exclude test instance which are not up for a long time
    };
  }

  private void updateTelemetryProps(long now) {
    internalProperties.write(I_PROP_LAST_PING, String.valueOf(now));

    Optional<String> currentSequence = internalProperties.read(I_PROP_MESSAGE_SEQUENCE);
    if (currentSequence.isEmpty()) {
      internalProperties.write(I_PROP_MESSAGE_SEQUENCE, String.valueOf(1));
      return;
    }

    long current = Long.parseLong(currentSequence.get());
    internalProperties.write(I_PROP_MESSAGE_SEQUENCE, String.valueOf(current + 1));
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

  private void uploadMetrics() throws IOException {
    TelemetryMetricsLoader.Context context = telemetryMetricsLoader.loadData();
    for (BaseMessage message : context.getMessages()) {
      String jsonString = MessageSerializer.serialize(message);
      telemetryClient.upload(jsonString);
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      context.getMetricsToUpdate().forEach(toUpdate -> dbClient.telemetryMetricsSentDao().upsert(dbSession, toUpdate));
      dbSession.commit();
    }
  }

  private void uploadLegacyTelemetry() throws IOException {
    TelemetryData statistics = dataLoader.load();
    StringWriter jsonString = new StringWriter();
    try (JsonWriter json = JsonWriter.of(jsonString)) {
      dataJsonWriter.writeTelemetryData(json, statistics);
    }
    telemetryClient.upload(jsonString.toString());
    dataLoader.reset();
  }

  private boolean shouldUploadStatistics(long now) {
    Optional<Long> lastPing = internalProperties.read(I_PROP_LAST_PING).map(Long::valueOf);
    return lastPing.isEmpty() || now - lastPing.get() >= ONE_DAY;
  }

  private int frequency() {
    return config.getInt(SONAR_TELEMETRY_FREQUENCY_IN_SECONDS.getKey())
      .orElseThrow(() -> new IllegalStateException(String.format("Setting '%s' must be provided.", SONAR_TELEMETRY_FREQUENCY_IN_SECONDS)));
  }

  private int lockDuration() {
    return config.getInt(LOCK_DELAY_SEC).orElse(60);
  }
}
