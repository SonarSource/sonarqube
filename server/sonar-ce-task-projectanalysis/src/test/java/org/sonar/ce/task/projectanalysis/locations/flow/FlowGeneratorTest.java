/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FlowGeneratorTest {

  private static final String COMPONENT_NAME = "test_comp";

  private final Random random = new SecureRandom();


  @Mock
  private TreeRootHolder treeRootHolder;

  @InjectMocks
  private FlowGenerator flowGenerator;

  @Test
  public void convertFlows_withNullDbLocations_returnsEmptyList() {
    assertThat(flowGenerator.convertFlows(COMPONENT_NAME, null)).isEmpty();
  }

  @Test
  public void convertFlows_withEmptyDbLocations_returnsEmptyList() {
    DbIssues.Locations issueLocations = DbIssues.Locations.newBuilder().build();
    assertThat(flowGenerator.convertFlows(COMPONENT_NAME, issueLocations)).isEmpty();
  }

  @Test
  public void convertFlows_withSingleDbLocations_returnsCorrectFlow() {
    DbIssues.Location location = createDbLocation("comp_id_1");
    DbIssues.Locations issueLocations = DbIssues.Locations.newBuilder()
      .addFlow(createFlow(location))
      .build();

    List<Flow> flows = flowGenerator.convertFlows(COMPONENT_NAME, issueLocations);

    assertThat(flows).hasSize(1);
    Flow singleFlow = flows.iterator().next();

    assertThat(singleFlow.getLocations()).hasSize(1);
    Location singleLocation = singleFlow.getLocations().iterator().next();

    assertLocationMatches(singleLocation, location);
  }

  @Test
  public void convertFlows_with2FlowsSingleDbLocations_returnsCorrectFlow() {
    DbIssues.Location location1 = createDbLocation("comp_id_1");
    DbIssues.Location location2 = createDbLocation("comp_id_2");
    DbIssues.Locations issueLocations = DbIssues.Locations.newBuilder()
      .addFlow(createFlow(location1))
      .addFlow(createFlow(location2))
      .build();

    List<Flow> flows = flowGenerator.convertFlows(COMPONENT_NAME, issueLocations);

    assertThat(flows).hasSize(2).extracting(f -> f.getLocations().size()).containsExactly(1, 1);
    Map<String, DbIssues.Location> toDbLocation = Map.of(
      "file_path_" + location1.getComponentId(), location1,
      "file_path_" + location2.getComponentId(), location2);
    flows.stream()
      .map(actualFlow -> actualFlow.getLocations().iterator().next())
      .forEach(l -> assertLocationMatches(l, toDbLocation.get(l.getFilePath())));
  }

  @Test
  public void convertFlows_with2DbLocations_returns() {
    DbIssues.Location location1 = createDbLocation("comp_id_1");
    DbIssues.Location location2 = createDbLocation("comp_id_2");
    DbIssues.Locations issueLocations = DbIssues.Locations.newBuilder()
      .addFlow(createFlow(location1, location2))
      .build();

    List<Flow> flows = flowGenerator.convertFlows(COMPONENT_NAME, issueLocations);

    assertThat(flows).hasSize(1);
    Flow singleFlow = flows.iterator().next();

    assertThat(singleFlow.getLocations()).hasSize(2);
    Map<String, Location> pathToLocations = singleFlow.getLocations()
      .stream()
      .collect(toMap(Location::getFilePath, identity()));

    assertLocationMatches(pathToLocations.get("file_path_comp_id_1"), location1);
    assertLocationMatches(pathToLocations.get("file_path_comp_id_2"), location2);

  }

  private DbIssues.Location createDbLocation(String componentId) {
    org.sonar.db.protobuf.DbCommons.TextRange textRange = org.sonar.db.protobuf.DbCommons.TextRange.newBuilder()
      .setStartLine(random.nextInt(Integer.MAX_VALUE))
      .setEndLine(random.nextInt(Integer.MAX_VALUE))
      .setStartOffset(random.nextInt(Integer.MAX_VALUE))
      .setEndOffset(random.nextInt(Integer.MAX_VALUE))
      .build();

    Component component = mock(Component.class);
    when(component.getName()).thenReturn("file_path_" + componentId);
    when(treeRootHolder.getComponentByUuid(componentId)).thenReturn(component);
    return DbIssues.Location.newBuilder()
      .setComponentId(componentId)
      .setChecksum("hash" + secure().nextAlphanumeric(10))
      .setTextRange(textRange)
      .setMsg("msg" + secure().nextAlphanumeric(15))
      .build();
  }

  private static DbIssues.Flow createFlow(DbIssues.Location ... locations) {
    return DbIssues.Flow.newBuilder()
      .addAllLocation(List.of(locations))
      .build();
  }

  private static void assertLocationMatches(Location actualLocation, DbIssues.Location sourceLocation) {
    assertThat(actualLocation.getMessage()).isEqualTo(sourceLocation.getMsg());
    DbCommons.TextRange textRange = sourceLocation.getTextRange();
    assertThat(actualLocation.getTextRange().getStartLine()).isEqualTo(textRange.getStartLine());
    assertThat(actualLocation.getTextRange().getEndLine()).isEqualTo(textRange.getEndLine());
    assertThat(actualLocation.getTextRange().getStartLineOffset()).isEqualTo(textRange.getStartOffset());
    assertThat(actualLocation.getTextRange().getEndLineOffset()).isEqualTo(textRange.getEndOffset());
    assertThat(actualLocation.getTextRange().getHash()).isEqualTo(sourceLocation.getChecksum());
  }

}
