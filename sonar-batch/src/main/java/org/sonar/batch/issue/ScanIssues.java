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
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.core.issue.DefaultIssue;

/**
 * Central component to manage issues
 */
public class ScanIssues implements BatchComponent {

  private final RulesProfile qProfile;
  private final IssueCache cache;
  private final Project project;

  public ScanIssues(RulesProfile qProfile, IssueCache cache, Project project) {
    this.qProfile = qProfile;
    this.cache = cache;
    this.project = project;
  }

  public boolean initAndAddIssue(DefaultIssue issue) {
    ActiveRule activeRule = qProfile.getActiveRule(issue.ruleKey().repository(), issue.ruleKey().rule());
    if (activeRule == null || activeRule.getRule() == null) {
      // rule does not exist or is not enabled -> ignore the issue
      return false;
    }
    issue.setCreationDate(project.getAnalysisDate());
    issue.setUpdateDate(project.getAnalysisDate());
    issue.setCloseDate(project.getAnalysisDate());
    if (issue.severity() == null) {
      issue.setSeverity(activeRule.getSeverity().name());
    }
    cache.put(issue);
    return true;
  }

}
