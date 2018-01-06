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
package org.sonar.api.issue.batch;

import org.sonar.api.issue.Issue;

/**
 * A filter chain is an object provided to issues filters for fine control over the filtering logic. Each filter has the choice to:
 * <ul>
 *  <li>Accept the issue</li>
 *  <li>Reject the issue</li>
 *  <li>Let downstream filters decide by passing the issue to the rest of the chain</li>
 * </ul>
 * @since 4.0
 * @deprecated since 5.3. Use {@link org.sonar.api.scan.issue.filter.IssueFilterChain} instead.
 */
@Deprecated
public interface IssueFilterChain {

  /**
   * Called by a filter to let downstream filters decide the fate of the issue
   */
  boolean accept(Issue issue);
}
