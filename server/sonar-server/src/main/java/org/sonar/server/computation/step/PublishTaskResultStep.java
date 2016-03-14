/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import javax.annotation.concurrent.Immutable;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.ce.queue.CeTaskResult;
import org.sonar.server.computation.taskprocessor.MutableTaskResultHolder;

public class PublishTaskResultStep implements ComputationStep {
  private final MutableTaskResultHolder taskResultHolder;
  private final TreeRootHolder treeRootHolder;
  private final DbIdsRepository dbIdsRepository;

  public PublishTaskResultStep(MutableTaskResultHolder taskResultHolder, TreeRootHolder treeRootHolder, DbIdsRepository dbIdsRepository) {
    this.taskResultHolder = taskResultHolder;
    this.treeRootHolder = treeRootHolder;
    this.dbIdsRepository = dbIdsRepository;
  }

  @Override
  public String getDescription() {
    return "Publish task results";
  }

  @Override
  public void execute() {
    long snapshotId = dbIdsRepository.getSnapshotId(treeRootHolder.getRoot());
    taskResultHolder.setResult(new CeTaskResultImpl(snapshotId));
  }

  @Immutable
  private static class CeTaskResultImpl implements CeTaskResult {
    private final long snapshotId;

    public CeTaskResultImpl(long snapshotId) {
      this.snapshotId = snapshotId;
    }

    @Override
    public Long getSnapshotId() {
      return snapshotId;
    }
  }
}
