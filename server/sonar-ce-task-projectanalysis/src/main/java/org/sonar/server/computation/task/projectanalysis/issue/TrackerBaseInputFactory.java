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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.server.computation.task.projectanalysis.filemove.MovedFilesRepository.OriginalFile;

/**
 * Factory of {@link Input} of base data for issue tracking. Data are lazy-loaded.
 */
public class TrackerBaseInputFactory {
  private static final LineHashSequence EMPTY_LINE_HASH_SEQUENCE = new LineHashSequence(Collections.emptyList());

  private final ComponentIssuesLoader issuesLoader;
  private final DbClient dbClient;
  private final MovedFilesRepository movedFilesRepository;

  public TrackerBaseInputFactory(ComponentIssuesLoader issuesLoader, DbClient dbClient, MovedFilesRepository movedFilesRepository) {
    this.issuesLoader = issuesLoader;
    this.dbClient = dbClient;
    this.movedFilesRepository = movedFilesRepository;
  }

  public Input<DefaultIssue> create(Component component) {
    return new BaseLazyInput(component, movedFilesRepository.getOriginalFile(component).orNull());
  }

  private class BaseLazyInput extends LazyInput<DefaultIssue> {
    private final Component component;
    @CheckForNull
    private final String effectiveUuid;

    private BaseLazyInput(Component component, @Nullable OriginalFile originalFile) {
      this.component = component;
      this.effectiveUuid = originalFile == null ? component.getUuid() : originalFile.getUuid();
    }

    @Override
    protected LineHashSequence loadLineHashSequence() {
      if (component.getType() != Component.Type.FILE) {
        return EMPTY_LINE_HASH_SEQUENCE;
      }

      try (DbSession session = dbClient.openSession(false)) {
        List<String> hashes = dbClient.fileSourceDao().selectLineHashes(session, effectiveUuid);
        if (hashes == null || hashes.isEmpty()) {
          return EMPTY_LINE_HASH_SEQUENCE;
        }
        return new LineHashSequence(hashes);
      }
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      return issuesLoader.loadForComponentUuid(effectiveUuid);
    }
  }
}
