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

import org.sonar.api.component.Component;
import org.sonar.api.issue.Issuable;
import org.sonar.api.resources.Scopes;
import org.sonar.batch.ProjectTree;
import org.sonar.core.component.PerspectiveBuilder;
import org.sonar.core.component.ResourceComponent;

import javax.annotation.CheckForNull;

/**
 * Create the perspective {@link Issuable} on components.
 * @since 3.6
 */
public class IssuableFactory extends PerspectiveBuilder<Issuable> {

  private final ModuleIssues moduleIssues;
  private final IssueCache cache;
  private final ProjectTree projectTree;

  public IssuableFactory(ModuleIssues moduleIssues, IssueCache cache, ProjectTree projectTree) {
    super(Issuable.class);
    this.moduleIssues = moduleIssues;
    this.cache = cache;
    this.projectTree = projectTree;
  }

  @CheckForNull
  @Override
  public Issuable loadPerspective(Class<Issuable> perspectiveClass, Component component) {
    boolean supported = true;
    if (component instanceof ResourceComponent) {
      supported = Scopes.isHigherThanOrEquals(((ResourceComponent) component).scope(), Scopes.FILE);
    }
    return supported ? new DefaultIssuable(component, projectTree.getRootProject(), moduleIssues, cache) : null;
  }
}
