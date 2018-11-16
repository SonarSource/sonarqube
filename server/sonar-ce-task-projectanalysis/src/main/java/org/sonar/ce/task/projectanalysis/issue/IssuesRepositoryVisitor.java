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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.ArrayList;
import java.util.List;

import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

/**
 * Saves issues in the ComponentIssuesRepository
 * The repository should only hold the issues for a single component, so we use a mutable list
 */
public class IssuesRepositoryVisitor extends IssueVisitor {
  private final MutableComponentIssuesRepository componentIssuesRepository;
  private final List<DefaultIssue> componentIssues = new ArrayList<>();

  public IssuesRepositoryVisitor(MutableComponentIssuesRepository componentIssuesRepository) {
    this.componentIssuesRepository = componentIssuesRepository;
  }

  @Override
  public void beforeComponent(Component component) {
    componentIssues.clear();
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    componentIssues.add(issue);
  }

  @Override
  public void afterComponent(Component component) {
    componentIssuesRepository.setIssues(component, componentIssues);
  }
}
