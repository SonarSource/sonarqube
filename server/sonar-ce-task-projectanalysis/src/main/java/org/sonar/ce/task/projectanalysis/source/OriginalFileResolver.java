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

import java.util.Optional;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.ce.task.projectanalysis.period.NewCodeReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;

import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;

/**
 * Business logic to retrieve the uuid of a file in other branches/analysis.
 * It supports different scenarios depending on if the file exists in the reference branch or in the new code period, or if the file was moved since the last analysis.
 */
public class OriginalFileResolver {
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MovedFilesRepository movedFilesRepository;
  private final ReferenceBranchComponentUuids referenceBranchComponentUuids;
  private final NewCodeReferenceBranchComponentUuids newCodeReferenceBranchComponentUuids;
  private final PeriodHolder periodHolder;

  public OriginalFileResolver(AnalysisMetadataHolder analysisMetadataHolder,
    MovedFilesRepository movedFilesRepository,
    ReferenceBranchComponentUuids referenceBranchComponentUuids,
    NewCodeReferenceBranchComponentUuids newCodeReferenceBranchComponentUuids,
    PeriodHolder periodHolder) {

    this.analysisMetadataHolder = analysisMetadataHolder;
    this.movedFilesRepository = movedFilesRepository;
    this.referenceBranchComponentUuids = referenceBranchComponentUuids;
    this.newCodeReferenceBranchComponentUuids = newCodeReferenceBranchComponentUuids;
    this.periodHolder = periodHolder;
  }

  public Optional<String> getFileUuid(Component file) {

    if (analysisMetadataHolder.isPullRequest()) {
      return Optional.ofNullable(referenceBranchComponentUuids.getComponentUuid(file.getKey()));
    }

    if (analysisMetadataHolder.isFirstAnalysis()) {

      if (isNewCodePeriodReferenceBranch()) {

        String componentUuidFromNewCodeReference = newCodeReferenceBranchComponentUuids.getComponentUuid(file.getKey());
        if (componentUuidFromNewCodeReference != null) {
          return Optional.of(componentUuidFromNewCodeReference);
        }

      }

      if (!analysisMetadataHolder.getBranch().isMain()) {
        String componentUuidFromReferenceBranch = referenceBranchComponentUuids.getComponentUuid(file.getKey());
        if (componentUuidFromReferenceBranch != null) {
          return Optional.of(componentUuidFromReferenceBranch);
        }
      }

    }

    return getOrignalFileIfMoved(file);

  }

  private boolean isNewCodePeriodReferenceBranch() {
    return periodHolder.hasPeriod() && REFERENCE_BRANCH.name().equals(periodHolder.getPeriod().getMode());
  }

  private Optional<String> getOrignalFileIfMoved(Component file) {
    return movedFilesRepository.getOriginalFile(file)
      .map(MovedFilesRepository.OriginalFile::uuid)
      .or(() -> Optional.ofNullable(file.getUuid()));
  }
}
