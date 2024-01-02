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
package org.sonar.db.audit.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualitygate.QualityGateUserPermissionsDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QProfileEditUsersDto;
import org.sonar.db.user.UserDto;

public class UserEditorNewValue extends AbstractEditorNewValue {
  @Nullable
  private String userUuid;
  @Nullable
  private String userLogin;

  public UserEditorNewValue(QualityGateUserPermissionsDto qualityGateUserPermissionsDto, String qualityGateName, String userLogin) {
    this.qualityGateUuid = qualityGateUserPermissionsDto.getQualityGateUuid();
    this.qualityGateName = qualityGateName;
    this.userUuid = qualityGateUserPermissionsDto.getUserUuid();
    this.userLogin = userLogin;
  }

  public UserEditorNewValue(QualityGateDto qualityGateDto, UserDto userDto) {
    this.qualityGateUuid = qualityGateDto.getUuid();
    this.qualityGateName = qualityGateDto.getName();
    this.userUuid = userDto.getUuid();
    this.userLogin = userDto.getLogin();
  }

  public UserEditorNewValue(QualityGateDto qualityGateDto) {
    this.qualityGateUuid = qualityGateDto.getUuid();
    this.qualityGateName = qualityGateDto.getName();
  }

  public UserEditorNewValue(UserDto userDto) {
    this.userUuid = userDto.getUuid();
    this.userLogin = userDto.getLogin();
  }

  public UserEditorNewValue(QProfileEditUsersDto qProfileEditUsersDto, String qualityProfileName, String userLogin) {
    this.qualityProfileUuid = qProfileEditUsersDto.getQProfileUuid();
    this.qualityProfileName = qualityProfileName;
    this.userUuid = qProfileEditUsersDto.getUserUuid();
    this.userLogin = userLogin;
  }

  public UserEditorNewValue(QProfileDto qProfileDto, UserDto userDto) {
    this.qualityProfileUuid = qProfileDto.getKee();
    this.qualityProfileName = qProfileDto.getName();
    this.userUuid = userDto.getUuid();
    this.userLogin = userDto.getLogin();
  }

  public UserEditorNewValue(QProfileDto qProfileDto) {
    this.qualityProfileUuid = qProfileDto.getKee();
    this.qualityProfileName = qProfileDto.getName();
  }

  @CheckForNull
  public String getUserUuid() {
    return this.userUuid;
  }

  @CheckForNull
  public String getUserLogin() {
    return this.userLogin;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"qualityGateUuid\": ", this.qualityGateUuid, true);
    addField(sb, "\"qualityGateName\": ", this.qualityGateName, true);
    addField(sb, "\"qualityProfileUuid\": ", this.qualityProfileUuid, true);
    addField(sb, "\"qualityProfileName\": ", this.qualityProfileName, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    endString(sb);
    return sb.toString();
  }
}
