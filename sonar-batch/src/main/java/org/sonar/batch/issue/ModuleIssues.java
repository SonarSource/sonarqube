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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.sonar.api.BatchComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.OnIssueCreation;

import java.util.Collection;
import java.util.UUID;

public class ModuleIssues implements OnIssueCreation, BatchComponent {

  private final RulesProfile qProfile;
  private final IssueCache cache;
  private final Project project;

  public ModuleIssues(RulesProfile qProfile, IssueCache cache, Project project) {
    this.qProfile = qProfile;
    this.cache = cache;
    this.project = project;
  }

  public Collection<Issue> issues(String componentKey) {
    return cache.componentIssues(componentKey);
  }

  public ModuleIssues addOrUpdate(DefaultIssue issue) {
    Preconditions.checkState(!Strings.isNullOrEmpty(issue.key()), "Missing issue key");
    cache.addOrUpdate(issue);
    return this;
  }

  @Override
  public void onIssueCreation(DefaultIssue issue) {
    ActiveRule activeRule = qProfile.getActiveRule(issue.ruleKey().repository(), issue.ruleKey().rule());
    if (activeRule == null || activeRule.getRule() == null) {
      // rule does not exist or is not enabled -> ignore the issue
      return;
    }

    String key = UUID.randomUUID().toString();
    Preconditions.checkState(!Strings.isNullOrEmpty(key), "Fail to generate issue key");
    issue.setKey(key);
    issue.setCreatedAt(project.getAnalysisDate());
    issue.setStatus(Issue.STATUS_OPEN);
    if (issue.severity() == null) {
      issue.setSeverity(activeRule.getSeverity().name());
    }

    cache.addOrUpdate(issue);
  }
}
