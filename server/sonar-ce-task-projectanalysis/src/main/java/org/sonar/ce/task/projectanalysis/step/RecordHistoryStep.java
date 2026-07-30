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

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.history.RecordHistoryDelegate;
import org.sonar.ce.task.step.ComputationStep;
import org.sonarsource.history.model.EntityType;

import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;

/**
 * Records issue count and measures history for the current entity.
 * Delegates to the CE-provided {@link RecordHistoryDelegate} implementation.
 */
public class RecordHistoryStep implements ComputationStep {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecordHistoryStep.class);

  private final TreeRootHolder treeRootHolder;
  private final RecordHistoryDelegate delegate;

  public RecordHistoryStep(TreeRootHolder treeRootHolder, RecordHistoryDelegate delegate) {
    this.treeRootHolder = treeRootHolder;
    this.delegate = delegate;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    Component root = treeRootHolder.getRoot();
    String entityUuid = root.getUuid();
    try {
      delegate.recordHistory(entityUuid, getEntityType(root), getIssueSourceBranchUuids(root));
    } catch (Exception e) {
      LOGGER.warn("Failed to record issue count and measures history for entity {}", entityUuid, e);
    }
  }

  private static EntityType getEntityType(Component root) {
    if (root.getType() == PROJECT) {
      return EntityType.PROJECT_BRANCH;
    }
    if (root.getType() == VIEW) {
      return switch (root.getViewAttributes().getType()) {
        case APPLICATION -> EntityType.APPLICATION;
        case PORTFOLIO -> EntityType.PORTFOLIO;
      };
    }
    throw new IllegalArgumentException("History cannot be recorded for root component type " + root.getType());
  }

  /**
   * Issues are attached to project branches: the root itself for a branch analysis, or every referenced project view
   * below an application or portfolio.
   */
  private static Set<String> getIssueSourceBranchUuids(Component root) {
    if (root.getType() == PROJECT) {
      return Set.of(root.getUuid());
    }
    Set<String> branchUuids = new HashSet<>();
    collectIssueSourceBranchUuids(root, branchUuids);
    return branchUuids;
  }

  private static void collectIssueSourceBranchUuids(Component component, Set<String> branchUuids) {
    if (component.getType() == PROJECT_VIEW) {
      branchUuids.add(component.getProjectViewAttributes().getUuid());
    }
    component.getChildren().forEach(child -> collectIssueSourceBranchUuids(child, branchUuids));
  }

  @Override
  public String getDescription() {
    return "Record issue count and measures history";
  }
}
