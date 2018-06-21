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

import com.google.common.base.Optional;
import java.util.Date;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.server.computation.task.projectanalysis.filemove.MovedFilesRepository.OriginalFile;
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
    checkState(originalFile.getUuid().equals(issue.componentUuid()),
      "Issue %s doesn't belong to file %s registered as original file of current file %s",
      issue, originalFile.getUuid(), component);

    // changes the issue's component uuid, add a change and set issue as changed to enforce it is persisted to DB
    issueUpdater.setIssueMoved(issue, component.getUuid(), IssueChangeContext.createUser(new Date(analysisMetadataHolder.getAnalysisDate()), null));
    // other fields (such as module, modulePath, componentKey) are read-only and set/reset for consistency only
    issue.setComponentKey(component.getPublicKey());
    issue.setModuleUuid(null);
    issue.setModuleUuidPath(null);
  }
}
