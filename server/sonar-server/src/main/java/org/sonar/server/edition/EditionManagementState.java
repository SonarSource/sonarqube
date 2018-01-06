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

public interface EditionManagementState {
  /**
   * @return {@link Optional#empty() empty} if there is no edition installed
   */
  Optional<String> getCurrentEditionKey();

  /**
   * @return the pending installation status.
   */
  PendingStatus getPendingInstallationStatus();

  /**
   * @return {@link Optional#empty() empty} when {@link #getPendingInstallationStatus()} returns {@link PendingStatus#NONE},
   *         otherwise a {@link String}
   */
  Optional<String> getPendingEditionKey();

  /**
   * The license string.
   *
   * @return {@link Optional#empty() empty} when {@link #getPendingInstallationStatus()} returns {@link PendingStatus#NONE},
   *         otherwise a {@link String}
   */
  Optional<String> getPendingLicense();

  /**
   * The message explaining the error that made the install fail (if any).
   *
   * @return a {@link String} if {@link #getPendingInstallationStatus()} returns {@link PendingStatus#NONE} and an error
   *         occurred during install, otherwise {@link Optional#empty() empty}
   */
  Optional<String> getInstallErrorMessage();

  enum PendingStatus {
    NONE,
    AUTOMATIC_IN_PROGRESS,
    AUTOMATIC_READY,
    MANUAL_IN_PROGRESS,
    UNINSTALL_IN_PROGRESS
  }
}
