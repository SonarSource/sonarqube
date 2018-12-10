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
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class ComponentIssuesRepositoryImpl implements MutableComponentIssuesRepository {

  @CheckForNull
  private List<DefaultIssue> issues;

  @CheckForNull
  private Component component;

  @Override
  public void setIssues(Component component, List<DefaultIssue> issues) {
    this.issues = requireNonNull(issues, "issues cannot be null");
    this.component = requireNonNull(component, "component cannot be null");
  }

  @Override
  public List<DefaultIssue> getIssues(Component component) {
    if (component.getType() == Component.Type.DIRECTORY) {
      // No issues on directories
      return Collections.emptyList();
    }
    checkState(this.component != null && this.issues != null, "Issues have not been initialized");
    checkArgument(component.equals(this.component),
      "Only issues from component '%s' are available, but wanted component is '%s'.",
      this.component.getReportAttributes().getRef(), component.getReportAttributes().getRef());
    return issues;
  }
}
