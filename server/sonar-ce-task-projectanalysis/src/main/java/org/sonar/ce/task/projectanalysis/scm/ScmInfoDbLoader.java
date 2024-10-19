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
package org.sonar.ce.task.projectanalysis.scm;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.ce.task.projectanalysis.period.NewCodeReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.source.FileSourceDto;

public class ScmInfoDbLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(ScmInfoDbLoader.class);

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MovedFilesRepository movedFilesRepository;
  private final DbClient dbClient;
  private final ReferenceBranchComponentUuids referenceBranchComponentUuid;
  private final NewCodeReferenceBranchComponentUuids newCodeReferenceBranchComponentUuids;
  private final PeriodHolder periodHolder;

  public ScmInfoDbLoader(AnalysisMetadataHolder analysisMetadataHolder, MovedFilesRepository movedFilesRepository,
      DbClient dbClient,
      ReferenceBranchComponentUuids referenceBranchComponentUuid,
      NewCodeReferenceBranchComponentUuids newCodeReferenceBranchComponentUuids,
      PeriodHolder periodHolder) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.movedFilesRepository = movedFilesRepository;
    this.dbClient = dbClient;
    this.referenceBranchComponentUuid = referenceBranchComponentUuid;
    this.newCodeReferenceBranchComponentUuids = newCodeReferenceBranchComponentUuids;
    this.periodHolder = periodHolder;
  }

  public Optional<DbScmInfo> getScmInfo(Component file) {
    Optional<String> uuid = getFileUUid(file);
    if (uuid.isEmpty()) {
      return Optional.empty();
    }

    LOGGER.trace("Reading SCM info from DB for file '{}'", uuid.get());
    try (DbSession dbSession = dbClient.openSession(false)) {
      FileSourceDto dto = dbClient.fileSourceDao().selectByFileUuid(dbSession, uuid.get());
      if (dto == null) {
        return Optional.empty();
      }
      return DbScmInfo.create(dto.getSourceData().getLinesList(), dto.getLineCount(), dto.getSrcHash());
    }
  }

  private Optional<String> getFileUUid(Component file) {
    if (!analysisMetadataHolder.isFirstAnalysis() && !analysisMetadataHolder.isPullRequest() && !isReferenceBranch()) {
      Optional<MovedFilesRepository.OriginalFile> originalFile = movedFilesRepository.getOriginalFile(file);
      if (originalFile.isPresent()) {
        return originalFile.map(MovedFilesRepository.OriginalFile::uuid);
      }
      return Optional.of(file.getUuid());
    }

    if (isReferenceBranch()) {
      var referencedBranchComponentUuid = newCodeReferenceBranchComponentUuids.getComponentUuid(file.getKey());
      if (referencedBranchComponentUuid != null) {
        return Optional.of(referencedBranchComponentUuid);
      }
      // no file to diff was found or missing reference branch changeset - use existing file
      return Optional.of(file.getUuid());
    }

    // at this point, it's the first analysis of a branch with copyFromPrevious flag true or any analysis of a PR
    Branch branch = analysisMetadataHolder.getBranch();
    if (!branch.isMain()) {
      return Optional.ofNullable(referenceBranchComponentUuid.getComponentUuid(file.getKey()));
    }

    return Optional.empty();
  }

  private boolean isReferenceBranch() {
    return periodHolder.hasPeriod() && periodHolder.getPeriod().getMode().equals(NewCodePeriodType.REFERENCE_BRANCH.name());
  }

}
