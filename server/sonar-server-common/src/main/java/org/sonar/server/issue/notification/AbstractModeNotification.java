/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.issue.notification;

import org.sonar.api.notifications.Notification;

public abstract class AbstractModeNotification extends Notification {
  private final boolean isMQRModeEnabled;

  protected AbstractModeNotification(String type, boolean isMQRModeEnabled) {
    super(type);
    this.isMQRModeEnabled = isMQRModeEnabled;
  }

  public boolean isMQRModeEnabled() {
    return isMQRModeEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    if (!super.equals(o)) {
      return false;
    }

    AbstractModeNotification that = (AbstractModeNotification) o;
    return isMQRModeEnabled == that.isMQRModeEnabled;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Boolean.hashCode(isMQRModeEnabled);
    return result;
  }
}
