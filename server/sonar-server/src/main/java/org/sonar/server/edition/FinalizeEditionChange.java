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
package org.sonar.server.edition;

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.edition.EditionManagementState.PendingStatus;
import org.sonar.server.license.LicenseCommit;

public class FinalizeEditionChange implements Startable {
  private static final Logger LOG = Loggers.get(FinalizeEditionChange.class);

  private final MutableEditionManagementState editionManagementState;
  @CheckForNull
  private final LicenseCommit licenseCommit;

  /**
   * Used by Pico when license-manager is not installed and therefore no implementation of {@link LicenseCommit} is
   * is available.
   */
  public FinalizeEditionChange(MutableEditionManagementState editionManagementState) {
    this(editionManagementState, null);
  }

  public FinalizeEditionChange(MutableEditionManagementState editionManagementState, @Nullable LicenseCommit licenseCommit) {
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
        finalizeInstall();
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

  private void finalizeInstall() {
    String errorMessage = null;

    try {
      if (licenseCommit == null) {
        errorMessage = "Edition installation didn't complete. Some plugins were not installed.";
        LOG.warn(errorMessage);
        return;
      }

      Optional<String> newLicense = editionManagementState.getPendingLicense();
      if (!newLicense.isPresent()) {
        errorMessage = "Edition installation didn't complete. License was not found.";
        LOG.warn(errorMessage);
        return;
      }

      try {
        licenseCommit.update(newLicense.get());
      } catch (IllegalArgumentException e) {
        errorMessage = "Edition installation didn't complete. License is not valid. Please set a new license.";
        LOG.warn(errorMessage, e);
      }
    } finally {
      editionManagementState.finalizeInstallation(errorMessage);
    }
  }

  @Override
  public void stop() {
    EditionManagementState.PendingStatus status = editionManagementState.getPendingInstallationStatus();
    if (status == PendingStatus.UNINSTALL_IN_PROGRESS) {
      if (licenseCommit != null) {
        LOG.debug("Removing license");
        licenseCommit.delete();
      } else {
        LOG.warn("License Manager plugin not found - cannot remove the license");
      }
    }
  }
}
