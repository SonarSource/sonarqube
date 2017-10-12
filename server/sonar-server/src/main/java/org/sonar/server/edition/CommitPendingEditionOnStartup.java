/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.edition;

import org.sonar.api.Startable;

public class CommitPendingEditionOnStartup implements Startable {
  private final MutableEditionManagementState editionManagementState;

  public CommitPendingEditionOnStartup(MutableEditionManagementState editionManagementState) {
    this.editionManagementState = editionManagementState;
  }

  @Override
  public void start() {
    EditionManagementState.PendingStatus status = editionManagementState.getPendingInstallationStatus();
    switch (status) {
      case NONE:
        return;
      case MANUAL_IN_PROGRESS:
      case AUTOMATIC_READY:
        // TODO save new license with plugin manager
        editionManagementState.finalizeInstallation();
        break;
      case AUTOMATIC_IN_PROGRESS:
        // FIXME temporary hack until download of edition is implemented
        editionManagementState.automaticInstallReady();
        editionManagementState.finalizeInstallation();
        break;
      default:
        throw new IllegalStateException("Unsupported status " + status);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
