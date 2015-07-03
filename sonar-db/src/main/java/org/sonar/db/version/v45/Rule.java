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
package org.sonar.db.version.v45;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * SONAR-5575
 * <p/>
 * Used in the Active Record Migration 601.
 *
 * @since 4.5
 */
public final class Rule {

  private Integer id;
  private String repositoryKey;
  private String ruleKey;
  private boolean isTemplate;
  private Integer templateId;

  public Integer getId() {
    return id;
  }

  public Rule setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public Rule setRepositoryKey(String repositoryKey) {
    this.repositoryKey = repositoryKey;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public Rule setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public boolean isTemplate() {
    return isTemplate;
  }

  public Rule setIsTemplate(boolean isTemplate) {
    this.isTemplate = isTemplate;
    return this;
  }

  @CheckForNull
  public Integer getTemplateId() {
    return templateId;
  }

  public Rule setTemplateId(@Nullable Integer templateId) {
    this.templateId = templateId;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Rule)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    Rule other = (Rule) obj;
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

}
