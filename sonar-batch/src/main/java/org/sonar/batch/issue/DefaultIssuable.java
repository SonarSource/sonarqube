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
package org.sonar.batch.issue;

import com.google.common.collect.Lists;
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Project;
import org.sonar.core.issue.DefaultIssueBuilder;

import java.util.List;

/**
 * @since 3.6
 */
public class DefaultIssuable implements Issuable {

  private final ModuleIssues moduleIssues;
  private final IssueCache cache;
  private final Component component;
  private final Project project;

  DefaultIssuable(Component component, Project project, ModuleIssues moduleIssues, IssueCache cache) {
    this.component = component;
    this.project = project;
    this.moduleIssues = moduleIssues;
    this.cache = cache;
  }

  @Override
  public IssueBuilder newIssueBuilder() {
    return new DefaultIssueBuilder().componentKey(component.key()).projectKey(project.getKey());
  }

  @Override
  public boolean addIssue(Issue issue) {
    return moduleIssues.initAndAddIssue((DefaultIssue) issue);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Issue> resolvedIssues() {
    List<Issue> result = Lists.newArrayList();
    for (DefaultIssue issue : cache.byComponent(component.key())) {
      if (issue.resolution()!=null) {
        result.add(issue);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Issue> issues() {
    List<Issue> result = Lists.newArrayList();
    for (DefaultIssue issue : cache.byComponent(component.key())) {
      if (issue.resolution()==null) {
        result.add(issue);
      }
    }
    return result;
  }

  @Override
  public Component component() {
    return component;
  }
}
