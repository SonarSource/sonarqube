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
package org.sonar.api.batch.sensor.issue.internal;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.NewIssue.FlowType;

public class DefaultIssueFlow implements Issue.Flow {
  private final List<IssueLocation> locations;
  private final FlowType type;
  @Nullable
  private final String description;

  public DefaultIssueFlow(List<IssueLocation> locations, FlowType type, @Nullable String description) {
    this.locations = locations;
    this.type = type;
    this.description = description;
  }

  @Override
  public List<IssueLocation> locations() {
    return locations;
  }

  @Override
  public FlowType type() {
    return type;
  }

  @CheckForNull
  @Override
  public String description() {
    return description;
  }
}
