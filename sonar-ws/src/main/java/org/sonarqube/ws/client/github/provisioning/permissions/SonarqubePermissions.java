/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarqube.ws.client.github.provisioning.permissions;

public class SonarqubePermissions {
  private final boolean user;
  private final boolean codeViewer;
  private final boolean issueAdmin;
  private final boolean securityHotspotAdmin;

  private final boolean admin;

  private final boolean scan;

  public SonarqubePermissions(boolean user, boolean codeViewer, boolean issueAdmin, boolean securityHotspotAdmin, boolean admin, boolean scan) {
    this.user = user;
    this.codeViewer = codeViewer;
    this.issueAdmin = issueAdmin;
    this.securityHotspotAdmin = securityHotspotAdmin;
    this.admin = admin;
    this.scan = scan;
  }

  public boolean isUser() {
    return user;
  }

  public boolean isCodeViewer() {
    return codeViewer;
  }

  public boolean isIssueAdmin() {
    return issueAdmin;
  }

  public boolean isSecurityHotspotAdmin() {
    return securityHotspotAdmin;
  }

  public boolean isAdmin() {
    return admin;
  }

  public boolean isScan() {
    return scan;
  }
}
