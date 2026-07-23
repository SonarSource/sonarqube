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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.history.RecordHistoryDelegate;
import org.sonar.ce.task.step.ComputationStep;

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
    // Use the tree root UUID (branch component UUID) since issues and measures are keyed on the branch component UUID.
    String entityUuid = treeRootHolder.getRoot().getUuid();
    try {
      delegate.recordHistory(entityUuid);
    } catch (Exception e) {
      LOGGER.warn("Failed to record issue count and measures history for entity {}", entityUuid, e);
    }
  }

  @Override
  public String getDescription() {
    return "Record issue count and measures history";
  }
}
