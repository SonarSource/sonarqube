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
package org.sonar.server.rule.index;

import com.google.common.base.Preconditions;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;

import static java.util.Arrays.asList;

public class RuleQuery {

  private String key;
  private String queryText;
  private Collection<String> languages;
  private Collection<String> repositories;
  private Collection<String> severities;
  private Collection<RuleStatus> statuses;
  private Collection<String> tags;
  private Collection<RuleType> types;
  private Boolean activation;
  private QProfileDto profile;
  private QProfileDto compareToQProfile;
  private Collection<String> inheritance;
  private Collection<String> activeSeverities;
  private String templateKey;
  private Boolean isTemplate;
  private Long availableSince;
  private String sortField;
  private boolean ascendingSort = true;
  private String internalKey;
  private String ruleKey;
  private OrganizationDto organization;
  private boolean includeExternal;
  private Collection<String> owaspTop10;
  private Collection<String> sansTop25;
  private Collection<String> cwe;
  private Collection<String> sonarsourceSecurity;

  @CheckForNull
  public QProfileDto getQProfile() {
    return profile;
  }

  public RuleQuery setQProfile(@Nullable QProfileDto p) {
    this.profile = p;
    return this;
  }

  public RuleQuery setActivation(@Nullable Boolean activation) {
    this.activation = activation;
    return this;
  }

  @CheckForNull
  public Boolean getActivation() {
    return this.activation;
  }

  @CheckForNull
  public String getKey() {
    return key;
  }

  public RuleQuery setKey(@Nullable String key) {
    this.key = key;
    return this;
  }

  @CheckForNull
  public String getQueryText() {
    return queryText;
  }

  /**
   * Ignored if null or blank
   */
  public RuleQuery setQueryText(@Nullable String queryText) {
    this.queryText = queryText;
    return this;
  }

  @CheckForNull
  public Collection<String> getLanguages() {
    return languages;
  }

  public RuleQuery setLanguages(@Nullable Collection<String> languages) {
    this.languages = languages;
    return this;
  }

  @CheckForNull
  public Collection<String> getRepositories() {
    return repositories;
  }

  public RuleQuery setRepositories(@Nullable Collection<String> repositories) {
    this.repositories = repositories;
    return this;
  }

  @CheckForNull
  public Collection<String> getSeverities() {
    return severities;
  }

  public RuleQuery setSeverities(@Nullable Collection<String> severities) {
    if (severities != null) {
      for (String severity : severities) {
        Preconditions.checkArgument(Severity.ALL.contains(severity), "Unknown severity: " + severity);
      }
    }
    this.severities = severities;
    return this;
  }

  public RuleQuery setSeverities(@Nullable String... severities) {
    if (severities != null) {
      return setSeverities(asList(severities));
    }
    return this;
  }

  @CheckForNull
  public Collection<RuleStatus> getStatuses() {
    return statuses;
  }

  public RuleQuery setStatuses(@Nullable Collection<RuleStatus> statuses) {
    this.statuses = statuses;
    return this;
  }

  @CheckForNull
  public Collection<String> getTags() {
    return tags;
  }

  public RuleQuery setTags(@Nullable Collection<String> tags) {
    this.tags = tags;
    return this;
  }

  @CheckForNull
  public Collection<RuleType> getTypes() {
    return types;
  }

  public RuleQuery setTypes(@Nullable Collection<RuleType> types) {
    this.types = types;
    return this;
  }

  @CheckForNull
  public Collection<String> getInheritance() {
    return inheritance;
  }

  public RuleQuery setInheritance(@Nullable Collection<String> inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  @CheckForNull
  public Collection<String> getActiveSeverities() {
    return activeSeverities;
  }

  public RuleQuery setActiveSeverities(@Nullable Collection<String> severities) {
    if (severities != null) {
      for (String severity : severities) {
        Preconditions.checkArgument(Severity.ALL.contains(severity), "Unknown severity: " + severity);
      }
    }
    this.activeSeverities = severities;
    return this;
  }

  @CheckForNull
  public Boolean isTemplate() {
    return isTemplate;
  }

  public RuleQuery setIsTemplate(@Nullable Boolean b) {
    this.isTemplate = b;
    return this;
  }

  public boolean includeExternal() {
    return includeExternal;
  }

  public RuleQuery setIncludeExternal(boolean b) {
    this.includeExternal = b;
    return this;
  }

  @CheckForNull
  public String templateKey() {
    return templateKey;
  }

  public RuleQuery setTemplateKey(@Nullable String templateKey) {
    this.templateKey = templateKey;
    return this;
  }

  public String getSortField() {
    return this.sortField;
  }

  public RuleQuery setSortField(@Nullable String field) {
    if (field != null && !RuleIndexDefinition.SORT_FIELDS.contains(field)) {
      throw new IllegalStateException(String.format("Field '%s' is not sortable", field));
    }
    this.sortField = field;
    return this;
  }

  public boolean isAscendingSort() {
    return ascendingSort;
  }

  public RuleQuery setAscendingSort(boolean b) {
    this.ascendingSort = b;
    return this;
  }

  public RuleQuery setAvailableSince(@Nullable Long l) {
    this.availableSince = l;
    return this;
  }

  public Long getAvailableSinceLong() {
    return this.availableSince;
  }

  public RuleQuery setInternalKey(@Nullable String s) {
    this.internalKey = s;
    return this;
  }

  @CheckForNull
  public String getInternalKey() {
    return internalKey;
  }

  public RuleQuery setRuleKey(@Nullable String s) {
    this.ruleKey = s;
    return this;
  }

  @CheckForNull
  public String getRuleKey() {
    return ruleKey;
  }

  public OrganizationDto getOrganization() {
    return organization;
  }

  public RuleQuery setOrganization(OrganizationDto o) {
    this.organization = o;
    return this;
  }

  @CheckForNull
  public QProfileDto getCompareToQProfile() {
    return compareToQProfile;
  }

  public RuleQuery setCompareToQProfile(@Nullable QProfileDto compareToQProfile) {
    this.compareToQProfile = compareToQProfile;
    return this;
  }

  @CheckForNull
  public Collection<String> getCwe() {
    return cwe;
  }

  public RuleQuery setCwe(@Nullable Collection<String> cwe) {
    this.cwe = cwe;
    return this;
  }

  @CheckForNull
  public Collection<String> getOwaspTop10() {
    return owaspTop10;
  }

  public RuleQuery setOwaspTop10(@Nullable Collection<String> owaspTop10) {
    this.owaspTop10 = owaspTop10;
    return this;
  }

  @CheckForNull
  public Collection<String> getSansTop25() {
    return sansTop25;
  }

  public RuleQuery setSansTop25(@Nullable Collection<String> sansTop25) {
    this.sansTop25 = sansTop25;
    return this;
  }

  @CheckForNull
  public Collection<String> getSonarsourceSecurity() {
    return sonarsourceSecurity;
  }

  public RuleQuery setSonarsourceSecurity(@Nullable Collection<String> sonarsourceSecurity) {
    this.sonarsourceSecurity = sonarsourceSecurity;
    return this;
  }
}
