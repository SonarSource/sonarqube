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
import org.sonar.db.qualitygate.QualityGateGroupPermissionsDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QProfileEditGroupsDto;
import org.sonar.db.user.GroupDto;

public class GroupEditorNewValue extends AbstractEditorNewValue {
  @Nullable
  private String groupUuid;
  @Nullable
  private String groupName;

  public GroupEditorNewValue(QualityGateGroupPermissionsDto qualityGateGroupPermissionsDto, String qualityGateName, String groupName) {
    this.qualityGateUuid = qualityGateGroupPermissionsDto.getQualityGateUuid();
    this.qualityGateName = qualityGateName;
    this.groupUuid = qualityGateGroupPermissionsDto.getGroupUuid();
    this.groupName = groupName;
  }

  public GroupEditorNewValue(QualityGateDto qualityGateDto, GroupDto groupDto) {
    this.qualityGateUuid = qualityGateDto.getUuid();
    this.qualityGateName = qualityGateDto.getName();
    this.groupUuid = groupDto.getUuid();
    this.groupName = groupDto.getName();
  }

  public GroupEditorNewValue(QualityGateDto qualityGateDto) {
    this.qualityGateUuid = qualityGateDto.getUuid();
    this.qualityGateName = qualityGateDto.getName();
  }

  public GroupEditorNewValue(GroupDto groupDto) {
    this.groupUuid = groupDto.getUuid();
    this.groupName = groupDto.getName();
  }

  public GroupEditorNewValue(QProfileEditGroupsDto qProfileEditGroupsDto, String qualityProfileName, String groupName) {
    this.qualityProfileUuid = qProfileEditGroupsDto.getQProfileUuid();
    this.qualityProfileName = qualityProfileName;
    this.groupUuid = qProfileEditGroupsDto.getGroupUuid();
    this.groupName = groupName;
  }

  public GroupEditorNewValue(QProfileDto qualityProfileDto, GroupDto groupDto) {
    this.qualityProfileUuid = qualityProfileDto.getKee();
    this.qualityProfileName = qualityProfileDto.getName();
    this.groupUuid = groupDto.getUuid();
    this.groupName = groupDto.getName();
  }

  public GroupEditorNewValue(QProfileDto qualityProfileDto) {
    this.qualityProfileUuid = qualityProfileDto.getKee();
    this.qualityProfileName = qualityProfileDto.getName();
  }

  @CheckForNull
  public String getGroupUuid() {
    return this.groupUuid;
  }

  @CheckForNull
  public String getGroupName() {
    return this.groupName;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"qualityGateUuid\": ", this.qualityGateUuid, true);
    addField(sb, "\"qualityGateName\": ", this.qualityGateName, true);
    addField(sb, "\"qualityProfileUuid\": ", this.qualityProfileUuid, true);
    addField(sb, "\"qualityProfileName\": ", this.qualityProfileName, true);
    addField(sb, "\"groupUuid\": ", this.groupUuid, true);
    addField(sb, "\"groupName\": ", this.groupName, true);
    endString(sb);
    return sb.toString();
  }
}
