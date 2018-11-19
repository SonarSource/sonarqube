/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.qualityprofile;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.Preconditions.checkState;

public class ActiveRuleCountQuery {

  private final OrganizationDto organization;
  private final List<String> profileUuids;
  private final RuleStatus ruleStatus;
  private final String inheritance;

  public ActiveRuleCountQuery(Builder builder) {
    this.profileUuids = builder.profiles.stream().map(QProfileDto::getKee).collect(MoreCollectors.toList());
    this.ruleStatus = builder.ruleStatus;
    this.inheritance = builder.inheritance;
    this.organization = builder.organization;
  }

  public OrganizationDto getOrganization() {
    return organization;
  }

  public List<String> getProfileUuids() {
    return profileUuids;
  }

  /**
   * When no rule status is set, removed rules are not returned
   */
  @CheckForNull
  public RuleStatus getRuleStatus() {
    return ruleStatus;
  }

  @CheckForNull
  public String getInheritance() {
    return inheritance;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private OrganizationDto organization;
    private List<QProfileDto> profiles;
    private RuleStatus ruleStatus;
    private String inheritance;

    public Builder setOrganization(OrganizationDto organization) {
      this.organization = organization;
      return this;
    }

    public Builder setProfiles(List<QProfileDto> profiles) {
      this.profiles = profiles;
      return this;
    }

    public Builder setRuleStatus(@Nullable RuleStatus ruleStatus) {
      this.ruleStatus = ruleStatus;
      return this;
    }

    public Builder setInheritance(@Nullable String inheritance) {
      this.inheritance = inheritance;
      return this;
    }

    public ActiveRuleCountQuery build() {
      checkState(organization != null, "Organization cannot be null");
      checkState(profiles != null, "Profiles cannot be null");
      return new ActiveRuleCountQuery(this);
    }
  }

}
