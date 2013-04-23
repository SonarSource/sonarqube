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
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;
import org.sonar.api.issue.IssueChanges;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.OnIssueCreation;
import org.sonar.core.issue.workflow.IssueWorkflow;

import java.util.Collection;
import java.util.UUID;

/**
 * Central component to manage issues
 */
public class ScanIssues implements IssueChanges, OnIssueCreation {

  private final RulesProfile qProfile;
  private final IssueCache cache;
  private final Project project;
  private final IssueWorkflow workflow;

  public ScanIssues(RulesProfile qProfile, IssueCache cache, Project project, IssueWorkflow workflow) {
    this.qProfile = qProfile;
    this.cache = cache;
    this.project = project;
    this.workflow = workflow;
  }

  @Override
  public Issue change(Issue issue, IssueChange change) {
    if (!change.hasChanges()) {
      return issue;
    }
    DefaultIssue reloaded = reload(issue);
    workflow.change(reloaded, change);
    cache.addOrUpdate(reloaded);
    return reloaded;
  }

  private DefaultIssue reload(Issue issue) {
    DefaultIssue reloaded = (DefaultIssue) cache.componentIssue(issue.componentKey(), issue.key());
    if (reloaded == null) {
      throw new IllegalStateException("Bad API usage. Unregistered issues can't be changed.");
    }
    return reloaded;
  }

  public Collection<Issue> issues(String componentKey) {
    return cache.componentIssues(componentKey);
  }

  public ScanIssues addOrUpdate(DefaultIssue issue) {
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
    issue.setResolution(Issue.RESOLUTION_OPEN);
    issue.setStatus(Issue.STATUS_OPEN);
    if (issue.severity() == null) {
      issue.setSeverity(activeRule.getSeverity().name());
    }
    cache.addOrUpdate(issue);
  }
}
