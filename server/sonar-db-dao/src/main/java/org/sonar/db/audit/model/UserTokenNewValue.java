/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

public class UserTokenNewValue extends NewValue {
  @Nullable
  private String tokenUuid;

  private String userUuid;

  @Nullable
  private String userLogin;

  @Nullable
  private String tokenName;

  @Nullable
  private Long lastConnectionDate;

  public UserTokenNewValue(UserTokenDto userTokenDto, @Nullable String userLogin) {
    this.tokenUuid = userTokenDto.getUuid();
    this.tokenName = userTokenDto.getName();
    this.userUuid = userTokenDto.getUserUuid();
    this.lastConnectionDate = userTokenDto.getLastConnectionDate();
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

  @CheckForNull
  public String getTokenUuid() {
    return this.tokenUuid;
  }

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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"tokenUuid\": ", this.tokenUuid, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    addField(sb, "\"tokenName\": ", this.tokenName, true);
    addField(sb, "\"lastConnectionDate\": ", this.lastConnectionDate == null ?
      "" : DateUtils.formatDateTime(this.lastConnectionDate), false);
    endString(sb);
    return sb.toString();
  }
}
