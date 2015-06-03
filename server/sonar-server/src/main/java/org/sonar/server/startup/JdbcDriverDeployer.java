/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.startup;

import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.sonar.api.config.Settings;
import org.sonar.home.cache.FileHashes;
import org.sonar.process.ProcessProperties;
import org.sonar.server.platform.DefaultServerFileSystem;

public class JdbcDriverDeployer {

  private final DefaultServerFileSystem fileSystem;
  private final Settings settings;

  public JdbcDriverDeployer(DefaultServerFileSystem fileSystem, Settings settings) {
    this.fileSystem = fileSystem;
    this.settings = settings;
  }

  public void start() {
    // see initialization of this property in sonar-application
    String driverPath = settings.getString(ProcessProperties.JDBC_DRIVER_PATH);
    if (driverPath == null) {
      // Medium tests
      return;
    }
    File driver = new File(driverPath);
    File deployedDriver = new File(fileSystem.getDeployDir(), driver.getName());
    if (!deployedDriver.exists() || FileUtils.sizeOf(deployedDriver) != FileUtils.sizeOf(driver)) {
      try {
        FileUtils.copyFile(driver, deployedDriver);
      } catch (IOException e) {
        throw new IllegalStateException(
          String.format("Can not copy the JDBC driver from %s to %s", driver, deployedDriver), e);
      }
    }

    File deployedDriverIndex = fileSystem.getDeployedJdbcDriverIndex();
    try {
      FileUtils.writeStringToFile(deployedDriverIndex, driverIndexContent(deployedDriver));
    } catch (IOException e) {
      throw new IllegalStateException("Can not generate index of JDBC driver", e);
    }
  }

  private static String driverIndexContent(@Nullable File deployedDriver) {
    if (deployedDriver != null) {
      String hash = new FileHashes().of(deployedDriver);
      return deployedDriver.getName() + "|" + hash;
    }
    return "";
  }

}
