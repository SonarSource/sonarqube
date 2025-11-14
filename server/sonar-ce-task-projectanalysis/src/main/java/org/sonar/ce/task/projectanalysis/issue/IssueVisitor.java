/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;

public abstract class IssueVisitor {

  /**
   * This method is called for each component before processing its issues.
   * The component does not necessarily have issues.
   */
  public void beforeComponent(Component component) {

  }

  /**
   * This method is called for each issue of a component when tracking is done and issue is initialized.
   * That means that the following fields are set: resolution, status, line, creation date, uuid
   * and all the fields merged from base issues.
   * <br/>
   * The related rule is active in the Quality profile. Issues on inactive rules
   * are ignored.
   */
  public void onIssue(Component component, DefaultIssue issue) {

  }

  /**
   * This method is called for all raw issues of a component before tracking is done.
   */
  public void onRawIssues(Component component, Input<DefaultIssue> rawIssues, @Nullable Input<DefaultIssue> baseIssues) {

  }

  /**
   * This method is called on a component before issues are persisted to cache.
   */
  public void beforeCaching(Component component) {

  }

  public void afterComponent(Component component) {

  }
}
