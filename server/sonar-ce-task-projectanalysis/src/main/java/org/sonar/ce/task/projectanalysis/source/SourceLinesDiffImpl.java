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
package org.sonar.ce.task.projectanalysis.source;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.period.NewCodeReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.component.ReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.source.FileSourceDao;

public class SourceLinesDiffImpl implements SourceLinesDiff {

  private final DbClient dbClient;
  private final FileSourceDao fileSourceDao;
  private final SourceLinesHashRepository sourceLinesHash;
  private final ReferenceBranchComponentUuids referenceBranchComponentUuids;
  private final MovedFilesRepository movedFilesRepository;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PeriodHolder periodHolder;
  private final NewCodeReferenceBranchComponentUuids newCodeReferenceBranchComponentUuids;

  public SourceLinesDiffImpl(DbClient dbClient, FileSourceDao fileSourceDao, SourceLinesHashRepository sourceLinesHash, ReferenceBranchComponentUuids referenceBranchComponentUuids,
    MovedFilesRepository movedFilesRepository, AnalysisMetadataHolder analysisMetadataHolder, PeriodHolder periodHolder,
    NewCodeReferenceBranchComponentUuids newCodeReferenceBranchComponentUuids) {
    this.dbClient = dbClient;
    this.fileSourceDao = fileSourceDao;
    this.sourceLinesHash = sourceLinesHash;
    this.referenceBranchComponentUuids = referenceBranchComponentUuids;
    this.movedFilesRepository = movedFilesRepository;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.periodHolder = periodHolder;
    this.newCodeReferenceBranchComponentUuids = newCodeReferenceBranchComponentUuids;
  }

  @Override
  public int[] computeMatchingLines(Component component) {
    List<String> database = getDBLines(component);
    List<String> report = getReportLines(component);

    return new SourceLinesDiffFinder().findMatchingLines(database, report);
  }

  private List<String> getDBLines(Component component) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String uuid;
      if (analysisMetadataHolder.isPullRequest()) {
        uuid = referenceBranchComponentUuids.getComponentUuid(component.getKey());
      } else if (periodHolder.hasPeriod() && periodHolder.getPeriod().getMode().equals(NewCodePeriodType.REFERENCE_BRANCH.name())) {
        uuid = newCodeReferenceBranchComponentUuids.getComponentUuid(component.getKey());
      } else {
        Optional<MovedFilesRepository.OriginalFile> originalFile = movedFilesRepository.getOriginalFile(component);
        uuid = originalFile.map(MovedFilesRepository.OriginalFile::uuid).orElse(component.getUuid());
      }

      if (uuid == null) {
        return Collections.emptyList();
      }

      List<String> database = fileSourceDao.selectLineHashes(dbSession, uuid);
      if (database == null) {
        return Collections.emptyList();
      }
      return database;
    }
  }

  private List<String> getReportLines(Component component) {
    return sourceLinesHash.getLineHashesMatchingDBVersion(component);
  }

}
