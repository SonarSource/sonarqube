/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.ce.app;

import java.io.File;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.ProcessId;

import static org.sonar.server.app.ServerProcessLogging.STARTUP_LOGGER_NAME;

/**
 * Waits for the web server to be operational (started and datastores up-to-date)
 */
class WebServerBarrier implements StartupBarrier {
  private static final Logger LOG = Loggers.get(WebServerBarrier.class);
  private static final int POLL_DELAY = 200;
  // accounting only every 5 log calls so that only one every second (because delay is 200ms) is taken into account
  private static final int CALL_RATIO = 5;

  private final File sharedDir;

  public WebServerBarrier(File sharedDir) {
    this.sharedDir = sharedDir;
  }

  @Override
  public boolean waitForOperational() {
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(sharedDir, ProcessId.WEB_SERVER.getIpcIndex())) {
      if (processCommands.isOperational()) {
        return true;
      }

      Loggers.get(STARTUP_LOGGER_NAME).info("Compute Engine startup is on hold. Waiting for Web Server to be operational.");
      LOG.info("Waiting for Web Server to be operational...");
      Logger logarithmicLogger = LogarithmicLogger.from(LOG).applyingCallRatio(CALL_RATIO).build();
      while (!processCommands.isOperational()) {
        logarithmicLogger.info("Still waiting for WebServer...");
        try {
          Thread.sleep(POLL_DELAY);
        } catch (InterruptedException e) {
          return false;
        }
      }
      return true;
    }
  }
}
