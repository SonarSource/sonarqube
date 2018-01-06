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

import javax.annotation.Nullable;

/**
 * Provides access to operations which will alter the Edition management state.
 */
public interface MutableEditionManagementState extends EditionManagementState {
  /**
   * Stage the specified license and records that an automatic install of the plugins will be performed.
   *
   * @return the new {@link PendingStatus}, always {@link PendingStatus#AUTOMATIC_IN_PROGRESS}
   *         an exception.
   *
   * @throws IllegalStateException if current status is not {@link PendingStatus#NONE}
   */
  PendingStatus startAutomaticInstall(License license);

  /**
   * Stage the specified license and records that a manual install of the plugins will be performed.
   *
   * @return the new {@link PendingStatus}, always {@link PendingStatus#MANUAL_IN_PROGRESS}
   *
   * @throws IllegalStateException if current status is not {@link PendingStatus#NONE}
   */
  PendingStatus startManualInstall(License license);

  /**
   * Records that the specified edition has been installed without plugin changes.
   *
   * @param newEditionKey can't be {@code null} nor empty
   *
   * @return the new {@link PendingStatus}, always {@link PendingStatus#NONE}
   *
   * @throws IllegalStateException if current status is not {@link PendingStatus#NONE}
   */
  PendingStatus newEditionWithoutInstall(String newEditionKey);

  /**
   * Records that automatic install is ready to be finalized.
   *
   * @return the new pending status, always {@link PendingStatus#AUTOMATIC_READY}
   *
   * @throws IllegalStateException if current status is not {@link PendingStatus#AUTOMATIC_IN_PROGRESS}
   */
  PendingStatus automaticInstallReady();

  /**
   * Records that install failed with the specified (optional) error message to explain the cause of the
   * failure.
   *
   * @return the new pending status, always {@link PendingStatus#NONE}
   *
   * @throws IllegalStateException if current status is neither {@link PendingStatus#AUTOMATIC_IN_PROGRESS} nor
   *         {@link PendingStatus#MANUAL_IN_PROGRESS}
   */
  PendingStatus installFailed(@Nullable String errorMessage);

  /**
   * Clears the error message set by {@link #installFailed(String)} (String)} if there is any and which ever the current
   * status.
   */
  void clearInstallErrorMessage();

  /**
   * Uninstalls the currently installed edition
   *
   * @return the new pending status, always {@link PendingStatus#UNINSTALL_IN_PROGRESS}
   *
   * @throws IllegalStateException if current status is not {@link PendingStatus#NONE} or if there is
   * no edition currently installed.
   */
  PendingStatus uninstall();

  /**
   * Finalize an automatic or manual install with the specified (optional) error message to explain a partially
   * finalized install.
   *
   * @return the new pending status, always {@link PendingStatus#NONE}
   *
   * @throws IllegalStateException if current status is neither {@link PendingStatus#AUTOMATIC_READY} nor
   *         {@link PendingStatus#MANUAL_IN_PROGRESS}
   */
  PendingStatus finalizeInstallation(@Nullable String errorMessage);
}
