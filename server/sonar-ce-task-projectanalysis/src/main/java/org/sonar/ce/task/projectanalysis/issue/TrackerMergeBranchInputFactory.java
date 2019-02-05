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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.MergeBranchComponentUuids;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

public class TrackerMergeBranchInputFactory {
  private static final LineHashSequence EMPTY_LINE_HASH_SEQUENCE = new LineHashSequence(Collections.emptyList());

  private final ComponentIssuesLoader mergeIssuesLoader;
  private final DbClient dbClient;
  private final MergeBranchComponentUuids mergeBranchComponentUuids;

  public TrackerMergeBranchInputFactory(ComponentIssuesLoader mergeIssuesLoader, MergeBranchComponentUuids mergeBranchComponentUuids, DbClient dbClient) {
    this.mergeIssuesLoader = mergeIssuesLoader;
    this.mergeBranchComponentUuids = mergeBranchComponentUuids;
    this.dbClient = dbClient;
    // TODO detect file moves?
  }

  public boolean hasMergeBranchAnalysis() {
    return mergeBranchComponentUuids.hasMergeBranchAnalysis();
  }

  public Input<DefaultIssue> create(Component component) {
    String mergeBranchComponentUuid = mergeBranchComponentUuids.getUuid(component.getDbKey());
    return new MergeLazyInput(component.getType(), mergeBranchComponentUuid);
  }

  private class MergeLazyInput extends LazyInput<DefaultIssue> {
    private final Component.Type type;
    private final String mergeBranchComponentUuid;

    private MergeLazyInput(Component.Type type, @Nullable String mergeBranchComponentUuid) {
      this.type = type;
      this.mergeBranchComponentUuid = mergeBranchComponentUuid;
    }

    @Override
    protected LineHashSequence loadLineHashSequence() {
      if (mergeBranchComponentUuid == null || type != Component.Type.FILE) {
        return EMPTY_LINE_HASH_SEQUENCE;
      }

      try (DbSession session = dbClient.openSession(false)) {
        List<String> hashes = dbClient.fileSourceDao().selectLineHashes(session, mergeBranchComponentUuid);
        if (hashes == null || hashes.isEmpty()) {
          return EMPTY_LINE_HASH_SEQUENCE;
        }
        return new LineHashSequence(hashes);
      }
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      if (mergeBranchComponentUuid == null) {
        return Collections.emptyList();
      }
      return mergeIssuesLoader.loadOpenIssuesWithChanges(mergeBranchComponentUuid);
    }
  }

}
