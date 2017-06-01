/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.qualityprofile;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonarqube.ws.Common.Severity;

import static java.util.Objects.requireNonNull;

public class ActivateRuleWsRequest {
  private final Optional<String> params;
  private final String profileKey;
  private final Optional<Boolean> reset;
  private final String ruleKey;
  private final Optional<Severity> severity;
  private final Optional<String> organization;

  private ActivateRuleWsRequest(Builder builder) {
    organization = requireNonNull(builder.organization);
    params = requireNonNull(builder.params);
    profileKey = requireNonNull(builder.profileKey);
    reset = requireNonNull(builder.reset);
    ruleKey = requireNonNull(builder.ruleKey);
    severity = requireNonNull(builder.severity);
  }

  public static ActivateRuleWsRequest.Builder builder() {
    return new Builder();
  }

  public Optional<String> getParams() {
    return params;
  }

  public String getProfileKey() {
    return profileKey;
  }

  public Optional<Boolean> getReset() {
    return reset;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public Optional<Severity> getSeverity() {
    return severity;
  }

  public Optional<String> getOrganization() {
    return organization;
  }

  public static class Builder {
    private Optional<String> organization = Optional.empty();
    private Optional<String> params = Optional.empty();
    private String profileKey;
    private Optional<Boolean> reset = Optional.empty();
    private String ruleKey;
    private Optional<Severity> severity = Optional.empty();

    private Builder() {
    }

    public Builder setOrganization(@Nullable String organization) {
      this.organization = Optional.ofNullable(organization);
      return this;
    }

    public Builder setParams(@Nullable String params) {
      this.params = Optional.ofNullable(params);
      return this;
    }

    public Builder setProfileKey(String profileKey) {
      this.profileKey = profileKey;
      return this;
    }

    public Builder setReset(@Nullable Boolean reset) {
      this.reset = Optional.ofNullable(reset);
      return this;
    }

    public Builder setRuleKey(String ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public Builder setSeverity(@Nullable Severity severity) {
      this.severity = Optional.ofNullable(severity);
      return this;
    }

    public ActivateRuleWsRequest build() {
      return new ActivateRuleWsRequest(this);
    }
  }
}
