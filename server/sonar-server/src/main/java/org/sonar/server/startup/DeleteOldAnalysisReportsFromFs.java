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
package org.sonar.server.startup;

import java.io.File;
import org.picocontainer.Startable;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.platform.ServerFileSystem;

import static org.sonar.core.util.FileUtils.deleteQuietly;

/**
 * SONAR-7903 analysis reports are moved from file system to
 * database. This task cleans up the directory.
 */
public class DeleteOldAnalysisReportsFromFs implements Startable {

  private final ServerUpgradeStatus upgradeStatus;
  private final ServerFileSystem fs;

  public DeleteOldAnalysisReportsFromFs(ServerUpgradeStatus upgradeStatus, ServerFileSystem fs) {
    this.upgradeStatus = upgradeStatus;
    this.fs = fs;
  }

  @Override
  public void start() {
    if (upgradeStatus.isUpgraded()) {
      File dir = new File(fs.getDataDir(), "ce/reports");
      Loggers.get(getClass()).info("Delete unused directory of analysis reports: " + dir);
      deleteQuietly(dir);
    }
  }

  @Override
  public void stop() {
    // do nothing
  }
}
