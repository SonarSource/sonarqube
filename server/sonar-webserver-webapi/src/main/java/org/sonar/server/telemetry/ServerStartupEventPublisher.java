/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.platform.Server;
import org.sonarsource.sonarqube.events.api.BaseEvent;
import org.sonarsource.sonarqube.events.api.EventAsyncClient;
import org.sonarsource.sonarqube.events.api.EventMetadata;
import org.sonarsource.sonarqube.events.api.EventSourceBuilder;

/**
 * Publishes a single cross-domain telemetry event when the Web process starts, providing a baseline
 * "the server started" signal for the unified event pipeline. The event type is prefixed with
 * {@code Analytics.} so that it is routed to the telemetry handler; when telemetry is disabled the
 * event is simply ignored downstream.
 *
 * <p>Registered as a {@link Startable} in {@code PlatformLevel4}, so {@link #start()} runs once,
 * after database migrations, on every Web process startup.
 */
public class ServerStartupEventPublisher implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(ServerStartupEventPublisher.class);

  static final String EVENT_TYPE = "Analytics.Server.Started";
  static final String EVENT_VERSION = "1.0";
  static final String SOURCE_DOMAIN = "Server";
  static final String SOURCE_SERVICE = "ServerStartupEventPublisher";

  private final EventAsyncClient eventAsyncClient;
  private final EventSourceBuilder eventSourceBuilder;
  private final Server server;

  public ServerStartupEventPublisher(EventAsyncClient eventAsyncClient, EventSourceBuilder eventSourceBuilder, Server server) {
    this.eventAsyncClient = eventAsyncClient;
    this.eventSourceBuilder = eventSourceBuilder;
    this.server = server;
  }

  @Override
  public void start() {
    try {
      eventAsyncClient.publishCrossDomainEvent(new BaseEvent<>(
        new EventMetadata(eventSourceBuilder.build(SOURCE_DOMAIN, SOURCE_SERVICE), EVENT_TYPE, EVENT_VERSION),
        Map.of(
          "serverId", server.getId(),
          "version", server.getVersion())));
    } catch (RuntimeException e) {
      // Publishing telemetry must never prevent the server from starting.
      LOG.debug("Failed to publish server startup event", e);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
