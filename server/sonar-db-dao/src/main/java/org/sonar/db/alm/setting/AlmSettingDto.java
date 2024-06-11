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
package org.sonar.db.alm.setting;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.internal.Encryption;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AlmSettingDto {

  /**
   * Not empty. Max size is 40. Obviously it is unique.
   */
  private String uuid;

  /**
   * Non-empty and unique functional key. Max size is 40.
   */
  private String key;

  /**
   * Identifier of the ALM, like 'bitbucketcloud' or 'github', can't be null. Max size is 40.
   * Note that the db column is named alm_id.
   *
   * @see org.sonar.db.alm.setting.ALM for the list of available values
   */
  private String rawAlm;

  /**
   * URL of the ALM. Max size is 2000.
   * This column will only be fed when alm is GitHub or Bitbucket.
   * It will be null when the ALM is Azure DevOps.
   */
  private String url;

  /**
   * Application ID of the GitHub instance. Max size is 80.
   * This column will only be fed when alm is GitHub.
   * It will be null when the ALM is Azure DevOps or Bitbucket.
   */
  private String appId;
  /**
   * Application private key of the GitHub instance. Max size is 2000.
   * This column will only be fed when alm is GitHub.
   * It will be null when the ALM is Azure DevOps or Bitbucket.
   */
  private String privateKey;

  /**
   * Personal access token of the Azure DevOps / Bitbucket instance. Max size is 2000.
   * This column will only be fed when alm is Azure DevOps or Bitbucket.
   * It will be null when the ALM is GitHub.
   */
  private String personalAccessToken;

  /**
   * Application client Id of the GitHub instance. Max size is 40.
   * This column will only be fed when alm is GitHub.
   * It will be null when the ALM is Azure DevOps or Bitbucket.
   */
  private String clientId;

  /**
   * Application client secret of the GitHub instance. Max size is 80.
   * This column will only be fed when alm is GitHub.
   * It will be null when the ALM is Azure DevOps or Bitbucket.
   */
  private String clientSecret;

  /**
   * Webhook secret of the GitHub instance. Max size is 160.
   * This column will only be fed when alm is GitHub.
   * It will be null when the ALM is Azure DevOps or Bitbucket.
   */
  private String webhookSecret;

  private long updatedAt;
  private long createdAt;

  public String getUuid() {
    return uuid;
  }

  AlmSettingDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
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
  public String getDecryptedPrivateKey(Encryption encryption) {
    if (!isNullOrEmpty(privateKey) && encryption.isEncrypted(privateKey)) {
      return encryption.decrypt(privateKey);
    }
    return privateKey;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public AlmSettingDto setPrivateKey(@Nullable String privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  @CheckForNull
  public String getDecryptedPersonalAccessToken(Encryption encryption) {
    if (!isNullOrEmpty(personalAccessToken) && encryption.isEncrypted(personalAccessToken)) {
      return encryption.decrypt(personalAccessToken);
    }
    return personalAccessToken;
  }

  public AlmSettingDto setPersonalAccessToken(@Nullable String personalAccessToken) {
    this.personalAccessToken = personalAccessToken;
    return this;
  }

  @CheckForNull
  public String getClientId() {
    return clientId;
  }

  public AlmSettingDto setClientId(@Nullable String clientId) {
    this.clientId = clientId;
    return this;
  }

  @CheckForNull
  public String getDecryptedClientSecret(Encryption encryption) {
    if (!isNullOrEmpty(clientSecret) && encryption.isEncrypted(clientSecret)) {
      return encryption.decrypt(clientSecret);
    }
    return clientSecret;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public AlmSettingDto setClientSecret(@Nullable String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  @CheckForNull
  public String getDecryptedWebhookSecret(Encryption encryption) {
    if (!isNullOrEmpty(webhookSecret) && encryption.isEncrypted(webhookSecret)) {
      return encryption.decrypt(webhookSecret);
    }
    return webhookSecret;
  }

  public AlmSettingDto setWebhookSecret(@Nullable String webhookSecret) {
    this.webhookSecret = webhookSecret;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
}
