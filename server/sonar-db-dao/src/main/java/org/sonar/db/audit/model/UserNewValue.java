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

import org.apache.commons.lang.ObjectUtils;
import org.sonar.db.user.UserDto;

public class UserNewValue implements NewValue {
  private String userUuid;
  private String login;
  private String name;
  private String email;
  private Boolean isActive;
  private String scmAccounts;
  private String externalId;
  private String externalLogin;
  private String externalIdentityProvider;
  private Boolean local;
  private Boolean onboarded;
  private Boolean root;
  private Long lastConnectionDate;

  public UserNewValue(String userUuid, String userLogin) {
    this.userUuid = userUuid;
    this.login = userLogin;
  }

  public UserNewValue(UserDto userDto) {
    this.userUuid = userDto.getUuid();
    this.login = userDto.getLogin();
    this.name = userDto.getName();
    this.email = userDto.getEmail();
    this.isActive = userDto.isActive();
    this.scmAccounts = userDto.getScmAccounts();
    this.externalId = userDto.getExternalId();
    this.externalLogin = userDto.getExternalLogin();
    this.externalIdentityProvider = userDto.getExternalIdentityProvider();
    this.local = userDto.isLocal();
    this.onboarded = userDto.isOnboarded();
    this.root = userDto.isRoot();
    this.lastConnectionDate = userDto.getLastConnectionDate();
  }

  public String getUserUuid() {
    return this.userUuid;
  }

  public String getLogin() {
    return this.login;
  }

  public String getName() {
    return this.name;
  }

  public String getEmail() {
    return this.email;
  }

  public boolean isActive() {
    return this.isActive;
  }

  public String getScmAccounts() {
    return this.scmAccounts;
  }

  public String getExternalId() {
    return this.externalId;
  }

  public String getExternalLogin() {
    return this.externalLogin;
  }

  public String getExternalIdentityProvider() {
    return this.externalIdentityProvider;
  }

  public boolean isLocal() {
    return this.local;
  }

  public boolean isOnboarded() {
    return this.onboarded;
  }

  public boolean isRoot() {
    return this.root;
  }

  public Long getLastConnectionDate() {
    return this.lastConnectionDate;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "'userUuid':", this.userUuid, true);
    addField(sb, "'login':", this.login, true);
    addField(sb, "'name':", this.name, true);
    addField(sb, "'email':", this.email, true);
    addField(sb, "'isActive':", ObjectUtils.toString(this.isActive), false);
    addField(sb, "'scmAccounts':", this.scmAccounts, true);
    addField(sb, "'externalId':", this.externalId, true);
    addField(sb, "'externalLogin':", this.externalLogin, true);
    addField(sb, "'externalIdentityProvider':", this.externalIdentityProvider, true);
    addField(sb, "'local':", ObjectUtils.toString(this.local), false);
    addField(sb, "'onboarded':", ObjectUtils.toString(this.onboarded), false);
    addField(sb, "'root':", ObjectUtils.toString(this.root), false);
    addField(sb, "'lastConnectionDate':", ObjectUtils.toString(this.lastConnectionDate), false);
    sb.append("}");
    return sb.toString();
  }

}
