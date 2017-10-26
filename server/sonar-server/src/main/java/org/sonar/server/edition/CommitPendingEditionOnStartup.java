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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.license.LicenseCommit;

public class CommitPendingEditionOnStartup implements Startable {
  private static final Logger LOG = Loggers.get(CommitPendingEditionOnStartup.class);

  private final MutableEditionManagementState editionManagementState;
  @CheckForNull
  private final LicenseCommit licenseCommit;

  /**
   * Used by Pico when license-manager is not installed and therefore no implementation of {@link LicenseCommit} is
   * is available.
   */
  public CommitPendingEditionOnStartup(MutableEditionManagementState editionManagementState) {
    this(editionManagementState, null);
  }

  public CommitPendingEditionOnStartup(MutableEditionManagementState editionManagementState, @Nullable LicenseCommit licenseCommit) {
    this.editionManagementState = editionManagementState;
    this.licenseCommit = licenseCommit;
  }

  @Override
  public void start() {
    EditionManagementState.PendingStatus status = editionManagementState.getPendingInstallationStatus();
    switch (status) {
      case NONE:
        editionManagementState.clearInstallErrorMessage();
        return;
      case MANUAL_IN_PROGRESS:
      case AUTOMATIC_READY:
        finalizeInstall(status);
        break;
      case AUTOMATIC_IN_PROGRESS:
        editionManagementState.installFailed("SonarQube was restarted before asynchronous installation of edition completed");
        break;
      case UNINSTALL_IN_PROGRESS:
        failIfLicenseCommitIsPresent();
        editionManagementState.finalizeInstallation(null);
        break;
      default:
        throw new IllegalStateException("Unsupported status " + status);
    }
  }

  private void failIfLicenseCommitIsPresent() {
    if (licenseCommit != null) {
      throw new IllegalStateException("License Manager plugin is still present after uninstallation of the edition. Please remove it.");
    }
  }

  private void finalizeInstall(EditionManagementState.PendingStatus status) {
    // license manager is not installed, can't finalize
    if (licenseCommit == null) {
      LOG.info("No LicenseCommit instance is not available, can not finalize installation");
      return;
    }

    String newLicense = editionManagementState.getPendingLicense()
      .orElseThrow(() -> new IllegalStateException(String.format("When state is %s, a license should be available in staging", status)));
    try {
      licenseCommit.update(newLicense);
      editionManagementState.finalizeInstallation(null);
    } catch (IllegalArgumentException e) {
      String errorMessage = "Invalid staged license could not be commit on startup. Please input a new license.";
      LOG.warn(errorMessage, e);
      editionManagementState.finalizeInstallation(errorMessage);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
