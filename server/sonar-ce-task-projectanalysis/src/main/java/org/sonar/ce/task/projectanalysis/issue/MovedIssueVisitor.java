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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Date;
import java.util.Optional;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository.OriginalFile;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.issue.IssueFieldsSetter;

import static com.google.common.base.Preconditions.checkState;

public class MovedIssueVisitor extends IssueVisitor {
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MovedFilesRepository movedFilesRepository;
  private final IssueFieldsSetter issueUpdater;

  public MovedIssueVisitor(AnalysisMetadataHolder analysisMetadataHolder, MovedFilesRepository movedFilesRepository, IssueFieldsSetter issueUpdater) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.movedFilesRepository = movedFilesRepository;
    this.issueUpdater = issueUpdater;
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (component.getType() != Component.Type.FILE || component.getUuid().equals(issue.componentUuid())) {
      return;
    }
    Optional<OriginalFile> originalFileOptional = movedFilesRepository.getOriginalFile(component);
    checkState(originalFileOptional.isPresent(),
      "Issue %s for component %s has a different component key but no original file exist in MovedFilesRepository",
      issue, component);
    OriginalFile originalFile = originalFileOptional.get();
    String fileUuid = originalFile.uuid();
    checkState(fileUuid.equals(issue.componentUuid()),
      "Issue %s doesn't belong to file %s registered as original file of current file %s",
      issue, fileUuid, component);

    // changes the issue's component uuid, and set issue as changed, to enforce it is persisted to DB
    issueUpdater.setIssueComponent(issue, component.getUuid(), component.getKey(), new Date(analysisMetadataHolder.getAnalysisDate()));
  }
}
