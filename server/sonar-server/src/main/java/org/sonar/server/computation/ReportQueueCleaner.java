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

package org.sonar.server.computation;

import org.picocontainer.Startable;
import org.sonar.api.ServerSide;
import org.sonar.api.platform.ServerUpgradeStatus;

/**
 * Clean-up queue of reports at server startup:
 * <ul>
 *   <li>remove all reports if server being upgraded to a new version (we assume that
 *   format of reports is not forward-compatible)</li>
 *   <li>reset reports that were in status WORKING while server stopped</li>
 * </ul>
 */
@ServerSide
public class ReportQueueCleaner implements Startable {

  private final ServerUpgradeStatus serverUpgradeStatus;
  private final ReportQueue queue;

  public ReportQueueCleaner(ServerUpgradeStatus serverUpgradeStatus, ReportQueue queue) {
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.queue = queue;
  }

  @Override
  public void start() {
    if (serverUpgradeStatus.isUpgraded()) {
      queue.clear();
    } else {
      queue.resetToPendingStatus();
    }
  }

  @Override
  public void stop() {
    // do nothing
  }
}
