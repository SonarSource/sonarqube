/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.permission;

public enum OrganizationPermission {

  ADMINISTER("admin"),
  ADMINISTER_QUALITY_GATES("gateadmin"),
  ADMINISTER_QUALITY_PROFILES("profileadmin"),
  PROVISION_PROJECTS("provisioning"),
  SCAN("scan"),

  /**
   * @since 7.4
   */
  APPLICATION_CREATOR("applicationcreator"),
  PORTFOLIO_CREATOR("portfoliocreator");

  private final String key;

  OrganizationPermission(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  @Override
  public String toString() {
    return key;
  }

  public static OrganizationPermission fromKey(String key) {
    for (OrganizationPermission p : values()) {
      if (p.getKey().equals(key)) {
        return p;
      }
    }
    throw new IllegalArgumentException("Unsupported permission: " + key);
  }
}
