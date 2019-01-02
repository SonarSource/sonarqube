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
package org.sonar.server.issue.workflow;

import java.util.HashSet;
import java.util.Set;
import org.sonar.api.issue.Issue;

import static java.util.Arrays.asList;

public class HasResolution implements Condition {

  private final Set<String> resolutions;

  public HasResolution(String first, String... others) {
    this.resolutions = new HashSet<>();
    this.resolutions.add(first);
    this.resolutions.addAll(asList(others));
  }

  @Override
  public boolean matches(Issue issue) {
    return issue.resolution() != null && resolutions.contains(issue.resolution());
  }
}
