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
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Violation;
import org.sonar.core.issue.DefaultIssue;

import java.util.Collection;
import java.util.UUID;

public class DeprecatedViolations implements BatchComponent {

  private final IssueCache cache;

  public DeprecatedViolations(IssueCache cache) {
    this.cache = cache;
  }

  public void add(Violation violation) {
    DefaultIssue issue = toIssue(violation);
    cache.put(issue);
  }

  public Collection<Violation> get(Resource resource) {
    throw new UnsupportedOperationException("TODO");
  }

  DefaultIssue toIssue(Violation violation) {
    DefaultIssue issue = new DefaultIssue()
      .setComponentKey(violation.getResource().getEffectiveKey())
      .setKey(UUID.randomUUID().toString())
      .setRuleKey(RuleKey.of(violation.getRule().getRepositoryKey(), violation.getRule().getKey()))
      .setCost(violation.getCost())
      .setLine(violation.getLineId())
      .setDescription(violation.getMessage())
      .setResolution(Issue.RESOLUTION_OPEN)
      .setStatus(Issue.STATUS_OPEN)
      .setManualSeverity(false)
      .setManual(false)
      .setSeverity(violation.getSeverity() != null ? violation.getSeverity().name() : null);

    // FIXME
    //issue.setPerson(violation.getPersonId());
    return issue;
  }
}
