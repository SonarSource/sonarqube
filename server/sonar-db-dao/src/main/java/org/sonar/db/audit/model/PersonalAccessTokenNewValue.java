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
package org.sonar.db.audit.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;

public class PersonalAccessTokenNewValue extends NewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String patUuid;

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String userUuid;

  @Nullable
  private String userLogin;

  @Nullable
  private String almSettingUuid;

  @Nullable
  private String almSettingKey;

  public PersonalAccessTokenNewValue(AlmPatDto almPatDto, @Nullable String userLogin, @Nullable String almSettingKey) {
    this.patUuid = almPatDto.getUuid();
    this.userUuid = almPatDto.getUserUuid();
    this.almSettingUuid = almPatDto.getAlmSettingUuid();
    this.userLogin = userLogin;
    this.almSettingKey = almSettingKey;
  }

  public PersonalAccessTokenNewValue(UserDto userDto) {
    this.userUuid = userDto.getUuid();
    this.userLogin = userDto.getLogin();
  }

  public PersonalAccessTokenNewValue(AlmSettingDto almSettingDto) {
    this.almSettingUuid = almSettingDto.getUuid();
    this.almSettingKey = almSettingDto.getKey();
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getPatUuid() {
    return this.patUuid;
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getUserUuid() {
    return this.userUuid;
  }

  @CheckForNull
  public String getUserLogin() {
    return this.userLogin;
  }

  @CheckForNull
  public String getAlmSettingUuid() {
    return this.almSettingUuid;
  }

  @CheckForNull
  public String getAlmSettingKey() {
    return this.almSettingKey;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"patUuid\": ", this.patUuid, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    addField(sb, "\"almSettingUuid\": ", this.almSettingUuid, true);
    addField(sb, "\"almSettingKey\": ", this.almSettingKey, true);
    endString(sb);
    return sb.toString();
  }
}
