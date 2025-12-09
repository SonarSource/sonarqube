/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.ce.task.projectanalysis.component.BranchComponentUuidsDelegate;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;

/**
 * Business logic to retrieve the uuid of a file in other branches/analysis.
 * It supports different scenarios depending on if the file exists in the reference branch or in the new code period, or if the file was moved since the last analysis.
 */
public class OriginalFileResolver {
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MovedFilesRepository movedFilesRepository;
  private final BranchComponentUuidsDelegate branchComponentUuidsDelegate;

  public OriginalFileResolver(AnalysisMetadataHolder analysisMetadataHolder,
    MovedFilesRepository movedFilesRepository,
    BranchComponentUuidsDelegate branchComponentUuidsDelegate) {

    this.analysisMetadataHolder = analysisMetadataHolder;
    this.movedFilesRepository = movedFilesRepository;
    this.branchComponentUuidsDelegate = branchComponentUuidsDelegate;
  }

  public Optional<String> getFileUuid(Component file) {
    String componentUuid = branchComponentUuidsDelegate.getComponentUuid(file.getKey());
    if (analysisMetadataHolder.isPullRequest()) {
      return Optional.ofNullable(componentUuid);
    }
    if (componentUuid != null && analysisMetadataHolder.isFirstAnalysis()) {
      return Optional.of(componentUuid);
    }
    return getOriginalFileIfMoved(file);
  }

  private Optional<String> getOriginalFileIfMoved(Component file) {
    return movedFilesRepository.getOriginalFile(file)
      .map(MovedFilesRepository.OriginalFile::uuid)
      .or(() -> Optional.ofNullable(file.getUuid()));
  }
}
