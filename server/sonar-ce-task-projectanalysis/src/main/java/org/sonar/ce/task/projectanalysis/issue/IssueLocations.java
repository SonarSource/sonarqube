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

import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;

class IssueLocations {

  private IssueLocations() {
    // do not instantiate
  }

  /**
   * Extract the lines of all the locations in the specified component. All the flows and secondary locations
   * are taken into account. The lines present in multiple flows and locations are kept
   * duplicated. Ordering of results is not guaranteed.
   * <p>
   * TODO should be a method of DefaultIssue, as soon as it's no
   * longer in sonar-core and can access sonar-db-dao.
   */
  public static IntStream allLinesFor(DefaultIssue issue, String componentId) {
    DbIssues.Locations locations = issue.getLocations();
    if (locations == null) {
      return IntStream.empty();
    }

    Stream<DbCommons.TextRange> textRanges = Stream.concat(
      locations.hasTextRange() ? Stream.of(locations.getTextRange()) : Stream.empty(),
      locations.getFlowList().stream()
        .flatMap(f -> f.getLocationList().stream())
        .filter(l -> Objects.equals(componentIdOf(issue, l), componentId))
        .map(DbIssues.Location::getTextRange));
    return textRanges.flatMapToInt(range -> IntStream.rangeClosed(range.getStartLine(), range.getEndLine()));
  }

  private static String componentIdOf(DefaultIssue issue, DbIssues.Location location) {
    if (location.hasComponentId()) {
      return StringUtils.defaultIfEmpty(location.getComponentId(), issue.componentUuid());
    }
    return issue.componentUuid();
  }
}
