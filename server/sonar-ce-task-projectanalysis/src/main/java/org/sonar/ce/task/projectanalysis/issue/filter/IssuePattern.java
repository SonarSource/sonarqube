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
package org.sonar.ce.task.projectanalysis.issue.filter;

import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

public class IssuePattern {

  private WildcardPattern componentPattern;
  private WildcardPattern rulePattern;

  public IssuePattern(String componentPattern, String rulePattern) {
    this.componentPattern = WildcardPattern.create(componentPattern);
    this.rulePattern = WildcardPattern.create(rulePattern);
  }

  public WildcardPattern getComponentPattern() {
    return componentPattern;
  }

  public WildcardPattern getRulePattern() {
    return rulePattern;
  }

  boolean match(DefaultIssue issue, Component file) {
    return matchComponent(file.getName()) && matchRule(issue.ruleKey());
  }

  boolean matchRule(RuleKey rule) {
    return rulePattern.match(rule.toString());
  }

  boolean matchComponent(@Nullable String path) {
    return path != null && componentPattern.match(path);
  }

  @Override
  public String toString() {
    return "IssuePattern{" +
      "componentPattern=" + componentPattern +
      ", rulePattern=" + rulePattern +
      '}';
  }
}
