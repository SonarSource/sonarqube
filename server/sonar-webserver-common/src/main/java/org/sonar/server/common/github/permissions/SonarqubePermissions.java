/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.common.github.permissions;

public record SonarqubePermissions(
  boolean user,
  boolean codeViewer,
  boolean issueAdmin,
  boolean securityHotspotAdmin,
  boolean admin,
  boolean scan) {


  public static final class Builder {
    private boolean user = false;
    private boolean codeViewer = false;
    private boolean issueAdmin = false;
    private boolean securityHotspotAdmin = false;
    private boolean admin = false;
    private boolean scan = false;

    private Builder() {
    }

    public static Builder builder() {
      return new Builder();
    }

    public Builder user(boolean user) {
      this.user = user;
      return this;
    }

    public Builder codeViewer(boolean codeViewer) {
      this.codeViewer = codeViewer;
      return this;
    }

    public Builder issueAdmin(boolean issueAdmin) {
      this.issueAdmin = issueAdmin;
      return this;
    }

    public Builder securityHotspotAdmin(boolean securityHotspotAdmin) {
      this.securityHotspotAdmin = securityHotspotAdmin;
      return this;
    }

    public Builder admin(boolean admin) {
      this.admin = admin;
      return this;
    }

    public Builder scan(boolean scan) {
      this.scan = scan;
      return this;
    }

    public SonarqubePermissions build() {
      return new SonarqubePermissions(user, codeViewer, issueAdmin, securityHotspotAdmin, admin, scan);
    }
  }
}
