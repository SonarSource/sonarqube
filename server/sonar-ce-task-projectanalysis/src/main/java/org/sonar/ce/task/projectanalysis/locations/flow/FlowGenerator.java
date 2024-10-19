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
package org.sonar.ce.task.projectanalysis.locations.flow;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;

import static java.util.stream.Collectors.toCollection;

public class FlowGenerator {
  private final TreeRootHolder treeRootHolder;

  public FlowGenerator(TreeRootHolder treeRootHolder) {
    this.treeRootHolder = treeRootHolder;
  }

  public List<Flow> convertFlows(String componentName, @Nullable DbIssues.Locations issueLocations) {
    if (issueLocations == null) {
      return Collections.emptyList();
    }
    return issueLocations.getFlowList().stream()
      .map(sourceFlow -> toFlow(componentName, sourceFlow))
      .collect(Collectors.toCollection(LinkedList::new));
  }

  private Flow toFlow(String componentName, DbIssues.Flow sourceFlow) {
    Flow flow = new Flow();
    List<Location> locations = getFlowLocations(componentName, sourceFlow);
    flow.setLocations(locations);
    return flow;
  }

  private List<Location> getFlowLocations(String componentName, DbIssues.Flow sourceFlow) {
    return sourceFlow.getLocationList().stream()
      .map(sourceLocation -> toLocation(componentName, sourceLocation))
      .collect(toCollection(LinkedList::new));
  }

  private Location toLocation(String componentName, DbIssues.Location sourceLocation) {
    Location location = new Location();
    Component locationComponent = treeRootHolder.getComponentByUuid(sourceLocation.getComponentId());
    String filePath = Optional.ofNullable(locationComponent).map(Component::getName).orElse(componentName);
    location.setFilePath(filePath);
    location.setMessage(sourceLocation.getMsg());

    TextRange textRange = getTextRange(sourceLocation.getTextRange(), sourceLocation.getChecksum());
    location.setTextRange(textRange);
    return location;
  }

  private static TextRange getTextRange(DbCommons.TextRange source, String checksum) {
    TextRange textRange = new TextRange();
    textRange.setStartLine(source.getStartLine());
    textRange.setStartLineOffset(source.getStartOffset());
    textRange.setEndLine(source.getEndLine());
    textRange.setEndLineOffset(source.getEndOffset());
    textRange.setHash(checksum);
    return textRange;
  }
}
