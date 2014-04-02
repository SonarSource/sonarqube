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
package org.sonar.server.rule;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.check.Cardinality;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class Rule {

  public static final String MANUAL_REPOSITORY_KEY = "manual";

  private int id;
  private RuleKey ruleKey;
  private String language;
  private String name;
  private String description;
  private String severity;
  private String status;
  private String cardinality;
  private Integer templateId;
  private RuleNote ruleNote;
  private Collection<String> systemTags;
  private Collection<String> adminTags;
  private Collection<RuleParam> params;
  private String debtCharacteristicKey;
  private String debtSubCharacteristicKey;
  private DebtRemediationFunction debtRemediationFunction;
  private Date createdAt;
  private Date updatedAt;

  private Rule(Builder builder) {
    this.id = builder.id;
    this.ruleKey = RuleKey.of(builder.repositoryKey, builder.key);
    this.language = builder.language;
    this.name = builder.name;
    this.description = builder.description;
    this.severity = builder.severity;
    this.status = builder.status;
    this.cardinality = builder.cardinality;
    this.templateId = builder.templateId;
    this.ruleNote = builder.ruleNote;
    this.systemTags = defaultCollection(builder.systemTags);
    this.adminTags = defaultCollection(builder.adminTags);
    this.params = defaultCollection(builder.params);
    this.debtCharacteristicKey = builder.debtCharacteristicKey;
    this.debtSubCharacteristicKey = builder.debtSubCharacteristicKey;
    this.debtRemediationFunction = builder.debtRemediationFunction;
    this.createdAt = builder.createdAt;
    this.updatedAt = builder.updatedAt;
  }

  public int id() {
    return id;
  }

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public String language() {
    return language;
  }

  @CheckForNull
  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  @CheckForNull
  public String severity() {
    return severity;
  }

  public String status() {
    return status;
  }

  public String cardinality() {
    return cardinality;
  }

  @CheckForNull
  public Integer templateId() {
    return templateId;
  }

  @CheckForNull
  public RuleNote ruleNote() {
    return ruleNote;
  }

  public Collection<String> systemTags() {
    return systemTags;
  }

  public Collection<String> adminTags() {
    return adminTags;
  }

  public Collection<RuleParam> params() {
    return params;
  }

  @CheckForNull
  public String debtCharacteristicKey() {
    return debtCharacteristicKey;
  }

  @CheckForNull
  public String debtSubCharacteristicKey() {
    return debtSubCharacteristicKey;
  }

  @CheckForNull
  public DebtRemediationFunction debtRemediationFunction() {
    return debtRemediationFunction;
  }

  public Date createdAt() {
    return createdAt;
  }

  @CheckForNull
  public Date updatedAt() {
    return updatedAt;
  }

  public boolean isTemplate() {
    return Cardinality.MULTIPLE.toString().equals(cardinality);
  }

  public boolean isEditable() {
    return templateId != null;
  }


  public static class Builder {

    private int id;
    private String key;
    private String repositoryKey;
    private String language;
    private String name;
    private String description;
    private String severity;
    private String status;
    private String cardinality;
    private Integer templateId;
    private RuleNote ruleNote;
    private Collection<String> systemTags;
    private Collection<String> adminTags;
    private Collection<RuleParam> params;
    private String debtCharacteristicKey;
    private String debtCharacteristicName;
    private String debtSubCharacteristicKey;
    private String debtSubCharacteristicName;
    private DebtRemediationFunction debtRemediationFunction;
    private Date createdAt;
    private Date updatedAt;

    public Builder setId(int id) {
      this.id = id;
      return this;
    }

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setRepositoryKey(String repositoryKey) {
      this.repositoryKey = repositoryKey;
      return this;
    }

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    public Builder setName(@Nullable String name) {
      this.name = name;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * Should only be null for manual rule
     */
    public Builder setSeverity(@Nullable String severity) {
      this.severity = severity;
      return this;
    }

    public Builder setStatus(String status) {
      this.status = status;
      return this;
    }

    public Builder setCardinality(String cardinality) {
      this.cardinality = cardinality;
      return this;
    }

    public Builder setTemplateId(@Nullable Integer templateId) {
      this.templateId = templateId;
      return this;
    }

    public Builder setRuleNote(@Nullable RuleNote ruleNote) {
      this.ruleNote = ruleNote;
      return this;
    }

    public Builder setSystemTags(Collection<String> systemTags) {
      this.systemTags = systemTags;
      return this;
    }

    public Builder setAdminTags(Collection<String> adminTags) {
      this.adminTags = adminTags;
      return this;
    }

    public Builder setParams(Collection<RuleParam> params) {
      this.params = params;
      return this;
    }

    public Builder setDebtCharacteristicKey(@Nullable String debtCharacteristicKey) {
      this.debtCharacteristicKey = debtCharacteristicKey;
      return this;
    }

    public Builder setDebtSubCharacteristicKey(@Nullable String debtSubCharacteristicKey) {
      this.debtSubCharacteristicKey = debtSubCharacteristicKey;
      return this;
    }

    public Builder setDebtRemediationFunction(@Nullable DebtRemediationFunction debtRemediationFunction) {
      this.debtRemediationFunction = debtRemediationFunction;
      return this;
    }

    public Builder setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder setUpdatedAt(@Nullable Date updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Rule build() {
      // TODO Add validation on mandatory key, repokey, ...
      return new Rule(this);
    }
  }

  private static <T> Collection<T> defaultCollection(@Nullable Collection<T> c) {
    return c == null ? Collections.<T>emptyList() : Collections.unmodifiableCollection(c);
  }
}
