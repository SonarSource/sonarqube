/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ProjectViewAttributes;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.ViewAttributes;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.projectanalysis.history.RecordHistoryDelegate;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonarsource.history.model.EntityType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordHistoryStepTest {

  private static final String ENTITY_UUID = "entity-uuid";

  private final TreeRootHolder treeRootHolder = mock();
  private final Component root = mock();
  private final RecordHistoryDelegate delegate = mock();

  @RegisterExtension
  private final LogTesterJUnit5 logs = new LogTesterJUnit5();

  private final RecordHistoryStep underTest = new RecordHistoryStep(treeRootHolder, delegate);

  @BeforeEach
  void setUp() {
    when(treeRootHolder.getRoot()).thenReturn(root);
    when(root.getUuid()).thenReturn(ENTITY_UUID);
    when(root.getType()).thenReturn(Component.Type.PROJECT);
    when(root.getChildren()).thenReturn(List.of());
  }

  @Test
  void execute_delegatesHistoryRecording() {
    underTest.execute(new TestComputationStepContext());

    verify(delegate).recordHistory(ENTITY_UUID, EntityType.PROJECT_BRANCH, Set.of(ENTITY_UUID));
  }

  @Test
  void execute_whenApplication_shouldAggregateReferencedProjectBranches() {
    givenViewRoot(ViewAttributes.Type.APPLICATION, projectView("branch-1"), projectView("branch-2"));

    underTest.execute(new TestComputationStepContext());

    verify(delegate).recordHistory(ENTITY_UUID, EntityType.APPLICATION, Set.of("branch-1", "branch-2"));
  }

  @Test
  void execute_whenPortfolio_shouldMapToPortfolioEntityType() {
    Component project = projectView("branch-1");
    when(root.getType()).thenReturn(Component.Type.VIEW);
    when(root.getViewAttributes()).thenReturn(new ViewAttributes(ViewAttributes.Type.PORTFOLIO));
    when(root.getChildren()).thenReturn(List.of(project));

    underTest.execute(new TestComputationStepContext());

    verify(delegate).recordHistory(ENTITY_UUID, EntityType.PORTFOLIO, Set.of("branch-1"));
  }

  @Test
  void execute_whenPortfolioHasNestedViews_shouldAggregateUniqueReferencedProjectBranches() {
    Component nestedSubView = subView("nested-sub-view", projectView("branch-2"), projectView("branch-3"));
    givenViewRoot(ViewAttributes.Type.PORTFOLIO, subView("sub-view", nestedSubView), projectView("branch-2"));

    underTest.execute(new TestComputationStepContext());

    verify(delegate).recordHistory(ENTITY_UUID, EntityType.PORTFOLIO, Set.of("branch-2", "branch-3"));
  }

  @Test
  void execute_doesNotFailWhenHistoryRecordingFails() {
    RuntimeException exception = new RuntimeException("history failure");
    doThrow(exception).when(delegate).recordHistory(ENTITY_UUID, EntityType.PROJECT_BRANCH, Set.of(ENTITY_UUID));

    assertThatCode(() -> underTest.execute(new TestComputationStepContext())).doesNotThrowAnyException();

    assertThat(logs.getLogs(Level.WARN)).extracting(LogAndArguments::getFormattedMsg)
      .containsExactly("Failed to record issue count and measures history for entity entity-uuid");
    assertThat(logs.getLogs(Level.WARN)).extracting(LogAndArguments::getThrowable).containsExactly(exception);
  }

  @Test
  void execute_whenRootTypeIsUnsupported_shouldNotFail() {
    when(root.getType()).thenReturn(Component.Type.FILE);

    assertThatCode(() -> underTest.execute(new TestComputationStepContext())).doesNotThrowAnyException();

    assertThat(logs.getLogs(Level.WARN)).extracting(LogAndArguments::getFormattedMsg)
      .containsExactly("Failed to record issue count and measures history for entity entity-uuid");
  }

  @Test
  void getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Record issue count and measures history");
  }

  private void givenViewRoot(ViewAttributes.Type viewType, Component... children) {
    when(treeRootHolder.getRoot()).thenReturn(ViewsComponent.builder(Component.Type.VIEW, "view-key")
      .setUuid(ENTITY_UUID)
      .setViewAttributes(new ViewAttributes(viewType))
      .addChildren(children)
      .build());
  }

  private static Component projectView(String branchUuid) {
    return ViewsComponent.builder(Component.Type.PROJECT_VIEW, branchUuid)
      .setProjectViewAttributes(new ProjectViewAttributes(branchUuid, "project-key", null, false, null))
      .build();
  }

  private static Component subView(String key, Component... children) {
    return ViewsComponent.builder(Component.Type.SUBVIEW, key)
      .addChildren(children)
      .build();
  }
}
