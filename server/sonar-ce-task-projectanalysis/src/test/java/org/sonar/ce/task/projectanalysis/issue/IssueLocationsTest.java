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
package org.sonar.ce.task.projectanalysis.issue;

import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueLocationsTest {

  @Test
  public void allLinesFor_filters_lines_for_specified_component() {
    DbIssues.Locations.Builder locations = DbIssues.Locations.newBuilder();
    locations.addFlowBuilder()
      .addLocation(newLocation("file1", 5, 5))
      .addLocation(newLocation("file1", 10, 12))
      .addLocation(newLocation("file1", 15, 15))
      .addLocation(newLocation("file2", 10, 11))
      .build();
    DefaultIssue issue = new DefaultIssue().setLocations(locations.build());

    assertThat(IssueLocations.allLinesFor(issue, "file1")).containsExactlyInAnyOrder(5, 10, 11, 12, 15);
    assertThat(IssueLocations.allLinesFor(issue, "file2")).containsExactlyInAnyOrder(10, 11);
    assertThat(IssueLocations.allLinesFor(issue, "file3")).isEmpty();
  }

  @Test
  public void allLinesFor_traverses_all_flows() {
    DbIssues.Locations.Builder locations = DbIssues.Locations.newBuilder();
    locations.addFlowBuilder()
      .addLocation(newLocation("file1", 5, 5))
      .addLocation(newLocation("file2", 10, 11))
      .build();
    locations.addFlowBuilder()
      .addLocation(newLocation("file1", 7, 9))
      .addLocation(newLocation("file2", 12, 12))
      .build();
    DefaultIssue issue = new DefaultIssue().setLocations(locations.build());

    assertThat(IssueLocations.allLinesFor(issue, "file1")).containsExactlyInAnyOrder(5, 7, 8, 9);
    assertThat(IssueLocations.allLinesFor(issue, "file2")).containsExactlyInAnyOrder(10, 11, 12);
  }

  @Test
  public void allLinesFor_keeps_duplicated_lines() {
    DbIssues.Locations.Builder locations = DbIssues.Locations.newBuilder();
    locations.addFlowBuilder()
      .addLocation(newLocation("file1", 5, 5))
      .addLocation(newLocation("file1", 4, 6))
      .build();
    DefaultIssue issue = new DefaultIssue().setLocations(locations.build());

    assertThat(IssueLocations.allLinesFor(issue, "file1")).containsExactlyInAnyOrder(4, 5, 5, 6);
  }

  @Test
  public void allLinesFor_returns_empty_if_no_locations_are_set() {
    DefaultIssue issue = new DefaultIssue().setLocations(null);

    assertThat(IssueLocations.allLinesFor(issue, "file1")).isEmpty();
  }

  private static DbIssues.Location newLocation(String componentId, int startLine, int endLine) {
    return DbIssues.Location.newBuilder()
      .setComponentId(componentId)
      .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(startLine).setEndLine(endLine).build())
      .build();
  }
}
