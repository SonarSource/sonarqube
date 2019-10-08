/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.alm.setting;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class AlmSettingDto {

  private String uuid;
  private String key;
  private String rawAlm;
  private String url;
  private String appId;
  private String privateKey;
  private String personalAccessToken;
  private long updatedAt;
  private long createdAt;

  String getUuid() {
    return uuid;
  }

  void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getKey() {
    return key;
  }

  public AlmSettingDto setKey(String key) {
    this.key = key;
    return this;
  }

  public ALM getAlm() {
    return ALM.fromId(rawAlm);
  }

  public AlmSettingDto setAlm(ALM alm) {
    rawAlm = alm.getId();
    return this;
  }

  public String getRawAlm() {
    return rawAlm;
  }

  public AlmSettingDto setRawAlm(String rawAlm) {
    this.rawAlm = rawAlm;
    return this;
  }

  @CheckForNull
  public String getUrl() {
    return url;
  }

  public AlmSettingDto setUrl(@Nullable String url) {
    this.url = url;
    return this;
  }

  @CheckForNull
  public String getAppId() {
    return appId;
  }

  public AlmSettingDto setAppId(@Nullable String appId) {
    this.appId = appId;
    return this;
  }

  @CheckForNull
  public String getPrivateKey() {
    return privateKey;
  }

  public AlmSettingDto setPrivateKey(@Nullable String privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  @CheckForNull
  public String getPersonalAccessToken() {
    return personalAccessToken;
  }

  public AlmSettingDto setPersonalAccessToken(@Nullable String personalAccessToken) {
    this.personalAccessToken = personalAccessToken;
    return this;
  }

  long getUpdatedAt() {
    return updatedAt;
  }

  void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  long getCreatedAt() {
    return createdAt;
  }

  void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
}
