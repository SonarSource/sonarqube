/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.apache.commons.io.FileUtils;
import org.sonar.home.cache.FileHashes;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.io.IOException;

public class JdbcDriverDeployer {

  private final DefaultServerFileSystem fileSystem;

  public JdbcDriverDeployer(DefaultServerFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  public void start() {
    File driver = fileSystem.getJdbcDriver();
    File deployedDriver = new File(fileSystem.getDeployDir(), driver.getName());
    if (!deployedDriver.exists() || deployedDriver.length() != driver.length()) {
      try {
        FileUtils.copyFile(driver, deployedDriver);

      } catch (IOException e) {
        throw new IllegalStateException("Can not copy the JDBC driver from " + driver + " to " + deployedDriver, e);
      }
    }
    File deployedDriverIndex = fileSystem.getDeployedJdbcDriverIndex();
    try {
      String hash = new FileHashes().of(deployedDriver);
      FileUtils.writeStringToFile(deployedDriverIndex, deployedDriver.getName() + "|" + hash);
    } catch (IOException e) {
      throw new IllegalStateException("Can not generate index of JDBC driver", e);
    }
  }
}
