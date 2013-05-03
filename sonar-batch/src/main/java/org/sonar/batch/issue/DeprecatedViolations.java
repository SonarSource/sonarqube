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

import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Violation;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueBuilder;

import java.util.Collection;
import java.util.Date;

public class DeprecatedViolations implements BatchComponent {

  private final IssueCache cache;

  public DeprecatedViolations(IssueCache cache) {
    this.cache = cache;
  }

  public void add(Violation violation, Date creationDate) {
    DefaultIssue issue = toIssue(violation, creationDate);
    cache.put(issue);
  }

  public Collection<Violation> get(Resource resource) {
    throw new UnsupportedOperationException("TODO");
  }

  DefaultIssue toIssue(Violation violation, Date creationDate) {
    return (DefaultIssue) new DefaultIssueBuilder()
      .createdDate(creationDate)
      .componentKey(violation.getResource().getEffectiveKey())
      .ruleKey(RuleKey.of(violation.getRule().getRepositoryKey(), violation.getRule().getKey()))
      .effortToFix(violation.getCost())
      .line(violation.getLineId())
      .description(violation.getMessage())
      .severity(violation.getSeverity() != null ? violation.getSeverity().name() : Severity.MAJOR)
      .build();
  }
}
