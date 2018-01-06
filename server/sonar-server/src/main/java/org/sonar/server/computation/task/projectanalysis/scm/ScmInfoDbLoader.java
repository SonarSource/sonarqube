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
package org.sonar.server.computation.task.projectanalysis.scm;

import java.util.Optional;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.Component.Status;
import org.sonar.server.computation.task.projectanalysis.component.MergeBranchComponentUuids;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepositoryImpl.NoScmInfo;
import org.sonar.server.computation.task.projectanalysis.source.SourceHashRepository;

public class ScmInfoDbLoader {
  private static final Logger LOGGER = Loggers.get(ScmInfoDbLoader.class);

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private final SourceHashRepository sourceHashRepository;
  private final MergeBranchComponentUuids mergeBranchComponentUuid;

  public ScmInfoDbLoader(AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient,
    SourceHashRepository sourceHashRepository, MergeBranchComponentUuids mergeBranchComponentUuid) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
    this.sourceHashRepository = sourceHashRepository;
    this.mergeBranchComponentUuid = mergeBranchComponentUuid;
  }

  public ScmInfo getScmInfoFromDb(Component file) {
    Optional<String> uuid = getFileUUid(file);

    if (!uuid.isPresent()) {
      return NoScmInfo.INSTANCE;
    }

    LOGGER.trace("Reading SCM info from db for file '{}'", uuid.get());
    try (DbSession dbSession = dbClient.openSession(false)) {
      FileSourceDto dto = dbClient.fileSourceDao().selectSourceByFileUuid(dbSession, uuid.get());
      if (dto == null || !isDtoValid(file, dto)) {
        return NoScmInfo.INSTANCE;
      }
      return DbScmInfo.create(file, dto.getSourceData().getLinesList()).or(NoScmInfo.INSTANCE);
    }
  }

  private Optional<String> getFileUUid(Component file) {
    if (!analysisMetadataHolder.isFirstAnalysis()) {
      return Optional.of(file.getUuid());
    }

    // at this point, it's the first analysis but had copyFromPrevious flag true
    Branch branch = analysisMetadataHolder.getBranch();
    if (branch.getMergeBranchUuid().isPresent()) {
      return Optional.ofNullable(mergeBranchComponentUuid.getUuid(file.getKey()));
    }

    return Optional.empty();
  }

  private boolean isDtoValid(Component file, FileSourceDto dto) {
    if (file.getStatus() == Status.SAME) {
      return true;
    }
    return sourceHashRepository.getRawSourceHash(file).equals(dto.getSrcHash());
  }
}
