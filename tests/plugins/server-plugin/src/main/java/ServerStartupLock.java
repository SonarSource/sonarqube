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
import java.io.File;
import java.util.Optional;
import org.sonar.api.SonarRuntime;
import org.sonar.api.Startable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@ServerSide
@ComputeEngineSide
public class ServerStartupLock implements Startable {

  private static final Logger LOGGER = Loggers.get(ServerStartupLock.class);

  private final Configuration configuration;
  private final SonarRuntime runtime;

  public ServerStartupLock(Configuration configuration, SonarRuntime runtime) {
    this.configuration = configuration;
    this.runtime = runtime;
  }

  @Override
  public void start() {
    Optional<String> path = configuration.get(propertyKey());
    if (path.isPresent()) {
      File lock = new File(path.get());
      try {
        while (lock.exists()) {
          LOGGER.info("ServerStartupLock - Waiting for file to be deleted: " + lock.getAbsolutePath());
          Thread.sleep(100L);
        }
        LOGGER.info("ServerStartupLock - File deleted. Resuming startup.");
      } catch (InterruptedException e) {
        LOGGER.info("ServerStartupLock - interrupted");
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private String propertyKey() {
    switch (runtime.getSonarQubeSide()) {
      case SERVER:
        return "sonar.web.startupLock.path";
      case COMPUTE_ENGINE:
        return "sonar.ce.startupLock.path";
      default:
        throw new IllegalArgumentException("Unsupported runtime: " + runtime.getSonarQubeSide());
    }
  }
}
