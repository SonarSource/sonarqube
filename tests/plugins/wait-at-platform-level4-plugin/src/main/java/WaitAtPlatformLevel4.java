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
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@ServerSide
public class WaitAtPlatformLevel4 implements Startable {

  private static final Logger LOGGER = Loggers.get(WaitAtPlatformLevel4.class);

  private final Configuration configuration;

  public WaitAtPlatformLevel4(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void start() {
    Optional<String> path = configuration.get("sonar.web.pause.path");
    path.ifPresent(WaitAtPlatformLevel4::waitForFileToBeDeleted);
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  private static void waitForFileToBeDeleted(String path) {
    LOGGER.info("PlatformLevel4 initialization phase is paused. Waiting for file to be deleted: " + path);
    File file = new File(path);
    try {
      while (file.exists()) {
        Thread.sleep(500L);
      }
      LOGGER.info("PlatformLevel4 initilization is resumed");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.info("PlatformLevel4 pause has been interrupted");
      throw new IllegalStateException("Platform4 pause has been interrupted");
    }
  }
}
