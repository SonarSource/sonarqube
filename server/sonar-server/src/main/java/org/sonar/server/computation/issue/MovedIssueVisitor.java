/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.issue;

import com.google.common.base.Optional;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.filemove.MovedFilesRepository;
import org.sonar.server.computation.filemove.MovedFilesRepository.OriginalFile;

import static com.google.common.base.Preconditions.checkState;

public class MovedIssueVisitor extends IssueVisitor {
  private final MovedFilesRepository movedFilesRepository;

  public MovedIssueVisitor(MovedFilesRepository movedFilesRepository) {
    this.movedFilesRepository = movedFilesRepository;
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

    // it's enough to change component uuid, only this field is written to table ISSUES
    // other fields (such as module, modulePath, componentKey) are read-only and result of a join with other tables
    issue.setComponentUuid(component.getUuid());
    issue.setComponentKey(component.getKey());
    issue.setModuleUuid(null);
    issue.setModuleUuidPath(null);

    // ensure issue is updated in DB
    issue.setChanged(true);
  }
}
