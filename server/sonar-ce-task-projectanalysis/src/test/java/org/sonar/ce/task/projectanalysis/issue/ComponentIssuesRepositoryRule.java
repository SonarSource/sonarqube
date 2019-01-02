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

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.junit.rules.ExternalResource;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.core.issue.DefaultIssue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class ComponentIssuesRepositoryRule extends ExternalResource implements MutableComponentIssuesRepository, ComponentIssuesRepository {

  private final TreeRootHolder treeRootHolder;

  @CheckForNull
  private List<DefaultIssue> issues;

  @CheckForNull
  private Component component;

  public ComponentIssuesRepositoryRule(TreeRootHolder treeRootHolder) {
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void setIssues(Component component, List<DefaultIssue> issues) {
    checkNotNull(component, "component cannot be null");
    setIssues(component.getReportAttributes().getRef(), issues);
  }

  public void setIssues(int componentRef, List<DefaultIssue> issues) {
    this.issues = requireNonNull(issues, "issues cannot be null");
    Component component = treeRootHolder.getComponentByRef(componentRef);
    checkArgument(component != null, String.format("Component '%s' does not exists in the report ", componentRef));
    this.component = component;
  }

  @Override
  public List<DefaultIssue> getIssues(Component component) {
    checkNotNull(component, "component cannot be null");
    // Views has no issues
    if (component.getType().equals(Component.Type.PROJECT_VIEW)
      || component.getType().equals(Component.Type.SUBVIEW)
      || component.getType().equals(Component.Type.VIEW)) {
      return Collections.emptyList();
    }
    return getIssues(component.getReportAttributes().getRef());
  }

  public List<DefaultIssue> getIssues(int componentRef) {
    checkState(this.component != null && this.issues != null, "Issues have not been initialized");
    Component component = treeRootHolder.getComponentByRef(componentRef);
    checkArgument(component != null, String.format("Component '%s' does not exists in the report ", componentRef));
    checkArgument(component == this.component,
      String.format("Only issues from component '%s' are available, but wanted component is '%s'.",
        this.component.getReportAttributes().getRef(), component.getReportAttributes().getRef()));
    return issues;
  }

}
