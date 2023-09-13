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
package org.sonar.server.v2.api.github.permissions.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sonar.server.v2.common.model.UpdateField;

public class PermissionMappingUpdate {
  private UpdateField<Boolean> user = UpdateField.undefined();
  private UpdateField<Boolean> codeViewer = UpdateField.undefined();
  private UpdateField<Boolean> issueAdmin = UpdateField.undefined();
  private UpdateField<Boolean> securityHotspotAdmin = UpdateField.undefined();
  private UpdateField<Boolean> admin = UpdateField.undefined();
  private UpdateField<Boolean> scan = UpdateField.undefined();

  @Schema(implementation = Boolean.class)
  public UpdateField<Boolean> getUser() {
    return user;
  }

  public void setUser(Boolean user) {
    this.user = UpdateField.withValue(user);
  }

  @Schema(implementation = Boolean.class)
  public UpdateField<Boolean> getCodeViewer() {
    return codeViewer;
  }

  public void setCodeViewer(Boolean codeViewer) {
    this.codeViewer = UpdateField.withValue(codeViewer);
  }

  @Schema(implementation = Boolean.class)
  public UpdateField<Boolean> getIssueAdmin() {
    return issueAdmin;
  }

  public void setIssueAdmin(Boolean issueAdmin) {
    this.issueAdmin = UpdateField.withValue(issueAdmin);
  }

  @Schema(implementation = Boolean.class)
  public UpdateField<Boolean> getSecurityHotspotAdmin() {
    return securityHotspotAdmin;
  }

  public void setSecurityHotspotAdmin(Boolean securityHotspotAdmin) {
    this.securityHotspotAdmin = UpdateField.withValue(securityHotspotAdmin);
  }

  @Schema(implementation = Boolean.class)
  public UpdateField<Boolean> getAdmin() {
    return admin;
  }

  public void setAdmin(Boolean admin) {
    this.admin = UpdateField.withValue(admin);
  }

  @Schema(implementation = Boolean.class)
  public UpdateField<Boolean> getScan() {
    return scan;
  }

  public void setScan(Boolean scan) {
    this.scan = UpdateField.withValue(scan);
  }
}
