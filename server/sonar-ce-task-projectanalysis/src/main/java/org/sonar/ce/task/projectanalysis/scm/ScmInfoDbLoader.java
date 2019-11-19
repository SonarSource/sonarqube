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
package org.sonar.ce.task.projectanalysis.scm;

import java.util.Optional;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReferenceBranchComponentUuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.source.FileSourceDto;

public class ScmInfoDbLoader {
  private static final Logger LOGGER = Loggers.get(ScmInfoDbLoader.class);

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private final ReferenceBranchComponentUuids referenceBranchComponentUuid;

  public ScmInfoDbLoader(AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient, ReferenceBranchComponentUuids referenceBranchComponentUuid) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
    this.referenceBranchComponentUuid = referenceBranchComponentUuid;
  }

  public Optional<DbScmInfo> getScmInfo(Component file) {
    Optional<String> uuid = getFileUUid(file);
    if (!uuid.isPresent()) {
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
    if (!analysisMetadataHolder.isFirstAnalysis() && !analysisMetadataHolder.isPullRequest()) {
      return Optional.of(file.getUuid());
    }

    // at this point, it's the first analysis of a branch with copyFromPrevious flag true or any analysis of a PR
    Branch branch = analysisMetadataHolder.getBranch();
    if (!branch.isMain()) {
      return Optional.ofNullable(referenceBranchComponentUuid.getComponentUuid(file.getDbKey()));
    }

    return Optional.empty();
  }

}
