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
package org.sonar.server.v2.api.github.config.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sonar.server.v2.api.model.ProvisioningType;
import org.sonar.server.v2.common.model.UpdateField;

public class GithubConfigurationUpdateRestRequest {

  private UpdateField<Boolean> enabled = UpdateField.undefined();
  private UpdateField<String> clientId = UpdateField.undefined();
  private UpdateField<String> clientSecret = UpdateField.undefined();
  private UpdateField<String> applicationId = UpdateField.undefined();
  private UpdateField<String> privateKey = UpdateField.undefined();
  private UpdateField<Boolean> synchronizeGroups = UpdateField.undefined();
  private UpdateField<String> apiUrl = UpdateField.undefined();
  private UpdateField<String> webUrl = UpdateField.undefined();
  private UpdateField<List<String>> allowedOrganizations = UpdateField.undefined();
  private UpdateField<ProvisioningType> provisioningType = UpdateField.undefined();
  private UpdateField<Boolean> allowUsersToSignUp = UpdateField.undefined();
  private UpdateField<Boolean> projectVisibility = UpdateField.undefined();
  private UpdateField<Boolean> userConsentRequiredAfterUpgrade = UpdateField.undefined();

  @Schema(implementation = Boolean.class, description = "Enable GitHub authentication")
  public UpdateField<Boolean> getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = UpdateField.withValue(enabled);
  }

  @Schema(implementation = String.class, description = "GitHub Client ID")
  public UpdateField<String> getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = UpdateField.withValue(clientId);
  }

  @Schema(implementation = String.class, description = "GitHub Client secret")
  public UpdateField<String> getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = UpdateField.withValue(clientSecret);
  }

  @Schema(implementation = String.class, description = "GitHub Application id")
  public UpdateField<String> getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = UpdateField.withValue(applicationId);
  }

  @Schema(implementation = String.class, description = "GitHub Private key")
  public UpdateField<String> getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = UpdateField.withValue(privateKey);
  }

  @Schema(implementation = Boolean.class, description = "Set whether to synchronize groups")
  public UpdateField<Boolean> getSynchronizeGroups() {
    return synchronizeGroups;
  }

  public void setSynchronizeGroups(Boolean synchronizeGroups) {
    this.synchronizeGroups = UpdateField.withValue(synchronizeGroups);
  }

  @Schema(implementation = String.class, description = "Url of GitHub instance for API connectivity (for instance https://api.github.com)")
  public UpdateField<String> getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = UpdateField.withValue(apiUrl);
  }

  @Schema(implementation = String.class, description = "Url of GitHub instance for authentication (for instance https://github.com)")
  public UpdateField<String> getWebUrl() {
    return webUrl;
  }

  public void setWebUrl(String webUrl) {
    this.webUrl = UpdateField.withValue(webUrl);
  }

  @ArraySchema(arraySchema = @Schema(description = "GitHub organizations allowed to authenticate and provisioned"), schema = @Schema(implementation = String.class))
  public UpdateField<List<String>> getAllowedOrganizations() {
    return allowedOrganizations;
  }

  public void setAllowedOrganizations(List<String> allowedOrganizations) {
    this.allowedOrganizations = UpdateField.withValue(allowedOrganizations);
  }

  @Schema(implementation = ProvisioningType.class, description = "Type of synchronization")
  public UpdateField<ProvisioningType> getProvisioningType() {
    return provisioningType;
  }

  public void setProvisioningType(ProvisioningType provisioningType) {
    this.provisioningType = UpdateField.withValue(provisioningType);
  }

  @Schema(implementation = Boolean.class, description = "Allow user to sign up")
  public UpdateField<Boolean> getAllowUsersToSignUp() {
    return allowUsersToSignUp;
  }

  public void setAllowUsersToSignUp(Boolean allowUsersToSignUp) {
    this.allowUsersToSignUp = UpdateField.withValue(allowUsersToSignUp);
  }

  @Schema(implementation = Boolean.class, description = "Sync project visibility")
  public UpdateField<Boolean> getProjectVisibility() {
    return projectVisibility;
  }

  public void setProjectVisibility(Boolean projectVisibility) {
    this.projectVisibility = UpdateField.withValue(projectVisibility);
  }

  @Schema(implementation = Boolean.class, description = "Admin consent to synchronize permissions from GitHub")
  public UpdateField<Boolean> getUserConsentRequiredAfterUpgrade() {
    return userConsentRequiredAfterUpgrade;
  }

  public void setUserConsentRequiredAfterUpgrade(Boolean userConsentRequiredAfterUpgrade) {
    this.userConsentRequiredAfterUpgrade = UpdateField.withValue(userConsentRequiredAfterUpgrade);
  }
}
