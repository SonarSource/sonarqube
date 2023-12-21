/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.common.gitlab.config;

import java.util.Set;
import org.sonar.server.common.NonNullUpdatedValue;
import org.sonar.server.common.UpdatedValue;

public record UpdateGitlabConfigurationRequest(
  String gitlabConfigurationId,
  NonNullUpdatedValue<Boolean> enabled,
  NonNullUpdatedValue<String> applicationId,
  NonNullUpdatedValue<String> url,
  NonNullUpdatedValue<String> secret,
  NonNullUpdatedValue<Boolean> synchronizeGroups,
  NonNullUpdatedValue<ProvisioningType> provisioningType,
  NonNullUpdatedValue<Boolean> allowUsersToSignUp,
  UpdatedValue<String> provisioningToken,
  NonNullUpdatedValue<Set<String>> provisioningGroups
) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String gitlabConfigurationId;
    private NonNullUpdatedValue<Boolean> enabled = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<String> applicationId = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<String> url = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<String> secret = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<Boolean> synchronizeGroups = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<ProvisioningType> provisioningType = NonNullUpdatedValue.undefined();
    private NonNullUpdatedValue<Boolean> allowUserToSignUp = NonNullUpdatedValue.undefined();
    private UpdatedValue<String> provisioningToken = UpdatedValue.undefined();
    private NonNullUpdatedValue<Set<String>> provisioningGroups = NonNullUpdatedValue.undefined();

    private Builder() {
    }

    public Builder gitlabConfigurationId(String gitlabConfigurationId) {
      this.gitlabConfigurationId = gitlabConfigurationId;
      return this;
    }

    public Builder enabled(NonNullUpdatedValue<Boolean> enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder applicationId(NonNullUpdatedValue<String> applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder url(NonNullUpdatedValue<String> url) {
      this.url = url;
      return this;
    }

    public Builder secret(NonNullUpdatedValue<String> secret) {
      this.secret = secret;
      return this;
    }

    public Builder synchronizeGroups(NonNullUpdatedValue<Boolean> synchronizeGroups) {
      this.synchronizeGroups = synchronizeGroups;
      return this;
    }

    public Builder provisioningType(NonNullUpdatedValue<ProvisioningType> provisioningType) {
      this.provisioningType = provisioningType;
      return this;
    }

    public Builder allowUserToSignUp(NonNullUpdatedValue<Boolean> allowUserToSignUp) {
      this.allowUserToSignUp = allowUserToSignUp;
      return this;
    }

    public Builder provisioningToken(UpdatedValue<String> provisioningToken) {
      this.provisioningToken = provisioningToken;
      return this;
    }

    public Builder provisioningGroups(NonNullUpdatedValue<Set<String>> provisioningGroups) {
      this.provisioningGroups = provisioningGroups;
      return this;
    }

    public UpdateGitlabConfigurationRequest build() {
      return new UpdateGitlabConfigurationRequest(gitlabConfigurationId, enabled, applicationId, url, secret, synchronizeGroups, provisioningType, allowUserToSignUp,
        provisioningToken, provisioningGroups);
    }
  }
}
