/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;
import org.sonar.api.platform.ServerStopHandler;

/**
 * @since 2.2
 */
public class ServerLifecycleNotifier implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(ServerLifecycleNotifier.class);
  private final ServerStartHandler[] startHandlers;
  private final ServerStopHandler[] stopHandlers;
  private final Server server;

  public ServerLifecycleNotifier(Server server, @Nullable ServerStartHandler[] startHandlers, @Nullable ServerStopHandler[] stopHandlers) {
    this.startHandlers = startHandlers != null ? startHandlers : new ServerStartHandler[0];
    this.stopHandlers = stopHandlers != null ? stopHandlers : new ServerStopHandler[0];
    this.server = server;
  }

  @Override
  public void start() {
    /*
     * IMPORTANT :
     * we want to be sure that handlers are notified when all other services are started.
     * That's why the class Platform explicitely executes the method notifyStart(), instead of letting the ioc container
     * choose the startup order.
     */
  }

  public void notifyStart() {
    LOG.debug("Notify {} handlers...", ServerStartHandler.class.getSimpleName());
    for (ServerStartHandler handler : startHandlers) {
      handler.onServerStart(server);
    }
  }

  @Override
  public void stop() {
    LOG.debug("Notify {} handlers...", ServerStopHandler.class.getSimpleName());
    for (ServerStopHandler handler : stopHandlers) {
      handler.onServerStop(server);
    }
  }
}
