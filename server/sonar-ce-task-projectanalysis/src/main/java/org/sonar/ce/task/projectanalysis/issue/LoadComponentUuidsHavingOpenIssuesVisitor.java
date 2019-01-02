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

import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;

import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Load all open components having open issues of the project
 */
public class LoadComponentUuidsHavingOpenIssuesVisitor extends TypeAwareVisitorAdapter {

  private final BaseIssuesLoader baseIssuesLoader;
  private final ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues;

  public LoadComponentUuidsHavingOpenIssuesVisitor(BaseIssuesLoader baseIssuesLoader, ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues) {
    super(CrawlerDepthLimit.PROJECT, PRE_ORDER);
    this.baseIssuesLoader = baseIssuesLoader;
    this.componentsWithUnprocessedIssues = componentsWithUnprocessedIssues;
  }

  @Override
  public void visitProject(Component project) {
    componentsWithUnprocessedIssues.setUuids(baseIssuesLoader.loadUuidsOfComponentsWithOpenIssues());
  }
}
