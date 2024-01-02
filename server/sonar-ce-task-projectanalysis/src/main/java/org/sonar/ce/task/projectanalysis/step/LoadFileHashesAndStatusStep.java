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
package org.sonar.ce.task.projectanalysis.step;

import java.util.HashMap;
import java.util.Map;
import org.sonar.ce.task.projectanalysis.component.FileStatusesImpl;
import org.sonar.ce.task.projectanalysis.component.PreviousSourceHashRepositoryImpl;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.source.FileHashesDto;
import org.sonar.db.source.FileSourceDao;

public class LoadFileHashesAndStatusStep implements ComputationStep {
  private final DbClient dbClient;
  private final PreviousSourceHashRepositoryImpl previousFileHashesRepository;
  private final FileStatusesImpl fileStatuses;
  private final FileSourceDao fileSourceDao;
  private final TreeRootHolder treeRootHolder;

  public LoadFileHashesAndStatusStep(DbClient dbClient, PreviousSourceHashRepositoryImpl previousFileHashesRepository,
    FileStatusesImpl fileStatuses, FileSourceDao fileSourceDao, TreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.previousFileHashesRepository = previousFileHashesRepository;
    this.fileStatuses = fileStatuses;
    this.fileSourceDao = fileSourceDao;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute(Context context) {
    Map<String, FileHashesDto> previousFileHashesByUuid = new HashMap<>();
    String projectUuid = treeRootHolder.getRoot().getUuid();

    try (DbSession session = dbClient.openSession(false)) {
      fileSourceDao.scrollFileHashesByProjectUuid(session, projectUuid, ctx -> {
        FileHashesDto dto = ctx.getResultObject();
        previousFileHashesByUuid.put(dto.getFileUuid(), dto);
      });
    }
    previousFileHashesRepository.set(previousFileHashesByUuid);
    fileStatuses.initialize();
  }

  @Override
  public String getDescription() {
    return "Load file hashes and statuses";
  }
}
