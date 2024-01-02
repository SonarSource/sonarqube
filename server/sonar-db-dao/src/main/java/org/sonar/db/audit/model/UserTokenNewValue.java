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
import org.sonar.api.utils.DateUtils;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

public class UserTokenNewValue extends NewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String tokenUuid;

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String userUuid;

  @Nullable
  private String userLogin;

  @Nullable
  private String tokenName;

  @Nullable
  private Long lastConnectionDate;

  @Nullable
  private String projectKey;

  @Nullable
  private String type;

  public UserTokenNewValue(UserTokenDto userTokenDto, @Nullable String userLogin) {
    this.tokenUuid = userTokenDto.getUuid();
    this.tokenName = userTokenDto.getName();
    this.userUuid = userTokenDto.getUserUuid();
    this.lastConnectionDate = userTokenDto.getLastConnectionDate();
    this.projectKey = userTokenDto.getProjectKey();
    this.type = userTokenDto.getType();
    this.userLogin = userLogin;
  }

  public UserTokenNewValue(UserDto userDto) {
    this.userUuid = userDto.getUuid();
    this.userLogin = userDto.getLogin();
  }

  public UserTokenNewValue(UserDto userDto, String tokenName) {
    this(userDto);
    this.tokenName = tokenName;
  }

  public UserTokenNewValue(String projectKey) {
    this.projectKey = projectKey;
    this.type = TokenType.PROJECT_ANALYSIS_TOKEN.name();
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getTokenUuid() {
    return this.tokenUuid;
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  public String getUserUuid() {
    return this.userUuid;
  }

  @CheckForNull
  public String getUserLogin() {
    return this.userLogin;
  }

  @CheckForNull
  public String getTokenName() {
    return this.tokenName;
  }

  @CheckForNull
  public Long getLastConnectionDate() {
    return this.lastConnectionDate;
  }

  @CheckForNull
  public String getProjectKey() {
    return this.projectKey;
  }

  @CheckForNull
  public String getType() {
    return this.type;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"tokenUuid\": ", this.tokenUuid, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    addField(sb, "\"tokenName\": ", this.tokenName, true);
    addField(sb, "\"lastConnectionDate\": ", this.lastConnectionDate == null ? "" : DateUtils.formatDateTime(this.lastConnectionDate), false);
    addField(sb, "\"projectKey\": ", this.projectKey, true);
    addField(sb, "\"type\": ", this.type, true);
    endString(sb);
    return sb.toString();
  }
}
