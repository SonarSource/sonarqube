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
package org.sonar.ce.task.projectanalysis.issue;

import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

public class RuleTypeCopier extends IssueVisitor {

  private final RuleRepository ruleRepository;

  public RuleTypeCopier(RuleRepository ruleRepository) {
    this.ruleRepository = ruleRepository;
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    Rule rule = ruleRepository.getByKey(issue.ruleKey());
    if (issue.type() == null) {
      if (!rule.isExternal()) {
        // rule type should never be null for rules created by plugins (non-external rules)
        issue.setType(rule.getType());
      }
    }
    issue.setIsFromHotspot(rule.getType() == RuleType.SECURITY_HOTSPOT);
  }
}
