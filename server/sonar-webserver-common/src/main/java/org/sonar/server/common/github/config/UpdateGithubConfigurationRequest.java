/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common.github.config;

import java.util.Set;
import org.sonar.server.common.NonNullUpdatedValue;
import org.sonar.server.common.gitlab.config.ProvisioningType;

public record UpdateGithubConfigurationRequest(
  String githubConfigurationId,
  NonNullUpdatedValue<Boolean> enabled,
  NonNullUpdatedValue<String> clientId,
  NonNullUpdatedValue<String> clientSecret,
  NonNullUpdatedValue<String> applicationId,
  NonNullUpdatedValue<String> privateKey,
  NonNullUpdatedValue<Boolean> synchronizeGroups,
  NonNullUpdatedValue<String> apiUrl,
  NonNullUpdatedValue<String> webUrl,
  NonNullUpdatedValue<Set<String>> allowedOrganizations,
  NonNullUpdatedValue<ProvisioningType> provisioningType,
  NonNullUpdatedValue<Boolean> allowUsersToSignUp,
  NonNullUpdatedValue<Boolean> projectVisibility,
  NonNullUpdatedValue<Boolean> userConsentRequiredAfterUpgrade
) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String githubConfigurationId;
    private NonNullUpdatedValue<Boolean> enabled = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<String> clientId = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<String> clientSecret = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<String> applicationId = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<String> privateKey = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<Boolean> synchronizeGroups = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<String> apiUrl = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<String> webUrl = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<Set<String>> allowedOrganizations = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<ProvisioningType> provisioningType = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<Boolean> allowUsersToSignUp = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<Boolean> projectVisibility = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<Boolean> userConsentRequiredAfterUpgrade = NonNullUpdatedValue.undefined();

    private Builder() {
    }

    public Builder githubConfigurationId(String githubConfigurationId) {
      this.githubConfigurationId = githubConfigurationId;
      return this;
    }

    public Builder enabled(NonNullUpdatedValue<Boolean> enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder clientId(NonNullUpdatedValue<String> clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder clientSecret(NonNullUpdatedValue<String> clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    public Builder applicationId(NonNullUpdatedValue<String> applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder privateKey(NonNullUpdatedValue<String> privateKey) {
      this.privateKey = privateKey;
      return this;
    }

    public Builder synchronizeGroups(NonNullUpdatedValue<Boolean> synchronizeGroups) {
      this.synchronizeGroups = synchronizeGroups;
      return this;
    }

    public Builder apiUrl(NonNullUpdatedValue<String> apiUrl) {
      this.apiUrl = apiUrl;
      return this;
    }

    public Builder webUrl(NonNullUpdatedValue<String> webUrl) {
      this.webUrl = webUrl;
      return this;
    }

    public Builder allowedOrganizations(NonNullUpdatedValue<Set<String>> allowedOrganizations) {
      this.allowedOrganizations = allowedOrganizations;
      return this;
    }

    public Builder provisioningType(NonNullUpdatedValue<ProvisioningType> provisioningType) {
      this.provisioningType = provisioningType;
      return this;
    }

    public Builder allowUsersToSignUp(NonNullUpdatedValue<Boolean> allowUsersToSignUp) {
      this.allowUsersToSignUp = allowUsersToSignUp;
      return this;
    }

    public Builder projectVisibility(NonNullUpdatedValue<Boolean> projectVisibility) {
      this.projectVisibility = projectVisibility;
      return this;
    }

    public Builder userConsentRequiredAfterUpgrade(NonNullUpdatedValue<Boolean> userConsentRequiredAfterUpgrade) {
      this.userConsentRequiredAfterUpgrade = userConsentRequiredAfterUpgrade;
      return this;
    }

    public UpdateGithubConfigurationRequest build() {
      return new UpdateGithubConfigurationRequest(githubConfigurationId, enabled, clientId, clientSecret, applicationId, privateKey, synchronizeGroups, apiUrl, webUrl,
        allowedOrganizations, provisioningType, allowUsersToSignUp, projectVisibility, userConsentRequiredAfterUpgrade);
    }
  }
}
