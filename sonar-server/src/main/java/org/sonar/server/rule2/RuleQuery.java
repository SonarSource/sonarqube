/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule2;

import com.google.common.base.Preconditions;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class RuleQuery {

  private String key;
  private String queryText;
  private String[] languages;
  private String[] repositories;
  private String[] severities;
  private RuleStatus[] statuses;
  private String[] tags;
  private String[] debtCharacteristics;
  private Boolean hasDebtCharacteristic;

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

  public RuleQuery setQueryText(@Nullable String queryText) {
    this.queryText = queryText;
    return this;
  }

  @CheckForNull
  public String[] getLanguages() {
    return languages;
  }

  public RuleQuery setLanguages(@Nullable String[] languages) {
    this.languages = languages;
    return this;
  }

  @CheckForNull
  public String[] getRepositories() {
    return repositories;
  }

  public RuleQuery setRepositories(@Nullable String[] repositories) {
    this.repositories = repositories;
    return this;
  }

  @CheckForNull
  public String[] getSeverities() {
    return severities;
  }

  public RuleQuery setSeverities(@Nullable String[] severities) {
    if (severities != null) {
      for (String severity : severities) {
        Preconditions.checkArgument(Severity.ALL.contains(severity), "Unknown severity: " + severity);
      }
    }
    this.severities = severities;
    return this;
  }

  @CheckForNull
  public RuleStatus[] getStatuses() {
    return statuses;
  }

  public RuleQuery setStatuses(@Nullable RuleStatus[] statuses) {
    this.statuses = statuses;
    return this;
  }

  @CheckForNull
  public String[] getTags() {
    return tags;
  }

  public RuleQuery setTags(@Nullable String[] tags) {
    this.tags = tags;
    return this;
  }

  @CheckForNull
  public String[] getDebtCharacteristics() {
    return debtCharacteristics;
  }

  public RuleQuery setDebtCharacteristics(@Nullable String[] debtCharacteristics) {
    this.debtCharacteristics = debtCharacteristics;
    return this;
  }

  @CheckForNull
  public Boolean getHasDebtCharacteristic() {
    return hasDebtCharacteristic;
  }

  public RuleQuery setHasDebtCharacteristic(@Nullable Boolean hasDebtCharacteristic) {
    this.hasDebtCharacteristic = hasDebtCharacteristic;
    return this;
  }
}
