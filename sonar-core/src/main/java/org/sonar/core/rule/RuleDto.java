/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.rule;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.check.Cardinality;

import java.util.Date;

public final class RuleDto {
  private Integer id;
  private String repositoryKey;
  private String ruleKey;
  private String description;
  private String status;
  private String name;
  private String configKey;
  private Integer severity;
  private Cardinality cardinality;
  private String language;
  private Integer parentId;
  private Date createdAt;
  private Date updatedAt;
  private String noteData;
  private String noteUserLogin;
  private Date noteCreatedAt;
  private Date noteUpdatedAt;

  public Integer getId() {
    return id;
  }

  public RuleDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public RuleDto setRepositoryKey(String repositoryKey) {
    this.repositoryKey = repositoryKey;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public RuleDto setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public RuleDto setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public RuleDto setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getName() {
    return name;
  }

  public RuleDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getConfigKey() {
    return configKey;
  }

  public RuleDto setConfigKey(String configKey) {
    this.configKey = configKey;
    return this;
  }

  public Integer getSeverity() {
    return severity;
  }

  public RuleDto setSeverity(Integer severity) {
    this.severity = severity;
    return this;
  }

  public Cardinality getCardinality() {
    return cardinality;
  }

  public RuleDto setCardinality(Cardinality cardinality) {
    this.cardinality = cardinality;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public RuleDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  public Integer getParentId() {
    return parentId;
  }

  public RuleDto setParentId(Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public RuleDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public RuleDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getNoteData() {
    return noteData;
  }

  public RuleDto setNoteData(String noteData) {
    this.noteData = noteData;
    return this;
  }

  public String getNoteUserLogin() {
    return noteUserLogin;
  }

  public RuleDto setNoteUserLogin(String noteUserLogin) {
    this.noteUserLogin = noteUserLogin;
    return this;
  }

  public Date getNoteCreatedAt() {
    return noteCreatedAt;
  }

  public RuleDto setNoteCreatedAt(Date noteCreatedAt) {
    this.noteCreatedAt = noteCreatedAt;
    return this;
  }

  public Date getNoteUpdatedAt() {
    return noteUpdatedAt;
  }

  public RuleDto setNoteUpdatedAt(Date noteUpdatedAt) {
    this.noteUpdatedAt = noteUpdatedAt;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RuleDto)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RuleDto other = (RuleDto) obj;
    return new EqualsBuilder()
      .append(repositoryKey, other.getRepositoryKey())
      .append(ruleKey, other.getRuleKey())
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(repositoryKey)
      .append(ruleKey)
      .toHashCode();
  }

  @Override
  public String toString() {
    // Note that ReflectionToStringBuilder will not work here - see SONAR-3077
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("id", id)
      .append("name", name)
      .append("key", ruleKey)
      .append("configKey", configKey)
      .append("plugin", repositoryKey)
      .append("severity", getSeverity())
      .append("cardinality", cardinality)
      .append("status", status)
      .append("language", language)
      .append("parentId", parentId)
      .toString();
  }
}
