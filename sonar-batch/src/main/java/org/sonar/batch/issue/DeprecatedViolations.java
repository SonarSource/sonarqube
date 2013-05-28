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
package org.sonar.batch.issue;

import com.google.common.collect.Lists;
import org.sonar.api.BatchComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.batch.index.ResourceCache;
import org.sonar.core.issue.DefaultIssue;

import java.util.Collection;
import java.util.List;

/**
 * Bridge with violations, that have been deprecated in 3.6.
 * @since 3.6
 */
public class DeprecatedViolations implements BatchComponent {

  private final IssueCache issueCache;
  private final RuleFinder ruleFinder;
  private final ResourceCache resourceCache;

  public DeprecatedViolations(IssueCache issueCache, RuleFinder ruleFinder, ResourceCache resourceCache) {
    this.issueCache = issueCache;
    this.ruleFinder = ruleFinder;
    this.resourceCache = resourceCache;
  }

  public List<Violation> get(String componentKey) {
    Collection<DefaultIssue> issues = issueCache.byComponent(componentKey);
    List<Violation> violations = Lists.newArrayList();
    for (DefaultIssue issue : issues) {
      violations.add(toViolation(issue));
    }
    return violations;
  }

  public Violation toViolation(DefaultIssue issue) {
    Rule rule = ruleFinder.findByKey(issue.ruleKey());
    Resource resource = resourceCache.get(issue.componentKey());
    Violation violation = new Violation(rule, resource);
    violation.setNew(issue.isNew());
    violation.setChecksum(issue.checksum());
    violation.setMessage(issue.message());
    violation.setCost(issue.effortToFix());
    violation.setLineId(issue.line());
    violation.setCreatedAt(issue.creationDate());
    violation.setManual(issue.reporter() != null);
    violation.setSeverity(RulePriority.valueOf(issue.severity()));
    violation.setSwitchedOff(Issue.RESOLUTION_FALSE_POSITIVE.equals(issue.resolution()));
    return violation;
  }
}
