/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

public class TrackerTargetBranchInputFactory {
  private static final LineHashSequence EMPTY_LINE_HASH_SEQUENCE = new LineHashSequence(Collections.emptyList());

  private final ComponentIssuesLoader componentIssuesLoader;
  private final DbClient dbClient;
  private final TargetBranchComponentUuids targetBranchComponentUuids;

  public TrackerTargetBranchInputFactory(ComponentIssuesLoader componentIssuesLoader, TargetBranchComponentUuids targetBranchComponentUuids,
    DbClient dbClient) {
    this.componentIssuesLoader = componentIssuesLoader;
    this.targetBranchComponentUuids = targetBranchComponentUuids;
    this.dbClient = dbClient;
    // TODO detect file moves?
  }

  public boolean hasTargetBranchAnalysis() {
    return targetBranchComponentUuids.hasTargetBranchAnalysis();
  }

  public Input<DefaultIssue> createForTargetBranch(Component component) {
    String targetBranchComponentUuid = targetBranchComponentUuids.getTargetBranchComponentUuid(component.getDbKey());
    return new TargetLazyInput(component.getType(), targetBranchComponentUuid);
  }

  private class TargetLazyInput extends LazyInput<DefaultIssue> {
    private final Component.Type type;
    private final String targetBranchComponentUuid;

    private TargetLazyInput(Component.Type type, @Nullable String targetBranchComponentUuid) {
      this.type = type;
      this.targetBranchComponentUuid = targetBranchComponentUuid;
    }

    @Override
    protected LineHashSequence loadLineHashSequence() {
      if (targetBranchComponentUuid == null || type != Component.Type.FILE) {
        return EMPTY_LINE_HASH_SEQUENCE;
      }

      try (DbSession session = dbClient.openSession(false)) {
        List<String> hashes = dbClient.fileSourceDao().selectLineHashes(session, targetBranchComponentUuid);
        if (hashes == null || hashes.isEmpty()) {
          return EMPTY_LINE_HASH_SEQUENCE;
        }
        return new LineHashSequence(hashes);
      }
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      if (targetBranchComponentUuid == null) {
        return Collections.emptyList();
      }
      return componentIssuesLoader.loadOpenIssuesWithChanges(targetBranchComponentUuid);
    }
  }

}
