/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.issue.index;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.server.issue.index.AsyncIssueIndexing;

@ComputeEngineSide
public class NoAsyncIssueIndexing implements AsyncIssueIndexing {
  @Override
  public void triggerOnIndexCreation() {
    throw new IllegalStateException("Async issue indexing should not be triggered in Compute Engine");
  }

  @Override
  public void triggerForProject(String projectUuid) {
    throw new IllegalStateException("Async issue indexing should not be triggered in Compute Engine");
  }
}
