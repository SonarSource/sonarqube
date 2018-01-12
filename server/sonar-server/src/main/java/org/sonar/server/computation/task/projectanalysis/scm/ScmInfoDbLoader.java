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
import org.sonar.server.computation.task.projectanalysis.component.MergeBranchComponentUuids;

public class ScmInfoDbLoader {
  private static final Logger LOGGER = Loggers.get(ScmInfoDbLoader.class);

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private final MergeBranchComponentUuids mergeBranchComponentUuid;

  public ScmInfoDbLoader(AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient, MergeBranchComponentUuids mergeBranchComponentUuid) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
    this.mergeBranchComponentUuid = mergeBranchComponentUuid;
  }

  public Optional<DbScmInfo> getScmInfo(Component file) {
    Optional<String> uuid = getFileUUid(file);
    if (!uuid.isPresent()) {
      return Optional.empty();
    }

    LOGGER.trace("Reading SCM info from db for file '{}'", uuid.get());
    try (DbSession dbSession = dbClient.openSession(false)) {
      FileSourceDto dto = dbClient.fileSourceDao().selectSourceByFileUuid(dbSession, uuid.get());
      if (dto == null) {
        return Optional.empty();
      }
      return DbScmInfo.create(dto.getSourceData().getLinesList(), dto.getSrcHash());
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

}
