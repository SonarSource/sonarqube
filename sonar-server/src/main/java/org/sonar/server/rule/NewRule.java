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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class NewRule {

  private boolean isTemplate;
  private RuleKey templateKey;
  private String name, htmlDescription, severity;
  private RuleStatus status;
  private List<NewRuleParam> params = newArrayList();

  public boolean isTemplate() {
    return isTemplate;
  }

  public void setIsTemplate(boolean template) {
    this.isTemplate = template;
  }

  @CheckForNull
  public RuleKey templateKey() {
    return templateKey;
  }

  public NewRule setTemplateKey(@Nullable RuleKey templateKey) {
    this.templateKey = templateKey;
    return this;
  }

  @CheckForNull
  public String name() {
    return name;
  }

  public NewRule setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String htmlDescription() {
    return htmlDescription;
  }

  public NewRule setHtmlDescription(@Nullable String htmlDescription) {
    this.htmlDescription = htmlDescription;
    return this;
  }

  @CheckForNull
  public String severity() {
    return severity;
  }

  public NewRule setSeverity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  @CheckForNull
  public RuleStatus status() {
    return status;
  }

  public NewRule setStatus(@Nullable RuleStatus status) {
    this.status = status;
    return this;
  }

  public List<NewRuleParam> params() {
    return params;
  }

  @CheckForNull
  public NewRuleParam param(final String paramKey) {
    return Iterables.find(params, new Predicate<NewRuleParam>() {
      @Override
      public boolean apply(@Nullable NewRuleParam input) {
        return input != null && input.key().equals(paramKey);
      }
    }, null);
  }

  public NewRule setParams(List<NewRuleParam> params) {
    this.params = params;
    return this;
  }
}
