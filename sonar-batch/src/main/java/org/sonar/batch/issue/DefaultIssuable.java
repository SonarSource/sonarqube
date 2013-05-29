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
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueBuilder;

import java.util.List;

/**
 * @since 3.6
 */
public class DefaultIssuable implements Issuable {

  private final ScanIssues scanIssues;
  private final IssueCache cache;
  private final Component component;

  DefaultIssuable(Component component, ScanIssues scanIssues, IssueCache cache) {
    this.component = component;
    this.scanIssues = scanIssues;
    this.cache = cache;
  }

  @Override
  public IssueBuilder newIssueBuilder() {
    return new DefaultIssueBuilder().componentKey(component.key());
  }

  @Override
  public boolean addIssue(Issue issue) {
    return scanIssues.initAndAddIssue(((DefaultIssue) issue));
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Issue> issues() {
    Iterable<DefaultIssue> elements = cache.byComponent(component.key());
    return Lists.<Issue>newArrayList(elements);
  }

  @Override
  public Component component() {
    return component;
  }
}
