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
package org.sonar.server.computation.issue;

import java.util.Set;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.computation.component.Component;

import static com.google.common.collect.Sets.union;

public class RuleTagsCopier extends IssueListener {

  private final RuleCache ruleCache;

  public RuleTagsCopier(RuleCache ruleCache) {
    this.ruleCache = ruleCache;
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.isNew()) {
      // analyzer can provide some tags. They must be merged with rule tags
      RuleDto rule = ruleCache.get(issue.ruleKey());
      Set<String> ruleTags = union(rule.getTags(), rule.getSystemTags());
      issue.setTags(union(issue.tags(), ruleTags));
    }
  }
}
