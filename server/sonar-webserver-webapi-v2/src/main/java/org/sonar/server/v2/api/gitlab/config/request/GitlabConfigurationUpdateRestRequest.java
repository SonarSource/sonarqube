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
package org.sonar.server.v2.api.gitlab.config.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Size;
import org.sonar.server.v2.api.model.ProvisioningType;
import org.sonar.server.v2.common.model.UpdateField;

public class GitlabConfigurationUpdateRestRequest {

  private UpdateField<Boolean> enabled = UpdateField.undefined();
  private UpdateField<String> applicationId = UpdateField.undefined();
  private UpdateField<String> url = UpdateField.undefined();
  private UpdateField<String> secret = UpdateField.undefined();
  private UpdateField<Boolean> synchronizeGroups = UpdateField.undefined();
  private UpdateField<List<String>> allowedGroups = UpdateField.undefined();
  private UpdateField<ProvisioningType> provisioningType = UpdateField.undefined();
  private UpdateField<Boolean> allowUsersToSignUp = UpdateField.undefined();
  private UpdateField<String> provisioningToken = UpdateField.undefined();

  @Schema(implementation = Boolean.class, description = "Enable Gitlab authentication")
  public UpdateField<Boolean> getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = UpdateField.withValue(enabled);
  }

  @Schema(implementation = String.class, description = "Gitlab Application id")
  public UpdateField<String> getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = UpdateField.withValue(applicationId);
  }

  @Schema(implementation = String.class, description = "Url of Gitlab instance for authentication (for instance https://gitlab.com/api/v4)")
  public UpdateField<String> getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = UpdateField.withValue(url);
  }

  @Schema(implementation = String.class, description = "Secret of the application", nullable = true)
  public UpdateField<String> getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = UpdateField.withValue(secret);
  }

  @Schema(implementation = Boolean.class, description = "Set whether to synchronize groups")
  public UpdateField<Boolean> getSynchronizeGroups() {
    return synchronizeGroups;
  }

  public void setSynchronizeGroups(Boolean synchronizeGroups) {
    this.synchronizeGroups = UpdateField.withValue(synchronizeGroups);
  }

  @ArraySchema(arraySchema = @Schema(description = "Root Gitlab groups allowed to authenticate and provisioned"), schema = @Schema(implementation = String.class))
  public UpdateField<List<String>> getAllowedGroups() {
    return allowedGroups;
  }

  public void setAllowedGroups(List<String> allowedGroups) {
    this.allowedGroups = UpdateField.withValue(allowedGroups);
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

  @Size(min = 1)
  @Schema(implementation = String.class, description = "Gitlab token for provisioning", nullable = true)
  public UpdateField<String> getProvisioningToken() {
    return provisioningToken;
  }

  public void setProvisioningToken(String provisioningToken) {
    this.provisioningToken = UpdateField.withValue(provisioningToken);
  }
}
