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
package org.sonar.scanner.issue.ignore.pattern;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WildcardPattern;

@Immutable
public class IssuePattern {

  private final WildcardPattern filePattern;
  private final WildcardPattern rulePattern;

  public IssuePattern(String filePattern, String rulePattern) {
    this.filePattern = WildcardPattern.create(filePattern);
    this.rulePattern = WildcardPattern.create(rulePattern);
  }

  public WildcardPattern getRulePattern() {
    return rulePattern;
  }

  public boolean matchRule(RuleKey rule) {
    return rulePattern.match(rule.toString());
  }

  public boolean matchFile(@Nullable String filePath) {
    return filePath != null && filePattern.match(filePath);
  }

}
