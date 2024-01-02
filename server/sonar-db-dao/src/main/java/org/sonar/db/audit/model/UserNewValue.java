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
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.user.UserDto;

import static java.util.Objects.requireNonNull;

public class UserNewValue extends NewValue {
  private String userUuid;
  private String userLogin;

  @Nullable
  private String name;

  @Nullable
  private String email;

  @Nullable
  private Boolean isActive;

  @Nullable
  private String scmAccounts;

  @Nullable
  private String externalId;

  @Nullable
  private String externalLogin;

  @Nullable
  private String externalIdentityProvider;

  @Nullable
  private Boolean local;

  @Nullable
  private Long lastConnectionDate;

  public UserNewValue(String userUuid, String userLogin) {
    this.userUuid = requireNonNull(userUuid);
    this.userLogin = requireNonNull(userLogin);
  }

  public UserNewValue(UserDto userDto) {
    this.userUuid = userDto.getUuid();
    this.userLogin = userDto.getLogin();
    this.name = userDto.getName();
    this.email = userDto.getEmail();
    this.isActive = userDto.isActive();
    this.scmAccounts = userDto.getScmAccounts();
    this.externalId = userDto.getExternalId();
    this.externalLogin = userDto.getExternalLogin();
    this.externalIdentityProvider = userDto.getExternalIdentityProvider();
    this.local = userDto.isLocal();
    this.lastConnectionDate = userDto.getLastConnectionDate();
  }

  public String getUserUuid() {
    return this.userUuid;
  }

  public String getUserLogin() {
    return this.userLogin;
  }

  @CheckForNull
  public String getName() {
    return this.name;
  }

  @CheckForNull
  public String getEmail() {
    return this.email;
  }

  @CheckForNull
  public Boolean isActive() {
    return this.isActive;
  }

  @CheckForNull
  public String getScmAccounts() {
    return this.scmAccounts;
  }

  @CheckForNull
  public String getExternalId() {
    return this.externalId;
  }

  @CheckForNull
  public String getExternalLogin() {
    return this.externalLogin;
  }

  @CheckForNull
  public String getExternalIdentityProvider() {
    return this.externalIdentityProvider;
  }

  @CheckForNull
  public Boolean isLocal() {
    return this.local;
  }

  @CheckForNull
  public Long getLastConnectionDate() {
    return this.lastConnectionDate;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    addField(sb, "\"name\": ", this.name, true);
    addField(sb, "\"email\": ", this.email, true);
    addField(sb, "\"isActive\": ", ObjectUtils.toString(this.isActive), false);
    addField(sb, "\"scmAccounts\": ", this.scmAccounts, true);
    addField(sb, "\"externalId\": ", this.externalId, true);
    addField(sb, "\"externalLogin\": ", this.externalLogin, true);
    addField(sb, "\"externalIdentityProvider\": ", this.externalIdentityProvider, true);
    addField(sb, "\"local\": ", ObjectUtils.toString(this.local), false);
    addField(sb, "\"lastConnectionDate\": ", this.lastConnectionDate == null ?
      "" : DateUtils.formatDateTime(this.lastConnectionDate), true);
    endString(sb);
    return sb.toString();
  }
}
