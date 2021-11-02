/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

  public UserEditorNewValue(@Nullable QualityGateDto qualityGateDto, @Nullable UserDto userDto) {
    if (qualityGateDto != null) {
      this.qualityGateUuid = qualityGateDto.getUuid();
      this.qualityGateName = qualityGateDto.getName();
    }

    if (userDto != null) {
      this.userUuid = userDto.getUuid();
      this.userLogin = userDto.getLogin();
    }
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
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    endString(sb);
    return sb.toString();
  }
}
