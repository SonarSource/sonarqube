/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.List;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.task.projectanalysis.component.Component;

/**
 * This repository contains issues for only one component at a time. It's populated by {@link IntegrateIssuesVisitor} and
 * it's mainly used by {@link org.sonar.api.ce.measure.MeasureComputer.MeasureComputerContext} in order for a {@link MeasureComputer}
 * to access to the issues of a component.
 *
 * This repository must NEVER contains more issues than in issues from one component order to not consume to much memory.
 */
public interface ComponentIssuesRepository {

  /**
   * Return issues from the component
   *
   * @throws IllegalStateException if no issues have been set
   * @throws IllegalArgumentException if the component is not the component that contains current issues.
   */
  List<DefaultIssue> getIssues(Component component);

}
