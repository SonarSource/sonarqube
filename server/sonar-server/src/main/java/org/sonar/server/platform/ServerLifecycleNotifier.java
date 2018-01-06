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
package org.sonar.server.platform;

import org.picocontainer.Startable;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;
import org.sonar.api.platform.ServerStopHandler;

/**
 * @since 2.2
 */
public class ServerLifecycleNotifier implements Startable {

  private ServerStartHandler[] startHandlers;
  private ServerStopHandler[] stopHandlers;
  private Server server;

  public ServerLifecycleNotifier(Server server, ServerStartHandler[] startHandlers, ServerStopHandler[] stopHandlers) {
    this.startHandlers = startHandlers;
    this.stopHandlers = stopHandlers;
    this.server = server;
  }

  public ServerLifecycleNotifier(Server server, ServerStartHandler[] startHandlers) {
    this(server, startHandlers, new ServerStopHandler[0]);
  }

  public ServerLifecycleNotifier(Server server, ServerStopHandler[] stopHandlers) {
    this(server, new ServerStartHandler[0], stopHandlers);
  }

  public ServerLifecycleNotifier(Server server) {
    this(server, new ServerStartHandler[0], new ServerStopHandler[0]);
  }

  @Override
  public void start() {
    /* IMPORTANT :
     we want to be sure that handlers are notified when all other services are started.
     That's why the class Platform explicitely executes the method notifyStart(), instead of letting picocontainer
     choose the startup order.
     */
  }

  public void notifyStart() {
    Loggers.get(ServerLifecycleNotifier.class).debug("Notify " + ServerStartHandler.class.getSimpleName() + " handlers...");
    for (ServerStartHandler handler : startHandlers) {
      handler.onServerStart(server);
    }
  }

  @Override
  public void stop() {
    Loggers.get(ServerLifecycleNotifier.class).debug("Notify " + ServerStopHandler.class.getSimpleName() + " handlers...");
    for (ServerStopHandler handler : stopHandlers) {
      handler.onServerStop(server);
    }
  }
}
